import localeMessageBox from '@/components/message-box/locale/zh-CN';
import localeLogin from '@/views/login/locale/zh-CN';

import localeSettings from './zh-CN/settings';

export default {
  'menu.dashboard': '首页',
  'menu.dashboard.workplace': '数据看板',
  'menu.admin': '系统管理',
  'menu.admin.users': '用户管理',
  'menu.admin.warehouses': '仓库管理',
  'menu.admin.locations': '库位管理',
  'menu.admin.categories': '商品分类',
  'menu.admin.products': '商品管理',
  'menu.admin.suppliers': '供应商管理',
  'menu.admin.customers': '客户管理',
  'menu.admin.alertRules': '预警规则',
  'menu.admin.stockAdjust': '库存盘点',
  'menu.admin.operationLogs': '操作日志',
  'menu.inventory': '库存查询',
  'menu.alert': '库存预警',
  'menu.inbound': '入库单',
  'menu.outbound': '出库单',
  'menu.user': '个人中心',
  'navbar.action.locale': '切换为中文',
  ...localeSettings,
  ...localeMessageBox,
  ...localeLogin,
};
