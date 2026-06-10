package com.erp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.QueryPageParam;
import com.erp.dto.StockIoRequest;
import com.erp.entity.User;
import com.erp.entity.WarehouseStock;
import com.erp.entity.WarehouseStockFlow;
import com.erp.mapper.WarehouseStockFlowMapper;
import com.erp.mapper.WarehouseStockMapper;
import com.erp.service.UserService;
import com.erp.service.WarehouseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.HashMap;

@Service
public class WarehouseServiceImpl implements WarehouseService {
    private static final String FLOW_IN = "in";
    private static final String FLOW_OUT = "out";
    private static final String FLOW_PENDING_IN = "pin";
    private static final String FLOW_PENDING_OUT = "pout";
    private static final String FLOW_REJECTED_IN = "rin";
    private static final String FLOW_REJECTED_OUT = "rout";

    @Resource
    private WarehouseStockMapper warehouseStockMapper;
    @Resource
    private WarehouseStockFlowMapper warehouseStockFlowMapper;
    @Resource
    private UserService userService;

    private static boolean isAlertOnly(Object o) {
        return Boolean.TRUE.equals(o) || "true".equalsIgnoreCase(String.valueOf(o));
    }

    @Override
    public IPage<WarehouseStock> pageStock(QueryPageParam query) {
        HashMap param = query.getParam();
        String keyword = param == null ? null : (String) param.get("keyword");
        Object alertOnlyObj = param == null ? null : param.get("alertOnly");

        Page<WarehouseStock> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<WarehouseStock> qw = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(keyword) && !"null".equals(keyword)) {
            qw.and(w -> w.like(WarehouseStock::getProductCode, keyword)
                    .or().like(WarehouseStock::getProductName, keyword)
                    .or().like(WarehouseStock::getSpecification, keyword));
        }
        if (isAlertOnly(alertOnlyObj)) {
            qw.apply("(quantity <= 0 OR (min_stock > 0 AND quantity <= min_stock))");
        }
        qw.orderByDesc(WarehouseStock::getId);
        return warehouseStockMapper.selectPage(page, qw);
    }

    @Override
    public IPage<WarehouseStockFlow> pageFlow(QueryPageParam query) {
        HashMap param = query.getParam();
        String keyword = param == null ? null : (String) param.get("keyword");
        String flowType = param == null ? null : (String) param.get("flowType");

        Page<WarehouseStockFlow> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<WarehouseStockFlow> qw = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(keyword) && !"null".equals(keyword)) {
            qw.and(w -> w.like(WarehouseStockFlow::getFlowNo, keyword)
                    .or().like(WarehouseStockFlow::getProductCode, keyword)
                    .or().like(WarehouseStockFlow::getRemark, keyword));
        }
        if (StringUtils.isNotBlank(flowType) && !"null".equals(flowType)) {
            if ("pending".equals(flowType)) {
                qw.in(WarehouseStockFlow::getFlowType, FLOW_PENDING_IN, FLOW_PENDING_OUT);
            } else if ("rejected".equals(flowType)) {
                qw.in(WarehouseStockFlow::getFlowType, FLOW_REJECTED_IN, FLOW_REJECTED_OUT);
            } else {
                qw.eq(WarehouseStockFlow::getFlowType, flowType);
            }
        }
        qw.orderByDesc(WarehouseStockFlow::getId);
        return warehouseStockFlowMapper.selectPage(page, qw);
    }

    @Override
    public WarehouseStock getStock(Long id) {
        return warehouseStockMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveStock(WarehouseStock stock) {
        stock.setId(null);
        if (stock.getQuantity() == null) {
            stock.setQuantity(BigDecimal.ZERO);
        }
        if (stock.getMinStock() == null) {
            stock.setMinStock(BigDecimal.ZERO);
        }
        return warehouseStockMapper.insert(stock) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStock(WarehouseStock stock) {
        return warehouseStockMapper.updateById(stock) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void inbound(StockIoRequest req) {
        WarehouseStock stock = requireStock(req, true);
        applyInbound(stock, req.getQuantity());
        WarehouseStockFlow flow = createFlowFromReq(req, stock, FLOW_IN, "IN");
        warehouseStockFlowMapper.insert(flow);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void outbound(StockIoRequest req) {
        WarehouseStock stock = requireStock(req, false);
        applyOutbound(stock, req.getQuantity());
        WarehouseStockFlow flow = createFlowFromReq(req, stock, FLOW_OUT, "OUT");
        warehouseStockFlowMapper.insert(flow);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitInboundApproval(StockIoRequest req) {
        assertRequester(req.getOperatorId());
        WarehouseStock stock = requireStock(req, true);
        WarehouseStockFlow flow = createFlowFromReq(req, stock, FLOW_PENDING_IN, "PIN");
        warehouseStockFlowMapper.insert(flow);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitOutboundApproval(StockIoRequest req) {
        assertRequester(req.getOperatorId());
        WarehouseStock stock = requireStock(req, false);
        if (stock.getQuantity().compareTo(req.getQuantity()) < 0) {
            throw new IllegalStateException("库存不足，无法提交出库审批");
        }
        WarehouseStockFlow flow = createFlowFromReq(req, stock, FLOW_PENDING_OUT, "POUT");
        warehouseStockFlowMapper.insert(flow);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveFlow(Long flowId, Integer operatorId) {
        assertApprover(operatorId);
        if (flowId == null) {
            throw new IllegalStateException("缺少流水ID");
        }
        WarehouseStockFlow flow = warehouseStockFlowMapper.selectById(flowId);
        if (flow == null) {
            throw new IllegalStateException("审批流水不存在");
        }
        if (!FLOW_PENDING_IN.equals(flow.getFlowType()) && !FLOW_PENDING_OUT.equals(flow.getFlowType())) {
            throw new IllegalStateException("仅待审批流水可执行审批");
        }
        StockIoRequest req = reqFromFlow(flow);
        WarehouseStock stock = requireStock(req, FLOW_PENDING_IN.equals(flow.getFlowType()));
        if (FLOW_PENDING_IN.equals(flow.getFlowType())) {
            applyInbound(stock, req.getQuantity());
            flow.setFlowType(FLOW_IN);
        } else {
            applyOutbound(stock, req.getQuantity());
            flow.setFlowType(FLOW_OUT);
        }
        flow.setStockId(stock.getId());
        flow.setProductName(stock.getProductName());
        if (StringUtils.isBlank(flow.getUnit())) {
            flow.setUnit(stock.getUnit());
        }
        warehouseStockFlowMapper.updateById(flow);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectFlow(Long flowId, Integer operatorId) {
        assertApprover(operatorId);
        if (flowId == null) {
            throw new IllegalStateException("缺少流水ID");
        }
        WarehouseStockFlow flow = warehouseStockFlowMapper.selectById(flowId);
        if (flow == null) {
            throw new IllegalStateException("审批流水不存在");
        }
        if (!FLOW_PENDING_IN.equals(flow.getFlowType()) && !FLOW_PENDING_OUT.equals(flow.getFlowType())) {
            throw new IllegalStateException("仅待审批流水可驳回");
        }
        flow.setFlowType(FLOW_PENDING_IN.equals(flow.getFlowType()) ? FLOW_REJECTED_IN : FLOW_REJECTED_OUT);
        String remark = flow.getRemark() == null ? "" : flow.getRemark();
        if (!remark.contains("审批驳回")) {
            flow.setRemark((remark + " | 审批驳回").trim());
        }
        warehouseStockFlowMapper.updateById(flow);
    }

    private WarehouseStockFlow createFlowFromReq(StockIoRequest req, WarehouseStock stock, String flowType, String prefix) {
        WarehouseStockFlow flow = new WarehouseStockFlow();
        flow.setFlowNo(prefix + System.currentTimeMillis() + (int) (Math.random() * 10000));
        flow.setFlowType(flowType);
        flow.setStockId(stock.getId());
        flow.setProductCode(stock.getProductCode());
        flow.setProductName(StringUtils.isNotBlank(req.getProductName()) ? req.getProductName().trim() : stock.getProductName());
        flow.setQuantity(req.getQuantity());
        flow.setUnit(StringUtils.isNotBlank(req.getUnit()) ? req.getUnit().trim() : stock.getUnit());
        flow.setBizDate(req.getBizDate());
        flow.setRemark(req.getRemark());
        return flow;
    }

    private StockIoRequest reqFromFlow(WarehouseStockFlow flow) {
        StockIoRequest req = new StockIoRequest();
        req.setProductCode(flow.getProductCode());
        req.setProductName(flow.getProductName());
        req.setQuantity(flow.getQuantity());
        req.setUnit(flow.getUnit());
        req.setBizDate(flow.getBizDate());
        req.setRemark(flow.getRemark());
        return req;
    }

    private WarehouseStock requireStock(StockIoRequest req, boolean inbound) {
        validateBaseReq(req, inbound);
        WarehouseStock stock = warehouseStockMapper.selectOne(
                new LambdaQueryWrapper<WarehouseStock>()
                        .eq(WarehouseStock::getProductCode, req.getProductCode().trim()));
        if (stock == null) {
            throw new IllegalStateException(inbound ? "请先建档后再入库" : "库存档案不存在");
        }
        if (stock.getQuantity() == null) {
            stock.setQuantity(BigDecimal.ZERO);
        }
        return stock;
    }

    private void validateBaseReq(StockIoRequest req, boolean inbound) {
        if (req == null || req.getProductCode() == null || req.getProductCode().trim().isEmpty()) {
            throw new IllegalStateException("产品编号不能为空");
        }
        if (req.getQuantity() == null || req.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException((inbound ? "入库" : "出库") + "数量必须大于0");
        }
        if (req.getBizDate() == null) {
            throw new IllegalStateException("请选择业务日期");
        }
    }

    private void applyInbound(WarehouseStock stock, BigDecimal qty) {
        stock.setQuantity(stock.getQuantity().add(qty));
        warehouseStockMapper.updateById(stock);
    }

    private void applyOutbound(WarehouseStock stock, BigDecimal qty) {
        if (stock.getQuantity().compareTo(qty) < 0) {
            throw new IllegalStateException("库存不足");
        }
        stock.setQuantity(stock.getQuantity().subtract(qty));
        warehouseStockMapper.updateById(stock);
    }

    private void assertRequester(Integer operatorId) {
        if (operatorId == null) {
            throw new IllegalStateException("缺少操作人信息");
        }
        User u = userService.getById(operatorId);
        if (u == null || u.getRoleId() == null) {
            throw new IllegalStateException("操作人不存在");
        }
    }

    private void assertApprover(Integer operatorId) {
        if (operatorId == null) {
            throw new IllegalStateException("缺少审批人信息");
        }
        User u = userService.getById(operatorId);
        if (u == null || u.getRoleId() == null || u.getRoleId() > 1) {
            throw new IllegalStateException("仅管理员或超级管理员可审批");
        }
    }
}
