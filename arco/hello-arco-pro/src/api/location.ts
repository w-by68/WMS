import { del, get, post, put, PageQuery, PageResult } from './http';

/** 库位（后端 StorageLocationVO） */
export interface LocationItem {
  id: number;
  warehouseId: number;
  warehouseName?: string;
  code: string;
  zone?: string;
  status: string;
  remark?: string;
  createTime?: string;
}

export interface LocationQuery extends PageQuery {
  warehouseId?: number;
  zone?: string;
  status?: string;
  keyword?: string;
}

export interface LocationSaveData {
  warehouseId: number;
  code: string;
  zone?: string;
  remark?: string;
}

export function getLocationPage(query: LocationQuery) {
  return get<PageResult<LocationItem>>('/api/locations', query);
}

export function getLocation(id: number) {
  return get<LocationItem>(`/api/locations/${id}`);
}

export function getLocationsByWarehouse(warehouseId: number) {
  return get<LocationItem[]>('/api/locations/by-warehouse', { warehouseId });
}

export function createLocation(data: LocationSaveData) {
  return post<LocationItem>('/api/locations', data);
}

export function updateLocation(id: number, data: Omit<LocationSaveData, 'warehouseId' | 'code'>) {
  return put<LocationItem>(`/api/locations/${id}`, data);
}

export function deleteLocation(id: number) {
  return del<void>(`/api/locations/${id}`);
}

export function updateLocationStatus(id: number, status: string) {
  return put<void>(`/api/locations/${id}/status`, undefined, {
    params: { status },
  });
}
