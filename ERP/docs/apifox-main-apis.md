# ERP 主要接口文档（Apifox 并发测试版）

本文档基于当前后端代码整理，适合用于 Apifox 的接口定义与并发压测场景设计。

## 1. 基础信息

- 服务地址：`http://localhost:8090`
- 鉴权说明：除 `POST /user/login` 外，其它接口都需要携带请求头  
  `Authorization: Bearer {{token}}`
- 通用返回结构：
  - `code`：`200` 成功，`400` 失败
  - `msg`：提示信息
  - `total`：分页总数（分页接口会返回）
  - `data`：业务数据

成功返回示例：

```json
{
  "code": 200,
  "msg": "success",
  "total": 0,
  "data": {}
}
```

失败返回示例：

```json
{
  "code": 400,
  "msg": "缺少 id 或 operatorId",
  "total": 0,
  "data": null
}
```

---

## 2. 用户与登录

### 2.1 登录

- 方法/路径：`POST /user/login`
- 说明：账号密码登录，返回用户信息、菜单、token
- 请求体：

```json
{
  "no": "sa1",
  "password": "123"
}
```

- 成功 `data` 结构（关键字段）：
  - `user`：用户对象（`password` 会被置空）
  - `menu`：菜单列表
  - `token`：JWT 字符串

---

## 3. 采购订单（高并发重点）

### 3.1 分页查询采购订单

- 方法/路径：`POST /purchase/listPageC1`
- 请求体：

```json
{
  "pageNum": 1,
  "pageSize": 20,
  "param": {
    "keyword": "PO2026",
    "status": "pending"
  }
}
```

- `status` 可选值：`draft` / `pending` / `completed` / `cancelled`

### 3.2 新增采购订单

- 方法/路径：`POST /purchase/save`
- 请求体（核心字段）：

```json
{
  "orderNo": "PO20260504001",
  "orderDate": "2026-05-04",
  "status": "pending",
  "supplierName": "华东供应商A",
  "supplierContact": "张三",
  "supplierPhone": "13800001111",
  "supplierAddress": "上海市浦东新区XX路",
  "remark": "并发压测单",
  "operatorId": 1,
  "items": [
    {
      "lineNo": 1,
      "productCode": "P-1001",
      "productName": "工业轴承",
      "specification": "6205",
      "unit": "个",
      "quantity": 100,
      "unitPrice": 12.5,
      "amount": 1250
    }
  ]
}
```

### 3.3 审批采购订单

- 方法/路径：`POST /purchase/approve`
- 说明：审批通过后会触发库存相关逻辑（服务层）
- 请求体：

```json
{
  "id": 101,
  "operatorId": 1
}
```

---

## 4. 销售订单（高并发重点）

### 4.1 分页查询销售订单

- 方法/路径：`POST /sales/listPageC1`
- 请求体：

```json
{
  "pageNum": 1,
  "pageSize": 20,
  "param": {
    "keyword": "SO2026",
    "status": "pending"
  }
}
```

### 4.2 新增销售订单

- 方法/路径：`POST /sales/save`
- 请求体（核心字段）：

```json
{
  "orderNo": "SO20260504001",
  "orderDate": "2026-05-04",
  "status": "pending",
  "customerName": "华南客户B",
  "customerContact": "李四",
  "customerPhone": "13900002222",
  "customerAddress": "广州市天河区XX路",
  "remark": "并发压测单",
  "operatorId": 1,
  "items": [
    {
      "lineNo": 1,
      "productCode": "P-1001",
      "productName": "工业轴承",
      "specification": "6205",
      "unit": "个",
      "quantity": 20,
      "unitPrice": 25,
      "amount": 500
    }
  ]
}
```

### 4.3 审批销售订单

- 方法/路径：`POST /sales/approve`
- 说明：待审核 -> 已完成，并触发出库逻辑
- 请求体：

```json
{
  "id": 201,
  "operatorId": 1
}
```

---

## 5. 仓库库存与流水（并发核心）

### 5.1 库存分页查询

- 方法/路径：`POST /warehouse/stock/listPageC1`
- 请求体：

```json
{
  "pageNum": 1,
  "pageSize": 20,
  "param": {
    "keyword": "P-1001"
  }
}
```

### 5.2 手工入库（直接改库存）

- 方法/路径：`POST /warehouse/inbound`
- 请求体：

```json
{
  "productCode": "P-1001",
  "productName": "工业轴承",
  "quantity": 10,
  "unit": "个",
  "bizDate": "2026-05-04",
  "remark": "手工入库并发测试",
  "operatorId": 1
}
```

### 5.3 手工出库（直接改库存）

- 方法/路径：`POST /warehouse/outbound`
- 请求体同 `inbound`

### 5.4 提交入库审批（不直接改库存）

- 方法/路径：`POST /warehouse/inbound/apply`
- 请求体同 `inbound`

### 5.5 提交出库审批（不直接改库存）

- 方法/路径：`POST /warehouse/outbound/apply`
- 请求体同 `outbound`

### 5.6 仓库流水分页

- 方法/路径：`POST /warehouse/flow/listPageC1`
- 请求体：

```json
{
  "pageNum": 1,
  "pageSize": 20,
  "param": {
    "productCode": "P-1001",
    "flowType": "pout"
  }
}
```

### 5.7 审批通过仓库流水

- 方法/路径：`POST /warehouse/flow/approve`
- 请求体：

```json
{
  "id": 301,
  "operatorId": 1
}
```

### 5.8 驳回仓库流水

- 方法/路径：`POST /warehouse/flow/reject`
- 请求体同 `approve`

---

## 6. 财务报表

### 6.1 月利润汇总

- 方法/路径：`POST /finance/profit/summary`
- 说明：仅管理员及以上可查
- 请求体：

```json
{
  "yearMonth": "2026-05",
  "operatorId": 1
}
```

---

## 7. Apifox 并发测试建议（可直接建场景）

### 场景 A：订单审批并发幂等

- 接口：`/purchase/approve`、`/sales/approve`
- 建议：同一个 `id` + `operatorId`，并发 20~100 请求
- 预期：只有 1 次成功，其余返回业务失败（如状态不合法/重复审批）

### 场景 B：库存扣减并发一致性

- 接口：`/warehouse/outbound` 或 `/warehouse/flow/approve`
- 建议：同一 `productCode` 并发出库，控制总扣减量接近库存上限
- 预期：不能出现负库存；失败请求应返回明确业务错误

### 场景 C：入库+出库混合并发

- 接口：`/warehouse/inbound` + `/warehouse/outbound`
- 建议：按 1:1 混合压测（例如各 50 并发）
- 预期：最终库存 = 初始库存 + 入库总量 - 出库总量

### 场景 D：列表查询读压

- 接口：`/purchase/listPageC1`、`/sales/listPageC1`、`/warehouse/stock/listPageC1`
- 建议：100~300 并发，关注响应时间 p95/p99
- 预期：成功率高、响应时间稳定，无明显超时

---

## 8. 推荐在 Apifox 里设置的变量

- `{{baseUrl}}` = `http://localhost:8090`
- `{{adminUserNo}}` = `sa1`
- `{{adminPassword}}` = `123`
- `{{operatorId}}` = `1`
- `{{purchaseOrderId}}`、`{{salesOrderId}}`、`{{flowId}}`：通过前置请求动态赋值

可通过登录接口把 `token` 保存为环境变量 `{{token}}`，后续接口统一使用 `Authorization: Bearer {{token}}`。
