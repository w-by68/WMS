/**
 * 状态字典与展示格式化：与 00-全局约定 第七节保持一致。
 * 页面统一从这里取 label / 颜色，避免各页面硬编码中文与颜色。
 */

export interface DictItem {
  label: string;
  /** Arco Tag 的 color：预置色名或 CSS 颜色 */
  color?: string;
}

/** 用户角色 */
export const ROLE_DICT: Record<string, DictItem> = {
  ADMIN: { label: '管理员', color: 'red' },
  USER: { label: '普通用户', color: 'arcoblue' },
};

/** 启用状态（用户 / 仓库 / 商品 / 供应商 / 客户） */
export const ENABLE_DICT: Record<string, DictItem> = {
  ENABLED: { label: '启用', color: 'green' },
  DISABLED: { label: '停用', color: 'gray' },
};

/** 库位状态 */
export const LOCATION_STATUS_DICT: Record<string, DictItem> = {
  EMPTY: { label: '空闲', color: 'gray' },
  USING: { label: '占用', color: 'arcoblue' },
  DISABLED: { label: '禁用', color: 'red' },
};

/** 单据状态（入库单 / 出库单） */
export const ORDER_STATUS_DICT: Record<string, DictItem> = {
  DRAFT: { label: '草稿', color: 'gray' },
  PENDING: { label: '待审核', color: 'orange' },
  APPROVED: { label: '已通过', color: 'green' },
  REJECTED: { label: '已驳回', color: 'red' },
  CANCELED: { label: '已作废', color: 'gray' },
};

/** 入库类型 */
export const INBOUND_TYPE_DICT: Record<string, DictItem> = {
  PURCHASE: { label: '采购入库' },
  RETURN: { label: '退货入库' },
  ADJUST: { label: '调整入库' },
};

/** 出库类型 */
export const OUTBOUND_TYPE_DICT: Record<string, DictItem> = {
  SALE: { label: '销售出库' },
  PICK: { label: '领用出库' },
  SCRAP: { label: '报损出库' },
};

/** 库存流水类型 */
export const BIZ_TYPE_DICT: Record<string, DictItem> = {
  INBOUND: { label: '入库', color: 'green' },
  OUTBOUND: { label: '出库', color: 'red' },
  LOCK: { label: '锁定', color: 'orange' },
  UNLOCK: { label: '解锁', color: 'arcoblue' },
  ADJUST: { label: '盘点调整', color: 'purple' },
};

/** 预警类型 */
export const ALERT_TYPE_DICT: Record<string, DictItem> = {
  SHORTAGE: { label: '缺货', color: 'red' },
  OVERSTOCK: { label: '积压', color: 'orange' },
  DEAD_STOCK: { label: '呆滞', color: 'purple' },
};

/** 预警级别 */
export const ALERT_LEVEL_DICT: Record<string, DictItem> = {
  INFO: { label: '提示', color: 'gray' },
  WARNING: { label: '警告', color: 'orange' },
  CRITICAL: { label: '严重', color: 'red' },
};

/** 预警处理状态 */
export const ALERT_STATUS_DICT: Record<string, DictItem> = {
  UNRESOLVED: { label: '未处理', color: 'red' },
  RESOLVED: { label: '已处理', color: 'green' },
  IGNORED: { label: '已忽略', color: 'gray' },
};

/** 操作日志结果 */
export const SUCCESS_DICT: Record<string, DictItem> = {
  '1': { label: '成功', color: 'green' },
  '0': { label: '失败', color: 'red' },
};

/** 通用取字典 label，取不到时原样返回，避免页面出现空白 */
export function dictLabel(dict: Record<string, DictItem>, value?: string | number) {
  if (value === undefined || value === null || value === '') return '-';
  return dict[String(value)]?.label ?? String(value);
}

/** 通用取字典 color */
export function dictColor(dict: Record<string, DictItem>, value?: string | number) {
  if (value === undefined || value === null || value === '') return undefined;
  return dict[String(value)]?.color;
}

/**
 * 时间格式化：统一 yyyy-MM-dd HH:mm:ss。
 * 后端 LocalDateTime 可能被序列化成 ISO 字符串、数组或时间戳，这里都兼容。
 */
export function formatDateTime(value?: unknown): string {
  if (value === undefined || value === null || value === '') return '-';
  if (Array.isArray(value)) {
    const [y, m, d, h = 0, mi = 0, s = 0] = value as number[];
    return `${y}-${pad(m)}-${pad(d)} ${pad(h)}:${pad(mi)}:${pad(s)}`;
  }
  if (typeof value === 'number') {
    const date = new Date(value);
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(
      date.getHours()
    )}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
  }
  const str = String(value).replace('T', ' ');
  return str.length > 19 ? str.slice(0, 19) : str;
}

function pad(n: number) {
  return String(n).padStart(2, '0');
}
