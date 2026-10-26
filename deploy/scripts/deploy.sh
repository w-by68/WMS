#!/usr/bin/env bash
# =============================================================
# WMS 本地一键部署脚本（在你的 Windows 电脑上执行）
#
# 作用：构建前端和后端 → 上传到 ECS → 重启容器 → 冒烟测试
#
# 用法：
#   bash scripts/deploy.sh <ECS公网IP>
#
# 前置条件：
#   1. 本地已装 JDK 17、Maven、Node.js
#   2. 已经能在终端免密登录服务器（配了 SSH 密钥），否则每次都要输密码
#      配置方法：
#        ssh-keygen -t ed25519 -C "wms-deploy"        # 一路回车
#        ssh-copy-id root@<ECS公网IP>                  # Windows 没有 ssh-copy-id 的话见文档
#   3. 服务器上已跑过 server-init.sh，且 .env 已填好
#
# 报 $'\r': command not found 的话先执行：sed -i 's/\r$//' scripts/deploy.sh
# =============================================================
set -euo pipefail

SERVER_IP="${1:-}"
SSH_USER="${2:-root}"
REMOTE_DIR=/opt/wms

if [ -z "$SERVER_IP" ]; then
  echo "用法：bash scripts/deploy.sh <ECS公网IP>"
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_DIR="$(dirname "$SCRIPT_DIR")"
PROJECT_ROOT="$(dirname "$DEPLOY_DIR")"
BACKEND_DIR="$PROJECT_ROOT/springBoot"
FRONTEND_DIR="$PROJECT_ROOT/arco/hello-arco-pro"
REMOTE="$SSH_USER@$SERVER_IP"

step() { echo -e "\n\033[36m======== $* ========\033[0m"; }
die()  { echo -e "\033[31m[错误] $*\033[0m"; exit 1; }

# ---------- 0. 前置检查 ----------
step "0/6 检查本地环境与服务器连通性"
command -v mvn  >/dev/null 2>&1 || die "没找到 mvn，请先装 Maven 并配好 PATH"
command -v npm  >/dev/null 2>&1 || die "没找到 npm，请先装 Node.js"
command -v ssh  >/dev/null 2>&1 || die "没找到 ssh 客户端"

ssh -o ConnectTimeout=8 -o BatchMode=yes "$REMOTE" "echo ok" >/dev/null 2>&1 \
  || die "无法免密连接 $REMOTE。
      请先配置 SSH 密钥：
        ssh-keygen -t ed25519
        ssh root@$SERVER_IP 'mkdir -p ~/.ssh && chmod 700 ~/.ssh'
        cat ~/.ssh/id_ed25519.pub | ssh root@$SERVER_IP 'cat >> ~/.ssh/authorized_keys && chmod 600 ~/.ssh/authorized_keys'"

# 服务器上必须已经有 .env，否则首次部署会失败（避免脚本用错误的默认值直接跑起来）
ssh "$REMOTE" "[ -f $REMOTE_DIR/.env ]" \
  || die "服务器上 $REMOTE_DIR/.env 不存在。
      请先登录服务器：cp $REMOTE_DIR/.env.example $REMOTE_DIR/.env && vim $REMOTE_DIR/.env（填 RDS 地址和密码、JWT 密钥）"
echo "服务器连接正常，.env 已就位"

# ---------- 1. 同步生产配置到后端 resources ----------
step "1/6 同步生产配置到后端"
cp "$DEPLOY_DIR/backend/application-prod.yml" "$BACKEND_DIR/src/main/resources/application-prod.yml"
echo "已复制 application-prod.yml 到 src/main/resources/"

# ---------- 2. 构建后端 ----------
step "2/6 构建后端（mvn package）"
cd "$BACKEND_DIR"
# -DskipTests：部署流水线先跳过单测保证速度，空出时间再单独跑
mvn -q clean package -DskipTests
JAR_PATH="$BACKEND_DIR/target/wms-server.jar"
[ -f "$JAR_PATH" ] || die "构建产物不存在：$JAR_PATH"
echo "后端构建完成：$(du -h "$JAR_PATH" | cut -f1)"

# ---------- 3. 构建前端 ----------
step "3/6 构建前端（npm run build）"
cd "$FRONTEND_DIR"
npm run build
DIST_DIR="$FRONTEND_DIR/dist"
[ -f "$DIST_DIR/index.html" ] || die "前端产物不存在：$DIST_DIR/index.html"
echo "前端构建完成：$(du -sh "$DIST_DIR" | cut -f1)"

# ---------- 4. 上传 ----------
step "4/6 上传产物到 $REMOTE_DIR"

# 快照当前正在跑的版本（第一次部署时还没有，报错忽略即可）
# 保留最近 3 份：40G 磁盘经不起无限存 jar
ssh "$REMOTE" "mkdir -p $REMOTE_DIR/app/releases $REMOTE_DIR/scripts && \
  cd $REMOTE_DIR/app && \
  (cp -f wms-server.jar releases/wms-server.\$(date +%Y%m%d_%H%M%S).jar 2>/dev/null || true); \
  ls -t releases/*.jar 2>/dev/null | tail -n +4 | xargs -r rm -f"
echo "√ 旧版本已快照"

# 后端 jar
scp -q "$JAR_PATH" "$REMOTE:$REMOTE_DIR/app/wms-server.jar"
echo "√ 后端 jar"

# 前端 dist：先本地打包再传，比逐个文件传快得多（几百个小文件逐个 scp 会非常慢）
TMP_TAR="$(mktemp -t wms-dist-XXXXXX.tar.gz)"
tar -czf "$TMP_TAR" -C "$DIST_DIR" .
scp -q "$TMP_TAR" "$REMOTE:$REMOTE_DIR/app/dist.tar.gz"
ssh "$REMOTE" "cd $REMOTE_DIR/app && rm -rf dist && mkdir -p dist && tar -xzf dist.tar.gz -C dist && rm -f dist.tar.gz"
rm -f "$TMP_TAR"
echo "√ 前端 dist"

# 编排与配置（每次同步，保证服务器上永远是最新的配置）
scp -q "$DEPLOY_DIR/docker-compose.yml" "$REMOTE:$REMOTE_DIR/docker-compose.yml"
scp -q "$DEPLOY_DIR/nginx/wms.conf"     "$REMOTE:$REMOTE_DIR/nginx/wms.conf"
scp -q "$DEPLOY_DIR/.env.example"       "$REMOTE:$REMOTE_DIR/.env.example"
echo "√ docker-compose.yml / nginx 配置"

# 运维脚本（备份 / 回滚 / 巡检 / 恢复演练），每次部署顺手同步成最新版
for f in rollback.sh backup.sh restore-test.sh check.sh; do
  scp -q "$SCRIPT_DIR/$f" "$REMOTE:$REMOTE_DIR/scripts/$f"
done
echo "√ 运维脚本"

# ---------- 5. 重启服务 ----------
step "5/6 重启容器"
ssh "$REMOTE" "cd $REMOTE_DIR && docker compose up -d && sleep 3 && docker compose ps"

# ---------- 6. 冒烟测试 ----------
step "6/6 冒烟测试"
echo "等待后端启动（Spring Boot 约需 20-40 秒）..."
for i in $(seq 1 20); do
  CODE=$(curl -s -o /dev/null -w "%{http_code}" -m 5 "http://$SERVER_IP/" || echo 000)
  if [ "$CODE" = "200" ]; then break; fi
  echo "  第 ${i} 次探测：HTTP $CODE，继续等待..."
  sleep 5
done

FRONT_CODE=$(curl -s -o /dev/null -w "%{http_code}" -m 5 "http://$SERVER_IP/" || echo 000)
# /api 下随便打一个不存在的路径：能返回 JSON（而不是 502）就说明 Nginx 已经成功转发到后端
API_BODY=$(curl -s -m 10 "http://$SERVER_IP/api/auth/me" | head -c 200 || true)

echo
echo "前端页面  HTTP $FRONT_CODE"
echo "后端接口  响应：$API_BODY"
echo
if [ "$FRONT_CODE" = "200" ]; then
  echo -e "\033[32m部署成功 → http://$SERVER_IP/\033[0m"
  echo "若接口不通，登录服务器看后端日志：cd $REMOTE_DIR && docker compose logs -f wms-server"
else
  echo -e "\033[31m前端未返回 200，请检查：cd $REMOTE_DIR && docker compose logs\033[0m"
  echo -e "\033[33m要回退到上一版本，登录服务器执行：bash $REMOTE_DIR/scripts/rollback.sh\033[0m"
fi
echo "出问题时回滚：ssh $REMOTE 'bash /opt/wms/scripts/rollback.sh'"
