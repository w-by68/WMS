export type RoleType = '' | 'ADMIN' | 'USER';

/** 登录用户信息：字段与后端 LoginVO / UserProfileVO 对齐 */
export interface UserState {
  userId?: number;
  username?: string;
  realName?: string;
  role: RoleType;
  phone?: string;
  email?: string;
  status?: string;
  lastLoginTime?: string;
  avatar?: string;
}
