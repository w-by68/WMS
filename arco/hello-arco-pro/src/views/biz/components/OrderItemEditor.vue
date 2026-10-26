<!--
  单据明细编辑器（入库单 / 出库单共用）：
  - 可增删行的表格；商品用本地搜索下拉（数据来自 /api/products/options，后端只放开了 options 接口）；
  - 库位下拉由父组件按所选仓库联动传入；
  - mode='outbound' 时，选完商品+库位会实时查该库位可用库存并显示，超量标红。
  行数据直接是父组件的 items 数组元素（对象引用共享），字段改动不需要额外事件同步。
-->
<template>
  <div class="order-items">
    <div class="order-items-head">
      <span class="order-items-title">明细</span>
      <a-button
        type="primary"
        size="small"
        :disabled="disabled"
        @click="addRow"
      >
        <template #icon><icon-plus /></template>
        添加一行
      </a-button>
    </div>

    <a-table
      :data="rows"
      :pagination="false"
      size="small"
      :scroll="{ x: 1080 }"
      row-key="rowKey"
    >
      <template #columns>
        <a-table-column title="商品" :width="260">
          <template #cell="{ record }">
            <a-select
              v-model="record.productId"
              placeholder="输入 SKU / 名称搜索"
              allow-search
              allow-clear
              :disabled="disabled"
              style="width: 100%"
              @change="onProductChange(record)"
            >
              <a-option
                v-for="p in products"
                :key="p.id"
                :value="p.id"
                :label="`${p.sku} ${p.name}`"
              />
            </a-select>
          </template>
        </a-table-column>

        <a-table-column title="库位" :width="200">
          <template #cell="{ record }">
            <a-select
              v-model="record.locationId"
              :placeholder="locations.length ? '请选择库位' : '请先选择仓库'"
              allow-search
              allow-clear
              :disabled="disabled || !locations.length"
              style="width: 100%"
              @change="onLocationChange(record)"
            >
              <a-option
                v-for="loc in locations"
                :key="loc.id"
                :value="loc.id"
                :label="loc.code"
              />
            </a-select>
          </template>
        </a-table-column>

        <a-table-column title="数量" :width="140">
          <template #cell="{ record }">
            <a-input-number
              v-model="record.quantity"
              :min="1"
              :precision="0"
              :error="isOverStock(record)"
              :disabled="disabled"
              style="width: 100%"
            />
          </template>
        </a-table-column>

        <a-table-column title="单价（元）" :width="140">
          <template #cell="{ record }">
            <a-input-number
              v-model="record.price"
              :min="0"
              :precision="2"
              :disabled="disabled"
              style="width: 100%"
            />
          </template>
        </a-table-column>

        <a-table-column v-if="mode === 'outbound'" title="可用库存" :width="130">
          <template #cell="{ record }">
            <span :class="{ 'stock-lack': isOverStock(record) }">
              {{ availableText(record) }}
            </span>
          </template>
        </a-table-column>

        <a-table-column title="金额（元）" :width="120">
          <template #cell="{ record }">{{ lineAmount(record) }}</template>
        </a-table-column>

        <a-table-column title="操作" :width="80" fixed="right">
          <template #cell="{ rowIndex }">
            <a-button
              type="text"
              size="small"
              status="danger"
              :disabled="disabled"
              @click="removeRow(rowIndex)"
            >
              删除
            </a-button>
          </template>
        </a-table-column>
      </template>
    </a-table>

    <div class="order-items-total">
      合计数量：<b>{{ totalQuantity }}</b>，合计金额：<b>{{ totalAmount }}</b> 元
    </div>
  </div>
</template>

<script lang="ts" setup>
  import { computed } from 'vue';
  import { getInventoryByProductLocation } from '@/api/inventory';
  import type { ProductItem } from '@/api/product';
  import type { LocationItem } from '@/api/location';

  // 行数据是动态增删的表格行，用 any[] 与 arco a-table 的 TableData 兼容
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  type Row = any;

  const props = defineProps<{
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    modelValue: any[];
    products: ProductItem[];
    locations: LocationItem[];
    /** inbound 走入库（价格取成本价），outbound 走出库（价格取售价 + 校验可用库存） */
    mode: 'inbound' | 'outbound';
    disabled?: boolean;
  }>();

  const emit = defineEmits<{
    (e: 'update:modelValue', rows: Row[]): void;
  }>();

  const rows = computed(() => props.modelValue);
  const disabled = computed(() => props.disabled === true);

  let seed = 0;
  const addRow = () => {
    seed += 1;
    emit('update:modelValue', [
      ...props.modelValue,
      { rowKey: `new-${seed}`, quantity: 1, price: 0 },
    ]);
  };

  const removeRow = (index: number) => {
    const next = [...props.modelValue];
    next.splice(index, 1);
    emit('update:modelValue', next);
  };

  /** 选完商品：自动带出单价，并在出库时查可用库存 */
  const onProductChange = (record: Row) => {
    const product = props.products.find((p) => p.id === record.productId);
    if (product) {
      record.price =
        props.mode === 'inbound'
          ? product.costPrice ?? 0
          : product.salePrice ?? 0;
    }
    refreshAvailable(record);
  };

  const onLocationChange = (record: Row) => {
    refreshAvailable(record);
  };

  /** 出库：查该商品在该库位的可用库存 */
  const refreshAvailable = async (record: Row) => {
    if (props.mode !== 'outbound') return;
    if (!record.productId || !record.locationId) {
      record.availableStock = undefined;
      return;
    }
    try {
      const inv = await getInventoryByProductLocation(
        record.productId,
        record.locationId
      );
      record.availableStock = inv ? inv.availableQuantity : 0;
    } catch {
      record.availableStock = undefined;
    }
  };

  /** 出库且数量超过可用库存 */
  const isOverStock = (record: Row) =>
    props.mode === 'outbound' &&
    record.availableStock != null &&
    Number(record.quantity || 0) > Number(record.availableStock);

  const availableText = (record: Row) => {
    if (record.availableStock == null) return '-';
    return isOverStock(record)
      ? `仅剩 ${record.availableStock}`
      : `${record.availableStock}`;
  };

  const lineAmount = (record: Row) =>
    (Number(record.quantity || 0) * Number(record.price || 0)).toFixed(2);

  const totalQuantity = computed(() =>
    props.modelValue.reduce((sum, r) => sum + Number(r.quantity || 0), 0)
  );

  const totalAmount = computed(() =>
    props.modelValue
      .reduce(
        (sum, r) => sum + Number(r.quantity || 0) * Number(r.price || 0),
        0
      )
      .toFixed(2)
  );
</script>

<style scoped lang="less">
  .order-items-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 8px;
  }

  .order-items-title {
    font-weight: 600;
  }

  .order-items-total {
    margin-top: 12px;
    text-align: right;
  }

  .stock-lack {
    color: rgb(var(--danger-6));
    font-weight: 600;
  }
</style>
