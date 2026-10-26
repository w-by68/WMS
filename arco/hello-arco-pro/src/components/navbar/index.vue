<template>
  <div class="navbar">
    <div class="left-side">
      <a-space>
        <icon-storage class="logo-icon" />
        <a-typography-title
          :style="{ margin: 0, fontSize: '18px' }"
          :heading="5"
        >
          WMS 仓储管理系统
        </a-typography-title>
        <icon-menu-fold
          v-if="!topMenu && appStore.device === 'mobile'"
          style="font-size: 22px; cursor: pointer"
          @click="toggleDrawerMenu"
        />
      </a-space>
    </div>
    <div class="center-side">
      <Menu v-if="topMenu" />
    </div>
    <ul class="right-side">
      <li>
        <a-dropdown trigger="click">
          <a-space class="user-trigger">
            <a-avatar :size="32" :style="{ backgroundColor: '#165DFF' }">
              <icon-user />
            </a-avatar>
            <span class="user-name">{{ displayName }}</span>
          </a-space>
          <template #content>
            <a-doption>
              <a-space @click="passwordVisible = true">
                <icon-lock />
                <span>修改密码</span>
              </a-space>
            </a-doption>
            <a-doption>
              <a-space @click="handleLogout">
                <icon-export />
                <span>退出登录</span>
              </a-space>
            </a-doption>
          </template>
        </a-dropdown>
      </li>
    </ul>

    <a-modal
      v-model:visible="passwordVisible"
      title="修改密码"
      :ok-loading="passwordLoading"
      @ok="handleChangePassword"
    >
      <a-form ref="passwordFormRef" :model="passwordForm" :rules="rules">
        <a-form-item field="oldPassword" label="原密码" validate-trigger="input">
          <a-input-password
            v-model="passwordForm.oldPassword"
            placeholder="请输入原密码"
            allow-clear
          />
        </a-form-item>
        <a-form-item field="newPassword" label="新密码" validate-trigger="input">
          <a-input-password
            v-model="passwordForm.newPassword"
            placeholder="请输入新密码"
            allow-clear
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
  import { computed, ref, inject, reactive } from 'vue';
  import { Message } from '@arco-design/web-vue';
  import { useAppStore, useUserStore } from '@/store';
  import useUser from '@/hooks/user';
  import { changePassword } from '@/api/auth';
  import Menu from '@/components/menu/index.vue';

  const appStore = useAppStore();
  const userStore = useUserStore();
  const { logout } = useUser();
  const topMenu = computed(() => appStore.topMenu && appStore.menu);
  const displayName = computed(
    () => userStore.realName || userStore.username || '未登录'
  );

  const passwordVisible = ref(false);
  const passwordLoading = ref(false);
  const passwordFormRef = ref();
  const passwordForm = reactive({
    oldPassword: '',
    newPassword: '',
  });
  const rules = {
    oldPassword: [{ required: true, message: '请输入原密码' }],
    newPassword: [
      { required: true, message: '请输入新密码' },
      { minLength: 6, message: '密码长度不少于 6 位' },
    ],
  };

  const handleChangePassword = async () => {
    const errors = await passwordFormRef.value?.validate();
    if (errors) return;
    passwordLoading.value = true;
    try {
      await changePassword({ ...passwordForm });
      Message.success('密码修改成功，请重新登录');
      passwordVisible.value = false;
      passwordForm.oldPassword = '';
      passwordForm.newPassword = '';
      await logout();
    } catch (error) {
      // 错误提示已由 axios 拦截器统一处理
    } finally {
      passwordLoading.value = false;
    }
  };

  const handleLogout = () => {
    logout();
  };

  const toggleDrawerMenu = inject('toggleDrawerMenu') as () => void;
</script>

<style scoped lang="less">
  .navbar {
    display: flex;
    justify-content: space-between;
    height: 100%;
    background-color: var(--color-bg-2);
    border-bottom: 1px solid var(--color-border);
  }

  .left-side {
    display: flex;
    align-items: center;
    padding-left: 20px;

    .logo-icon {
      color: #165dff;
      font-size: 24px;
    }
  }

  .center-side {
    flex: 1;
  }

  .right-side {
    display: flex;
    padding-right: 20px;
    list-style: none;
    li {
      display: flex;
      align-items: center;
      padding: 0 10px;
    }

    .user-trigger {
      cursor: pointer;
    }

    .user-name {
      color: var(--color-text-1);
      font-size: 14px;
    }
  }
</style>
