import { DEFAULT_LAYOUT } from '../base';
import { AppRouteRecordRaw } from '../types';

/**
 * 系统管理（仅管理员可见）：用户 / 仓库 / 库位 / 分类 / 商品 / 供应商 / 客户 / 预警规则 / 盘点 / 操作日志
 */
const ADMIN: AppRouteRecordRaw = {
  path: '/admin',
  name: 'Admin',
  component: DEFAULT_LAYOUT,
  meta: {
    locale: 'menu.admin',
    requiresAuth: true,
    icon: 'icon-settings',
    roles: ['ADMIN'],
    order: 9,
  },
  children: [
    {
      path: 'users',
      name: 'AdminUser',
      component: () => import('@/views/admin/UserManage.vue'),
      meta: {
        locale: 'menu.admin.users',
        requiresAuth: true,
        roles: ['ADMIN'],
      },
    },
    {
      path: 'warehouses',
      name: 'AdminWarehouse',
      component: () => import('@/views/admin/Warehouse.vue'),
      meta: {
        locale: 'menu.admin.warehouses',
        requiresAuth: true,
        roles: ['ADMIN'],
      },
    },
    {
      path: 'locations',
      name: 'AdminLocation',
      component: () => import('@/views/admin/Location.vue'),
      meta: {
        locale: 'menu.admin.locations',
        requiresAuth: true,
        roles: ['ADMIN'],
      },
    },
    {
      path: 'categories',
      name: 'AdminCategory',
      component: () => import('@/views/admin/Category.vue'),
      meta: {
        locale: 'menu.admin.categories',
        requiresAuth: true,
        roles: ['ADMIN'],
      },
    },
    {
      path: 'products',
      name: 'AdminProduct',
      component: () => import('@/views/admin/Product.vue'),
      meta: {
        locale: 'menu.admin.products',
        requiresAuth: true,
        roles: ['ADMIN'],
      },
    },
    {
      path: 'suppliers',
      name: 'AdminSupplier',
      component: () => import('@/views/admin/Supplier.vue'),
      meta: {
        locale: 'menu.admin.suppliers',
        requiresAuth: true,
        roles: ['ADMIN'],
      },
    },
    {
      path: 'customers',
      name: 'AdminCustomer',
      component: () => import('@/views/admin/Customer.vue'),
      meta: {
        locale: 'menu.admin.customers',
        requiresAuth: true,
        roles: ['ADMIN'],
      },
    },
    {
      path: 'alert-rules',
      name: 'AdminAlertRule',
      component: () => import('@/views/admin/AlertRule.vue'),
      meta: {
        locale: 'menu.admin.alertRules',
        requiresAuth: true,
        roles: ['ADMIN'],
      },
    },
    {
      path: 'stock-adjust',
      name: 'AdminStockAdjust',
      component: () => import('@/views/admin/StockAdjust.vue'),
      meta: {
        locale: 'menu.admin.stockAdjust',
        requiresAuth: true,
        roles: ['ADMIN'],
      },
    },
    {
      path: 'operation-logs',
      name: 'AdminOperationLog',
      component: () => import('@/views/admin/OperationLog.vue'),
      meta: {
        locale: 'menu.admin.operationLogs',
        requiresAuth: true,
        roles: ['ADMIN'],
      },
    },
  ],
};

export default ADMIN;
