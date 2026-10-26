#!/usr/bin/env bash
# =============================================================
# WMS 服务器初始化脚本（在 ECS 上执行，只需跑一次）
# 适用环境：Ubuntu 22.04 / 2核4G
#
# 用法：
#   bash server-init.sh
#   bash server-init.sh https://xxxx.mirror.aliyuncs.com    # 带镜像加速地址（推荐）
#
# 如果执行时报 $'\r': command not found，说明文件是 Windows 换行符，先执行：
#   sed -i 's/\r$//' server-init.sh
# =============================================================
set -euo pipefail

MIRROR="${1:-}"
DEPLOY_DIR=/opt/wms

log() { echo -e "\n\033[32m>>> $*\033[0m"; }

if [ "$(id -u)" -ne 0 ]; then
  echo "请在 root 下执行（先 sudo -i，或 sudo bash server-init.sh）"
  exit 1
fi

log "1/7 更新软件包索引"
export DEBIAN_FRONTEND=noninteractive
apt-get update -y

log "2/7 安装基础工具"
apt-get install -y curl wget vim unzip ca-certificates gnupg

log "3/7 设置时区为 Asia/Shanghai"
timedatectl set-timezone Asia/Shanghai 2>/dev/null || ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime
date

log "4/7 检查 swap（4G 内存的机器建议加 2G，防止内存峰值被系统 OOM 杀掉）"
if [ "$(free -m | awk '/^Swap:/ {print $2}')" -eq 0 ]; then
  fallocate -l 2G /swapfile 2>/dev/null || dd if=/dev/zero of=/swapfile bs=1M count=2048
  chmod 600 /swapfile
  mkswap /swapfile >/dev/null
  swapon /swapfile
  grep -q '/swapfile' /etc/fstab || echo '/swapfile none swap sw 0 0' >> /etc/fstab
  echo "已创建 2G swap"
else
  echo "已存在 swap，跳过"
fi

log "5/7 安装 Docker"
if command -v docker >/dev/null 2>&1; then
  echo "Docker 已安装：$(docker --version)，跳过"
else
  # 注意：国内 ECS 直连 Docker Hub 经常超时，必须配镜像加速器，见第 6 步
  curl -fsSL https://get.docker.com | bash -s docker --mirror Aliyun
fi

log "6/7 配置 Docker（镜像加速 + 日志轮转，防日志写满磁盘）"
mkdir -p /etc/docker
if [ -n "$MIRROR" ]; then
  cat > /etc/docker/daemon.json <<EOF
{
  "registry-mirrors": ["$MIRROR"],
  "log-driver": "json-file",
  "log-opts": { "max-size": "10m", "max-file": "3" }
}
EOF
  echo "已写入镜像加速地址：$MIRROR"
else
  cat > /etc/docker/daemon.json <<'EOF'
{
  "log-driver": "json-file",
  "log-opts": { "max-size": "10m", "max-file": "3" }
}
EOF
  echo "未提供镜像加速地址 —— 若之后 docker pull 卡住/超时，"
  echo "请去阿里云控制台【容器镜像服务 ACR → 镜像工具 → 镜像加速器】拿专属地址后重跑本脚本。"
fi

systemctl daemon-reload
systemctl enable docker >/dev/null 2>&1
systemctl restart docker

log "7/7 创建部署目录 $DEPLOY_DIR"
mkdir -p "$DEPLOY_DIR"/app "$DEPLOY_DIR"/nginx "$DEPLOY_DIR"/logs
chmod 755 "$DEPLOY_DIR"

log "初始化完成"
echo "Docker ：$(docker --version)"
echo "Compose：$(docker compose version 2>/dev/null || echo '未检测到 compose 插件')"
echo
echo "下一步：把产物上传到 $DEPLOY_DIR/ 下（app/、nginx/、docker-compose.yml、.env）"
