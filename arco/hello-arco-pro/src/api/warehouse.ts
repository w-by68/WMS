import { del, get, post, put, PageQuery, PageResult } from './http';

/** 仓库（后端 WarehouseVO） */
export interface WarehouseItem {
  id: number;
  code: string;
  name: string;
  address?: string;
  manager?: string;
  phone?: string;
  status: string;
  createTime?: string;
}

export interface WarehouseQuery extends PageQuery {
  keyword?: string;
  status?: string;
}

export interface WarehouseSaveData {
  code?: string;
  name: string;
  address?: string;
  manager?: string;
  phone?: string;
  status?: string;
}

export function getWarehousePage(query: WarehouseQuery) {
  return get<PageResult<WarehouseItem>>('/api/warehouses', query);
}

export function getWarehouse(id: number) {
  return get<WarehouseItem>(`/api/warehouses/${id}`);
}

/** 启用状态的仓库下拉（供库位/盘点/单据选择） */
export function getWarehouseOptions() {
  return get<WarehouseItem[]>('/api/warehouses/options');
}

export function createWarehouse(data: WarehouseSaveData) {
  return post<WarehouseItem>('/api/warehouses', data);
}

export function updateWarehouse(id: number, data: WarehouseSaveData) {
  return put<WarehouseItem>(`/api/warehouses/${id}`, data);
}

export function deleteWarehouse(id: number) {
  return del<void>(`/api/warehouses/${id}`);
}

export function updateWarehouseStatus(id: number, status: string) {
  return put<void>(`/api/warehouses/${id}/status`, undefined, {
    params: { status },
  });
}
