package com.erp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.OrderStockHelper;
import com.erp.dto.SalesOrderDto;
import com.erp.dto.StockIoRequest;
import com.erp.entity.SalesOrder;
import com.erp.entity.SalesOrderItem;
import com.erp.entity.User;
import com.erp.mapper.SalesOrderItemMapper;
import com.erp.mapper.SalesOrderMapper;
import com.erp.service.SalesOrderService;
import com.erp.service.UserService;
import com.erp.service.WarehouseService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;

@Service
public class SalesOrderServiceImpl extends ServiceImpl<SalesOrderMapper, SalesOrder> implements SalesOrderService {

    @Resource
    private SalesOrderItemMapper salesOrderItemMapper;
    @Resource
    private UserService userService;
    @Resource
    private WarehouseService warehouseService;
    @Resource
    private OrderStockHelper orderStockHelper;

    private void fillAmount(SalesOrderItem it) {
        if (it == null) {
            return;
        }
        BigDecimal q = it.getQuantity() == null ? BigDecimal.ZERO : it.getQuantity();
        BigDecimal p = it.getUnitPrice() == null ? BigDecimal.ZERO : it.getUnitPrice();
        it.setAmount(q.multiply(p).setScale(2, RoundingMode.HALF_UP));
    }

    private void recomputeTotals(SalesOrderDto dto) {
        BigDecimal tq = BigDecimal.ZERO;
        BigDecimal ta = BigDecimal.ZERO;
        if (dto.getItems() == null) {
            return;
        }
        for (SalesOrderItem it : dto.getItems()) {
            if (it == null) {
                throw new IllegalStateException("明细行不能为空");
            }
            fillAmount(it);
            tq = tq.add(it.getQuantity() != null ? it.getQuantity() : BigDecimal.ZERO);
            ta = ta.add(it.getAmount() != null ? it.getAmount() : BigDecimal.ZERO);
        }
        dto.setTotalQuantity(tq);
        dto.setTotalAmount(ta.setScale(2, RoundingMode.HALF_UP));
        dto.setItemCount(dto.getItems().size());
    }

    private void trimSalesDto(SalesOrderDto dto) {
        if (dto.getOrderNo() != null) {
            dto.setOrderNo(dto.getOrderNo().trim());
        }
        if (dto.getCustomerName() != null) {
            dto.setCustomerName(dto.getCustomerName().trim());
        }
        if (dto.getCustomerContact() != null) {
            dto.setCustomerContact(dto.getCustomerContact().trim());
        }
        if (dto.getCustomerPhone() != null) {
            dto.setCustomerPhone(dto.getCustomerPhone().trim());
        }
        if (dto.getCustomerAddress() != null) {
            dto.setCustomerAddress(dto.getCustomerAddress().trim());
        }
        if (dto.getRemark() != null) {
            dto.setRemark(dto.getRemark().trim());
        }
        if (dto.getItems() != null) {
            for (SalesOrderItem it : dto.getItems()) {
                if (it == null) {
                    continue;
                }
                if (it.getProductCode() != null) {
                    it.setProductCode(it.getProductCode().trim());
                }
                if (it.getProductName() != null) {
                    it.setProductName(it.getProductName().trim());
                }
                if (it.getSpecification() != null) {
                    it.setSpecification(it.getSpecification().trim());
                }
                if (it.getUnit() != null) {
                    it.setUnit(it.getUnit().trim());
                }
            }
        }
    }

    private void assertSaveAllowed(SalesOrderDto dto) {
        if ("completed".equals(dto.getStatus())) {
            throw new IllegalStateException("不能直接保存为已完成，请使用「审批通过」流程");
        }
        if (dto.getOperatorId() != null) {
            User u = userService.getById(dto.getOperatorId());
            if (u != null && u.getRoleId() != null && u.getRoleId() == 2) {
                String st = dto.getStatus();
                if (!"draft".equals(st) && !"pending".equals(st) && !"cancelled".equals(st)) {
                    throw new IllegalStateException("普通用户仅可选择草稿、待审核或已取消");
                }
            }
        }
    }

    @Override
    public SalesOrderDto getDetail(Long id) {
        SalesOrder main = getById(id);
        if (main == null) {
            return null;
        }
        List<SalesOrderItem> items = salesOrderItemMapper.selectList(
                new LambdaQueryWrapper<SalesOrderItem>()
                        .eq(SalesOrderItem::getOrderId, id)
                        .orderByAsc(SalesOrderItem::getLineNo));
        SalesOrderDto dto = new SalesOrderDto();
        BeanUtils.copyProperties(main, dto);
        dto.setItems(items);
        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveWithItems(SalesOrderDto dto) {
        assertSaveAllowed(dto);
        trimSalesDto(dto);
        recomputeTotals(dto);
        SalesOrder main = new SalesOrder();
        BeanUtils.copyProperties(dto, main, "id", "createdAt", "updatedAt");
        main.setId(null);
        save(main);
        int line = 1;
        for (SalesOrderItem it : dto.getItems()) {
            if (it == null) {
                throw new IllegalStateException("明细行不能为空");
            }
            it.setId(null);
            it.setOrderId(main.getId());
            it.setLineNo(line++);
            fillAmount(it);
            salesOrderItemMapper.insert(it);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateWithItems(SalesOrderDto dto) {
        if (dto.getId() == null) {
            return false;
        }
        assertSaveAllowed(dto);
        trimSalesDto(dto);
        recomputeTotals(dto);
        SalesOrder main = new SalesOrder();
        BeanUtils.copyProperties(dto, main, "createdAt");
        updateById(main);
        salesOrderItemMapper.delete(new LambdaQueryWrapper<SalesOrderItem>()
                .eq(SalesOrderItem::getOrderId, dto.getId()));
        int line = 1;
        for (SalesOrderItem it : dto.getItems()) {
            if (it == null) {
                throw new IllegalStateException("明细行不能为空");
            }
            it.setId(null);
            it.setOrderId(dto.getId());
            it.setLineNo(line++);
            fillAmount(it);
            salesOrderItemMapper.insert(it);
        }
        return true;
    }

    private void assertApprover(Integer operatorId) {
        if (operatorId == null) {
            throw new IllegalStateException("缺少操作人信息");
        }
        User u = userService.getById(operatorId);
        if (u == null || u.getRoleId() == null || u.getRoleId() > 1) {
            throw new IllegalStateException("无审批权限，仅超级管理员或管理员可操作");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long orderId, Integer operatorId) {
        assertApprover(operatorId);
        SalesOrder order = getById(orderId);
        if (order == null) {
            throw new IllegalStateException("订单不存在");
        }
        if (!"pending".equals(order.getStatus())) {
            throw new IllegalStateException("仅「待审核」状态可审批通过");
        }
        List<SalesOrderItem> items = salesOrderItemMapper.selectList(
                new LambdaQueryWrapper<SalesOrderItem>()
                        .eq(SalesOrderItem::getOrderId, orderId)
                        .orderByAsc(SalesOrderItem::getLineNo));
        if (items == null || items.isEmpty()) {
            throw new IllegalStateException("订单无明细");
        }
        Date bizDate = order.getOrderDate();
        if (bizDate == null) {
            throw new IllegalStateException("订单缺少销售日期");
        }
        order.setStatus("completed");
        updateById(order);

        String orderNo = order.getOrderNo() != null ? order.getOrderNo() : String.valueOf(orderId);
        for (SalesOrderItem it : items) {
            String code = orderStockHelper.resolveProductCode(it.getProductCode(), it.getProductName());
            StockIoRequest req = new StockIoRequest();
            req.setProductCode(code);
            req.setQuantity(it.getQuantity());
            req.setBizDate(bizDate);
            req.setUnit(it.getUnit());
            req.setRemark("销售审批出库 " + orderNo + " 行" + it.getLineNo());
            warehouseService.outbound(req);
        }
    }
}
