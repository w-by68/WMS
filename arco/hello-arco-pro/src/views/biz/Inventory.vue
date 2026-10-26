<!--
  库存查询（只读，管理员与普通用户共用）：
  - 分页展示 SKU/商品/规格/仓库/库位/账面/锁定/可用/安全库存；
  - 可用数量低于安全库存的行整行标红；
  - 顶部支持关键词、仓库、"只看低于安全库存"筛选；
  - 点商品名展开该商品在各库位/仓库的库存分布。
-->
<template>
  <div class="inventory-page">
    <page-table
      :data="list"
      :loading="loading"
      :total="total"
      :scroll="{ x: 1200 }"
      :row-class="rowClass"
      v-model:page="query.pageNum"
      v-model:page-size="query.pageSize"
      @change="load"
    >
      <template #toolbar>
        <div class="toolbar">
          <a-form :model="query" layout="inline">
            <a-form-item field="keyword" label="商品">
              <a-input
                v-model="query.keyword"
                placeholder="SKU / 名称"
                allow-clear
                style="width: 180px"
                @press-enter="search"
              />
            </a-form-item>
            <a-form-item field="warehouseId" label="仓库">
              <a-select
                v-model="query.warehouseId"
                placeholder="全部"
                allow-search
                allow-clear
                style="width: 180px"
              >
                <a-option
                  v-for="w in warehouses"
                  :key="w.id"
                  :value="w.id"
                  :label="`${w.code} ${w.name}`"
                />
              </a-select>
            </a-form-item>
            <a-form-item field="onlyShortage" label="只看缺货">
              <a-switch v-model="query.onlyShortage" @change="search" />
            </a-form-item>
            <a-form-item>
              <a-space>
                <a-button type="primary" @click="search">
                  <template #icon><icon-search /></template>
                  查询
                </a-button>
                <a-button @click="reset">
                  <template #icon><icon-refresh /></template>
                  重置
                </a-button>
              </a-space>
            </a-form-item>
          </a-form>
        </div>
      </template>

      <template #columns>
        <a-table-column title="SKU" data-index="sku" :width="140" />
        <a-table-column title="商品名称" :width="180">
          <template #cell="{ record }">
            <a-link @click="openDistribution(record)">{{ record.productName }}</a-link>
          </template>
        </a-table-column>
        <a-table-column title="规格" data-index="spec" :width="120">
          <template #cell="{ record }">{{ record.spec || '-' }}</template>
        </a-table-column>
        <a-table-column title="仓库" data-index="warehouseName" :width="140" />
        <a-table-column title="库位" data-index="locationCode" :width="140" />
        <a-table-column title="账面数量" data-index="quantity" :width="100" />
        <a-table-column title="锁定数量" data-index="lockedQuantity" :width="100" />
        <a-table-column title="可用数量" :width="120">
          <template #cell="{ record }">
            <span :class="{ 'text-danger': record.shortage }">
              {{ record.availableQuantity }}
            </span>
          </template>
        </a-table-column>
        <a-table-column title="安全库存" :width="120">
          <template #cell="{ record }">
            {{ record.safetyStock ? record.safetyStock : '未设置' }}
          </template>
        </a-table-column>
        <a-table-column title="最近入库" :width="180">
          <template #cell="{ record }">{{ formatDateTime(record.lastInboundTime) }}</template>
        </a-table-column>
        <a-table-column title="最近出库" :width="180">
          <template #cell="{ record }">{{ formatDateTime(record.lastOutboundTime) }}</template>
        </a-table-column>
      </template>
    </page-table>

    <!-- 某商品在各库位/仓库的分布 -->
    <a-drawer
      v-model:visible="distVisible"
      :width="680"
      placement="right"
      title="库存分布"
      unmount-on-close
    >
      <a-spin :loading="distLoading" style="width: 100%">
        <a-descriptions :column="1" bordered style="margin-bottom: 12px">
          <a-descriptions-item label="商品">
            {{ distTitle }}
          </a-descriptions-item>
        </a-descriptions>
        <a-table
          :data="distList"
          :pagination="false"
          size="small"
          row-key="id"
        >
          <template #columns>
            <a-table-column title="仓库" data-index="warehouseName" :width="140" />
            <a-table-column title="库位" data-index="locationCode" :width="140" />
            <a-table-column title="账面数量" data-index="quantity" :width="100" />
            <a-table-column title="锁定数量" data-index="lockedQuantity" :width="100" />
            <a-table-column title="可用数量" data-index="availableQuantity" :width="100" />
            <a-table-column title="安全库存" :width="100">
              <template #cell="{ record }">
                {{ record.safetyStock ? record.safetyStock : '未设置' }}
              </template>
            </a-table-column>
          </template>
        </a-table>
      </a-spin>
    </a-drawer>
  </div>
</template>

<script lang="ts" setup>
  import { onMounted, reactive, ref } from 'vue';
  import {
    getInventoryPage,
    getInventoryByProduct,
    type InventoryItem,
  } from '@/api/inventory';
  import { getWarehouseOptions, type WarehouseItem } from '@/api/warehouse';
  import { formatDateTime } from '@/utils/dict';

  const query = reactive({
    pageNum: 1,
    pageSize: 10,
    keyword: '',
    warehouseId: undefined as number | undefined,
    onlyShortage: false,
  });

  const list = ref<InventoryItem[]>([]);
  const total = ref(0);
  const loading = ref(false);
  const warehouses = ref<WarehouseItem[]>([]);

  const load = async () => {
    loading.value = true;
    try {
      const res = await getInventoryPage({ ...query });
      list.value = res.list;
      total.value = res.total;
    } catch {
      /* 错误提示已由响应拦截器统一处理 */
    } finally {
      loading.value = false;
    }
  };

  const search = () => {
    query.pageNum = 1;
    load();
  };

  const reset = () => {
    query.keyword = '';
    query.warehouseId = undefined;
    query.onlyShortage = false;
    query.pageNum = 1;
    load();
  };

  /** 低于安全库存整行标红 */
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const rowClass = (record: any) =>
    record.shortage ? 'row-shortage' : '';

  // ---- 库存分布 ----
  const distVisible = ref(false);
  const distLoading = ref(false);
  const distTitle = ref('');
  const distList = ref<InventoryItem[]>([]);

  const openDistribution = async (record: InventoryItem) => {
    distVisible.value = true;
    distLoading.value = true;
    distTitle.value = `${record.sku} ${record.productName}`;
    distList.value = [];
    try {
      const res = await getInventoryByProduct(record.productId);
      distList.value = res.list;
    } catch {
      /* 错误提示已由响应拦截器统一处理 */
    } finally {
      distLoading.value = false;
    }
  };

  onMounted(async () => {
    try {
      warehouses.value = await getWarehouseOptions();
    } catch {
      /* 错误提示已由响应拦截器统一处理 */
    }
    load();
  });
</script>

<style scoped lang="less">
  .toolbar {
    display: flex;
    flex-wrap: wrap;
    align-items: flex-start;
    justify-content: space-between;
    gap: 12px;
  }

  .text-danger {
    color: rgb(var(--danger-6));
    font-weight: 600;
  }

  :deep(.row-shortage) td {
    background-color: rgb(var(--danger-1));
  }
</style>
