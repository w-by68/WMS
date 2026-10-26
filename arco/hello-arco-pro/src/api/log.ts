import { get, PageQuery, PageResult } from './http';

/** 操作日志（后端 OperationLogVO） */
export interface OperationLogItem {
  id: number;
  userId?: number;
  username?: string;
  module?: string;
  operation?: string;
  method?: string;
  requestUrl?: string;
  ip?: string;
  costMs?: number;
  success: number;
  errorMsg?: string;
  createTime?: string;
}

export interface OperationLogQuery extends PageQuery {
  username?: string;
  module?: string;
  success?: number;
  startTime?: string;
  endTime?: string;
}

export function getOperationLogPage(query: OperationLogQuery) {
  return get<PageResult<OperationLogItem>>('/api/operation-logs', query);
}
