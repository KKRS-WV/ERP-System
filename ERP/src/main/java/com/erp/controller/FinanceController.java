package com.erp.controller;

import com.erp.common.Result;
import com.erp.entity.User;
import com.erp.service.FinanceService;
import com.erp.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Map;

@RestController
@RequestMapping("/finance")
public class FinanceController {

    @Resource
    private FinanceService financeService;
    @Resource
    private UserService userService;

    @PostMapping("/profit/summary")
    public Result profitSummary(@RequestBody Map<String, Object> body) {
        Integer operatorId = parseOperatorId(body == null ? null : body.get("operatorId"));
        String authErr = assertAdminOrAbove(operatorId);
        if (authErr != null) {
            return Result.fail(authErr);
        }
        String yearMonth = body == null || body.get("yearMonth") == null ? null : body.get("yearMonth").toString();
        if (yearMonth == null || yearMonth.trim().isEmpty()) {
            return Result.fail("请选择报表月份");
        }
        try {
            Map<String, Object> data = financeService.profitSummary(yearMonth.trim());
            return Result.suc(data);
        } catch (Exception e) {
            return Result.fail("汇总失败: " + e.getMessage());
        }
    }

    private String assertAdminOrAbove(Integer operatorId) {
        if (operatorId == null) {
            return "缺少操作人信息";
        }
        User op = userService.getById(operatorId);
        if (op == null || op.getRoleId() == null || op.getRoleId() > 1) {
            return "仅管理员及以上可查看财务报表";
        }
        return null;
    }

    private static Integer parseOperatorId(Object raw) {
        if (raw == null) {
            return null;
        }
        String text = raw.toString();
        if (text.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(text.trim());
        } catch (Exception e) {
            return null;
        }
    }
}
