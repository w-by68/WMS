import { defineStore } from 'pinia';
import {
  login as userLogin,
  logout as userLogout,
  getProfile,
  LoginData,
} from '@/api/auth';
import { setToken, clearToken } from '@/utils/auth';
import { removeRouteListener } from '@/utils/route-listener';
import { UserState } from './types';
import useAppStore from '../app';

const useUserStore = defineStore('user', {
  state: (): UserState => ({
    userId: undefined,
    username: undefined,
    realName: undefined,
    role: '',
    phone: undefined,
    email: undefined,
    status: undefined,
    lastLoginTime: undefined,
    avatar: undefined,
  }),

  getters: {
    userInfo(state: UserState): UserState {
      return { ...state };
    },
  },

  actions: {
    setInfo(partial: Partial<UserState>) {
      this.$patch(partial);
    },

    resetInfo() {
      this.$reset();
    },

    /** 拉取当前登录人信息（路由守卫在仅有 token 时调用） */
    async info() {
      const res = await getProfile();
      this.setInfo({
        userId: res.id,
        username: res.username,
        realName: res.realName,
        role: res.role as UserState['role'],
        phone: res.phone,
        email: res.email,
        status: res.status,
        lastLoginTime: res.lastLoginTime,
      });
    },

    /**
     * 登录：后端已返回 role，登录时直接写入 store，
     * 这样路由守卫无需再额外请求 profile 就能完成权限判定。
     */
    async login(loginForm: LoginData) {
      try {
        const res = await userLogin(loginForm);
        setToken(res.token);
        this.setInfo({
          userId: res.userId,
          username: res.username,
          realName: res.realName,
          role: res.role as UserState['role'],
        });
      } catch (err) {
        clearToken();
        throw err;
      }
    },

    logoutCallBack() {
      const appStore = useAppStore();
      this.resetInfo();
      clearToken();
      removeRouteListener();
      appStore.clearServerMenu();
    },

    async logout() {
      try {
        await userLogout();
      } finally {
        this.logoutCallBack();
      }
    },
  },
});

export default useUserStore;
