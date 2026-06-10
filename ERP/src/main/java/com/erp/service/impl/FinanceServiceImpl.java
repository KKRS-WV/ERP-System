package com.erp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.entity.ExpenseOrder;
import com.erp.entity.PurchaseOrder;
import com.erp.entity.SalesOrder;
import com.erp.mapper.ExpenseOrderMapper;
import com.erp.mapper.PurchaseOrderMapper;
import com.erp.mapper.SalesOrderMapper;
import com.erp.service.FinanceService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FinanceServiceImpl implements FinanceService {

    @Resource
    private SalesOrderMapper salesOrderMapper;
    @Resource
    private PurchaseOrderMapper purchaseOrderMapper;
    @Resource
    private ExpenseOrderMapper expenseOrderMapper;

    private static Date toDate(LocalDate d) {
        return Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    @Override
    public Map<String, Object> profitSummary(String yearMonth) {
        YearMonth ym = YearMonth.parse(yearMonth);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        Date startD = toDate(start);
        Date endD = toDate(end);

        LambdaQueryWrapper<SalesOrder> sqw = new LambdaQueryWrapper<SalesOrder>()
                .ge(SalesOrder::getOrderDate, startD)
                .le(SalesOrder::getOrderDate, endD)
                .ne(SalesOrder::getStatus, "cancelled");
        List<SalesOrder> salesList = salesOrderMapper.selectList(sqw);
        BigDecimal revenue = BigDecimal.ZERO;
        for (SalesOrder o : salesList) {
            if (o.getTotalAmount() != null) {
                revenue = revenue.add(o.getTotalAmount());
            }
        }
        long salesCnt = salesList.size();

        LambdaQueryWrapper<PurchaseOrder> pqw = new LambdaQueryWrapper<PurchaseOrder>()
                .ge(PurchaseOrder::getOrderDate, startD)
                .le(PurchaseOrder::getOrderDate, endD)
                .ne(PurchaseOrder::getStatus, "cancelled");
        List<PurchaseOrder> purList = purchaseOrderMapper.selectList(pqw);
        BigDecimal cost = BigDecimal.ZERO;
        for (PurchaseOrder o : purList) {
            if (o.getTotalAmount() != null) {
                cost = cost.add(o.getTotalAmount());
            }
        }
        long purCnt = purList.size();

        LambdaQueryWrapper<ExpenseOrder> eqw = new LambdaQueryWrapper<ExpenseOrder>()
                .ge(ExpenseOrder::getBizDate, startD)
                .le(ExpenseOrder::getBizDate, endD);
        List<ExpenseOrder> expList = expenseOrderMapper.selectList(eqw);
        BigDecimal expense = BigDecimal.ZERO;
        for (ExpenseOrder e : expList) {
            if (e.getAmount() != null) {
                expense = expense.add(e.getAmount());
            }
        }
        long expCnt = expList.size();

        BigDecimal gross = revenue.subtract(cost).setScale(2, RoundingMode.HALF_UP);
        BigDecimal net = gross.subtract(expense).setScale(2, RoundingMode.HALF_UP);

        Map<String, Object> m = new HashMap<>();
        m.put("yearMonth", yearMonth);
        m.put("revenue", revenue.setScale(2, RoundingMode.HALF_UP));
        m.put("cost", cost.setScale(2, RoundingMode.HALF_UP));
        m.put("expense", expense.setScale(2, RoundingMode.HALF_UP));
        m.put("grossProfit", gross);
        m.put("netProfit", net);
        m.put("salesOrderCount", salesCnt);
        m.put("purchaseOrderCount", purCnt);
        m.put("expenseOrderCount", expCnt);
        return m;
    }
}
