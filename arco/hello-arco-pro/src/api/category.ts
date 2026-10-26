import { del, get, post, put } from './http';

/** 商品分类（后端 ProductCategoryVO，树形） */
export interface CategoryItem {
  id: number;
  name: string;
  parentId?: number;
  sort?: number;
  createTime?: string;
  children?: CategoryItem[];
}

export interface CategorySaveData {
  name: string;
  parentId?: number;
  sort?: number;
}

/** 分类树（后端仅提供 /tree，下拉也用同一份数据） */
export function getCategoryTree() {
  return get<CategoryItem[]>('/api/product-categories/tree');
}

export function getCategory(id: number) {
  return get<CategoryItem>(`/api/product-categories/${id}`);
}

export function createCategory(data: CategorySaveData) {
  return post<CategoryItem>('/api/product-categories', data);
}

export function updateCategory(id: number, data: CategorySaveData) {
  return put<CategoryItem>(`/api/product-categories/${id}`, data);
}

export function deleteCategory(id: number) {
  return del<void>(`/api/product-categories/${id}`);
}
