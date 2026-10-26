import { del, get, post, put, PageQuery, PageResult } from './http';

/** 商品（后端 ProductVO） */
export interface ProductItem {
  id: number;
  sku: string;
  name: string;
  categoryId?: number;
  categoryName?: string;
  spec?: string;
  unit?: string;
  barcode?: string;
  costPrice?: number;
  salePrice?: number;
  status: string;
  createTime?: string;
}

export interface ProductQuery extends PageQuery {
  keyword?: string;
  categoryId?: number;
  status?: string;
}

export interface ProductSaveData {
  sku?: string;
  name: string;
  categoryId?: number;
  spec?: string;
  unit?: string;
  barcode?: string;
  costPrice?: number;
  salePrice?: number;
  status?: string;
}

export function getProductPage(query: ProductQuery) {
  return get<PageResult<ProductItem>>('/api/products', query);
}

export function getProduct(id: number) {
  return get<ProductItem>(`/api/products/${id}`);
}

/** 启用状态的商品下拉（供预警规则/盘点/单据选择） */
export function getProductOptions() {
  return get<ProductItem[]>('/api/products/options');
}

export function createProduct(data: ProductSaveData) {
  return post<ProductItem>('/api/products', data);
}

export function updateProduct(id: number, data: ProductSaveData) {
  return put<ProductItem>(`/api/products/${id}`, data);
}

export function deleteProduct(id: number) {
  return del<void>(`/api/products/${id}`);
}

export function updateProductStatus(id: number, status: string) {
  return put<void>(`/api/products/${id}/status`, undefined, {
    params: { status },
  });
}
