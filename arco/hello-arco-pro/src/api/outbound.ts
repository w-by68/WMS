import { del, get, post, put, PageQuery, PageResult } from './http';
import type { OrderItem } from './inbound';

/** 出库单（后端 OutboundOrderVO） */
export interface OutboundOrderItem {
  id: number;
  orderNo: string;
  type: string;
  warehouseId: number;
  warehouseName?: string;
  customerId?: number;
  customerName?: string;
  status: string;
  totalQuantity?: number;
  totalAmount?: number;
  applicantId?: number;
  applicantName?: string;
  auditorId?: number;
  auditTime?: string;
  auditRemark?: string;
  remark?: string;
  createTime?: string;
  items?: OrderItem[];
}

export interface OutboundOrderQuery extends PageQuery {
  status?: string;
  type?: string;
  keyword?: string;
}

export interface OutboundOrderSaveData {
  type: string;
  warehouseId: number;
  customerId?: number;
  remark?: string;
  items: {
    productId: number;
    locationId: number;
    quantity: number;
    price?: number;
  }[];
}

export function getOutboundOrderPage(query: OutboundOrderQuery) {
  return get<PageResult<OutboundOrderItem>>('/api/outbound-orders', query);
}

export function getOutboundOrder(id: number) {
  return get<OutboundOrderItem>(`/api/outbound-orders/${id}`);
}

export function createOutboundOrder(data: OutboundOrderSaveData) {
  return post<OutboundOrderItem>('/api/outbound-orders', data);
}

export function updateOutboundOrder(id: number, data: OutboundOrderSaveData) {
  return put<OutboundOrderItem>(`/api/outbound-orders/${id}`, data);
}

export function deleteOutboundOrder(id: number) {
  return del<void>(`/api/outbound-orders/${id}`);
}

export function submitOutboundOrder(id: number) {
  return put<void>(`/api/outbound-orders/${id}/submit`);
}

export function approveOutboundOrder(id: number) {
  return put<void>(`/api/outbound-orders/${id}/approve`);
}

export function rejectOutboundOrder(id: number, auditRemark: string) {
  return put<void>(`/api/outbound-orders/${id}/reject`, { auditRemark });
}

export function cancelOutboundOrder(id: number) {
  return put<void>(`/api/outbound-orders/${id}/cancel`);
}
