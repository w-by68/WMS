import { get, post, put } from './http';

export interface LoginData {
  username: string;
  password: string;
}

/** 登录返回体（后端 LoginVO） */
export interface LoginResult {
  token: string;
  userId: number;
  username: string;
  realName: string;
  role: string;
}

/** 当前登录人信息（后端 UserProfileVO） */
export interface ProfileResult {
  id: number;
  username: string;
  realName: string;
  role: string;
  phone?: string;
  email?: string;
  status: string;
  lastLoginTime?: string;
}

export function login(data: LoginData) {
  return post<LoginResult>('/api/auth/login', data);
}

export function logout() {
  return post<void>('/api/auth/logout');
}

export function getProfile() {
  return get<ProfileResult>('/api/auth/profile');
}

/** 修改自己的资料：仅姓名/手机/邮箱（账号与角色由管理员维护） */
export function updateProfile(data: {
  realName: string;
  phone?: string;
  email?: string;
}) {
  return put<void>('/api/auth/profile', data);
}

export function changePassword(data: {
  oldPassword: string;
  newPassword: string;
}) {
  return put<void>('/api/auth/password', data);
}
