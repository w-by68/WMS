import { del, get, post, put, PageQuery, PageResult } from './http';

/** 单据明细（入库/出库共用结构） */
export interface OrderItem {
  id?: number;
  productId: number;
  productName?: string;
  sku?: string;
  locationId: number;
  locationCode?: string;
  quantity: number;
  price?: number;
  amount?: number;
}

/** 入库单（后端 InboundOrderVO） */
export interface InboundOrderItem {
  id: number;
  orderNo: string;
  type: string;
  warehouseId: number;
  warehouseName?: string;
  supplierId?: number;
  supplierName?: string;
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

export interface InboundOrderQuery extends PageQuery {
  status?: string;
  type?: string;
  keyword?: string;
}

export interface InboundOrderSaveData {
  type: string;
  warehouseId: number;
  supplierId?: number;
  remark?: string;
  items: {
    productId: number;
    locationId: number;
    quantity: number;
    price?: number;
  }[];
}

export function getInboundOrderPage(query: InboundOrderQuery) {
  return get<PageResult<InboundOrderItem>>('/api/inbound-orders', query);
}

export function getInboundOrder(id: number) {
  return get<InboundOrderItem>(`/api/inbound-orders/${id}`);
}

export function createInboundOrder(data: InboundOrderSaveData) {
  return post<InboundOrderItem>('/api/inbound-orders', data);
}

export function updateInboundOrder(id: number, data: InboundOrderSaveData) {
  return put<InboundOrderItem>(`/api/inbound-orders/${id}`, data);
}

export function deleteInboundOrder(id: number) {
  return del<void>(`/api/inbound-orders/${id}`);
}

export function submitInboundOrder(id: number) {
  return put<void>(`/api/inbound-orders/${id}/submit`);
}

export function approveInboundOrder(id: number) {
  return put<void>(`/api/inbound-orders/${id}/approve`);
}

export function rejectInboundOrder(id: number, auditRemark: string) {
  return put<void>(`/api/inbound-orders/${id}/reject`, { auditRemark });
}

export function cancelInboundOrder(id: number) {
  return put<void>(`/api/inbound-orders/${id}/cancel`);
}
