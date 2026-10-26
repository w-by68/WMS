import { del, get, post, put, PageQuery, PageResult } from './http';

/** 供应商（后端 SupplierVO） */
export interface SupplierItem {
  id: number;
  code: string;
  name: string;
  contact?: string;
  phone?: string;
  address?: string;
  status: string;
  createTime?: string;
}

export interface SupplierQuery extends PageQuery {
  keyword?: string;
  status?: string;
}

export interface SupplierSaveData {
  code?: string;
  name: string;
  contact?: string;
  phone?: string;
  address?: string;
  status?: string;
}

export function getSupplierPage(query: SupplierQuery) {
  return get<PageResult<SupplierItem>>('/api/suppliers', query);
}

export function getSupplier(id: number) {
  return get<SupplierItem>(`/api/suppliers/${id}`);
}

export function getSupplierOptions() {
  return get<SupplierItem[]>('/api/suppliers/options');
}

export function createSupplier(data: SupplierSaveData) {
  return post<SupplierItem>('/api/suppliers', data);
}

export function updateSupplier(id: number, data: SupplierSaveData) {
  return put<SupplierItem>(`/api/suppliers/${id}`, data);
}

export function deleteSupplier(id: number) {
  return del<void>(`/api/suppliers/${id}`);
}

export function updateSupplierStatus(id: number, status: string) {
  return put<void>(`/api/suppliers/${id}/status`, undefined, {
    params: { status },
  });
}
