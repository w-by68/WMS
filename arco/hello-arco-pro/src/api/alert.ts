import { get, post, PageQuery, PageResult } from './http';

/** 预警记录（后端 AlertVO） */
export interface AlertItem {
  id: number;
  alertNo: string;
  ruleId?: number;
  productId: number;
  productName?: string;
  sku?: string;
  warehouseId: number;
  warehouseName?: string;
  alertType: string;
  level: string;
  currentQuantity: number;
  thresholdValue?: number;
  suggestQuantity?: number;
  status: string;
  handlerId?: number;
  handlerName?: string;
  handleTime?: string;
  handleRemark?: string;
  createTime?: string;
}

export interface AlertQuery extends PageQuery {
  alertType?: string;
  level?: string;
  status?: string;
  productId?: number;
  warehouseId?: number;
}

export function getAlertPage(query: AlertQuery) {
  return get<PageResult<AlertItem>>('/api/alerts', query);
}

export function getAlert(id: number) {
  return get<AlertItem>(`/api/alerts/${id}`);
}

/** 处理预警：status = RESOLVED / IGNORED */
export function handleAlert(id: number, data: { status: string; handleRemark?: string }) {
  return post<void>(`/api/alerts/${id}/handle`, data);
}

/** 批量处理所有未处理预警，返回处理条数 */
export function batchHandleAlerts(data: { status: string; handleRemark?: string }) {
  return post<number>('/api/alerts/batch-handle', data);
}

/** 手动触发一次预警扫描，返回新增/更新条数 */
export function scanAlerts() {
  return post<number>('/api/alerts/scan');
}
