package com.erp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.erp.dto.PurchaseOrderDto;
import com.erp.entity.PurchaseOrder;

public interface PurchaseOrderService extends IService<PurchaseOrder> {

    PurchaseOrderDto getDetail(Long id);

    boolean saveWithItems(PurchaseOrderDto dto);

    boolean updateWithItems(PurchaseOrderDto dto);

    /**
     * 待审核 → 已完成，并按明细执行入库（仅管理员/超管可调用）
     */
    void approve(Long orderId, Integer operatorId);
}
