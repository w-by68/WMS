import { del, get, post, put, PageQuery, PageResult } from './http';

/** 客户（后端 CustomerVO） */
export interface CustomerItem {
  id: number;
  code: string;
  name: string;
  contact?: string;
  phone?: string;
  address?: string;
  status: string;
  createTime?: string;
}

export interface CustomerQuery extends PageQuery {
  keyword?: string;
  status?: string;
}

export interface CustomerSaveData {
  code?: string;
  name: string;
  contact?: string;
  phone?: string;
  address?: string;
  status?: string;
}

export function getCustomerPage(query: CustomerQuery) {
  return get<PageResult<CustomerItem>>('/api/customers', query);
}

export function getCustomer(id: number) {
  return get<CustomerItem>(`/api/customers/${id}`);
}

export function getCustomerOptions() {
  return get<CustomerItem[]>('/api/customers/options');
}

export function createCustomer(data: CustomerSaveData) {
  return post<CustomerItem>('/api/customers', data);
}

export function updateCustomer(id: number, data: CustomerSaveData) {
  return put<CustomerItem>(`/api/customers/${id}`, data);
}

export function deleteCustomer(id: number) {
  return del<void>(`/api/customers/${id}`);
}

export function updateCustomerStatus(id: number, status: string) {
  return put<void>(`/api/customers/${id}/status`, undefined, {
    params: { status },
  });
}
