package com.erp.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.QueryPageParam;
import com.erp.common.Result;
import com.erp.dto.SalesOrderDto;
import com.erp.entity.SalesOrder;
import com.erp.entity.SalesOrderItem;
import com.erp.service.SalesOrderService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/sales")
public class SalesController {

    @Resource
    private SalesOrderService salesOrderService;

    @PostMapping("/listPageC1")
    public Result listPageC1(@RequestBody QueryPageParam query) {
        HashMap param = query.getParam();
        String keyword = param == null ? null : (String) param.get("keyword");
        String status = param == null ? null : (String) param.get("status");

        Page<SalesOrder> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<SalesOrder> qw = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(keyword) && !"null".equals(keyword)) {
            qw.and(w -> w.like(SalesOrder::getOrderNo, keyword)
                    .or().like(SalesOrder::getCustomerName, keyword)
                    .or().like(SalesOrder::getCustomerPhone, keyword)
                    .or().like(SalesOrder::getCustomerAddress, keyword));
        }
        if (StringUtils.isNotBlank(status) && !"null".equals(status)) {
            qw.eq(SalesOrder::getStatus, status);
        }
        qw.orderByDesc(SalesOrder::getId);
        IPage<SalesOrder> result = salesOrderService.page(page, qw);
        return Result.suc(result.getRecords(), result.getTotal());
    }

    @GetMapping("/detail")
    public Result detail(@RequestParam Long id) {
        SalesOrderDto dto = salesOrderService.getDetail(id);
        return dto == null ? Result.fail("记录不存在") : Result.suc(dto);
    }

    @PostMapping("/save")
    public Result save(@RequestBody SalesOrderDto dto) {
        dto.setStatus(normalizeStatus(dto.getStatus()));
        String mainErr = validateSalesMain(dto);
        if (mainErr != null) {
            return Result.fail(mainErr);
        }
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            return Result.fail("明细不能为空");
        }
        String itemErr = validateSalesItems(dto);
        if (itemErr != null) {
            return Result.fail(itemErr);
        }
        try {
            return salesOrderService.saveWithItems(dto) ? Result.suc() : Result.fail();
        } catch (IllegalStateException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/update")
    public Result update(@RequestBody SalesOrderDto dto) {
        if (dto.getId() == null) {
            return Result.fail("缺少订单ID");
        }
        dto.setStatus(normalizeStatus(dto.getStatus()));
        String mainErr = validateSalesMain(dto);
        if (mainErr != null) {
            return Result.fail(mainErr);
        }
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            return Result.fail("明细不能为空");
        }
        String itemErr = validateSalesItems(dto);
        if (itemErr != null) {
            return Result.fail(itemErr);
        }
        try {
            return salesOrderService.updateWithItems(dto) ? Result.suc() : Result.fail();
        } catch (IllegalStateException e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 审批通过：body 含 id、operatorId（当前登录用户 id），仅超管/管理员；待审核→已完成并触发出库
     */
    @PostMapping("/approve")
    public Result approve(@RequestBody Map<String, Object> body) {
        if (body == null || body.get("id") == null || body.get("operatorId") == null) {
            return Result.fail("缺少 id 或 operatorId");
        }
        Long id = Long.valueOf(body.get("id").toString());
        Integer operatorId = Integer.valueOf(body.get("operatorId").toString());
        try {
            salesOrderService.approve(id, operatorId);
            return Result.suc();
        } catch (IllegalStateException e) {
            return Result.fail(e.getMessage());
        }
    }

    @DeleteMapping("/del")
    public Result del(@RequestParam String id) {
        return salesOrderService.removeById(id) ? Result.suc() : Result.fail();
    }

    private static String validateSalesMain(SalesOrderDto dto) {
        if (StringUtils.isBlank(dto.getOrderNo())) {
            return "订单号不能为空";
        }
        if (dto.getOrderDate() == null) {
            return "请选择销售日期";
        }
        if (StringUtils.isBlank(dto.getCustomerName())) {
            return "请输入客户名称";
        }
        if (StringUtils.isBlank(dto.getCustomerContact())) {
            return "请输入联系人";
        }
        if (StringUtils.isBlank(dto.getCustomerPhone())) {
            return "请输入联系电话";
        }
        if (StringUtils.isBlank(dto.getCustomerAddress())) {
            return "请输入联系地址";
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

    private static String validateSalesItems(SalesOrderDto dto) {
        for (SalesOrderItem it : dto.getItems()) {
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
