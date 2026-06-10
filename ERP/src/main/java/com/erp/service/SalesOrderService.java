package com.erp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.erp.dto.SalesOrderDto;
import com.erp.entity.SalesOrder;

public interface SalesOrderService extends IService<SalesOrder> {

    SalesOrderDto getDetail(Long id);

    boolean saveWithItems(SalesOrderDto dto);

    boolean updateWithItems(SalesOrderDto dto);

    /**
     * 待审核 → 已完成，并按明细执行出库（仅管理员/超管可调用）
     */
    void approve(Long orderId, Integer operatorId);
}
