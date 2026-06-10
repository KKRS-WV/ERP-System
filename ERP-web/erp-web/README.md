# ERP-Web

ERP-System 的 Vue 2 前端管理端，基于 Vue CLI、Vue Router、Vuex、Element UI 和 Axios 构建。

## 功能页面

- 登录页
- 首页工作台
- 用户管理
- 采购管理
- 销售管理
- 仓库管理
- 财务报表

## Project setup

```
npm install
```

### Compiles and hot-reloads for development

```
npm run serve
```

### Compiles and minifies for production

```
npm run build
```

### Lints and fixes files

```
npm run lint
```

## 后端地址

默认后端地址在 `src/main.js` 中配置：

```js
Vue.prototype.$httpUrl = 'http://localhost:8090'
```

如后端端口或部署地址发生变化，请同步修改该配置。
