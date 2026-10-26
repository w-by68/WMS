#!/usr/bin/env bash
# =============================================================
# WMS 服务巡检 + 钉钉告警（在 ECS 上执行）
#
# 放置位置：/opt/wms/scripts/check.sh
# 用法：bash /opt/wms/scripts/check.sh
#
# 配置：把下面 WEBHOOK 换成你自己的钉钉机器人地址
#       （钉钉群 → 群设置 → 智能群助手 → 添加机器人 → 自定义 → 复制 Webhook）
#       安全设置里【选「自定义关键词」，填 WMS】最省事；
#       选「加签」的话要额外算签名，没必要给自己找麻烦。
#
# 告警去重：用一个状态文件记录「当前是否处于告警中」，
#           异常时只推一次，恢复正常后再出问题才会推第二次。
#           否则服务挂一晚上，你的手机会被 100 多条消息炸醒。
# =============================================================
set -uo pipefail

WEBHOOK="https://oapi.dingtalk.com/robot/send?access_token=这里换成你的TOKEN"
STATE=/tmp/wms_check_alerting
HOST_INFO=$(hostname)
MSG=""

# ---------- 1. 首页可用性 ----------
CODE=$(curl -s -o /dev/null -w "%{http_code}" -m 8 http://127.0.0.1/ || echo 000)
[ "$CODE" = "200" ] || MSG="${MSG}\n[首页异常] HTTP $CODE"

# ---------- 2. 容器是否还活着 ----------
for c in wms-server wms-web; do
  docker inspect -f '{{.State.Running}}' "$c" 2>/dev/null | grep -q true \
    || MSG="${MSG}\n[容器已停止] $c"
done

# ---------- 3. 容器健康状态（docker compose ps 里的 healthy/unhealthy）----------
for c in wms-server wms-web; do
  H=$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{end}}' "$c" 2>/dev/null)
  [ -z "$H" ] && continue
  [ "$H" = "healthy" ] || MSG="${MSG}\n[容器亚健康] $c 状态 $H"
done

# ---------- 4. 磁盘水位 ----------
DISK=$(df / | awk 'NR==2{print $5}' | tr -d '%')
[ "$DISK" -gt 85 ] && MSG="${MSG}\n[磁盘告警] 已用 ${DISK}%"

# ---------- 5. 内存水位 ----------
MEM=$(free | awk '/Mem:/{printf "%d", $3/$2*100}')
[ "$MEM" -gt 90 ] && MSG="${MSG}\n[内存告警] 已用 ${MEM}%"

# ---------- 6. 数据库连通性 ----------
mysql -e "SELECT 1" >/dev/null 2>&1 || MSG="${MSG}\n[数据库不可达] RDS 连接失败"

# ---------- 7. 备份文件是否新鲜（超过 26 小时没新备份就是备份任务挂了）----------
LATEST_BK=$(ls -t /opt/wms/backup/*.sql.gz 2>/dev/null | head -1)
if [ -z "$LATEST_BK" ]; then
  MSG="${MSG}\n[备份缺失] 从未产生过备份文件"
else
  AGE=$(( ($(date +%s) - $(stat -c %Y "$LATEST_BK")) / 3600 ))
  [ "$AGE" -gt 26 ] && MSG="${MSG}\n[备份过期] 最新备份已是 ${AGE} 小时前"
fi

# ---------- 告警推送（带去重）----------
if [ -n "$MSG" ]; then
  if [ ! -f "$STATE" ]; then
    # 注意：JSON 里的换行必须是字面 \n，不能用真实换行，否则钉钉报解析错误
    # 所以 MSG 累积时用的是 "\n" 两个字符，这里直接拼进 JSON 即可
    TEXT="WMS 巡检告警\n主机：$HOST_INFO\n时间：$(date '+%F %T')$MSG"
    curl -s -X POST "$WEBHOOK" \
      -H 'Content-Type: application/json' \
      -d "$(printf '{"msgtype":"text","text":{"content":"%s"}}' "$TEXT")" >/dev/null
    echo -e "$(date '+%F %T') 已推送告警$MSG"
    touch "$STATE"
  fi
else
  rm -f "$STATE"
fi
