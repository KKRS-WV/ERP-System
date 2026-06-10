import axios from 'axios'

/** 销售订单接口前缀，后端实现时保持一致 */
const SALES_PREFIX = '/sales'

/**
 * 分页查询销售订单
 * POST { pageNum, pageSize, param: { keyword, status } }
 */
export function getSalesOrderPage(baseUrl, payload) {
  return axios.post(baseUrl + SALES_PREFIX + '/listPageC1', payload)
}

/**
 * 销售订单详情
 * GET ?id=
 */
export function getSalesOrderDetail(baseUrl, id) {
  return axios.get(baseUrl + SALES_PREFIX + '/detail?id=' + id)
}

/**
 * 新建销售订单
 * POST body: 订单主表 + items 明细数组
 */
export function createSalesOrder(baseUrl, payload) {
  return axios.post(baseUrl + SALES_PREFIX + '/save', payload)
}

/**
 * 更新销售订单
 * POST body: 含 id
 */
export function updateSalesOrder(baseUrl, payload) {
  return axios.post(baseUrl + SALES_PREFIX + '/update', payload)
}

/**
 * 删除销售订单
 * DELETE ?id=
 */
export function deleteSalesOrder(baseUrl, id) {
  return axios.delete(baseUrl + SALES_PREFIX + '/del?id=' + id)
}

/** 审批通过（管理员/超管），body: { id, operatorId } */
export function approveSalesOrder(baseUrl, payload) {
  return axios.post(baseUrl + SALES_PREFIX + '/approve', payload)
}
