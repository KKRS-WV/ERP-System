package com.erp.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.common.QueryPageParam;
import com.erp.dto.StockIoRequest;
import com.erp.entity.WarehouseStock;
import com.erp.entity.WarehouseStockFlow;

public interface WarehouseService {

    IPage<WarehouseStock> pageStock(QueryPageParam query);

    IPage<WarehouseStockFlow> pageFlow(QueryPageParam query);

    WarehouseStock getStock(Long id);

    boolean saveStock(WarehouseStock stock);

    boolean updateStock(WarehouseStock stock);

    void inbound(StockIoRequest req);

    void outbound(StockIoRequest req);

    void submitInboundApproval(StockIoRequest req);

    void submitOutboundApproval(StockIoRequest req);

    void approveFlow(Long flowId, Integer operatorId);

    void rejectFlow(Long flowId, Integer operatorId);
}
