package com.erp.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.QueryPageParam;
import com.erp.common.Result;
import com.erp.dto.PurchaseOrderDto;
import com.erp.entity.PurchaseOrder;
import com.erp.entity.PurchaseOrderItem;
import com.erp.service.PurchaseOrderService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/purchase")
public class PurchaseController {

    @Resource
    private PurchaseOrderService purchaseOrderService;

    @PostMapping("/listPageC1")
    public Result listPageC1(@RequestBody QueryPageParam query) {
        HashMap param = query.getParam();
        String keyword = param == null ? null : (String) param.get("keyword");
        String status = param == null ? null : (String) param.get("status");

        Page<PurchaseOrder> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<PurchaseOrder> qw = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(keyword) && !"null".equals(keyword)) {
            qw.and(w -> w.like(PurchaseOrder::getOrderNo, keyword)
                    .or().like(PurchaseOrder::getSupplierName, keyword)
                    .or().like(PurchaseOrder::getSupplierPhone, keyword)
                    .or().like(PurchaseOrder::getSupplierAddress, keyword));
        }
        if (StringUtils.isNotBlank(status) && !"null".equals(status)) {
            qw.eq(PurchaseOrder::getStatus, status);
        }
        qw.orderByDesc(PurchaseOrder::getId);
        IPage<PurchaseOrder> result = purchaseOrderService.page(page, qw);
        return Result.suc(result.getRecords(), result.getTotal());
    }

    @GetMapping("/detail")
    public Result detail(@RequestParam Long id) {
        PurchaseOrderDto dto = purchaseOrderService.getDetail(id);
        return dto == null ? Result.fail("记录不存在") : Result.suc(dto);
    }

    @PostMapping("/save")
    public Result save(@RequestBody PurchaseOrderDto dto) {
        dto.setStatus(normalizeStatus(dto.getStatus()));
        String mainErr = validatePurchaseMain(dto);
        if (mainErr != null) {
            return Result.fail(mainErr);
        }
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            return Result.fail("明细不能为空");
        }
        String itemErr = validatePurchaseItems(dto);
        if (itemErr != null) {
            return Result.fail(itemErr);
        }
        try {
            return purchaseOrderService.saveWithItems(dto) ? Result.suc() : Result.fail();
        } catch (IllegalStateException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/update")
    public Result update(@RequestBody PurchaseOrderDto dto) {
        if (dto.getId() == null) {
            return Result.fail("缺少订单ID");
        }
        dto.setStatus(normalizeStatus(dto.getStatus()));
        String mainErr = validatePurchaseMain(dto);
        if (mainErr != null) {
            return Result.fail(mainErr);
        }
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            return Result.fail("明细不能为空");
        }
        String itemErr = validatePurchaseItems(dto);
        if (itemErr != null) {
            return Result.fail(itemErr);
        }
        try {
            return purchaseOrderService.updateWithItems(dto) ? Result.suc() : Result.fail();
        } catch (IllegalStateException e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 审批通过：body 含 id、operatorId（当前登录用户 id）
     */
    @PostMapping("/approve")
    public Result approve(@RequestBody Map<String, Object> body) {
        if (body == null || body.get("id") == null || body.get("operatorId") == null) {
            return Result.fail("缺少 id 或 operatorId");
        }
        Long id = Long.valueOf(body.get("id").toString());
        Integer operatorId = Integer.valueOf(body.get("operatorId").toString());
        try {
            purchaseOrderService.approve(id, operatorId);
            return Result.suc();
        } catch (IllegalStateException e) {
            return Result.fail(e.getMessage());
        }
    }

    @DeleteMapping("/del")
    public Result del(@RequestParam String id) {
        return purchaseOrderService.removeById(id) ? Result.suc() : Result.fail();
    }

    private static String validatePurchaseMain(PurchaseOrderDto dto) {
        if (StringUtils.isBlank(dto.getOrderNo())) {
            return "订单号不能为空";
        }
        if (dto.getOrderDate() == null) {
            return "请选择采购日期";
        }
        if (StringUtils.isBlank(dto.getSupplierName())) {
            return "请输入供应商名称";
        }
        if (StringUtils.isBlank(dto.getSupplierAddress())) {
            return "请输入供应商地址";
        }
        if (StringUtils.isBlank(dto.getStatus())) {
            return "请选择订单状态";
        }
        if (!"draft".equals(dto.getStatus())
                && !"pending".equals(dto.getStatus())
                && !"completed".equals(dto.getStatus())
                && !"cancelled".equals(dto.getStatus())) {
            return "订单状态不合法";
        }
        return null;
    }

    /** 明细校验，避免 NOT NULL 等约束落到数据库后表现为 500 */
    private static String validatePurchaseItems(PurchaseOrderDto dto) {
        for (PurchaseOrderItem it : dto.getItems()) {
            if (it == null || StringUtils.isBlank(it.getProductName())) {
                return "每行明细需填写商品名称";
            }
            if (it.getQuantity() == null || it.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                return "每行明细数量需大于 0";
            }
        }
        return null;
    }

    private static String normalizeStatus(String raw) {
        if (StringUtils.isBlank(raw)) {
            return raw;
        }
        String s = raw.trim().toLowerCase();
        if ("草稿".equals(s)) {
            return "draft";
        }
        if ("待审核".equals(s)) {
            return "pending";
        }
        if ("已完成".equals(s)) {
            return "completed";
        }
        if ("已取消".equals(s)) {
            return "cancelled";
        }
        return s;
    }
}
