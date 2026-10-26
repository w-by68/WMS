<!--
  个人中心（管理员与普通用户共用）：
  - 展示账号/姓名/角色/手机/邮箱/状态/最近登录时间；
  - 修改个人资料（姓名、手机、邮箱）；
  - 修改登录密码（原密码 + 新密码 + 确认新密码），改完强制重新登录。
-->
<template>
  <div class="profile-page">
    <a-row :gutter="16">
      <a-col :span="12">
        <a-card :bordered="false" title="基本信息">
          <a-descriptions :column="1" bordered>
            <a-descriptions-item label="登录账号">
              {{ profile.username || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="姓名">
              {{ profile.realName || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="角色">
              <dict-tag :dict="ROLE_DICT" :value="profile.role" />
            </a-descriptions-item>
            <a-descriptions-item label="手机">{{ profile.phone || '-' }}</a-descriptions-item>
            <a-descriptions-item label="邮箱">{{ profile.email || '-' }}</a-descriptions-item>
            <a-descriptions-item label="账号状态">
              <dict-tag :dict="ENABLE_DICT" :value="profile.status" />
            </a-descriptions-item>
            <a-descriptions-item label="最近登录">
              {{ formatDateTime(profile.lastLoginTime) }}
            </a-descriptions-item>
          </a-descriptions>
        </a-card>
      </a-col>

      <a-col :span="12">
        <a-card :bordered="false" title="修改个人资料">
          <a-form :model="infoForm" layout="vertical">
            <a-form-item label="姓名" required>
              <a-input v-model="infoForm.realName" placeholder="请输入姓名" allow-clear />
            </a-form-item>
            <a-form-item label="手机">
              <a-input v-model="infoForm.phone" placeholder="选填" allow-clear />
            </a-form-item>
            <a-form-item label="邮箱">
              <a-input v-model="infoForm.email" placeholder="选填" allow-clear />
            </a-form-item>
            <a-form-item>
              <a-button type="primary" :loading="infoSaving" @click="saveInfo">
                保存资料
              </a-button>
            </a-form-item>
          </a-form>
        </a-card>

        <a-card :bordered="false" title="修改密码" style="margin-top: 16px">
          <a-form :model="pwdForm" layout="vertical">
            <a-form-item label="原密码" required>
              <a-input-password
                v-model="pwdForm.oldPassword"
                placeholder="请输入原密码"
                allow-clear
              />
            </a-form-item>
            <a-form-item label="新密码" required>
              <a-input-password
                v-model="pwdForm.newPassword"
                placeholder="至少 6 位"
                allow-clear
              />
            </a-form-item>
            <a-form-item label="确认新密码" required>
              <a-input-password
                v-model="pwdForm.confirmPassword"
                placeholder="请再次输入新密码"
                allow-clear
              />
            </a-form-item>
            <a-form-item>
              <a-button type="primary" :loading="pwdSaving" @click="savePassword">
                修改密码
              </a-button>
            </a-form-item>
          </a-form>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script lang="ts" setup>
  import { onMounted, reactive, ref } from 'vue';
  import { useRouter } from 'vue-router';
  import { Message } from '@arco-design/web-vue';
  import { getProfile, updateProfile, changePassword } from '@/api/auth';
  import { ROLE_DICT, ENABLE_DICT, formatDateTime } from '@/utils/dict';
  import { useUserStore } from '@/store';

  const router = useRouter();
  const userStore = useUserStore();

  const profile = reactive({
    username: '',
    realName: '',
    role: '',
    phone: '',
    email: '',
    status: '',
    lastLoginTime: '' as string | undefined,
  });

  const infoForm = reactive({
    realName: '',
    phone: '',
    email: '',
  });

  const pwdForm = reactive({
    oldPassword: '',
    newPassword: '',
    confirmPassword: '',
  });

  const infoSaving = ref(false);
  const pwdSaving = ref(false);

  /** 拉取当前登录人信息并同步到两个表单 */
  const loadProfile = async () => {
    const res = await getProfile();
    profile.username = res.username;
    profile.realName = res.realName;
    profile.role = res.role;
    profile.phone = res.phone ?? '';
    profile.email = res.email ?? '';
    profile.status = res.status;
    profile.lastLoginTime = res.lastLoginTime;
    infoForm.realName = res.realName ?? '';
    infoForm.phone = res.phone ?? '';
    infoForm.email = res.email ?? '';
  };

  onMounted(async () => {
    try {
      await loadProfile();
    } catch {
      /* 错误提示已由响应拦截器统一处理 */
    }
  });

  /** 保存资料：成功后同步刷新 store，保持顶栏姓名一致 */
  const saveInfo = async () => {
    if (infoSaving.value) return;
    if (!infoForm.realName.trim()) {
      Message.warning('姓名不能为空');
      return;
    }
    infoSaving.value = true;
    try {
      await updateProfile({
        realName: infoForm.realName.trim(),
        phone: infoForm.phone || undefined,
        email: infoForm.email || undefined,
      });
      await loadProfile();
      await userStore.info();
      Message.success('资料已保存');
    } catch {
      /* 错误提示已由响应拦截器统一处理 */
    } finally {
      infoSaving.value = false;
    }
  };

  /** 改密码：校验两次输入一致、长度 >= 6；成功后登出要求重新登录 */
  const savePassword = async () => {
    if (pwdSaving.value) return;
    if (!pwdForm.oldPassword) {
      Message.warning('请输入原密码');
      return;
    }
    if (!pwdForm.newPassword || pwdForm.newPassword.length < 6) {
      Message.warning('新密码长度至少 6 位');
      return;
    }
    if (pwdForm.newPassword !== pwdForm.confirmPassword) {
      Message.warning('两次输入的新密码不一致');
      return;
    }
    pwdSaving.value = true;
    try {
      await changePassword({
        oldPassword: pwdForm.oldPassword,
        newPassword: pwdForm.newPassword,
      });
      Message.success('密码修改成功，请用新密码重新登录');
      await userStore.logout();
      router.push({ name: 'login' });
    } catch {
      /* 错误提示已由响应拦截器统一处理 */
    } finally {
      pwdSaving.value = false;
    }
  };
</script>

<style scoped lang="less">
  .profile-page {
    padding: 16px 0;
  }
</style>
