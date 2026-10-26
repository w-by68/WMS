import { del, get, post, put, PageQuery, PageResult } from './http';

/** 预警规则（后端 AlertRuleVO） */
export interface AlertRuleItem {
  id: number;
  productId: number;
  productName?: string;
  sku?: string;
  warehouseId?: number;
  warehouseName?: string;
  /** 是否全局规则（不绑定具体商品/仓库） */
  global?: boolean;
  safetyStock?: number;
  maxStock?: number;
  deadStockDays?: number;
  enabled: number;
  createTime?: string;
  updateTime?: string;
}

export interface AlertRuleQuery extends PageQuery {
  productId?: number;
  warehouseId?: number;
  enabled?: number;
}

export interface AlertRuleSaveData {
  productId?: number;
  warehouseId?: number;
  safetyStock?: number;
  maxStock?: number;
  deadStockDays?: number;
  enabled?: number;
}

export function getAlertRulePage(query: AlertRuleQuery) {
  return get<PageResult<AlertRuleItem>>('/api/alert-rules', query);
}

export function getAlertRule(id: number) {
  return get<AlertRuleItem>(`/api/alert-rules/${id}`);
}

export function createAlertRule(data: AlertRuleSaveData) {
  return post<AlertRuleItem>('/api/alert-rules', data);
}

export function updateAlertRule(id: number, data: AlertRuleSaveData) {
  return put<AlertRuleItem>(`/api/alert-rules/${id}`, data);
}

export function deleteAlertRule(id: number) {
  return del<void>(`/api/alert-rules/${id}`);
}

/** 启停规则：enabled = 1 启用 / 0 停用 */
export function toggleAlertRule(id: number, enabled: number) {
  return put<void>(`/api/alert-rules/${id}/enabled`, undefined, {
    params: { enabled },
  });
}
