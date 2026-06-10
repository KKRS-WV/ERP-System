package com.erp.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.common.QueryPageParam;
import com.erp.common.Result;
import com.erp.dto.StockIoRequest;
import com.erp.entity.WarehouseStock;
import com.erp.entity.WarehouseStockFlow;
import com.erp.service.WarehouseService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Map;

@RestController
@RequestMapping("/warehouse")
public class WarehouseController {

    @Resource
    private WarehouseService warehouseService;

    @PostMapping("/stock/listPageC1")
    public Result stockListPage(@RequestBody QueryPageParam query) {
        IPage<WarehouseStock> page = warehouseService.pageStock(query);
        return Result.suc(page.getRecords(), page.getTotal());
    }

    @GetMapping("/stock/detail")
    public Result stockDetail(@RequestParam Long id) {
        WarehouseStock s = warehouseService.getStock(id);
        return s == null ? Result.fail("记录不存在") : Result.suc(s);
    }

    @PostMapping("/stock/save")
    public Result stockSave(@RequestBody WarehouseStock stock) {
        return warehouseService.saveStock(stock) ? Result.suc() : Result.fail();
    }

    @PostMapping("/stock/update")
    public Result stockUpdate(@RequestBody WarehouseStock stock) {
        if (stock.getId() == null) {
            return Result.fail("缺少ID");
        }
        return warehouseService.updateStock(stock) ? Result.suc() : Result.fail();
    }

    @PostMapping("/inbound")
    public Result inbound(@RequestBody StockIoRequest req) {
        try {
            warehouseService.inbound(req);
            return Result.suc();
        } catch (IllegalStateException e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 手工入库提交审批：不直接改库存，写入待审批流水（pin）
     */
    @PostMapping("/inbound/apply")
    public Result inboundApply(@RequestBody StockIoRequest req) {
        try {
            warehouseService.submitInboundApproval(req);
            return Result.suc();
        } catch (IllegalStateException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/outbound")
    public Result outbound(@RequestBody StockIoRequest req) {
        try {
            warehouseService.outbound(req);
            return Result.suc();
        } catch (IllegalStateException e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 手工出库提交审批：不直接改库存，写入待审批流水（pout）
     */
    @PostMapping("/outbound/apply")
    public Result outboundApply(@RequestBody StockIoRequest req) {
        try {
            warehouseService.submitOutboundApproval(req);
            return Result.suc();
        } catch (IllegalStateException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/flow/listPageC1")
    public Result flowListPage(@RequestBody QueryPageParam query) {
        IPage<WarehouseStockFlow> page = warehouseService.pageFlow(query);
        return Result.suc(page.getRecords(), page.getTotal());
    }

    /**
     * 仓库手工出入库审批通过（仅管理员/超管）
     */
    @PostMapping("/flow/approve")
    public Result flowApprove(@RequestBody Map<String, Object> body) {
        if (body == null || body.get("id") == null || body.get("operatorId") == null) {
            return Result.fail("缺少 id 或 operatorId");
        }
        try {
            Long id = Long.valueOf(body.get("id").toString());
            Integer operatorId = Integer.valueOf(body.get("operatorId").toString());
            warehouseService.approveFlow(id, operatorId);
            return Result.suc();
        } catch (IllegalStateException e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 仓库手工出入库审批驳回（仅管理员/超管）
     */
    @PostMapping("/flow/reject")
    public Result flowReject(@RequestBody Map<String, Object> body) {
        if (body == null || body.get("id") == null || body.get("operatorId") == null) {
            return Result.fail("缺少 id 或 operatorId");
        }
        try {
            Long id = Long.valueOf(body.get("id").toString());
            Integer operatorId = Integer.valueOf(body.get("operatorId").toString());
            warehouseService.rejectFlow(id, operatorId);
            return Result.suc();
        } catch (IllegalStateException e) {
            return Result.fail(e.getMessage());
        }
    }
}
