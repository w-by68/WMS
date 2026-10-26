import { get, post, PageQuery, PageResult } from './http';

/** 库存（后端 InventoryVO） */
export interface InventoryItem {
  id: number;
  productId: number;
  productName?: string;
  sku?: string;
  /** 商品规格 */
  spec?: string;
  warehouseId: number;
  warehouseName?: string;
  locationId?: number;
  locationCode?: string;
  quantity: number;
  lockedQuantity: number;
  availableQuantity: number;
  /** 安全库存下限（取自预警规则；0 表示未设置下限） */
  safetyStock?: number;
  /** 是否低于安全库存（可用库存 < 安全库存），用于整行标红 */
  shortage?: boolean;
  version?: number;
  lastInboundTime?: string;
  lastOutboundTime?: string;
}

export interface InventoryQuery extends PageQuery {
  productId?: number;
  warehouseId?: number;
  locationId?: number;
  keyword?: string;
  /** 只看低于安全库存的记录 */
  onlyShortage?: boolean;
}

/** 库存流水（后端 StockTransactionVO） */
export interface StockTransactionItem {
  id: number;
  transactionNo: string;
  productId: number;
  productName?: string;
  warehouseId: number;
  warehouseName?: string;
  locationId?: number;
  locationCode?: string;
  bizType: string;
  bizNo?: string;
  changeQuantity: number;
  beforeQuantity: number;
  afterQuantity: number;
  operatorId?: number;
  remark?: string;
  createTime?: string;
}

export interface StockTransactionQuery extends PageQuery {
  productId?: number;
  bizType?: string;
  bizNo?: string;
  startTime?: string;
  endTime?: string;
}

export function getInventoryPage(query: InventoryQuery) {
  return get<PageResult<InventoryItem>>('/api/inventory', query);
}

export function getInventory(id: number) {
  return get<InventoryItem>(`/api/inventory/${id}`);
}

/**
 * 查某商品在某库位的库存记录（制单页实时显示可用库存用）。
 * 后端没有 `/api/inventories/{productId}/locations` 这种路径，
 * 用分页接口按 productId + locationId 过滤即可。
 */
export function getInventoryByProductLocation(
  productId: number,
  locationId: number
) {
  return get<PageResult<InventoryItem>>('/api/inventory', {
    productId,
    locationId,
    pageNum: 1,
    pageSize: 1,
  }).then((res) => (res.list?.length ? res.list[0] : null));
}

/** 查某商品在各库位/各仓库的库存分布（库存查询页点商品名展开用） */
export function getInventoryByProduct(productId: number) {
  return get<PageResult<InventoryItem>>('/api/inventory', {
    productId,
    pageNum: 1,
    pageSize: 100,
  });
}

/** 库存盘点调整：把某商品在某库位的账面数量改成实际数量 */
export function adjustInventory(data: {
  productId: number;
  locationId: number;
  actualQuantity: number;
  remark?: string;
}) {
  return post<void>('/api/inventory/adjust', data);
}

export function getStockTransactionPage(query: StockTransactionQuery) {
  return get<PageResult<StockTransactionItem>>('/api/stock-transactions', query);
}
