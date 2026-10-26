import localeMessageBox from '@/components/message-box/locale/en-US';
import localeLogin from '@/views/login/locale/en-US';

import localeSettings from './en-US/settings';

export default {
  'menu.dashboard': 'Dashboard',
  'menu.dashboard.workplace': 'Overview',
  'menu.admin': 'System',
  'menu.admin.users': 'Users',
  'menu.admin.warehouses': 'Warehouses',
  'menu.admin.locations': 'Locations',
  'menu.admin.categories': 'Categories',
  'menu.admin.products': 'Products',
  'menu.admin.suppliers': 'Suppliers',
  'menu.admin.customers': 'Customers',
  'menu.admin.alertRules': 'Alert Rules',
  'menu.admin.stockAdjust': 'Stock Adjust',
  'menu.admin.operationLogs': 'Operation Logs',
  'menu.inventory': 'Inventory',
  'menu.alert': 'Alerts',
  'menu.inbound': 'Inbound Orders',
  'menu.outbound': 'Outbound Orders',
  'menu.user': 'User Center',
  'navbar.action.locale': 'Switch to English',
  ...localeSettings,
  ...localeMessageBox,
  ...localeLogin,
};
