#!/usr/bin/env bash
# =============================================================
# WMS 一键回滚（在 ECS 上执行）
#
# 放置位置：/opt/wms/scripts/rollback.sh
# 用法：bash /opt/wms/scripts/rollback.sh
#
# 前提：每次 deploy.sh 部署时都会把当前版本快照到 app/releases/，
#      所以这里只要取最近一份快照覆盖回去、重启、冒烟即可。
#      做过的版本保留最近 3 份（deploy.sh 里自动淘汰）。
# =============================================================
set -uo pipefail

cd /opt/wms
RELEASES_DIR=/opt/wms/app/releases

PREV=$(ls -t "$RELEASES_DIR"/*.jar 2>/dev/null | head -1)
if [ -z "$PREV" ]; then
  echo "[错误] $RELEASES_DIR 里没有可用版本，无法回滚"
  echo "       第一次部署前是没有历史版本的，属于正常情况"
  exit 1
fi

echo "[1/4] 回滚目标：$PREV"
cp -f "$PREV" app/wms-server.jar

echo "[2/4] 重启容器"
docker compose up -d

echo "[3/4] 等待启动（Spring Boot 约 20-40 秒）"
sleep 20

echo "[4/4] 冒烟测试"
CODE=000
for i in $(seq 1 12); do
  CODE=$(curl -s -o /dev/null -w "%{http_code}" -m 5 http://127.0.0.1/ || echo 000)
  [ "$CODE" = "200" ] && break
  echo "  第 ${i} 次探测：HTTP $CODE，继续等待..."
  sleep 10
done

if [ "$CODE" = "200" ]; then
  # 顺手打印一下业务健康状态（actuator 没启用的话这里会空，不影响结果）
  HEALTH=$(curl -s -m 5 http://127.0.0.1/actuator/health 2>/dev/null || true)
  echo
  echo "回滚完成，首页 HTTP 200"
  [ -n "$HEALTH" ] && echo "业务健康状态：$HEALTH"
  echo "请再手工点几个页面确认功能正常"
else
  echo
  echo "[错误] 回滚后首页仍不可访问，立即查看日志："
  echo "       cd /opt/wms && docker compose logs --tail=100 wms-server"
  exit 1
fi
