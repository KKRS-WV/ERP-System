import axios from 'axios'

const FIN_PREFIX = '/finance'

/**
 * 月度利润简表（由后端汇总：销售出库收入、采购入库成本、费用单等）
 * POST body: { yearMonth: 'yyyy-MM' }
 *
 * 期望响应 data 示例：
 * {
 *   yearMonth, revenue, cost, expense,
 *   grossProfit, netProfit,
 *   salesOrderCount?, purchaseOrderCount?, expenseOrderCount?,
 *   revenueNote?, costNote?, expenseNote?
 * }
 */
export function getProfitSummary(baseUrl, payload) {
  return axios.post(baseUrl + FIN_PREFIX + '/profit/summary', payload)
}
