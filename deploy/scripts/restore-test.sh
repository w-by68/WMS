#!/usr/bin/env bash
# =============================================================
# WMS 备份恢复演练（在 ECS 上执行）
#
# 放置位置：/opt/wms/scripts/restore-test.sh
# 用法：bash /opt/wms/scripts/restore-test.sh
#
# 目的：证明「备份是真的能恢复的」。
#      没做恢复演练的备份等于没有备份——这是运维岗面试里很值钱的一句话。
#
# 安全设计：恢复到临时库 wms_restore_test，验证完立即删除，
#          绝不会碰到生产库 wms_db。
# =============================================================
set -euo pipefail

TEST_DB=wms_restore_test
LATEST=$(ls -t /opt/wms/backup/*.sql.gz 2>/dev/null | head -1)

if [ -z "$LATEST" ]; then
  echo "[错误] /opt/wms/backup 下没有备份文件，先跑一次 backup.sh"
  exit 1
fi

echo "[1/4] 演练对象：$LATEST"

echo "[2/4] 建临时库并恢复"
mysql -e "DROP DATABASE IF EXISTS $TEST_DB; CREATE DATABASE $TEST_DB DEFAULT CHARACTER SET utf8mb4;"
gunzip < "$LATEST" | mysql --default-character-set=utf8mb4 "$TEST_DB"

echo "[3/4] 校验数据"
mysql -e "SELECT COUNT(*) AS 表数量 FROM information_schema.tables WHERE table_schema='$TEST_DB';"
mysql "$TEST_DB" -e "
  SELECT COUNT(*) AS 用户数   FROM sys_user;
  SELECT COUNT(*) AS 商品数   FROM product;
  SELECT COUNT(*) AS 库存记录 FROM inventory;
  SELECT COUNT(*) AS 入库单   FROM inbound_order;
"

echo "[4/4] 清理临时库"
mysql -e "DROP DATABASE $TEST_DB;"

echo
echo "恢复演练完成：$LATEST 可正常恢复"
echo "请核对上面的行数是否为 0——如果全为 0，说明 dump 有问题，要立刻查"
