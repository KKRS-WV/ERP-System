package com.erp.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 将常见异常转为 Result，避免未捕获时返回 HTTP 500；并记录日志便于排查。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DataIntegrityViolationException.class)
    public Result handleDataIntegrity(DataIntegrityViolationException e) {
        log.warn("Data integrity: {}", messageOf(e));
        return translateConstraint(e);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result handleBadJson(HttpMessageNotReadableException e) {
        log.warn("Bad request body: {}", e.getMessage());
        return Result.fail("请求数据格式不正确，请检查日期、数字等字段是否填写完整");
    }

    /**
     * 兜底：MyBatis 的 PersistenceException、连接失败、语法错误等常不在 DataIntegrityViolationException 下。
     */
    @ExceptionHandler(Exception.class)
    public Result handleAny(Exception e) {
        log.error("Unhandled request exception", e);
        String m = rootMessage(e);
        if (isDbUnavailable(m)) {
            return Result.fail("数据库连接失败，请确认数据库服务已启动并检查连接配置");
        }
        if (m != null) {
            if (m.contains("Duplicate entry") || m.contains("uk_po_order_no") || m.contains("uk_so_order_no")) {
                return Result.fail("订单号已存在，请更换订单号后重试");
            }
            if (m.contains("cannot be null")) {
                return Result.fail("必填字段缺失，请检查订单主表与明细是否填写完整");
            }
            if (m.contains("Unknown column") || m.contains("doesn't exist")) {
                return Result.fail("数据库表或字段与程序不一致，请执行项目中的最新 SQL 脚本");
            }
        }
        if (e instanceof NullPointerException) {
            return Result.fail("数据不完整，请检查明细行是否填写完整");
        }
        String hint = (m != null && m.length() < 220) ? m : e.getClass().getSimpleName();
        return Result.fail("保存失败：" + hint);
    }

    private static boolean isDbUnavailable(String message) {
        if (message == null) {
            return false;
        }
        return message.contains("Communications link failure")
                || message.contains("Connection refused")
                || message.contains("No operations allowed after connection closed")
                || message.contains("The last packet successfully received from the server");
    }

    private static Result translateConstraint(DataIntegrityViolationException e) {
        String m = messageOf(e);
        if (m.contains("Duplicate entry") || m.contains("uk_po_order_no") || m.contains("uk_so_order_no")) {
            return Result.fail("订单号已存在，请更换订单号后重试");
        }
        if (m.contains("cannot be null")) {
            return Result.fail("必填字段缺失，请检查订单与明细是否填写完整");
        }
        return Result.fail("数据保存失败，请检查必填项与唯一约束");
    }

    private static String messageOf(DataIntegrityViolationException e) {
        Throwable c = e.getMostSpecificCause();
        if (c != null && c.getMessage() != null) {
            return c.getMessage();
        }
        return e.getMessage() != null ? e.getMessage() : "";
    }

    private static String rootMessage(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null && t.getCause() != t) {
            t = t.getCause();
        }
        return t.getMessage();
    }
}
