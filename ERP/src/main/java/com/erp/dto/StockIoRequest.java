package com.erp.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 入出库请求（与前端 inboundForm / outboundForm 字段一致）
 */
@Data
public class StockIoRequest {

    private String productCode;

    private String productName;

    private BigDecimal quantity;

    private String unit;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date bizDate;

    private String remark;

    /** 当前登录用户（用于手工出入库提交审批） */
    private Integer operatorId;
}
