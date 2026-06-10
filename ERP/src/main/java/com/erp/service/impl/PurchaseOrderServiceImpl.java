package com.erp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.OrderStockHelper;
import com.erp.dto.PurchaseOrderDto;
import com.erp.dto.StockIoRequest;
import com.erp.entity.PurchaseOrder;
import com.erp.entity.PurchaseOrderItem;
import com.erp.entity.User;
import com.erp.mapper.PurchaseOrderItemMapper;
import com.erp.mapper.PurchaseOrderMapper;
import com.erp.service.PurchaseOrderService;
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
public class PurchaseOrderServiceImpl extends ServiceImpl<PurchaseOrderMapper, PurchaseOrder> implements PurchaseOrderService {

    @Resource
    private PurchaseOrderItemMapper purchaseOrderItemMapper;
    @Resource
    private UserService userService;
    @Resource
    private WarehouseService warehouseService;
    @Resource
    private OrderStockHelper orderStockHelper;

    private void fillAmount(PurchaseOrderItem it) {
        if (it == null) {
            return;
        }
        BigDecimal q = it.getQuantity() == null ? BigDecimal.ZERO : it.getQuantity();
        BigDecimal p = it.getUnitPrice() == null ? BigDecimal.ZERO : it.getUnitPrice();
        it.setAmount(q.multiply(p).setScale(2, RoundingMode.HALF_UP));
    }

    private void recomputeTotals(PurchaseOrderDto dto) {
        BigDecimal tq = BigDecimal.ZERO;
        BigDecimal ta = BigDecimal.ZERO;
        if (dto.getItems() == null) {
            return;
        }
        for (PurchaseOrderItem it : dto.getItems()) {
            if (it == null) {
                throw new IllegalStateException("明细行不能为空");
            }
            fillAmount(it);
            tq = tq.add(it.getQuantity() != null ? it.getQuantity() : BigDecimal.ZERO);
            ta = ta.add(it.getAmount() != null ? it.getAmount() : BigDecimal.ZERO);
        }
        dto.setTotalQuantity(tq);
        dto.setTotalAmount(ta.setScale(2, RoundingMode.HALF_UP));
    }

    /** 禁止直接保存为已完成；普通用户仅允许草稿/待审核/已取消 */
    /** 去掉前后空格，避免与库中唯一约束不一致或 NOT NULL 误判 */
    private void trimPurchaseDto(PurchaseOrderDto dto) {
        if (dto.getOrderNo() != null) {
            dto.setOrderNo(dto.getOrderNo().trim());
        }
        if (dto.getSupplierName() != null) {
            dto.setSupplierName(dto.getSupplierName().trim());
        }
        if (dto.getSupplierContact() != null) {
            dto.setSupplierContact(dto.getSupplierContact().trim());
        }
        if (dto.getSupplierPhone() != null) {
            dto.setSupplierPhone(dto.getSupplierPhone().trim());
        }
        if (dto.getSupplierAddress() != null) {
            dto.setSupplierAddress(dto.getSupplierAddress().trim());
        }
        if (dto.getRemark() != null) {
            dto.setRemark(dto.getRemark().trim());
        }
        if (dto.getItems() != null) {
            for (PurchaseOrderItem it : dto.getItems()) {
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

    private void assertSaveAllowed(PurchaseOrderDto dto) {
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
    public PurchaseOrderDto getDetail(Long id) {
        PurchaseOrder main = getById(id);
        if (main == null) {
            return null;
        }
        List<PurchaseOrderItem> items = purchaseOrderItemMapper.selectList(
                new LambdaQueryWrapper<PurchaseOrderItem>()
                        .eq(PurchaseOrderItem::getOrderId, id)
                        .orderByAsc(PurchaseOrderItem::getLineNo));
        PurchaseOrderDto dto = new PurchaseOrderDto();
        BeanUtils.copyProperties(main, dto);
        dto.setItems(items);
        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveWithItems(PurchaseOrderDto dto) {
        assertSaveAllowed(dto);
        trimPurchaseDto(dto);
        recomputeTotals(dto);
        PurchaseOrder main = new PurchaseOrder();
        BeanUtils.copyProperties(dto, main, "id", "createdAt", "updatedAt");
        main.setId(null);
        save(main);
        int line = 1;
        for (PurchaseOrderItem it : dto.getItems()) {
            if (it == null) {
                throw new IllegalStateException("明细行不能为空");
            }
            it.setId(null);
            it.setOrderId(main.getId());
            it.setLineNo(line++);
            fillAmount(it);
            purchaseOrderItemMapper.insert(it);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateWithItems(PurchaseOrderDto dto) {
        if (dto.getId() == null) {
            return false;
        }
        assertSaveAllowed(dto);
        trimPurchaseDto(dto);
        recomputeTotals(dto);
        PurchaseOrder main = new PurchaseOrder();
        BeanUtils.copyProperties(dto, main, "createdAt");
        updateById(main);
        purchaseOrderItemMapper.delete(new LambdaQueryWrapper<PurchaseOrderItem>()
                .eq(PurchaseOrderItem::getOrderId, dto.getId()));
        int line = 1;
        for (PurchaseOrderItem it : dto.getItems()) {
            if (it == null) {
                throw new IllegalStateException("明细行不能为空");
            }
            it.setId(null);
            it.setOrderId(dto.getId());
            it.setLineNo(line++);
            fillAmount(it);
            purchaseOrderItemMapper.insert(it);
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
        PurchaseOrder order = getById(orderId);
        if (order == null) {
            throw new IllegalStateException("订单不存在");
        }
        if (!"pending".equals(order.getStatus())) {
            throw new IllegalStateException("仅「待审核」状态可审批通过");
        }
        List<PurchaseOrderItem> items = purchaseOrderItemMapper.selectList(
                new LambdaQueryWrapper<PurchaseOrderItem>()
                        .eq(PurchaseOrderItem::getOrderId, orderId)
                        .orderByAsc(PurchaseOrderItem::getLineNo));
        if (items == null || items.isEmpty()) {
            throw new IllegalStateException("订单无明细");
        }
        Date bizDate = order.getOrderDate();
        if (bizDate == null) {
            throw new IllegalStateException("订单缺少采购日期");
        }
        order.setStatus("completed");
        updateById(order);

        String orderNo = order.getOrderNo() != null ? order.getOrderNo() : String.valueOf(orderId);
        for (PurchaseOrderItem it : items) {
            String code = orderStockHelper.resolveProductCode(it.getProductCode(), it.getProductName());
            StockIoRequest req = new StockIoRequest();
            req.setProductCode(code);
            req.setQuantity(it.getQuantity());
            req.setBizDate(bizDate);
            req.setUnit(it.getUnit());
            req.setRemark("采购审批入库 " + orderNo + " 行" + it.getLineNo());
            warehouseService.inbound(req);
        }
    }
}
