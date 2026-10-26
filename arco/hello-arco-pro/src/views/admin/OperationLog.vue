<!-- 操作日志：只读查询系统操作日志记录 -->
<script setup lang="ts">
  import { onMounted, reactive, ref } from 'vue';
  import {
    getOperationLogPage,
    type OperationLogItem,
    type OperationLogQuery,
  } from '@/api/log';
  import { SUCCESS_DICT, formatDateTime } from '@/utils/dict';

  const successOptions = [
    { label: '成功', value: 1 },
    { label: '失败', value: 0 },
  ];

  const loading = ref(false);
  const list = ref<OperationLogItem[]>([]);
  const total = ref(0);
  const query = reactive<OperationLogQuery>({
    pageNum: 1,
    pageSize: 10,
    username: '',
    module: '',
    success: undefined,
    startTime: undefined,
    endTime: undefined,
  });

  const timeRange = ref<(string | number)[] | undefined>();

  const load = async () => {
    loading.value = true;
    try {
      const res = await getOperationLogPage(query);
      list.value = res.list;
      total.value = res.total;
    } finally {
      loading.value = false;
    }
  };

  const handleSearch = () => {
    const start = timeRange.value?.[0];
    const end = timeRange.value?.[1];
    query.startTime = start ? String(start) : undefined;
    query.endTime = end ? String(end) : undefined;
    query.pageNum = 1;
    load();
  };

  const handleReset = () => {
    query.username = '';
    query.module = '';
    query.success = undefined;
    query.startTime = undefined;
    query.endTime = undefined;
    timeRange.value = undefined;
    query.pageNum = 1;
    load();
  };

  const handleSuccessChange = (value: unknown) => {
    query.success = value === undefined || value === null || value === '' ? undefined : Number(value);
  };

  const handleTimeChange = (value: unknown) => {
    timeRange.value = Array.isArray(value) ? (value as (string | number)[]) : undefined;
  };

  onMounted(load);
</script>

<template>
  <page-table
    :data="list"
    :loading="loading"
    :total="total"
    v-model:page="query.pageNum"
    v-model:page-size="query.pageSize"
    :scroll="{ x: 1700 }"
    @change="load"
  >
    <template #toolbar>
      <a-form :model="query" layout="inline">
        <a-form-item field="username" label="操作人">
          <a-input
            v-model="query.username"
            placeholder="请输入操作人"
            allow-clear
            style="width: 160px"
            @press-enter="handleSearch"
          />
        </a-form-item>
        <a-form-item field="module" label="模块">
          <a-input
            v-model="query.module"
            placeholder="请输入模块"
            allow-clear
            style="width: 160px"
            @press-enter="handleSearch"
          />
        </a-form-item>
        <a-form-item field="success" label="结果">
          <a-select
            :model-value="query.success"
            :options="successOptions"
            allow-clear
            placeholder="全部结果"
            style="width: 140px"
            @change="handleSuccessChange"
          />
        </a-form-item>
        <a-form-item label="操作时间">
          <a-range-picker
            :model-value="timeRange"
            show-time
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            allow-clear
            style="width: 380px"
            @change="handleTimeChange"
          />
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" @click="handleSearch"><icon-search /> 查询</a-button>
            <a-button @click="handleReset"><icon-refresh /> 重置</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </template>

    <template #columns>
      <a-table-column title="ID" data-index="id" :width="80" />
      <a-table-column title="操作人" data-index="username" :width="120" />
      <a-table-column title="模块" data-index="module" :width="140" />
      <a-table-column title="操作" data-index="operation" :width="160" />
      <a-table-column title="请求方法" data-index="method" :width="110" />
      <a-table-column title="请求 URL" data-index="requestUrl" :width="220" />
      <a-table-column title="IP" data-index="ip" :width="140" />
      <a-table-column title="耗时" :width="100">
        <template #cell="{ record }">{{ record.costMs }} ms</template>
      </a-table-column>
      <a-table-column title="结果" :width="90">
        <template #cell="{ record }">
          <dict-tag :dict="SUCCESS_DICT" :value="record.success" />
        </template>
      </a-table-column>
      <a-table-column title="错误信息" data-index="errorMsg" :width="200" />
      <a-table-column title="操作时间" :width="180">
        <template #cell="{ record }">{{ formatDateTime(record.createTime) }}</template>
      </a-table-column>
    </template>
  </page-table>
</template>
