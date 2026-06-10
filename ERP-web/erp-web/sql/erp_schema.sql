-- ============================================================
-- ERP 前后端分离（当前 erp-web）可用库表脚本
-- 字符集：utf8mb4（建议 MySQL 5.7+ / 8.0）
-- 说明：JSON 字段名多为驼峰由后端映射；库表列使用 snake_case
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 先删子表、后删父表
-- ----------------------------
DROP TABLE IF EXISTS `purchase_order_item`;
DROP TABLE IF EXISTS `purchase_order`;
DROP TABLE IF EXISTS `sales_order_item`;
DROP TABLE IF EXISTS `sales_order`;
DROP TABLE IF EXISTS `warehouse_stock_flow`;
DROP TABLE IF EXISTS `warehouse_stock`;
DROP TABLE IF EXISTS `expense_order`;
DROP TABLE IF EXISTS `menu`;
DROP TABLE IF EXISTS `user`;

SET FOREIGN_KEY_CHECKS = 1;

-- ----------------------------
-- 用户表（与 /user/login、UserManage 字段对齐）
-- ----------------------------
CREATE TABLE `user` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `no` varchar(20) NOT NULL COMMENT '登录账号',
  `name` varchar(100) NOT NULL COMMENT '姓名',
  `password` varchar(100) NOT NULL COMMENT '密码（建议后端存哈希，长度留足）',
  `age` int(11) DEFAULT NULL COMMENT '年龄',
  `sex` int(11) DEFAULT NULL COMMENT '性别 1男 0女',
  `phone` varchar(20) DEFAULT NULL COMMENT '电话',
  `role_id` int(11) NOT NULL DEFAULT 2 COMMENT '角色 0超级管理员 1管理员 2普通用户',
  `is_valid` char(1) NOT NULL DEFAULT 'Y' COMMENT '是否有效 Y有效 N无效',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_no` (`no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户';

-- ----------------------------
-- 菜单表（登录接口返回 menu 列表；侧栏另有静态项）
-- menu_click 对应路由 path 去掉前导 /，如 User -> /User
-- ----------------------------
CREATE TABLE `menu` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `menu_code` varchar(32) DEFAULT NULL COMMENT '菜单编码',
  `menu_name` varchar(64) NOT NULL COMMENT '菜单名称',
  `menu_level` varchar(8) DEFAULT NULL COMMENT '层级',
  `menu_parent_code` varchar(32) DEFAULT NULL COMMENT '父编码',
  `menu_click` varchar(32) NOT NULL COMMENT '路由名/点击，如 Admin、User',
  `menu_right` varchar(32) DEFAULT NULL COMMENT '可见角色 0超管 1管理 2普通，逗号分隔',
  `menu_component` varchar(200) NOT NULL COMMENT '组件路径，相对 components/',
  `menu_icon` varchar(100) DEFAULT NULL COMMENT '图标 class',
  `sort_order` int(11) DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单（动态路由）';

INSERT INTO `menu` (`id`,`menu_code`,`menu_name`,`menu_level`,`menu_parent_code`,`menu_click`,`menu_right`,`menu_component`,`menu_icon`,`sort_order`) VALUES
(1,'001','管理员','1',NULL,'Admin','0','admin/AdminManage.vue','el-icon-s-custom',1),
(2,'002','用户管理','1',NULL,'User','0,1','user/UserManage.vue','el-icon-user-solid',2);

-- ----------------------------
-- 采购订单主表 / 明细（/purchase/*）
-- ----------------------------
CREATE TABLE `purchase_order` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `order_no` varchar(64) NOT NULL COMMENT '采购单号',
  `order_date` date NOT NULL COMMENT '采购日期',
  `status` varchar(32) NOT NULL DEFAULT 'draft' COMMENT 'draft pending completed cancelled',
  `supplier_name` varchar(200) NOT NULL COMMENT '供应商名称',
  `supplier_contact` varchar(100) DEFAULT NULL COMMENT '联系人',
  `supplier_phone` varchar(50) DEFAULT NULL COMMENT '联系电话',
  `supplier_address` varchar(500) NOT NULL COMMENT '供应商地址',
  `total_quantity` decimal(18,4) NOT NULL DEFAULT 0.0000 COMMENT '总数量',
  `total_amount` decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT '总金额',
  `remark` text COMMENT '备注',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_po_order_no` (`order_no`),
  KEY `idx_po_date` (`order_date`),
  KEY `idx_po_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购订单';

CREATE TABLE `purchase_order_item` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `order_id` bigint(20) NOT NULL COMMENT '采购订单ID',
  `line_no` int(11) NOT NULL DEFAULT 1 COMMENT '行号',
  `product_code` varchar(64) DEFAULT NULL COMMENT '产品编号，审批入库时优先使用',
  `product_name` varchar(200) NOT NULL COMMENT '商品名称',
  `specification` varchar(200) DEFAULT NULL COMMENT '规格',
  `unit` varchar(32) DEFAULT NULL COMMENT '单位',
  `quantity` decimal(18,4) NOT NULL DEFAULT 0.0000 COMMENT '数量',
  `unit_price` decimal(18,4) NOT NULL DEFAULT 0.0000 COMMENT '单价',
  `amount` decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT '小计',
  PRIMARY KEY (`id`),
  KEY `idx_poi_order` (`order_id`),
  CONSTRAINT `fk_poi_order` FOREIGN KEY (`order_id`) REFERENCES `purchase_order` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购订单明细';

-- ----------------------------
-- 销售订单主表 / 明细（/sales/*）
-- ----------------------------
CREATE TABLE `sales_order` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `order_no` varchar(64) NOT NULL COMMENT '销售单号',
  `order_date` date NOT NULL COMMENT '销售日期',
  `status` varchar(32) NOT NULL DEFAULT 'draft' COMMENT 'draft pending completed cancelled',
  `customer_name` varchar(200) NOT NULL COMMENT '客户名称',
  `customer_contact` varchar(100) NOT NULL COMMENT '联系人',
  `customer_phone` varchar(50) NOT NULL COMMENT '联系电话',
  `customer_address` varchar(500) NOT NULL COMMENT '联系地址',
  `item_count` int(11) NOT NULL DEFAULT 0 COMMENT '商品种类数',
  `total_quantity` decimal(18,4) NOT NULL DEFAULT 0.0000 COMMENT '总数量',
  `total_amount` decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT '总金额',
  `remark` text COMMENT '备注',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_so_order_no` (`order_no`),
  KEY `idx_so_date` (`order_date`),
  KEY `idx_so_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='销售订单';

CREATE TABLE `sales_order_item` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `order_id` bigint(20) NOT NULL COMMENT '销售订单ID',
  `line_no` int(11) NOT NULL DEFAULT 1 COMMENT '行号',
  `product_code` varchar(64) DEFAULT NULL COMMENT '产品编号，审批出库时优先使用',
  `product_name` varchar(200) NOT NULL COMMENT '商品名称',
  `specification` varchar(200) DEFAULT NULL COMMENT '规格',
  `unit` varchar(32) DEFAULT NULL COMMENT '单位',
  `quantity` decimal(18,4) NOT NULL DEFAULT 0.0000 COMMENT '数量',
  `unit_price` decimal(18,4) NOT NULL DEFAULT 0.0000 COMMENT '单价',
  `amount` decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT '小计',
  PRIMARY KEY (`id`),
  KEY `idx_soi_order` (`order_id`),
  CONSTRAINT `fk_soi_order` FOREIGN KEY (`order_id`) REFERENCES `sales_order` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='销售订单明细';

-- ----------------------------
-- 库存档案 + 出入库流水（/warehouse/*）
-- ----------------------------
CREATE TABLE `warehouse_stock` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `product_code` varchar(64) NOT NULL COMMENT '产品编号（唯一）',
  `product_name` varchar(200) NOT NULL COMMENT '产品名称',
  `specification` varchar(200) DEFAULT NULL COMMENT '规格',
  `unit` varchar(32) DEFAULT NULL COMMENT '单位',
  `quantity` decimal(18,4) NOT NULL DEFAULT 0.0000 COMMENT '当前库存数量',
  `min_stock` decimal(18,4) NOT NULL DEFAULT 0.0000 COMMENT '警戒数量',
  `warehouse_location` varchar(200) DEFAULT NULL COMMENT '仓位/库位',
  `remark` text COMMENT '备注',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ws_product_code` (`product_code`),
  KEY `idx_ws_name` (`product_name`(50))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存档案';

CREATE TABLE `warehouse_stock_flow` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `flow_no` varchar(64) NOT NULL COMMENT '流水单号',
  `flow_type` varchar(8) NOT NULL COMMENT 'in 入库 out 出库',
  `stock_id` bigint(20) DEFAULT NULL COMMENT '关联库存档案ID',
  `product_code` varchar(64) NOT NULL COMMENT '产品编号',
  `product_name` varchar(200) DEFAULT NULL COMMENT '产品名称',
  `quantity` decimal(18,4) NOT NULL COMMENT '数量',
  `unit` varchar(32) DEFAULT NULL COMMENT '单位',
  `biz_date` date NOT NULL COMMENT '业务日期',
  `remark` text COMMENT '备注',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_wsf_flow_no` (`flow_no`),
  KEY `idx_wsf_code` (`product_code`),
  KEY `idx_wsf_date` (`biz_date`),
  KEY `idx_wsf_type` (`flow_type`),
  CONSTRAINT `fk_wsf_stock` FOREIGN KEY (`stock_id`) REFERENCES `warehouse_stock` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出入库流水';

-- ----------------------------
-- 费用单（财务 /finance/profit/summary 中「期间费用」汇总用）
-- ----------------------------
CREATE TABLE `expense_order` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `expense_no` varchar(64) DEFAULT NULL COMMENT '费用单号（可空，由业务生成）',
  `biz_date` date NOT NULL COMMENT '费用发生日期',
  `amount` decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT '金额',
  `remark` varchar(1000) DEFAULT NULL COMMENT '摘要/备注',
  `status` varchar(32) DEFAULT 'completed' COMMENT '状态（预留）',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_eo_biz_date` (`biz_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='费用单';

-- ----------------------------
-- 初始数据：超级管理员（密码明文示例，生产请改）
-- ----------------------------
INSERT INTO `user` (`id`,`no`,`name`,`password`,`age`,`sex`,`phone`,`role_id`,`is_valid`) VALUES
(1,'sa','超级管理员','123',18,1,'111',0,'Y');
