package com.example.wms.service;

import com.example.wms.common.PageResult;
import com.example.wms.dto.inventory.StockTransactionQueryDTO;
import com.example.wms.vo.inventory.StockTransactionVO;

/** 库存流水查询服务 */
public interface StockTransactionService {

    PageResult<StockTransactionVO> page(StockTransactionQueryDTO query);
}
