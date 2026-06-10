package com.erp.common;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.erp.entity.WarehouseStock;
import com.erp.mapper.WarehouseStockMapper;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 订单明细行解析库存产品编号：优先 productCode，否则按商品名称匹配唯一库存档案。
 */
@Component
public class OrderStockHelper {

    @Resource
    private WarehouseStockMapper warehouseStockMapper;

    public String resolveProductCode(String productCode, String productName) {
        if (StringUtils.isNotBlank(productCode)) {
            return productCode.trim();
        }
        if (StringUtils.isBlank(productName)) {
            throw new IllegalStateException("明细缺少商品名称");
        }
        WarehouseStock s = warehouseStockMapper.selectOne(
                new LambdaQueryWrapper<WarehouseStock>()
                        .eq(WarehouseStock::getProductName, productName.trim())
                        .last("LIMIT 1"));
        if (s != null) {
            return s.getProductCode();
        }
        throw new IllegalStateException("请为商品「" + productName.trim() + "」填写产品编号，或先在仓库建立同名库存档案");
    }
}
