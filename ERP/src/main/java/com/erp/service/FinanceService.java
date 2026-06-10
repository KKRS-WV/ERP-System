package com.erp.service;

import java.util.Map;

public interface FinanceService {

    /**
     * 按月汇总利润简表
     *
     * @param yearMonth yyyy-MM
     */
    Map<String, Object> profitSummary(String yearMonth);
}
