import { del, get, post, put, PageQuery, PageResult } from './http';

/** 用户（后端 UserVO） */
export interface UserItem {
  id: number;
  username: string;
  realName: string;
  role: string;
  phone?: string;
  email?: string;
  status: string;
  lastLoginTime?: string;
  createTime?: string;
}

export interface UserQuery extends PageQuery {
  keyword?: string;
  role?: string;
  status?: string;
}

export interface UserSaveData {
  username?: string;
  password?: string;
  realName: string;
  role: string;
  phone?: string;
  email?: string;
  status?: string;
}

export function getUserPage(query: UserQuery) {
  return get<PageResult<UserItem>>('/api/users', query);
}

export function getUser(id: number) {
  return get<UserItem>(`/api/users/${id}`);
}

export function createUser(data: UserSaveData) {
  return post<UserItem>('/api/users', data);
}

export function updateUser(id: number, data: UserSaveData) {
  return put<UserItem>(`/api/users/${id}`, data);
}

export function deleteUser(id: number) {
  return del<void>(`/api/users/${id}`);
}

export function updateUserStatus(id: number, status: string) {
  return put<void>(`/api/users/${id}/status`, undefined, { params: { status } });
}

export function resetUserPassword(id: number, newPassword: string) {
  return put<void>(`/api/users/${id}/password`, { newPassword });
}
