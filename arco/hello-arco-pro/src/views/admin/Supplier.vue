<!-- 供应商管理：供应商的分页查询与新增、编辑、删除、启用停用 -->
<script setup lang="ts">
  import { onMounted, reactive, ref } from 'vue';
  import { Message, Modal } from '@arco-design/web-vue';
  import {
    getSupplierPage,
    createSupplier,
    updateSupplier,
    deleteSupplier,
    updateSupplierStatus,
    type SupplierItem,
    type SupplierQuery,
    type SupplierSaveData,
  } from '@/api/supplier';
  import { ENABLE_DICT, formatDateTime } from '@/utils/dict';

  const statusOptions = Object.entries(ENABLE_DICT).map(([value, item]) => ({
    label: item.label,
    value,
  }));

  const loading = ref(false);
  const list = ref<SupplierItem[]>([]);
  const total = ref(0);
  const query = reactive<SupplierQuery>({
    pageNum: 1,
    pageSize: 10,
    keyword: '',
    status: undefined,
  });

  const load = async () => {
    loading.value = true;
    try {
      const res = await getSupplierPage(query);
      list.value = res.list;
      total.value = res.total;
    } finally {
      loading.value = false;
    }
  };

  const handleSearch = () => {
    query.pageNum = 1;
    load();
  };

  const handleReset = () => {
    query.keyword = '';
    query.status = undefined;
    query.pageNum = 1;
    load();
  };

  const handleStatusChange = (value: unknown) => {
    query.status = value === undefined || value === null || value === '' ? undefined : String(value);
  };

  const visible = ref(false);
  const isEdit = ref(false);
  const formRef = ref();
  const form = reactive<SupplierSaveData & { id?: number }>({
    id: undefined,
    code: '',
    name: '',
    contact: '',
    phone: '',
    address: '',
    status: 'ENABLED',
  });

  const rules = {
    code: [{ required: true, message: '请输入供应商编码' }],
    name: [{ required: true, message: '请输入供应商名称' }],
  };

  const resetForm = () => {
    Object.assign(form, {
      id: undefined,
      code: '',
      name: '',
      contact: '',
      phone: '',
      address: '',
      status: 'ENABLED',
    });
  };

  const openCreate = () => {
    isEdit.value = false;
    resetForm();
    visible.value = true;
  };

  const openEdit = (record: SupplierItem) => {
    isEdit.value = true;
    Object.assign(form, {
      id: record.id,
      code: record.code,
      name: record.name,
      contact: record.contact,
      phone: record.phone,
      address: record.address,
      status: record.status,
    });
    visible.value = true;
  };

  const handleFormStatusChange = (value: unknown) => {
    form.status = value === undefined || value === null || value === '' ? undefined : String(value);
  };

  const handleBeforeOk = async () => {
    const errors = await formRef.value?.validate();
    if (errors) {
      return false;
    }
    const payload: SupplierSaveData = {
      name: form.name,
      contact: form.contact,
      phone: form.phone,
      address: form.address,
      status: form.status,
    };
    if (isEdit.value) {
      await updateSupplier(form.id as number, payload);
      Message.success('修改成功');
    } else {
      payload.code = form.code;
      await createSupplier(payload);
      Message.success('新增成功');
    }
    visible.value = false;
    load();
    return true;
  };

  const handleToggleStatus = (record: SupplierItem) => {
    const next = record.status === 'ENABLED' ? 'DISABLED' : 'ENABLED';
    const action = next === 'ENABLED' ? '启用' : '停用';
    Modal.warning({
      title: '操作确认',
      content: `确定要${action}供应商「${record.name}」吗？`,
      hideCancel: true,
      onOk: async () => {
        await updateSupplierStatus(record.id, next);
        Message.success(`${action}成功`);
        load();
      },
    });
  };

  const handleDelete = (record: SupplierItem) => {
    Modal.warning({
      title: '删除确认',
      content: `确定要删除供应商「${record.name}」吗？`,
      hideCancel: true,
      onOk: async () => {
        await deleteSupplier(record.id);
        Message.success('删除成功');
        load();
      },
    });
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
    :scroll="{ x: 1400 }"
    @change="load"
  >
    <template #toolbar>
      <a-form :model="query" layout="inline">
        <a-form-item field="keyword" label="关键词">
          <a-input
            v-model="query.keyword"
            placeholder="编码 / 名称"
            allow-clear
            style="width: 200px"
            @press-enter="handleSearch"
          />
        </a-form-item>
        <a-form-item field="status" label="状态">
          <a-select
            :model-value="query.status"
            :options="statusOptions"
            allow-clear
            placeholder="全部状态"
            style="width: 140px"
            @change="handleStatusChange"
          />
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" @click="handleSearch"><icon-search /> 查询</a-button>
            <a-button @click="handleReset"><icon-refresh /> 重置</a-button>
            <a-button type="primary" status="success" @click="openCreate">
              <icon-plus /> 新增
            </a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </template>

    <template #columns>
      <a-table-column title="ID" data-index="id" :width="80" />
      <a-table-column title="编码" data-index="code" :width="140" />
      <a-table-column title="名称" data-index="name" :width="180" />
      <a-table-column title="联系人" data-index="contact" :width="120" />
      <a-table-column title="电话" data-index="phone" :width="150" />
      <a-table-column title="地址" data-index="address" :width="200" />
      <a-table-column title="状态" :width="90">
        <template #cell="{ record }">
          <dict-tag :dict="ENABLE_DICT" :value="record.status" />
        </template>
      </a-table-column>
      <a-table-column title="创建时间" :width="180">
        <template #cell="{ record }">{{ formatDateTime(record.createTime) }}</template>
      </a-table-column>
      <a-table-column title="操作" :width="220" fixed="right">
        <template #cell="{ record }">
          <a-space>
            <a-button type="text" size="small" @click="openEdit(record)"><icon-edit /> 编辑</a-button>
            <a-button type="text" size="small" @click="handleToggleStatus(record)">
              {{ record.status === 'ENABLED' ? '停用' : '启用' }}
            </a-button>
            <a-button type="text" status="danger" size="small" @click="handleDelete(record)">
              <icon-delete /> 删除
            </a-button>
          </a-space>
        </template>
      </a-table-column>
    </template>
  </page-table>

  <a-modal
    v-model:visible="visible"
    :title="isEdit ? '编辑供应商' : '新增供应商'"
    unmount-on-close
    @before-ok="handleBeforeOk"
  >
    <a-form ref="formRef" :model="form" :rules="rules">
      <a-form-item v-if="!isEdit" field="code" label="编码">
        <a-input v-model="form.code" placeholder="请输入供应商编码" />
      </a-form-item>
      <a-form-item field="name" label="名称">
        <a-input v-model="form.name" placeholder="请输入供应商名称" />
      </a-form-item>
      <a-form-item field="contact" label="联系人">
        <a-input v-model="form.contact" placeholder="请输入联系人" />
      </a-form-item>
      <a-form-item field="phone" label="电话">
        <a-input v-model="form.phone" placeholder="请输入电话" />
      </a-form-item>
      <a-form-item field="address" label="地址">
        <a-input v-model="form.address" placeholder="请输入地址" />
      </a-form-item>
      <a-form-item field="status" label="状态">
        <a-select
          :model-value="form.status"
          :options="statusOptions"
          placeholder="请选择状态"
          @change="handleFormStatusChange"
        />
      </a-form-item>
    </a-form>
  </a-modal>
</template>
