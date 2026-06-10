<template>
  <div class="sm-page">
    <header class="sm-head">
      <div class="sm-head-text">
        <p class="sm-eyebrow"><span class="sm-dot" /> SALES · ORDER</p>
        <h1 class="sm-title">销售订单</h1>
        <p class="sm-desc">
          普通用户将订单置为「待审核」后，仅管理员/超级管理员可审批；审批通过将按明细自动出库（需库存充足，明细填写产品编号或与库存档案名称一致）。
        </p>
      </div>
      <div class="sm-head-glow" aria-hidden="true" />
    </header>

    <div class="sm-toolbar">
      <div class="sm-toolbar-inner">
        <el-input
          v-model="keyword"
          class="sm-input"
          clearable
          placeholder="订单号 / 客户 / 商品关键字"
          suffix-icon="el-icon-search"
          @keyup.enter.native="applySearch"
          @clear="onFilterCleared"
        />
        <el-select
          v-model="status"
          class="sm-select"
          clearable
          placeholder="筛选状态"
          @change="onStatusChange"
          @clear="onFilterCleared"
        >
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-button type="primary" icon="el-icon-search" class="sm-btn-search" @click="applySearch">搜索</el-button>
        <el-button type="primary" plain icon="el-icon-plus" class="sm-btn-add" @click="add">新增订单</el-button>
      </div>
    </div>

    <div class="sm-table-shell">
      <el-table :data="tableData" stripe class="sm-table" :header-cell-style="tableHeaderStyle">
        <el-table-column prop="orderNo" label="订单号" min-width="140" show-overflow-tooltip />
        <el-table-column prop="customerName" label="客户名称" min-width="120" show-overflow-tooltip />
        <el-table-column prop="customerPhone" label="联系电话" min-width="120" show-overflow-tooltip />
        <el-table-column prop="customerAddress" label="联系地址" min-width="180" show-overflow-tooltip />
        <el-table-column prop="itemCount" label="商品数" width="88" align="center" />
        <el-table-column prop="totalQuantity" label="总数量" width="96" align="center" />
        <el-table-column label="总金额" width="120" align="right">
          <template slot-scope="scope">￥{{ money(scope.row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120" align="center">
          <template slot-scope="scope">
            <span class="sm-status" :class="'sm-status--' + scope.row.status">{{ getStatusLabel(scope.row.status) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="orderDate" label="销售日期" width="120" align="center" />
        <el-table-column label="操作" width="268" fixed="right" align="center">
          <template slot-scope="scope">
            <el-button
              v-if="canApprove && scope.row.status === 'pending'"
              size="mini"
              class="sm-op sm-op--approve"
              @click="approve(scope.row)"
            >
              审批通过
            </el-button>
            <el-button size="mini" class="sm-op sm-op--edit" @click="mod(scope.row)">
              {{ scope.row.status === 'completed' ? '查看' : '编辑' }}
            </el-button>
            <el-popconfirm
              v-if="scope.row.status !== 'completed'"
              title="确定删除该销售单？"
              @confirm="del(scope.row.id)"
            >
              <el-button slot="reference" size="mini" class="sm-op sm-op--del">删除</el-button>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div class="sm-pagination">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :current-page="pageNum"
          :page-sizes="[5, 10, 20, 50]"
          :page-size="pageSize"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <el-dialog
      :title="dialogTitle"
      :visible.sync="dialogVisible"
      width="920px"
      custom-class="sm-dialog"
      :close-on-click-modal="false"
      append-to-body
    >
      <el-form ref="form" :model="form" :rules="rules" label-width="96px" class="sm-form">
        <el-row :gutter="14">
          <el-col :span="8">
            <el-form-item label="订单号" prop="orderNo">
              <el-input v-model="form.orderNo" placeholder="如 SO20260320001" :disabled="isReadOnly" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="销售日期" prop="orderDate">
              <el-date-picker
                v-model="form.orderDate"
                type="date"
                value-format="yyyy-MM-dd"
                placeholder="选择日期"
                style="width: 100%;"
                :disabled="isReadOnly"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态" prop="status">
              <el-select v-model="form.status" placeholder="请选择状态" style="width: 100%;" :disabled="isReadOnly">
                <el-option v-for="item in statusOptionsForForm" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="14">
          <el-col :span="8">
            <el-form-item label="客户名称" prop="customerName">
              <el-input v-model="form.customerName" placeholder="客户名称" :disabled="isReadOnly" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="联系人" prop="customerContact">
              <el-input v-model="form.customerContact" placeholder="联系人" :disabled="isReadOnly" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="联系电话" prop="customerPhone">
              <el-input v-model="form.customerPhone" placeholder="手机 / 固话" :disabled="isReadOnly" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="联系地址" prop="customerAddress">
          <el-input v-model="form.customerAddress" placeholder="收货或开票地址" :disabled="isReadOnly" />
        </el-form-item>

        <div class="sm-items-head">
          <span class="sm-items-title">销售商品明细</span>
          <el-button v-if="!isReadOnly" type="primary" plain size="mini" icon="el-icon-plus" @click="addItem">添加商品</el-button>
        </div>
        <el-table :data="form.items" size="mini" border class="sm-items-table">
          <el-table-column label="产品编号" min-width="120">
            <template slot-scope="scope">
              <el-input v-model="scope.row.productCode" placeholder="与仓库一致" size="mini" :disabled="isReadOnly" />
            </template>
          </el-table-column>
          <el-table-column label="商品名称" min-width="140">
            <template slot-scope="scope">
              <el-input v-model="scope.row.productName" placeholder="商品名称" size="mini" :disabled="isReadOnly" />
            </template>
          </el-table-column>
          <el-table-column label="规格" min-width="120">
            <template slot-scope="scope">
              <el-input v-model="scope.row.specification" placeholder="规格" size="mini" :disabled="isReadOnly" />
            </template>
          </el-table-column>
          <el-table-column label="数量" width="100">
            <template slot-scope="scope">
              <el-input-number
                v-model="scope.row.quantity"
                :min="1"
                :step="1"
                size="mini"
                :disabled="isReadOnly"
                @change="reCalcRow(scope.row)"
              />
            </template>
          </el-table-column>
          <el-table-column label="单价" width="130">
            <template slot-scope="scope">
              <el-input-number
                v-model="scope.row.unitPrice"
                :min="0"
                :precision="2"
                :step="0.1"
                size="mini"
                :disabled="isReadOnly"
                @change="reCalcRow(scope.row)"
              />
            </template>
          </el-table-column>
          <el-table-column label="小计" width="130" align="right">
            <template slot-scope="scope">￥{{ money(scope.row.amount) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="80" align="center">
            <template slot-scope="scope">
              <el-button v-if="!isReadOnly" type="text" class="sm-item-del" @click="removeItem(scope.$index)">删除</el-button>
              <span v-else>—</span>
            </template>
          </el-table-column>
        </el-table>

        <div class="sm-summary">
          <span>商品种类：{{ form.items.length }}</span>
          <span>总数量：{{ totalQuantity }}</span>
          <span>订单总金额：<b>￥{{ money(totalAmount) }}</b></span>
        </div>

        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="备注信息（可选）" :disabled="isReadOnly" />
        </el-form-item>
      </el-form>

      <span slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">{{ isReadOnly ? '关 闭' : '取 消' }}</el-button>
        <el-button v-if="!isReadOnly" type="primary" @click="save">确 定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import {
  approveSalesOrder,
  createSalesOrder,
  deleteSalesOrder,
  getSalesOrderDetail,
  getSalesOrderPage,
  updateSalesOrder
} from '../../api/sales'

const statusOptions = [
  { label: '草稿', value: 'draft' },
  { label: '待审核', value: 'pending' },
  { label: '已完成', value: 'completed' },
  { label: '已取消', value: 'cancelled' }
]

function normalizeStatusValue(status) {
  const raw = status == null ? '' : String(status).trim().toLowerCase()
  if (!raw) return ''
  if (raw === 'draft' || raw === 'pending' || raw === 'completed' || raw === 'cancelled') {
    return raw
  }
  if (raw === '草稿') return 'draft'
  if (raw === '待审核') return 'pending'
  if (raw === '已完成') return 'completed'
  if (raw === '已取消') return 'cancelled'
  return ''
}

function defaultItem() {
  return {
    productCode: '',
    productName: '',
    specification: '',
    quantity: 1,
    unitPrice: 0,
    amount: 0
  }
}

function defaultForm() {
  return {
    id: null,
    orderNo: '',
    orderDate: '',
    status: 'draft',
    customerName: '',
    customerContact: '',
    customerPhone: '',
    customerAddress: '',
    items: [defaultItem()],
    remark: ''
  }
}

function num(v, def) {
  if (v === null || v === undefined || v === '') return def
  const n = Number(v)
  return Number.isFinite(n) ? n : def
}

function pad2(n) {
  return String(n).padStart(2, '0')
}

function suggestSalesOrderNo() {
  const d = new Date()
  return (
    'SO' +
    d.getFullYear() +
    pad2(d.getMonth() + 1) +
    pad2(d.getDate()) +
    pad2(d.getHours()) +
    pad2(d.getMinutes()) +
    pad2(d.getSeconds()) +
    String(Math.floor(Math.random() * 900) + 100)
  )
}

function todayStr() {
  const d = new Date()
  return d.getFullYear() + '-' + pad2(d.getMonth() + 1) + '-' + pad2(d.getDate())
}

export default {
  name: 'SalesManage',
  data() {
    return {
      keyword: '',
      status: '',
      statusOptions,
      tableData: [],
      pageNum: 1,
      pageSize: 10,
      total: 0,
      dialogVisible: false,
      isReadOnly: false,
      tableHeaderStyle: {
        background: 'linear-gradient(180deg, #ecfdf5 0%, #d1fae5 100%)',
        color: '#064e3b',
        fontWeight: '600',
        borderBottom: '1px solid rgba(16, 185, 129, 0.18)'
      },
      form: defaultForm(),
      rules: {
        orderNo: [{ required: true, message: '请输入订单号', trigger: 'blur' }],
        orderDate: [{ required: true, message: '请选择销售日期', trigger: 'change' }],
        customerName: [{ required: true, message: '请输入客户名称', trigger: 'blur' }],
        customerContact: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
        customerPhone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }],
        customerAddress: [{ required: true, message: '请输入联系地址', trigger: 'blur' }],
        status: [{ required: true, message: '请选择状态', trigger: 'change' }]
      }
    }
  },
  computed: {
    dialogTitle() {
      if (this.isReadOnly) return '查看销售订单（已完成）'
      return this.form.id ? '编辑销售订单' : '新增销售订单'
    },
    canApprove() {
      try {
        const u = JSON.parse(sessionStorage.getItem('CurUser') || '{}')
        const r = Number(u.roleId)
        return r === 0 || r === 1
      } catch (e) {
        return false
      }
    },
    statusOptionsForForm() {
      return this.statusOptions.filter((o) => o.value !== 'completed')
    },
    totalQuantity() {
      return this.form.items.reduce((sum, item) => sum + Number(item.quantity || 0), 0)
    },
    totalAmount() {
      return this.form.items.reduce((sum, item) => sum + Number(item.amount || 0), 0)
    }
  },
  methods: {
    money(val) {
      return Number(val || 0).toFixed(2)
    },
    getStatusLabel(status) {
      const normalized = normalizeStatusValue(status)
      const target = this.statusOptions.find((item) => item.value === normalized)
      return target ? target.label : '未知'
    },
    onFilterCleared() {
      this.pageNum = 1
      this.loadPost()
    },
    onStatusChange() {
      this.pageNum = 1
      this.loadPost()
    },
    applySearch() {
      this.pageNum = 1
      this.loadPost()
    },
    handleSizeChange(val) {
      this.pageSize = val
      this.pageNum = 1
      this.loadPost()
    },
    handleCurrentChange(val) {
      this.pageNum = val
      this.loadPost()
    },
    normalizeItems(items) {
      if (!Array.isArray(items) || items.length === 0) {
        return [defaultItem()]
      }
      return items.map((item) => {
        const qty = num(item.quantity, 1)
        const price = num(item.unitPrice, 0)
        const computed = Number((qty * price).toFixed(2))
        let amt = item.amount != null && item.amount !== '' ? num(item.amount, computed) : computed
        if (!Number.isFinite(amt)) amt = computed
        return {
          productCode: item.productCode != null ? String(item.productCode) : '',
          productName: item.productName != null ? String(item.productName) : '',
          specification: item.specification != null ? String(item.specification) : '',
          quantity: qty,
          unitPrice: price,
          amount: Number(Number(amt).toFixed(2))
        }
      })
    },
    reCalcRow(row) {
      row.amount = Number((Number(row.quantity || 0) * Number(row.unitPrice || 0)).toFixed(2))
    },
    addItem() {
      this.form.items.push(defaultItem())
    },
    removeItem(index) {
      this.form.items.splice(index, 1)
      if (!this.form.items.length) {
        this.form.items.push(defaultItem())
      }
    },
    resetForm() {
      this.form = defaultForm()
      this.$nextTick(() => {
        if (this.$refs.form) {
          this.$refs.form.clearValidate()
        }
      })
    },
    add() {
      this.isReadOnly = false
      this.dialogVisible = true
      this.resetForm()
      this.form.orderNo = suggestSalesOrderNo()
      this.form.orderDate = todayStr()
    },
    mod(row) {
      if (!row || !row.id) return
      this.dialogVisible = true
      this.isReadOnly = row.status === 'completed'
      getSalesOrderDetail(this.$httpUrl, row.id)
        .then((res) => res.data)
        .then((res) => {
          if (res.code === 200 && res.data) {
            this.isReadOnly = res.data.status === 'completed'
            const d = res.data
            const { items: rawItems, ...rest } = d
            this.form = {
              ...defaultForm(),
              ...rest,
              status: normalizeStatusValue(rest.status) || 'draft',
              items: this.normalizeItems(rawItems)
            }
          } else {
            this.fillFormWithRow(row)
          }
        })
        .catch(() => {
          this.fillFormWithRow(row)
        })
    },
    fillFormWithRow(row) {
      this.isReadOnly = row.status === 'completed'
      this.form = {
        ...defaultForm(),
        ...row,
        status: normalizeStatusValue(row.status) || 'draft',
        items: this.normalizeItems(row.items)
      }
    },
    approve(row) {
      let u = {}
      try {
        u = JSON.parse(sessionStorage.getItem('CurUser') || '{}')
      } catch (e) {
        u = {}
      }
      if (!u.id) {
        this.$message.warning('请重新登录')
        return
      }
      this.$confirm('确认审批通过？将按明细数量执行出库（需库存充足，产品编号或名称可匹配）。', '审批', {
        type: 'warning',
        confirmButtonText: '确定',
        cancelButtonText: '取消'
      })
        .then(() => {
          approveSalesOrder(this.$httpUrl, { id: row.id, operatorId: u.id })
            .then((res) => res.data)
            .then((res) => {
              if (res.code === 200) {
                this.$message.success('审批通过，已出库')
                this.loadPost()
              } else {
                this.$message.error((res && res.msg) || '审批失败')
              }
            })
            .catch(() => {
              this.$message.error('审批失败')
            })
        })
        .catch(() => {})
    },
    del(id) {
      deleteSalesOrder(this.$httpUrl, id)
        .then((res) => res.data)
        .then((res) => {
          if (res.code === 200) {
            this.$message.success('删除成功')
            this.loadPost()
          } else {
            this.$message.error((res && res.msg) || '删除失败')
          }
        })
        .catch(() => {
          this.$message.error('删除失败，接口未就绪')
        })
    },
    buildItemsPayload() {
      return this.form.items.map((row) => {
        const qty = num(row.quantity, 1)
        const price = num(row.unitPrice, 0)
        return {
          productCode: (row.productCode || '').trim(),
          productName: (row.productName || '').trim(),
          specification: (row.specification || '').trim(),
          quantity: qty,
          unitPrice: price,
          amount: Number((qty * price).toFixed(2))
        }
      })
    },
    doSave(payload) {
      const isUpdate = payload.id != null && payload.id !== '' && Number(payload.id) > 0
      const request = isUpdate ? updateSalesOrder(this.$httpUrl, payload) : createSalesOrder(this.$httpUrl, payload)
      request
        .then((res) => res.data)
        .then((res) => {
          if (res.code === 200) {
            this.$message.success('保存成功')
            this.dialogVisible = false
            this.loadPost()
          } else {
            this.$message.error((res && res.msg) || '保存失败')
          }
        })
        .catch((err) => {
          const d = err.response && err.response.data
          const m =
            (d && (d.msg || d.message)) ||
            (typeof d === 'string' ? d : '') ||
            (err.message && String(err.message))
          this.$message.error(m || '保存失败，请检查网络或后端日志')
        })
    },
    save() {
      this.$refs.form.validate((valid) => {
        if (!valid) return false
        const hasInvalidItem = this.form.items.some(
          (item) =>
            !item ||
            !String(item.productName || '').trim() ||
            Number(item.quantity || 0) <= 0
        )
        if (hasInvalidItem) {
          this.$message.warning('请完善商品名称和数量')
          return false
        }
        let u = {}
        try {
          u = JSON.parse(sessionStorage.getItem('CurUser') || '{}')
        } catch (e) {
          u = {}
        }
        const payload = {
          orderNo: (this.form.orderNo || '').trim(),
          orderDate: this.form.orderDate,
          status: normalizeStatusValue(this.form.status) || 'draft',
          customerName: (this.form.customerName || '').trim(),
          customerContact: (this.form.customerContact || '').trim(),
          customerPhone: (this.form.customerPhone || '').trim(),
          customerAddress: (this.form.customerAddress || '').trim(),
          remark: (this.form.remark || '').trim(),
          items: this.buildItemsPayload(),
          itemCount: this.form.items.length,
          totalQuantity: this.totalQuantity,
          totalAmount: Number(this.totalAmount.toFixed(2)),
          operatorId: u.id
        }
        const fid = this.form.id
        if (fid !== null && fid !== undefined && fid !== '' && Number(fid) > 0) {
          payload.id = Number(fid)
        }
        this.doSave(payload)
        return true
      })
    },
    loadPost() {
      getSalesOrderPage(this.$httpUrl, {
        pageSize: this.pageSize,
        pageNum: this.pageNum,
        param: {
          keyword: this.keyword,
          status: this.status
        }
      })
        .then((res) => res.data)
        .then((res) => {
          if (res.code === 200) {
            this.tableData = (res.data || []).map((row) => ({
              ...row,
              status: normalizeStatusValue(row.status) || row.status,
              itemCount: row.itemCount || (Array.isArray(row.items) ? row.items.length : 0),
              totalQuantity: row.totalQuantity || 0,
              totalAmount: row.totalAmount || 0
            }))
            this.total = Number(res.total || 0)
          } else {
            this.$message.error((res && res.msg) || '获取销售数据失败')
          }
        })
        .catch(() => {
          this.tableData = []
          this.total = 0
          this.$message.warning('销售接口未就绪，当前展示为空列表')
        })
    }
  },
  beforeMount() {
    this.loadPost()
  }
}
</script>

<style scoped>
.sm-page {
  --sm-mint: #34d399;
  --sm-emerald: #059669;
  text-align: left;
  position: relative;
  overflow: hidden;
}

.sm-page::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  opacity: 0.35;
  background-image:
    linear-gradient(rgba(16, 185, 129, 0.08) 1px, transparent 1px),
    linear-gradient(90deg, rgba(16, 185, 129, 0.08) 1px, transparent 1px);
  background-size: 44px 44px;
  z-index: 0;
}

.sm-head {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 20px;
  padding: 22px 24px 20px;
  border-radius: 16px;
  overflow: hidden;
  background: linear-gradient(125deg, #064e3b 0%, #047857 48%, #0d9488 100%);
  box-shadow:
    0 16px 48px rgba(16, 185, 129, 0.22),
    0 0 0 1px rgba(255, 255, 255, 0.08) inset;
}

.sm-head-glow {
  position: absolute;
  right: -18%;
  top: -55%;
  width: 52%;
  height: 190%;
  background: radial-gradient(circle, rgba(52, 211, 153, 0.38) 0%, transparent 65%);
  pointer-events: none;
}

.sm-head-text {
  position: relative;
  z-index: 1;
}

.sm-eyebrow {
  margin: 0 0 8px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.2em;
  color: rgba(236, 253, 245, 0.88);
  display: flex;
  align-items: center;
  gap: 8px;
}

.sm-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--sm-mint);
  box-shadow: 0 0 12px var(--sm-mint);
}

.sm-title {
  margin: 0 0 6px;
  font-size: 24px;
  font-weight: 700;
  letter-spacing: -0.03em;
  color: #f0fdf4;
}

.sm-desc {
  margin: 0;
  font-size: 13px;
  color: rgba(236, 253, 245, 0.82);
}

.sm-toolbar {
  position: relative;
  z-index: 1;
  margin-bottom: 18px;
  padding: 1px;
  border-radius: 14px;
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.42), rgba(45, 212, 191, 0.28));
  box-shadow: 0 8px 32px rgba(6, 78, 59, 0.08);
}

.sm-toolbar-inner {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  padding: 14px 16px;
  border-radius: 13px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(12px);
}

.sm-input {
  width: 260px;
}

.sm-select {
  width: 170px;
}

.sm-btn-search,
.sm-btn-add {
  border-radius: 10px;
  font-weight: 600;
}

.sm-btn-search {
  box-shadow: 0 4px 14px rgba(5, 150, 105, 0.35);
}

.sm-btn-add {
  border-color: rgba(16, 185, 129, 0.45);
  color: #047857;
}

.sm-table-shell {
  position: relative;
  z-index: 1;
  border-radius: 16px;
  padding: 1px;
  background: linear-gradient(160deg, rgba(16, 185, 129, 0.12), rgba(13, 148, 136, 0.1));
  box-shadow: 0 12px 40px rgba(6, 78, 59, 0.06);
}

.sm-table {
  width: 100%;
  border-radius: 15px;
}

.sm-table >>> .el-table__row--striped td {
  background: rgba(240, 253, 244, 0.75);
}

.sm-pagination {
  padding: 14px 16px 16px;
  display: flex;
  justify-content: flex-end;
  background: #fff;
  border-radius: 0 0 15px 15px;
}

.sm-status {
  display: inline-block;
  font-size: 12px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 999px;
}

.sm-status--draft {
  color: #047857;
  background: rgba(209, 250, 229, 0.95);
}

.sm-status--pending {
  color: #b45309;
  background: rgba(254, 240, 138, 0.5);
}

.sm-status--completed {
  color: #047857;
  background: rgba(167, 243, 208, 0.55);
}

.sm-status--cancelled {
  color: #b91c1c;
  background: rgba(254, 226, 226, 0.85);
}

.sm-op {
  border-radius: 8px;
  font-weight: 600;
}

.sm-op--edit {
  color: #047857;
  border: 1px solid rgba(16, 185, 129, 0.45);
  background: rgba(255, 255, 255, 0.92);
}

.sm-op--del {
  margin-left: 8px;
  color: #dc2626;
  border: 1px solid rgba(248, 113, 113, 0.5);
  background: rgba(255, 255, 255, 0.95);
}

.sm-op--approve {
  margin-right: 6px;
  color: #047857;
  border: 1px solid rgba(16, 185, 129, 0.5);
  background: rgba(255, 255, 255, 0.95);
}

.sm-op--approve:hover {
  color: #fff;
  background: linear-gradient(135deg, #34d399, #059669);
  border-color: transparent;
}

.sm-items-head {
  margin: 4px 0 8px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.sm-items-title {
  font-weight: 700;
  color: #064e3b;
}

.sm-items-table {
  margin-bottom: 10px;
}

.sm-item-del {
  color: #dc2626;
  padding: 0;
}

.sm-summary {
  margin-bottom: 14px;
  display: flex;
  justify-content: flex-end;
  gap: 18px;
  color: #334155;
}

.sm-summary b {
  color: #064e3b;
}
</style>

<style>
.sm-dialog.el-dialog {
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid rgba(16, 185, 129, 0.22);
  box-shadow: 0 24px 64px rgba(6, 78, 59, 0.18), 0 0 0 1px rgba(255, 255, 255, 0.06) inset;
}

.sm-dialog .el-dialog__header {
  background: linear-gradient(125deg, #065f46, #0d9488);
  border-bottom: none;
  padding: 18px 20px;
}

.sm-dialog .el-dialog__title {
  color: #f0fdf4;
  font-weight: 700;
}

.sm-dialog .el-dialog__headerbtn .el-dialog__close {
  color: rgba(240, 253, 244, 0.85);
}

.sm-dialog .el-dialog__body {
  padding: 20px 22px 8px;
  background: #fafbfc;
}

.sm-form .el-input__inner {
  border-radius: 10px;
}
</style>
