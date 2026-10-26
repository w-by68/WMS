<!--
  库存预警（只读页，管理员与普通用户共用）：
  - 分页列表 + 类型/级别/状态筛选；
  - 类型、级别、状态用 DictTag 渲染；
  - 展示"建议补货量 / 建议消化量"（列头带 tooltip 说明口径）；
  - 本页不提供"处理 / 忽略"按钮，预警处理只在管理员看板里做。
-->
<template>
  <page-table
    :data="list"
    :loading="loading"
    :total="total"
    :scroll="{ x: 1500 }"
    v-model:page="query.pageNum"
    v-model:page-size="query.pageSize"
    @change="load"
  >
    <template #toolbar>
      <div class="toolbar">
        <a-form :model="query" layout="inline">
          <a-form-item field="alertType" label="类型">
            <a-select
              v-model="query.alertType"
              placeholder="全部"
              allow-clear
              style="width: 140px"
            >
              <a-option
                v-for="(item, key) in ALERT_TYPE_DICT"
                :key="key"
                :value="key"
              >
                {{ item.label }}
              </a-option>
            </a-select>
          </a-form-item>
          <a-form-item field="level" label="级别">
            <a-select
              v-model="query.level"
              placeholder="全部"
              allow-clear
              style="width: 140px"
            >
              <a-option
                v-for="(item, key) in ALERT_LEVEL_DICT"
                :key="key"
                :value="key"
              >
                {{ item.label }}
              </a-option>
            </a-select>
          </a-form-item>
          <a-form-item field="status" label="处理状态">
            <a-select
              v-model="query.status"
              placeholder="全部"
              allow-clear
              style="width: 140px"
            >
              <a-option
                v-for="(item, key) in ALERT_STATUS_DICT"
                :key="key"
                :value="key"
              >
                {{ item.label }}
              </a-option>
            </a-select>
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
      <a-table-column title="预警单号" data-index="alertNo" :width="170" />
      <a-table-column title="商品" :width="200">
        <template #cell="{ record }">
          {{ record.sku ? `${record.sku} ${record.productName}` : record.productName }}
        </template>
      </a-table-column>
      <a-table-column title="仓库" data-index="warehouseName" :width="140" />
      <a-table-column title="类型" :width="100">
        <template #cell="{ record }">
          <dict-tag :dict="ALERT_TYPE_DICT" :value="record.alertType" />
        </template>
      </a-table-column>
      <a-table-column title="级别" :width="100">
        <template #cell="{ record }">
          <dict-tag :dict="ALERT_LEVEL_DICT" :value="record.level" />
        </template>
      </a-table-column>
      <a-table-column title="当前库存" data-index="currentQuantity" :width="100" />
      <a-table-column title="阈值" :width="100">
        <template #cell="{ record }">{{ record.thresholdValue ?? '-' }}</template>
      </a-table-column>
      <a-table-column :width="180">
        <template #title>
          <a-space>
            建议补货量 / 建议消化量
            <a-tooltip>
              <template #content>
                缺货：安全库存 - 当前可用数量，即建议补货量；<br />
                积压：当前库存 - 库存上限，即建议消化量；<br />
                呆滞：无补货/消化建议，显示为 0。
              </template>
              <icon-info-circle />
            </a-tooltip>
          </a-space>
        </template>
        <template #cell="{ record }">{{ record.suggestQuantity ?? 0 }}</template>
      </a-table-column>
      <a-table-column title="处理状态" :width="110">
        <template #cell="{ record }">
          <dict-tag :dict="ALERT_STATUS_DICT" :value="record.status" />
        </template>
      </a-table-column>
      <a-table-column title="处理人" :width="120">
        <template #cell="{ record }">{{ record.handlerName || '-' }}</template>
      </a-table-column>
      <a-table-column title="处理时间" :width="180">
        <template #cell="{ record }">{{ formatDateTime(record.handleTime) }}</template>
      </a-table-column>
      <a-table-column title="处理备注" :width="180">
        <template #cell="{ record }">{{ record.handleRemark || '-' }}</template>
      </a-table-column>
    </template>
  </page-table>
</template>

<script lang="ts" setup>
  import { onMounted, reactive, ref } from 'vue';
  import { getAlertPage, type AlertItem } from '@/api/alert';
  import {
    ALERT_TYPE_DICT,
    ALERT_LEVEL_DICT,
    ALERT_STATUS_DICT,
    formatDateTime,
  } from '@/utils/dict';

  const query = reactive({
    pageNum: 1,
    pageSize: 10,
    alertType: undefined as string | undefined,
    level: undefined as string | undefined,
    status: undefined as string | undefined,
  });

  const list = ref<AlertItem[]>([]);
  const total = ref(0);
  const loading = ref(false);

  const load = async () => {
    loading.value = true;
    try {
      const res = await getAlertPage({ ...query });
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
    query.alertType = undefined;
    query.level = undefined;
    query.status = undefined;
    query.pageNum = 1;
    load();
  };

  onMounted(load);
</script>

<style scoped lang="less">
  .toolbar {
    display: flex;
    flex-wrap: wrap;
    align-items: flex-start;
    justify-content: space-between;
    gap: 12px;
  }
</style>
