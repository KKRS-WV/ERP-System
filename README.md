# ERP-System

基于 Spring Boot 2.7 + Vue 2 + Element UI 的前后端分离 ERP 管理系统。项目包含后端接口服务、前端管理端和 MySQL 初始化脚本，覆盖用户登录、菜单导航、采购、销售、库存和财务利润汇总等常见 ERP 业务流程。

## 项目特点

- 前后端分离：后端提供 REST API，前端通过 Axios 调用接口。
- 登录鉴权：登录成功后返回 JWT，前端统一在请求头中携带 `Authorization: Bearer <token>`。
- 业务闭环：采购入库、销售出库、库存流水审批、财务月度利润汇总之间有基础联动。
- 数据库脚本：提供完整建表和初始化数据，便于本地快速启动。
- 接口文档：后端目录下包含 Apifox/OpenAPI 文档，可用于接口调试和并发测试设计。

## 功能模块

| 模块 | 说明 |
| --- | --- |
| 用户与登录 | 用户列表、分页查询、新增、修改、删除、登录认证 |
| 菜单管理 | 登录后返回菜单数据，前端根据菜单恢复导航 |
| 采购管理 | 采购订单分页、详情、新增、修改、删除、审批入库 |
| 销售管理 | 销售订单分页、详情、新增、修改、删除、审批出库 |
| 仓库管理 | 库存档案、手工入库、手工出库、出入库申请、流水审批/驳回 |
| 财务报表 | 按月份汇总销售额、采购额、费用和利润 |

## 技术栈

### 后端

- Java 8
- Spring Boot 2.7.6
- MyBatis-Plus 3.4.1
- MySQL
- JWT
- Maven

### 前端

- Vue 2.6
- Vue Router
- Vuex
- Element UI
- Axios
- Vue CLI 5

## 目录结构

```text
.
|-- ERP/                         # Spring Boot 后端
|   |-- docs/                    # Apifox/OpenAPI 接口资料
|   |-- src/main/java/com/erp/   # 控制器、服务、实体、Mapper、通用组件
|   `-- src/main/resources/      # application.yml、Mapper XML、静态资源
|-- ERP-web/erp-web/             # Vue 前端
|   |-- src/api/                 # 前端 API 封装
|   |-- src/components/          # 页面组件
|   |-- src/router/              # 路由配置
|   `-- src/store/               # Vuex 状态
`-- 数据库sql/                    # MySQL 初始化脚本
```

## 环境准备

- JDK 8+
- Maven 3.6+
- Node.js 16+ 与 npm
- MySQL 5.7+ 或 8.x

默认后端配置位于 `ERP/src/main/resources/application.yml`：

```yaml
server:
  port: 8090

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/erp?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: root
```

如本地数据库账号或密码不同，请先调整该配置。

## 快速启动

### 1. 初始化数据库

创建数据库并导入脚本：

```sql
CREATE DATABASE erp DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```

然后导入：

```text
数据库sql/erp_schema.sql
数据库sql/erp_approval.sql
```

初始化脚本内包含示例用户数据，默认可使用：

```text
账号：sa1
密码：123
```

### 2. 启动后端

```bash
cd ERP
mvn spring-boot:run
```

后端默认运行在：

```text
http://localhost:8090
```

### 3. 启动前端

```bash
cd ERP-web/erp-web
npm install
npm run serve
```

前端默认通过 `src/main.js` 中的 `Vue.prototype.$httpUrl = 'http://localhost:8090'` 访问后端。

## 常用接口

| 方法 | 地址 | 说明 |
| --- | --- | --- |
| POST | `/user/login` | 用户登录，返回用户、菜单和 JWT |
| POST | `/purchase/listPageC1` | 采购订单分页查询 |
| POST | `/purchase/save` | 新增采购订单 |
| POST | `/purchase/approve` | 审批采购订单并触发入库 |
| POST | `/sales/listPageC1` | 销售订单分页查询 |
| POST | `/sales/approve` | 审批销售订单并触发出库 |
| POST | `/warehouse/stock/listPageC1` | 库存分页查询 |
| POST | `/warehouse/inbound` | 手工入库 |
| POST | `/warehouse/outbound` | 手工出库 |
| POST | `/warehouse/flow/approve` | 审批仓库流水 |
| POST | `/finance/profit/summary` | 月度利润汇总 |

更多接口说明见：

- `ERP/docs/apifox-openapi.yaml`
- `ERP/docs/apifox-main-apis.md`

## 构建

后端打包：

```bash
cd ERP
mvn clean package
```

前端生产构建：

```bash
cd ERP-web/erp-web
npm run build
```

## README 参考

README 结构参考了若依 RuoYi-Vue、mall 和 vhr 等开源后台管理项目的常见组织方式：先介绍项目定位，再说明功能模块、技术栈、目录结构、启动步骤和接口入口。
