#!/usr/bin/env bash
# =============================================================
# WMS 数据库定时备份（在 ECS 上执行）
#
# 放置位置：/opt/wms/scripts/backup.sh
# 用法：bash /opt/wms/scripts/backup.sh
#
# 前置条件（必做一次，否则会报 Access denied）：
#   1. 在 ECS 上装 mysql 客户端：apt update && apt install -y mysql-client
#   2. 建一个免明文密码的配置，避免密码出现在命令行和 ps 里：
#        cat > ~/.my.cnf <<'EOF'
#        [client]
#        host=<RDS内网地址>
#        user=<账号>
#        password=<密码>
#        EOF
#        chmod 600 ~/.my.cnf
#
# 设计说明（面试可以讲的点）：
#   --single-transaction  InnoDB 一致性快照，不锁表，备份时业务照常写入
#   gzip -t               备份完立刻校验文件完整性，避免拿到被截断的坏文件
#   find -mtime +7        滚动保留 7 天，防止备份自己把磁盘撑爆
# =============================================================
set -euo pipefail

DIR=/opt/wms/backup
TS=$(date +%F_%H%M)
mkdir -p "$DIR"
FILE="$DIR/wms_$TS.sql.gz"

# 备份（stderr 不吞，方便在 cron 日志里看到真实报错）
mysqldump \
  --single-transaction \
  --routines \
  --triggers \
  --set-gtid-purged=OFF \
  --default-character-set=utf8mb4 \
  wms_db | gzip > "$FILE"

# 完整性校验：磁盘满 / 网络断会产生截断文件，不校验等于没有备份
if ! gzip -t "$FILE"; then
  echo "[错误] 备份文件不完整，已删除：$FILE" >&2
  rm -f "$FILE"
  exit 1
fi

# 只保留 7 天
find "$DIR" -name '*.sql.gz' -mtime +7 -delete

echo "$(date '+%F %T') 备份成功 $(du -h "$FILE" | cut -f1) $FILE"
