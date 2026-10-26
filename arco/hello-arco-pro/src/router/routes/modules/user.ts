import { DEFAULT_LAYOUT } from '../base';
import { AppRouteRecordRaw } from '../types';
import type { Component } from '../types';

/**
 * 用户端页面（管理员与普通用户共用）：
 * 库存查询 / 库存预警 / 入库单 / 出库单 / 个人中心。
 *
 * 这 5 个都是"单页菜单"：父级只挂 layout 且 hideChildrenInMenu=true（否则
 * use-menu-tree 会把整个节点丢掉），真正的页面挂在子路由 index 上，
 * 父级 redirect 到 index —— 菜单点击是按父级的 name 跳转的，靠 redirect 落到页面。
 * extras 用于挂不入菜单的附加页（如制单页 new / edit/:id）。
 */
function leaf(
  path: string,
  name: string,
  locale: string,
  icon: string,
  order: number,
  childName: string,
  view: () => Promise<typeof import('*.vue')>,
  extras: AppRouteRecordRaw[] = []
): AppRouteRecordRaw {
  return {
    path,
    name,
    component: DEFAULT_LAYOUT,
    redirect: `${path}/index`,
    meta: {
      locale,
      requiresAuth: true,
      icon,
      roles: ['ADMIN', 'USER'],
      hideChildrenInMenu: true,
      order,
    },
    children: [
      {
        path: 'index',
        name: childName,
        component: view,
        meta: {
          requiresAuth: true,
          hideInMenu: true,
          roles: ['ADMIN', 'USER'],
        },
      },
      ...extras,
    ],
  };
}

/** 制单页的附加子路由（只在路由表里，不出现在菜单） */
function orderFormChildren(
  formComponent: Component,
  createName: string,
  editName: string
): AppRouteRecordRaw[] {
  const meta = {
    requiresAuth: true,
    hideInMenu: true,
    roles: ['ADMIN', 'USER'],
  };
  return [
    {
      path: 'new',
      name: createName,
      component: formComponent,
      meta,
    },
    {
      path: 'edit/:id',
      name: editName,
      component: formComponent,
      meta,
    },
  ];
}

const USER: AppRouteRecordRaw[] = [
  leaf(
    '/inventory',
    'Inventory',
    'menu.inventory',
    'icon-search',
    1,
    'InventoryIndex',
    () => import('@/views/biz/Inventory.vue')
  ),
  leaf(
    '/alert',
    'Alert',
    'menu.alert',
    'icon-exclamation-circle',
    2,
    'AlertIndex',
    () => import('@/views/biz/Alert.vue')
  ),
  leaf(
    '/inbound-orders',
    'InboundOrder',
    'menu.inbound',
    'icon-download',
    3,
    'InboundOrderIndex',
    () => import('@/views/biz/InboundOrder.vue'),
    orderFormChildren(
      () => import('@/views/biz/InboundOrderForm.vue'),
      'InboundOrderCreate',
      'InboundOrderEdit'
    )
  ),
  leaf(
    '/outbound-orders',
    'OutboundOrder',
    'menu.outbound',
    'icon-upload',
    4,
    'OutboundOrderIndex',
    () => import('@/views/biz/OutboundOrder.vue'),
    orderFormChildren(
      () => import('@/views/biz/OutboundOrderForm.vue'),
      'OutboundOrderCreate',
      'OutboundOrderEdit'
    )
  ),
  leaf(
    '/profile',
    'Profile',
    'menu.user',
    'icon-user',
    5,
    'ProfileIndex',
    () => import('@/views/profile/Index.vue')
  ),
];

export default USER;
