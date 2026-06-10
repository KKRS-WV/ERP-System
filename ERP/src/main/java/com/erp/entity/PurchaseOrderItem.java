package com.erp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@TableName("purchase_order_item")
public class PurchaseOrderItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Integer lineNo;

    /**
     * 兼容旧库（部分环境无 product_code 列）：
     * - 新库可保留该字段用于审批时优先匹配；
     * - 旧库下忽略该字段，审批将回退按商品名匹配。
     */
    @TableField(exist = false)
    private String productCode;

    private String productName;

    private String specification;

    private String unit;

    private BigDecimal quantity;

    private BigDecimal unitPrice;

    private BigDecimal amount;
}
