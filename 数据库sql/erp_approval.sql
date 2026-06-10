-- 审批与仓库联动：订单明细增加产品编号（关联 warehouse_stock.product_code）
ALTER TABLE `purchase_order_item`
  ADD COLUMN `product_code` varchar(64) DEFAULT NULL COMMENT '产品编号，审批入库时优先使用' AFTER `line_no`;

ALTER TABLE `sales_order_item`
  ADD COLUMN `product_code` varchar(64) DEFAULT NULL COMMENT '产品编号，审批出库时优先使用' AFTER `line_no`;
