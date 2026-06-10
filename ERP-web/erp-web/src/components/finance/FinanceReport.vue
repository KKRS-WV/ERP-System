<template>
  <div class="fr-page">
    <header class="fr-head">
      <div class="fr-head-text">
        <p class="fr-eyebrow"><span class="fr-dot" /> FINANCE · P&amp;L</p>
        <h1 class="fr-title">财务报表</h1>
        <p class="fr-desc">选择自然月，查看该月销售出库（收入）、采购入库（成本）、费用单（费用）汇总后的利润简表。</p>
      </div>
      <div class="fr-head-glow" aria-hidden="true" />
    </header>

    <div class="fr-toolbar">
      <div class="fr-toolbar-inner">
        <span class="fr-label">报表月份</span>
        <el-date-picker
          v-model="yearMonth"
          type="month"
          value-format="yyyy-MM"
          placeholder="选择月份"
          :clearable="false"
          class="fr-month"
          popper-class="fr-month-popper"
        />
        <el-button type="primary" icon="el-icon-data-analysis" class="fr-btn-query" :loading="loading" @click="loadReport">生成报表</el-button>
      </div>
    </div>

    <div v-if="loaded" class="fr-body">
      <div class="fr-period">
        <span class="fr-period-badge">{{ displayPeriod }}</span>
        <span class="fr-period-hint">数据来源：销售出库单 · 采购入库单 · 费用单（以后端汇总为准）</span>
      </div>

      <div class="fr-cards">
        <div class="fr-card fr-card--rev">
          <p class="fr-card-label">营业收入（销售出库）</p>
          <p class="fr-card-value">￥{{ money(summary.revenue) }}</p>
          <p v-if="summary.salesOrderCount != null" class="fr-card-meta">关联单据约 {{ summary.salesOrderCount }} 笔</p>
        </div>
        <div class="fr-card fr-card--cost">
          <p class="fr-card-label">营业成本（采购入库）</p>
          <p class="fr-card-value">￥{{ money(summary.cost) }}</p>
          <p v-if="summary.purchaseOrderCount != null" class="fr-card-meta">关联单据约 {{ summary.purchaseOrderCount }} 笔</p>
        </div>
        <div class="fr-card fr-card--exp">
          <p class="fr-card-label">期间费用（费用单）</p>
          <p class="fr-card-value">￥{{ money(summary.expense) }}</p>
          <p v-if="summary.expenseOrderCount != null" class="fr-card-meta">费用单 {{ summary.expenseOrderCount }} 笔</p>
        </div>
      </div>

      <div class="fr-statement">
        <h2 class="fr-st-title">利润简表</h2>
        <table class="fr-table">
          <tbody>
            <tr>
              <th scope="row">一、营业收入</th>
              <td class="fr-num">￥{{ money(summary.revenue) }}</td>
            </tr>
            <tr>
              <th scope="row">减：营业成本</th>
              <td class="fr-num fr-num--minus">￥{{ money(summary.cost) }}</td>
            </tr>
            <tr class="fr-subtotal">
              <th scope="row">毛利润</th>
              <td class="fr-num fr-num--emph">￥{{ money(grossProfit) }}</td>
            </tr>
            <tr>
              <th scope="row">减：期间费用</th>
              <td class="fr-num fr-num--minus">￥{{ money(summary.expense) }}</td>
            </tr>
            <tr class="fr-total">
              <th scope="row">二、净利润</th>
              <td class="fr-num fr-num--total">￥{{ money(netProfit) }}</td>
            </tr>
          </tbody>
        </table>
        <p class="fr-formula">
          毛利润 = 营业收入 − 营业成本；净利润 = 毛利润 − 期间费用（与后端计算口径一致）。
        </p>
      </div>
    </div>

    <div v-else class="fr-empty">
      <i class="el-icon-document" />
      <p>请选择月份并点击「生成报表」</p>
    </div>
  </div>
</template>

<script>
import { getProfitSummary } from '../../api/finance'

function currentYearMonth() {
  const d = new Date()
  const pad = (n) => (n < 10 ? '0' + n : '' + n)
  return d.getFullYear() + '-' + pad(d.getMonth() + 1)
}

export default {
  name: 'FinanceReport',
  data() {
    return {
      yearMonth: currentYearMonth(),
      loading: false,
      loaded: false,
      summary: {
        yearMonth: '',
        revenue: 0,
        cost: 0,
        expense: 0,
        grossProfit: null,
        netProfit: null,
        salesOrderCount: null,
        purchaseOrderCount: null,
        expenseOrderCount: null
      }
    }
  },
  computed: {
    curUser() {
      try {
        return JSON.parse(sessionStorage.getItem('CurUser') || '{}')
      } catch (e) {
        return {}
      }
    },
    canView() {
      return Number(this.curUser.roleId) <= 1
    },
    displayPeriod() {
      const ym = this.summary.yearMonth || this.yearMonth
      if (!ym || ym.length < 7) return ''
      const [y, m] = ym.split('-')
      return y + ' 年 ' + Number(m) + ' 月'
    },
    grossProfit() {
      if (this.summary.grossProfit != null) return Number(this.summary.grossProfit)
      return Number(this.summary.revenue || 0) - Number(this.summary.cost || 0)
    },
    netProfit() {
      if (this.summary.netProfit != null) return Number(this.summary.netProfit)
      return this.grossProfit - Number(this.summary.expense || 0)
    }
  },
  methods: {
    money(val) {
      return Number(val || 0).toFixed(2)
    },
    loadReport() {
      if (!this.canView) {
        this.$message.error('仅管理员及以上可查看财务报表')
        return
      }
      if (!this.yearMonth) {
        this.$message.warning('请选择报表月份')
        return
      }
      this.loading = true
      getProfitSummary(this.$httpUrl, { yearMonth: this.yearMonth, operatorId: this.curUser.id })
        .then((res) => res.data)
        .then((res) => {
          if (res.code === 200 && res.data) {
            this.summary = {
              yearMonth: res.data.yearMonth || this.yearMonth,
              revenue: Number(res.data.revenue != null ? res.data.revenue : 0),
              cost: Number(res.data.cost != null ? res.data.cost : 0),
              expense: Number(res.data.expense != null ? res.data.expense : 0),
              grossProfit: res.data.grossProfit != null ? Number(res.data.grossProfit) : null,
              netProfit: res.data.netProfit != null ? Number(res.data.netProfit) : null,
              salesOrderCount: res.data.salesOrderCount,
              purchaseOrderCount: res.data.purchaseOrderCount,
              expenseOrderCount: res.data.expenseOrderCount
            }
            this.loaded = true
          } else {
            this.$message.error((res && res.msg) || '获取报表失败')
          }
        })
        .catch(() => {
          this.$message.error('财务报表接口未就绪')
        })
        .finally(() => {
          this.loading = false
        })
    }
  },
  beforeMount() {
    if (!this.canView) {
      this.$message.error('仅管理员及以上可访问财务报表')
      this.$router.replace('/Home')
      return
    }
    this.loadReport()
  }
}
</script>

<style scoped>
.fr-page {
  --fr-violet: #8b5cf6;
  --fr-slate: #1e293b;
  text-align: left;
  position: relative;
  overflow: hidden;
}

.fr-page::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  opacity: 0.28;
  background-image:
    linear-gradient(rgba(139, 92, 246, 0.07) 1px, transparent 1px),
    linear-gradient(90deg, rgba(139, 92, 246, 0.07) 1px, transparent 1px);
  background-size: 40px 40px;
  z-index: 0;
}

.fr-head {
  position: relative;
  z-index: 1;
  margin-bottom: 18px;
  padding: 22px 24px 20px;
  border-radius: 16px;
  overflow: hidden;
  background: linear-gradient(125deg, #1e1b4b 0%, #4c1d95 42%, #6d28d9 100%);
  box-shadow:
    0 16px 48px rgba(91, 33, 182, 0.28),
    0 0 0 1px rgba(255, 255, 255, 0.08) inset;
}

.fr-head-glow {
  position: absolute;
  right: -14%;
  top: -45%;
  width: 48%;
  height: 170%;
  background: radial-gradient(circle, rgba(167, 139, 250, 0.4) 0%, transparent 65%);
  pointer-events: none;
}

.fr-head-text {
  position: relative;
  z-index: 1;
}

.fr-eyebrow {
  margin: 0 0 8px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.18em;
  color: rgba(237, 233, 254, 0.9);
  display: flex;
  align-items: center;
  gap: 8px;
}

.fr-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #c4b5fd;
  box-shadow: 0 0 12px #a78bfa;
}

.fr-title {
  margin: 0 0 6px;
  font-size: 24px;
  font-weight: 700;
  letter-spacing: -0.03em;
  color: #f5f3ff;
}

.fr-desc {
  margin: 0;
  font-size: 13px;
  color: rgba(237, 233, 254, 0.82);
  max-width: 720px;
  line-height: 1.55;
}

.fr-toolbar {
  position: relative;
  z-index: 1;
  margin-bottom: 20px;
  padding: 1px;
  border-radius: 14px;
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.45), rgba(56, 189, 248, 0.2));
  box-shadow: 0 8px 32px rgba(30, 27, 75, 0.1);
}

.fr-toolbar-inner {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  align-items: center;
  padding: 14px 18px;
  border-radius: 13px;
  background: rgba(255, 255, 255, 0.94);
  backdrop-filter: blur(12px);
}

.fr-label {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}

.fr-month {
  width: 200px;
}

.fr-btn-query {
  border-radius: 10px;
  font-weight: 600;
  box-shadow: 0 4px 14px rgba(109, 40, 217, 0.35);
}

.fr-body {
  position: relative;
  z-index: 1;
}

.fr-period {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 18px;
}

.fr-period-badge {
  display: inline-block;
  padding: 6px 14px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 700;
  color: #4c1d95;
  background: linear-gradient(135deg, #ede9fe, #e0e7ff);
  border: 1px solid rgba(139, 92, 246, 0.25);
}

.fr-period-hint {
  font-size: 12px;
  color: #64748b;
}

.fr-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 14px;
  margin-bottom: 22px;
}

.fr-card {
  border-radius: 14px;
  padding: 18px 18px 16px;
  border: 1px solid rgba(148, 163, 184, 0.25);
  background: #fff;
  box-shadow: 0 10px 36px rgba(15, 23, 42, 0.06);
}

.fr-card--rev {
  border-color: rgba(16, 185, 129, 0.25);
  background: linear-gradient(165deg, #ecfdf5 0%, #fff 55%);
}

.fr-card--cost {
  border-color: rgba(249, 115, 22, 0.25);
  background: linear-gradient(165deg, #fff7ed 0%, #fff 55%);
}

.fr-card--exp {
  border-color: rgba(59, 130, 246, 0.25);
  background: linear-gradient(165deg, #eff6ff 0%, #fff 55%);
}

.fr-card-label {
  margin: 0 0 8px;
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  letter-spacing: 0.02em;
}

.fr-card-value {
  margin: 0;
  font-size: 22px;
  font-weight: 800;
  letter-spacing: -0.02em;
  color: #0f172a;
}

.fr-card-meta {
  margin: 10px 0 0;
  font-size: 12px;
  color: #94a3b8;
}

.fr-statement {
  border-radius: 16px;
  padding: 1px;
  background: linear-gradient(145deg, rgba(139, 92, 246, 0.15), rgba(14, 165, 233, 0.1));
  box-shadow: 0 12px 40px rgba(15, 23, 42, 0.06);
}

.fr-st-title {
  margin: 0;
  padding: 16px 20px 12px;
  font-size: 15px;
  font-weight: 700;
  color: #312e81;
  background: #fafaff;
  border-radius: 15px 15px 0 0;
  border-bottom: 1px solid rgba(139, 92, 246, 0.12);
}

.fr-table {
  width: 100%;
  border-collapse: collapse;
  background: #fff;
  border-radius: 0 0 15px 15px;
  overflow: hidden;
}

.fr-table th {
  text-align: left;
  font-weight: 600;
  color: #334155;
  padding: 14px 20px;
  width: 55%;
  border-bottom: 1px solid #f1f5f9;
}

.fr-table td {
  padding: 14px 20px;
  border-bottom: 1px solid #f1f5f9;
}

.fr-num {
  text-align: right;
  font-variant-numeric: tabular-nums;
  font-weight: 700;
  color: #0f172a;
}

.fr-num--minus {
  color: #c2410c;
}

.fr-num--emph {
  color: #047857;
  font-size: 16px;
}

.fr-subtotal th,
.fr-subtotal td {
  background: rgba(236, 253, 245, 0.55);
}

.fr-total th,
.fr-total td {
  background: linear-gradient(90deg, rgba(237, 233, 254, 0.65), rgba(224, 231, 255, 0.45));
  border-bottom: none;
}

.fr-num--total {
  font-size: 20px;
  color: #4c1d95;
}

.fr-formula {
  margin: 0;
  padding: 12px 20px 16px;
  font-size: 12px;
  color: #64748b;
  line-height: 1.5;
  background: #fafafa;
  border-radius: 0 0 15px 15px;
}

.fr-empty {
  position: relative;
  z-index: 1;
  text-align: center;
  padding: 48px 24px;
  color: #94a3b8;
  font-size: 14px;
  border-radius: 16px;
  border: 1px dashed rgba(148, 163, 184, 0.45);
  background: rgba(248, 250, 252, 0.85);
}

.fr-empty i {
  font-size: 40px;
  display: block;
  margin-bottom: 12px;
  opacity: 0.55;
}
</style>

<style>
.fr-month .el-input__inner {
  border-radius: 10px;
  font-weight: 600;
}
</style>
