<!-- 仓库管理：仓库分页查询、新增/编辑、启用停用与删除 -->
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
        <a-form-item field="keyword" label="关键词">
          <a-input
            v-model="query.keyword"
            placeholder="编码/名称"
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
              v-for="opt in enableOptions"
              :key="opt.value"
              :value="opt.value"
            >
              {{ opt.label }}
            </a-option>
          </a-select>
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
      <a-table-column title="编码" data-index="code" :width="140" />
      <a-table-column title="名称" data-index="name" :width="160" />
      <a-table-column title="地址" data-index="address" />
      <a-table-column title="负责人" data-index="manager" :width="120" />
      <a-table-column title="电话" data-index="phone" :width="140" />
      <a-table-column title="状态" :width="90">
        <template #cell="{ record }">
          <dict-tag :dict="ENABLE_DICT" :value="record.status" />
        </template>
      </a-table-column>
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
              {{ record.status === 'ENABLED' ? '停用' : '启用' }}
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
    :title="formMode === 'add' ? '新增仓库' : '编辑仓库'"
    :width="520"
    :mask-closable="false"
    @before-ok="handleBeforeOk"
  >
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <a-form-item field="code" label="仓库编码">
        <a-input v-model="form.code" placeholder="请输入仓库编码" />
      </a-form-item>
      <a-form-item field="name" label="仓库名称">
        <a-input v-model="form.name" placeholder="请输入仓库名称" />
      </a-form-item>
      <a-form-item field="address" label="地址">
        <a-input v-model="form.address" placeholder="请输入地址" />
      </a-form-item>
      <a-form-item field="manager" label="负责人">
        <a-input v-model="form.manager" placeholder="请输入负责人" />
      </a-form-item>
      <a-form-item field="phone" label="电话">
        <a-input v-model="form.phone" placeholder="请输入电话" />
      </a-form-item>
      <a-form-item field="status" label="状态">
        <a-select v-model="form.status" placeholder="请选择状态">
          <a-option
            v-for="opt in enableOptions"
            :key="opt.value"
            :value="opt.value"
          >
            {{ opt.label }}
          </a-option>
        </a-select>
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
  import { nextTick, onMounted, reactive, ref } from 'vue';
  import { Message, Modal, type FormInstance } from '@arco-design/web-vue';
  import {
    createWarehouse,
    deleteWarehouse,
    getWarehousePage,
    updateWarehouse,
    updateWarehouseStatus,
  } from '@/api/warehouse';
  import type {
    WarehouseItem,
    WarehouseQuery,
    WarehouseSaveData,
  } from '@/api/warehouse';
  import { ENABLE_DICT, formatDateTime } from '@/utils/dict';

  const enableOptions = Object.entries(ENABLE_DICT).map(([value, item]) => ({
    value,
    label: item.label,
  }));

  const list = ref<WarehouseItem[]>([]);
  const total = ref(0);
  const loading = ref(false);
  const query = reactive<WarehouseQuery>({
    pageNum: 1,
    pageSize: 10,
    keyword: '',
    status: undefined,
  });

  async function load() {
    loading.value = true;
    try {
      const res = await getWarehousePage(query);
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
    query.keyword = '';
    query.status = undefined;
    load();
  }

  const formVisible = ref(false);
  const formMode = ref<'add' | 'edit'>('add');
  const currentId = ref<number>();
  const formRef = ref<FormInstance>();
  const form = reactive<WarehouseSaveData>({
    code: '',
    name: '',
    address: '',
    manager: '',
    phone: '',
    status: 'ENABLED',
  });

  const rules = {
    code: [{ required: true, message: '请输入仓库编码' }],
    name: [{ required: true, message: '请输入仓库名称' }],
    phone: [{ match: /^[0-9-]{6,20}$/, message: '电话格式不正确' }],
  };

  function handleAdd() {
    formMode.value = 'add';
    currentId.value = undefined;
    form.code = '';
    form.name = '';
    form.address = '';
    form.manager = '';
    form.phone = '';
    form.status = 'ENABLED';
    formVisible.value = true;
    nextTick(() => formRef.value?.clearValidate());
  }

  function handleEdit(record: WarehouseItem) {
    formMode.value = 'edit';
    currentId.value = record.id;
    form.code = record.code;
    form.name = record.name;
    form.address = record.address ?? '';
    form.manager = record.manager ?? '';
    form.phone = record.phone ?? '';
    form.status = record.status;
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
        await createWarehouse({ ...form });
        Message.success('新增成功');
      } else if (currentId.value !== undefined) {
        await updateWarehouse(currentId.value, { ...form });
        Message.success('更新成功');
      }
      load();
      return true;
    } catch {
      return false;
    }
  }

  function handleToggleStatus(record: WarehouseItem) {
    const nextStatus = record.status === 'ENABLED' ? 'DISABLED' : 'ENABLED';
    const action = nextStatus === 'ENABLED' ? '启用' : '停用';
    Modal.warning({
      title: `${action}确认`,
      content: `确定要${action}仓库「${record.name}」吗？`,
      hideCancel: true,
      onOk: async () => {
        try {
          await updateWarehouseStatus(record.id, nextStatus);
          Message.success('操作成功');
          load();
        } catch {
          // 请求失败提示由 axios 拦截器统一处理
        }
      },
    });
  }

  function handleDelete(record: WarehouseItem) {
    Modal.warning({
      title: '删除确认',
      content: `确定要删除仓库「${record.name}」吗？`,
      hideCancel: true,
      onOk: async () => {
        try {
          await deleteWarehouse(record.id);
          Message.success('删除成功');
          load();
        } catch {
          // 请求失败提示由 axios 拦截器统一处理
        }
      },
    });
  }

  onMounted(load);
</script>
