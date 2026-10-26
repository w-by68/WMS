<!-- 用户管理：用户分页查询、新增/编辑、启用停用、重置密码与删除 -->
<template>
  <page-table
    :data="list"
    :loading="loading"
    :total="total"
    v-model:page="query.pageNum"
    v-model:page-size="query.pageSize"
    :scroll="{ x: 1450 }"
    @change="load"
  >
    <template #toolbar>
      <a-form :model="query" layout="inline">
        <a-form-item field="keyword" label="关键词">
          <a-input
            v-model="query.keyword"
            placeholder="用户名/姓名"
            allow-clear
          />
        </a-form-item>
        <a-form-item field="role" label="角色">
          <a-select
            v-model="query.role"
            placeholder="全部"
            allow-clear
            style="width: 140px"
          >
            <a-option
              v-for="opt in roleOptions"
              :key="opt.value"
              :value="opt.value"
            >
              {{ opt.label }}
            </a-option>
          </a-select>
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
      <a-table-column title="用户名" data-index="username" :width="120" />
      <a-table-column title="姓名" data-index="realName" :width="120" />
      <a-table-column title="角色" :width="100">
        <template #cell="{ record }">
          <dict-tag :dict="ROLE_DICT" :value="record.role" />
        </template>
      </a-table-column>
      <a-table-column title="手机" data-index="phone" :width="140" />
      <a-table-column title="邮箱" data-index="email" :width="180" />
      <a-table-column title="状态" :width="90">
        <template #cell="{ record }">
          <dict-tag :dict="ENABLE_DICT" :value="record.status" />
        </template>
      </a-table-column>
      <a-table-column title="最后登录时间" :width="180">
        <template #cell="{ record }">
          {{ formatDateTime(record.lastLoginTime) }}
        </template>
      </a-table-column>
      <a-table-column title="创建时间" :width="180">
        <template #cell="{ record }">
          {{ formatDateTime(record.createTime) }}
        </template>
      </a-table-column>
      <a-table-column title="操作" :width="260" fixed="right">
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
              @click="handleOpenReset(record)"
            >
              重置密码
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
    :title="formMode === 'add' ? '新增用户' : '编辑用户'"
    :width="520"
    :mask-closable="false"
    @before-ok="handleBeforeOk"
  >
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <a-form-item field="username" label="用户名">
        <a-input
          v-model="form.username"
          :disabled="formMode === 'edit'"
          placeholder="请输入用户名"
        />
      </a-form-item>
      <a-form-item v-if="formMode === 'add'" field="password" label="密码">
        <a-input-password
          v-model="form.password"
          placeholder="请输入密码"
        />
      </a-form-item>
      <a-form-item field="realName" label="姓名">
        <a-input v-model="form.realName" placeholder="请输入姓名" />
      </a-form-item>
      <a-form-item field="role" label="角色">
        <a-select v-model="form.role" placeholder="请选择角色">
          <a-option
            v-for="opt in roleOptions"
            :key="opt.value"
            :value="opt.value"
          >
            {{ opt.label }}
          </a-option>
        </a-select>
      </a-form-item>
      <a-form-item field="phone" label="手机号">
        <a-input v-model="form.phone" placeholder="请输入手机号" />
      </a-form-item>
      <a-form-item field="email" label="邮箱">
        <a-input v-model="form.email" placeholder="请输入邮箱" />
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

  <a-modal
    v-model:visible="resetVisible"
    title="重置密码"
    :width="420"
    :mask-closable="false"
    @before-ok="handleResetOk"
  >
    <a-form ref="resetFormRef" :model="resetForm" :rules="resetRules">
      <a-form-item field="newPassword" label="新密码">
        <a-input-password
          v-model="resetForm.newPassword"
          placeholder="请输入新密码"
        />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
  import { nextTick, onMounted, reactive, ref } from 'vue';
  import {
    Message,
    Modal,
    type FieldRule,
    type FormInstance,
  } from '@arco-design/web-vue';
  import { useUserStore } from '@/store';
  import {
    createUser,
    deleteUser,
    getUserPage,
    resetUserPassword,
    updateUser,
    updateUserStatus,
  } from '@/api/user';
  import type { UserItem, UserQuery, UserSaveData } from '@/api/user';
  import { ENABLE_DICT, ROLE_DICT, formatDateTime } from '@/utils/dict';

  const userStore = useUserStore();

  const roleOptions = Object.entries(ROLE_DICT).map(([value, item]) => ({
    value,
    label: item.label,
  }));
  const enableOptions = Object.entries(ENABLE_DICT).map(([value, item]) => ({
    value,
    label: item.label,
  }));

  const list = ref<UserItem[]>([]);
  const total = ref(0);
  const loading = ref(false);
  const query = reactive<UserQuery>({
    pageNum: 1,
    pageSize: 10,
    keyword: '',
    role: undefined,
    status: undefined,
  });

  async function load() {
    loading.value = true;
    try {
      const res = await getUserPage(query);
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
    query.role = undefined;
    query.status = undefined;
    load();
  }

  const formVisible = ref(false);
  const formMode = ref<'add' | 'edit'>('add');
  const currentId = ref<number>();
  const formRef = ref<FormInstance>();
  const form = reactive<UserSaveData>({
    username: '',
    password: '',
    realName: '',
    role: 'USER',
    phone: '',
    email: '',
    status: 'ENABLED',
  });

  const rules: Record<string, FieldRule[]> = {
    username: [{ required: true, message: '请输入用户名' }],
    password: [{ required: true, message: '请输入密码' }],
    realName: [{ required: true, message: '请输入姓名' }],
    role: [{ required: true, message: '请选择角色' }],
    email: [{ type: 'email', message: '邮箱格式不正确' }],
    phone: [{ match: /^1[3-9]\d{9}$/, message: '手机号格式不正确' }],
  };

  function handleAdd() {
    formMode.value = 'add';
    currentId.value = undefined;
    form.username = '';
    form.password = '';
    form.realName = '';
    form.role = 'USER';
    form.phone = '';
    form.email = '';
    form.status = 'ENABLED';
    formVisible.value = true;
    nextTick(() => formRef.value?.clearValidate());
  }

  function handleEdit(record: UserItem) {
    formMode.value = 'edit';
    currentId.value = record.id;
    form.username = record.username;
    form.password = '';
    form.realName = record.realName;
    form.role = record.role;
    form.phone = record.phone ?? '';
    form.email = record.email ?? '';
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
        await createUser({ ...form });
        Message.success('新增成功');
      } else if (currentId.value !== undefined) {
        // 编辑不允许修改 username / password
        await updateUser(currentId.value, {
          realName: form.realName,
          role: form.role,
          phone: form.phone,
          email: form.email,
          status: form.status,
        });
        Message.success('更新成功');
      }
      load();
      return true;
    } catch {
      return false;
    }
  }

  function handleToggleStatus(record: UserItem) {
    const nextStatus = record.status === 'ENABLED' ? 'DISABLED' : 'ENABLED';
    if (nextStatus === 'DISABLED' && record.id === userStore.userId) {
      Message.warning('不能禁用当前登录账号');
      return;
    }
    const action = nextStatus === 'ENABLED' ? '启用' : '停用';
    Modal.warning({
      title: `${action}确认`,
      content: `确定要${action}用户「${record.realName || record.username}」吗？`,
      hideCancel: true,
      onOk: async () => {
        try {
          await updateUserStatus(record.id, nextStatus);
          Message.success('操作成功');
          load();
        } catch {
          // 请求失败提示由 axios 拦截器统一处理
        }
      },
    });
  }

  function handleDelete(record: UserItem) {
    Modal.warning({
      title: '删除确认',
      content: `确定要删除用户「${record.realName || record.username}」吗？`,
      hideCancel: true,
      onOk: async () => {
        try {
          await deleteUser(record.id);
          Message.success('删除成功');
          load();
        } catch {
          // 请求失败提示由 axios 拦截器统一处理
        }
      },
    });
  }

  const resetVisible = ref(false);
  const resetFormRef = ref<FormInstance>();
  const resetId = ref<number>();
  const resetForm = reactive({ newPassword: '' });
  const resetRules = {
    newPassword: [
      { required: true, message: '请输入新密码' },
      { minLength: 6, message: '密码至少 6 位' },
    ],
  };

  function handleOpenReset(record: UserItem) {
    resetId.value = record.id;
    resetForm.newPassword = '';
    resetVisible.value = true;
    nextTick(() => resetFormRef.value?.clearValidate());
  }

  async function handleResetOk() {
    const errors = await resetFormRef.value?.validate();
    if (errors) {
      Message.warning('请填写新密码');
      return false;
    }
    if (resetId.value === undefined) return false;
    try {
      await resetUserPassword(resetId.value, resetForm.newPassword);
      Message.success('密码重置成功');
      return true;
    } catch {
      return false;
    }
  }

  onMounted(load);
</script>
