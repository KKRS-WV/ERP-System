import axios from 'axios'

/** 仓库模块接口前缀，后端实现时保持一致 */
const WH_PREFIX = '/warehouse'

/**
 * 库存分页
 * POST { pageNum, pageSize, param: { keyword, alertOnly } }
 */
export function getStockPage(baseUrl, payload) {
  return axios.post(baseUrl + WH_PREFIX + '/stock/listPageC1', payload)
}

/**
 * 库存详情
 * GET ?id=
 */
export function getStockDetail(baseUrl, id) {
  return axios.get(baseUrl + WH_PREFIX + '/stock/detail?id=' + id)
}

/**
 * 新建库存档案（首次建档）
 * POST body: productCode, productName, specification, unit, minStock 等
 */
export function saveStock(baseUrl, payload) {
  return axios.post(baseUrl + WH_PREFIX + '/stock/save', payload)
}

/**
 * 更新库存档案（含警戒数量、名称规格等）
 * POST body: 含 id
 */
export function updateStock(baseUrl, payload) {
  return axios.post(baseUrl + WH_PREFIX + '/stock/update', payload)
}

/**
 * 产品入库
 * POST body: productCode, quantity, bizDate, remark, productName? ...
 */
export function inboundStock(baseUrl, payload) {
  return axios.post(baseUrl + WH_PREFIX + '/inbound', payload)
}

/** 手工入库提交审批（待审批，不直接改库存） */
export function applyInboundStock(baseUrl, payload) {
  return axios.post(baseUrl + WH_PREFIX + '/inbound/apply', payload)
}

/**
 * 产品出库
 * POST body: productCode, quantity, bizDate, remark ...
 */
export function outboundStock(baseUrl, payload) {
  return axios.post(baseUrl + WH_PREFIX + '/outbound', payload)
}

/** 手工出库提交审批（待审批，不直接改库存） */
export function applyOutboundStock(baseUrl, payload) {
  return axios.post(baseUrl + WH_PREFIX + '/outbound/apply', payload)
}

/**
 * 低于警戒的库存列表（可与 listPage 合并，此处预留独立接口）
 * GET
 */
export function getAlertStockList(baseUrl) {
  return axios.get(baseUrl + WH_PREFIX + '/alert/list')
}

/**
 * 出入库流水分页
 * POST { pageNum, pageSize, param: { keyword, flowType } } flowType: in | out | ''
 */
export function getFlowPage(baseUrl, payload) {
  return axios.post(baseUrl + WH_PREFIX + '/flow/listPageC1', payload)
}

/** 审批通过待审批流水（pin/pout -> in/out） */
export function approveFlow(baseUrl, payload) {
  return axios.post(baseUrl + WH_PREFIX + '/flow/approve', payload)
}

/** 驳回待审批流水（pin/pout -> rin/rout） */
export function rejectFlow(baseUrl, payload) {
  return axios.post(baseUrl + WH_PREFIX + '/flow/reject', payload)
}
