import axios from 'axios';
import type { AxiosRequestConfig, AxiosResponse } from 'axios';
import { Message } from '@arco-design/web-vue';
import { useUserStore } from '@/store';
import { getToken, clearToken } from '@/utils/auth';

/** 后端统一返回体（00-全局约定 第六节）：HTTP 恒为 200，业务码在 code 上 */
export interface HttpResponse<T = unknown> {
  code: number;
  message: string;
  data: T;
  timestamp: number;
}

if (import.meta.env.VITE_API_BASE_URL) {
  axios.defaults.baseURL = import.meta.env.VITE_API_BASE_URL;
}

axios.interceptors.request.use(
  (config: AxiosRequestConfig) => {
    const token = getToken();
    if (token) {
      if (!config.headers) {
        config.headers = {};
      }
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

axios.interceptors.response.use(
  (response: AxiosResponse<HttpResponse>) => {
    const res = response.data;
    // code === 200：直接返回业务数据，页面里无需层层 .data.data
    if (res.code === 200) {
      return res.data as never;
    }
    Message.error({ content: res.message || '请求失败', duration: 5 * 1000 });
    // 1002：未认证 / token 失效 → 清 token 并回登录页
    if (res.code === 1002) {
      clearToken();
      useUserStore().resetInfo();
      if (!window.location.pathname.startsWith('/login')) {
        window.location.href = '/login';
      }
    }
    return Promise.reject(new Error(res.message || '请求失败'));
  },
  (error) => {
    Message.error({ content: error.message || '网络异常', duration: 5 * 1000 });
    return Promise.reject(error);
  }
);
