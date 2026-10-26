import axios from 'axios';
import type { AxiosRequestConfig } from 'axios';

/** 后端分页返回体（00-全局约定）：list / total / pageNum / pageSize */
export interface PageResult<T> {
  list: T[];
  total: number;
  pageNum: number;
  pageSize: number;
}

/** 分页查询基础参数 */
export interface PageQuery {
  pageNum?: number;
  pageSize?: number;
  [key: string]: unknown;
}

/**
 * 请求便捷方法：响应拦截器已把后端 R 体剥成业务数据，
 * 因此这里的泛型 T 就是接口 data 字段的类型，页面直接用即可。
 */
export function get<T>(
  url: string,
  params?: Record<string, unknown>,
  config?: AxiosRequestConfig
): Promise<T> {
  return axios.get(url, { params, ...config }) as unknown as Promise<T>;
}

export function post<T>(
  url: string,
  data?: unknown,
  config?: AxiosRequestConfig
): Promise<T> {
  return axios.post(url, data, config) as unknown as Promise<T>;
}

export function put<T>(
  url: string,
  data?: unknown,
  config?: AxiosRequestConfig
): Promise<T> {
  return axios.put(url, data, config) as unknown as Promise<T>;
}

export function del<T>(
  url: string,
  params?: Record<string, unknown>,
  config?: AxiosRequestConfig
): Promise<T> {
  return axios.delete(url, { params, ...config }) as unknown as Promise<T>;
}
