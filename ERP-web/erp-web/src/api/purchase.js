import axios from 'axios'

const PURCHASE_PREFIX = '/purchase'

export function getPurchaseOrderPage(baseUrl, payload) {
  return axios.post(baseUrl + PURCHASE_PREFIX + '/listPageC1', payload)
}

export function getPurchaseOrderDetail(baseUrl, id) {
  return axios.get(baseUrl + PURCHASE_PREFIX + '/detail?id=' + id)
}

export function createPurchaseOrder(baseUrl, payload) {
  return axios.post(baseUrl + PURCHASE_PREFIX + '/save', payload)
}

export function updatePurchaseOrder(baseUrl, payload) {
  return axios.post(baseUrl + PURCHASE_PREFIX + '/update', payload)
}

export function deletePurchaseOrder(baseUrl, id) {
  return axios.delete(baseUrl + PURCHASE_PREFIX + '/del?id=' + id)
}

/** 审批通过（管理员/超管），body: { id, operatorId } */
export function approvePurchaseOrder(baseUrl, payload) {
  return axios.post(baseUrl + PURCHASE_PREFIX + '/approve', payload)
}
