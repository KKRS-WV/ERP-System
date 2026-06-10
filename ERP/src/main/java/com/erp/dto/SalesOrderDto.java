package com.erp.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.erp.entity.SalesOrderItem;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 销售订单（含明细）传输对象。
 * 不继承 SalesOrder，避免 Jackson 序列化 Result.data 时仅按父类输出导致 items 丢失。
 */
@Data
public class SalesOrderDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String orderNo;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date orderDate;

    private String status;

    private String customerName;

    private String customerContact;

    private String customerPhone;

    private String customerAddress;

    private Integer itemCount;

    private BigDecimal totalQuantity;

    private BigDecimal totalAmount;

    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updatedAt;

    private List<SalesOrderItem> items;

    /** 仅用于权限校验（非表字段），保存时可选 */
    private Integer operatorId;
}
