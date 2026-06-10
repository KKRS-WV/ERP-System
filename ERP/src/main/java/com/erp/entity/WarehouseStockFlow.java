package com.erp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("warehouse_stock_flow")
public class WarehouseStockFlow implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String flowNo;

    private String flowType;

    private Long stockId;

    private String productCode;

    private String productName;

    private BigDecimal quantity;

    private String unit;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date bizDate;

    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createdAt;
}
