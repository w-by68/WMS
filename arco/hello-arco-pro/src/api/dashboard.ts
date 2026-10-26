import { get } from './http';

/** 看板：分类库存占比 */
export interface CategoryStockItem {
  categoryName: string;
  quantity: number;
}

/** 看板：出入库趋势单点 */
export interface TrendPoint {
  date: string;
  inboundQty: number;
  outboundQty: number;
}

export interface OrderStats {
  pending: number;
  today: number;
}

export interface AlertStats {
  unresolved: number;
  shortage: number;
  overstock: number;
  deadStock: number;
  critical: number;
}

export interface WarehouseInventory {
  warehouseId: number;
  warehouseName?: string;
  totalQuantity: number;
  totalValue: number;
}

export interface RecentTransaction {
  id: number;
  transactionNo: string;
  productId: number;
  productName?: string;
  sku?: string;
  bizType: string;
  bizNo?: string;
  changeQuantity: number;
  afterQuantity: number;
  createTime?: string;
}

/** 看板概览（后端 DashboardVO） */
export interface DashboardOverview {
  totalSku: number;
  totalQuantity: number;
  totalValue: number;
  warehouseInventory: WarehouseInventory[];
  alertStats: AlertStats;
  inboundStats: OrderStats;
  outboundStats: OrderStats;
  recentTransactions: RecentTransaction[];
}

export function getDashboardOverview() {
  return get<DashboardOverview>('/api/dashboard/overview');
}

/** 近 N 天出入库趋势（后端已逐日补零） */
export function getDashboardTrend(days = 30) {
  return get<TrendPoint[]>('/api/dashboard/trend', { days });
}

export function getCategoryStock() {
  return get<CategoryStockItem[]>('/api/dashboard/category-stock');
}
