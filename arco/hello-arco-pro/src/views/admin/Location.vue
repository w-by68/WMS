<!-- 库位管理：库位分页查询、新增/编辑、启用禁用与删除 -->
<template>
  <page-table
    :data="list"
    :loading="loading"
    :total="total"
    v-model:page="query.pageNum"
    v-model:page-size="query.pageSize"
    :scroll="{ x: 1200 }"
    @change="load"
  >
    <template #toolbar>
      <a-form :model="query" layout="inline">
        <a-form-item field="warehouseId" label="所属仓库">
          <a-select
            v-model="query.warehouseId"
            placeholder="全部"
            allow-clear
            style="width: 180px"
          >
            <a-option
              v-for="w in warehouseOptions"
              :key="w.id"
              :value="w.id"
            >
              {{ w.name }}
            </a-option>
          </a-select>
        </a-form-item>
        <a-form-item field="zone" label="库区">
          <a-input
            v-model="query.zone"
            placeholder="请输入库区"
            allow-clear
          />
        </a-form-item>
        <a-form-item field="status" label="状态">
          <a-select
            v-model="query.status"
            placeholder="全部"
            allow-clear
            style="width: 140px"
          >
            <a-option
              v-for="opt in statusOptions"
              :key="opt.value"
              :value="opt.value"
            >
              {{ opt.label }}
            </a-option>
          </a-select>
        </a-form-item>
        <a-form-item field="keyword" label="关键词">
          <a-input
            v-model="query.keyword"
            placeholder="库位编码"
            allow-clear
          />
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" @click="handleSearch">
              <icon-search />
              查询
            </a-button>
            <a-button @click="handleReset">
              <icon-refresh />
              重置
            </a-button>
            <a-button type="primary" status="success" @click="handleAdd">
              <icon-plus />
              新增
            </a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </template>

    <template #columns>
      <a-table-column title="ID" data-index="id" :width="70" />
      <a-table-column title="所属仓库" data-index="warehouseName" :width="160" />
      <a-table-column title="库位编码" data-index="code" :width="140" />
      <a-table-column title="库区" data-index="zone" :width="120" />
      <a-table-column title="状态" :width="90">
        <template #cell="{ record }">
          <dict-tag :dict="LOCATION_STATUS_DICT" :value="record.status" />
        </template>
      </a-table-column>
      <a-table-column title="备注" data-index="remark" />
      <a-table-column title="创建时间" :width="180">
        <template #cell="{ record }">
          {{ formatDateTime(record.createTime) }}
        </template>
      </a-table-column>
      <a-table-column title="操作" :width="200" fixed="right">
        <template #cell="{ record }">
          <a-space>
            <a-button type="text" size="mini" @click="handleEdit(record)">
              编辑
            </a-button>
            <a-button
              type="text"
              size="mini"
              status="warning"
              @click="handleToggleStatus(record)"
            >
              {{ record.status === 'DISABLED' ? '启用' : '禁用' }}
            </a-button>
            <a-button
              type="text"
              size="mini"
              status="danger"
              @click="handleDelete(record)"
            >
              删除
            </a-button>
          </a-space>
        </template>
      </a-table-column>
    </template>
  </page-table>

  <a-modal
    v-model:visible="formVisible"
    :title="formMode === 'add' ? '新增库位' : '编辑库位'"
    :width="520"
    :mask-closable="false"
    @before-ok="handleBeforeOk"
  >
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <a-form-item field="warehouseId" label="所属仓库">
        <a-select
          v-model="form.warehouseId"
          :disabled="formMode === 'edit'"
          placeholder="请选择所属仓库"
        >
          <a-option
            v-for="w in warehouseOptions"
            :key="w.id"
            :value="w.id"
          >
            {{ w.name }}
          </a-option>
        </a-select>
      </a-form-item>
      <a-form-item field="code" label="库位编码">
        <a-input
          v-model="form.code"
          :disabled="formMode === 'edit'"
          placeholder="请输入库位编码"
        />
      </a-form-item>
      <a-form-item field="zone" label="库区">
        <a-input v-model="form.zone" placeholder="请输入库区" />
      </a-form-item>
      <a-form-item field="remark" label="备注">
        <a-textarea v-model="form.remark" placeholder="请输入备注" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
  import { nextTick, onMounted, reactive, ref } from 'vue';
  import { Message, Modal, type FormInstance } from '@arco-design/web-vue';
  import {
    createLocation,
    deleteLocation,
    getLocationPage,
    updateLocation,
    updateLocationStatus,
  } from '@/api/location';
  import type { LocationItem, LocationQuery } from '@/api/location';
  import { getWarehouseOptions } from '@/api/warehouse';
  import type { WarehouseItem } from '@/api/warehouse';
  import {
    LOCATION_STATUS_DICT,
    formatDateTime,
  } from '@/utils/dict';

  const statusOptions = Object.entries(LOCATION_STATUS_DICT).map(
    ([value, item]) => ({ value, label: item.label })
  );

  const warehouseOptions = ref<WarehouseItem[]>([]);
  const list = ref<LocationItem[]>([]);
  const total = ref(0);
  const loading = ref(false);
  const query = reactive<LocationQuery>({
    pageNum: 1,
    pageSize: 10,
    warehouseId: undefined,
    zone: '',
    status: undefined,
    keyword: '',
  });

  async function loadOptions() {
    try {
      warehouseOptions.value = await getWarehouseOptions();
    } catch {
      // 请求失败提示由 axios 拦截器统一处理
    }
  }

  async function load() {
    loading.value = true;
    try {
      const res = await getLocationPage(query);
      list.value = res.list;
      total.value = res.total;
    } catch {
      // 请求失败提示由 axios 拦截器统一处理
    } finally {
      loading.value = false;
    }
  }

  function handleSearch() {
    query.pageNum = 1;
    load();
  }

  function handleReset() {
    query.pageNum = 1;
    query.warehouseId = undefined;
    query.zone = '';
    query.status = undefined;
    query.keyword = '';
    load();
  }

  const formVisible = ref(false);
  const formMode = ref<'add' | 'edit'>('add');
  const currentId = ref<number>();
  const formRef = ref<FormInstance>();
  const form = reactive({
    warehouseId: undefined as number | undefined,
    code: '',
    zone: '',
    remark: '',
  });

  const rules = {
    warehouseId: [{ required: true, message: '请选择所属仓库' }],
    code: [{ required: true, message: '请输入库位编码' }],
  };

  function handleAdd() {
    formMode.value = 'add';
    currentId.value = undefined;
    form.warehouseId = undefined;
    form.code = '';
    form.zone = '';
    form.remark = '';
    formVisible.value = true;
    nextTick(() => formRef.value?.clearValidate());
  }

  function handleEdit(record: LocationItem) {
    formMode.value = 'edit';
    currentId.value = record.id;
    form.warehouseId = record.warehouseId;
    form.code = record.code;
    form.zone = record.zone ?? '';
    form.remark = record.remark ?? '';
    formVisible.value = true;
    nextTick(() => formRef.value?.clearValidate());
  }

  async function handleBeforeOk() {
    const errors = await formRef.value?.validate();
    if (errors) {
      Message.warning('请完善表单信息');
      return false;
    }
    try {
      if (formMode.value === 'add') {
        const warehouseId = form.warehouseId;
        if (warehouseId === undefined) return false;
        await createLocation({
          warehouseId,
          code: form.code,
          zone: form.zone,
          remark: form.remark,
        });
        Message.success('新增成功');
      } else if (currentId.value !== undefined) {
        // 编辑只允许修改库区与备注
        await updateLocation(currentId.value, {
          zone: form.zone,
          remark: form.remark,
        });
        Message.success('更新成功');
      }
      load();
      return true;
    } catch {
      return false;
    }
  }

  function handleToggleStatus(record: LocationItem) {
    const nextStatus = record.status === 'DISABLED' ? 'EMPTY' : 'DISABLED';
    const action = nextStatus === 'EMPTY' ? '启用' : '禁用';
    Modal.warning({
      title: `${action}确认`,
      content: `确定要${action}库位「${record.code}」吗？`,
      hideCancel: true,
      onOk: async () => {
        try {
          await updateLocationStatus(record.id, nextStatus);
          Message.success('操作成功');
          load();
        } catch {
          // 请求失败提示由 axios 拦截器统一处理
        }
      },
    });
  }

  function handleDelete(record: LocationItem) {
    Modal.warning({
      title: '删除确认',
      content: `确定要删除库位「${record.code}」吗？`,
      hideCancel: true,
      onOk: async () => {
        try {
          await deleteLocation(record.id);
          Message.success('删除成功');
          load();
        } catch {
          // 请求失败提示由 axios 拦截器统一处理
        }
      },
    });
  }

  onMounted(() => {
    loadOptions();
    load();
  });
</script>
