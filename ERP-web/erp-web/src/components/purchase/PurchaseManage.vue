<template>
  <div class="pm-page">
    <header class="pm-head">
      <div class="pm-head-text">
        <p class="pm-eyebrow"><span class="pm-dot" /> PURCHASE · ORDER</p>
        <h1 class="pm-title">采购订单</h1>
        <p class="pm-desc">
          普通用户将订单置为「待审核」后，仅管理员/超级管理员可审批；审批通过将按明细自动入库（需已建库存档案，明细填写产品编号或与档案名称一致）。
        </p>
      </div>
      <div class="pm-head-glow" aria-hidden="true" />
    </header>

    <div class="pm-toolbar">
      <div class="pm-toolbar-inner">
        <el-input
          v-model="keyword"
          class="pm-input"
          clearable
          placeholder="订单号 / 供应商 / 商品关键字"
          suffix-icon="el-icon-search"
          @keyup.enter.native="applySearch"
          @clear="onFilterCleared"
        />
        <el-select
          v-model="status"
          class="pm-select"
          clearable
          placeholder="筛选状态"
          @change="onStatusChange"
          @clear="onFilterCleared"
        >
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-button type="primary" icon="el-icon-search" class="pm-btn-search" @click="applySearch">搜索</el-button>
        <el-button type="primary" plain icon="el-icon-plus" class="pm-btn-add" @click="add">新增订单</el-button>
      </div>
    </div>

    <div class="pm-table-shell">
      <el-table :data="tableData" stripe class="pm-table" :header-cell-style="tableHeaderStyle">
        <el-table-column prop="orderNo" label="订单号" min-width="140" show-overflow-tooltip />
        <el-table-column prop="supplierName" label="供应商" min-width="130" show-overflow-tooltip />
        <el-table-column prop="supplierAddress" label="供应商地址" min-width="180" show-overflow-tooltip />
        <el-table-column prop="itemCount" label="商品数" width="88" align="center" />
        <el-table-column prop="totalQuantity" label="总数量" width="96" align="center" />
        <el-table-column label="总金额" width="120" align="right">
          <template slot-scope="scope">￥{{ money(scope.row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120" align="center">
          <template slot-scope="scope">
            <span class="pm-status" :class="'pm-status--' + scope.row.status">{{ getStatusLabel(scope.row.status) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="orderDate" label="采购日期" width="120" align="center" />
        <el-table-column label="操作" width="268" fixed="right" align="center">
          <template slot-scope="scope">
            <el-button
              v-if="canApprove && scope.row.status === 'pending'"
              size="mini"
              class="pm-op pm-op--approve"
              @click="approve(scope.row)"
            >
              审批通过
            </el-button>
            <el-button size="mini" class="pm-op pm-op--edit" @click="mod(scope.row)">
              {{ scope.row.status === 'completed' ? '查看' : '编辑' }}
            </el-button>
            <el-popconfirm
              v-if="scope.row.status !== 'completed'"
              title="确定删除该采购单？"
              @confirm="del(scope.row.id)"
            >
              <el-button slot="reference" size="mini" class="pm-op pm-op--del">删除</el-button>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div class="pm-pagination">
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
      custom-class="pm-dialog"
      :close-on-click-modal="false"
      append-to-body
    >
      <el-form ref="form" :model="form" :rules="rules" label-width="96px" class="pm-form">
        <el-row :gutter="14">
          <el-col :span="8">
            <el-form-item label="订单号" prop="orderNo">
              <el-input v-model="form.orderNo" placeholder="如 PO20260320001" :disabled="isReadOnly" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="采购日期" prop="orderDate">
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
              <el-select
                v-model="form.status"
                placeholder="请选择状态"
                style="width: 100%;"
                :disabled="isReadOnly"
              >
                <el-option v-for="item in statusOptionsForForm" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="14">
          <el-col :span="8">
            <el-form-item label="供应商名称" prop="supplierName">
              <el-input v-model="form.supplierName" placeholder="供应商名称" :disabled="isReadOnly" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="联系人">
              <el-input v-model="form.supplierContact" placeholder="联系人" :disabled="isReadOnly" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="联系电话">
              <el-input v-model="form.supplierPhone" placeholder="联系电话" :disabled="isReadOnly" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="供应商地址" prop="supplierAddress">
          <el-input v-model="form.supplierAddress" placeholder="供应商地址" :disabled="isReadOnly" />
        </el-form-item>

        <div class="pm-items-head">
          <span class="pm-items-title">采购商品明细</span>
          <el-button v-if="!isReadOnly" type="primary" plain size="mini" icon="el-icon-plus" @click="addItem">添加商品</el-button>
        </div>
        <el-table :data="form.items" size="mini" border class="pm-items-table">
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
                @change="reCalcRow(scope.row)"
                :disabled="isReadOnly"
              />
            </template>
          </el-table-column>
          <el-table-column label="小计" width="130" align="right">
            <template slot-scope="scope">￥{{ money(scope.row.amount) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="80" align="center">
            <template slot-scope="scope">
              <el-button v-if="!isReadOnly" type="text" class="pm-item-del" @click="removeItem(scope.$index)">删除</el-button>
              <span v-else>—</span>
            </template>
          </el-table-column>
        </el-table>

        <div class="pm-summary">
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
  approvePurchaseOrder,
  createPurchaseOrder,
  deletePurchaseOrder,
  getPurchaseOrderDetail,
  getPurchaseOrderPage,
  updatePurchaseOrder
} from '../../api/purchase'

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
    supplierName: '',
    supplierContact: '',
    supplierPhone: '',
    supplierAddress: '',
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

/** 新增时默认订单号，降低与库中唯一键冲突概率 */
function suggestPurchaseOrderNo() {
  const d = new Date()
  return (
    'PO' +
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
  name: 'PurchaseManage',
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
        background: 'linear-gradient(180deg, #f1f5f9 0%, #e8eef5 100%)',
        color: '#0f172a',
        fontWeight: '600',
        borderBottom: '1px solid rgba(99, 102, 241, 0.12)'
      },
      form: defaultForm(),
      rules: {
        orderNo: [{ required: true, message: '请输入订单号', trigger: 'blur' }],
        orderDate: [{ required: true, message: '请选择采购日期', trigger: 'change' }],
        supplierName: [{ required: true, message: '请输入供应商名称', trigger: 'blur' }],
        supplierAddress: [{ required: true, message: '请输入供应商地址', trigger: 'blur' }],
        status: [{ required: true, message: '请选择状态', trigger: 'change' }]
      }
    }
  },
  computed: {
    dialogTitle() {
      if (this.isReadOnly) return '查看采购订单（已完成）'
      return this.form.id ? '编辑采购订单' : '新增采购订单'
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
      this.form.orderNo = suggestPurchaseOrderNo()
      this.form.orderDate = todayStr()
    },
    mod(row) {
      if (!row || !row.id) return
      this.dialogVisible = true
      this.isReadOnly = row.status === 'completed'
      getPurchaseOrderDetail(this.$httpUrl, row.id)
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
      this.$confirm('确认审批通过？将按明细数量执行入库（需已建库存档案且产品编号或名称可匹配）。', '审批', {
        type: 'warning',
        confirmButtonText: '确定',
        cancelButtonText: '取消'
      })
        .then(() => {
          approvePurchaseOrder(this.$httpUrl, { id: row.id, operatorId: u.id })
            .then((res) => res.data)
            .then((res) => {
              if (res.code === 200) {
                this.$message.success('审批通过，已入库')
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
      deletePurchaseOrder(this.$httpUrl, id)
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
      const request = isUpdate ? updatePurchaseOrder(this.$httpUrl, payload) : createPurchaseOrder(this.$httpUrl, payload)
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
          supplierName: (this.form.supplierName || '').trim(),
          supplierContact: (this.form.supplierContact || '').trim(),
          supplierPhone: (this.form.supplierPhone || '').trim(),
          supplierAddress: (this.form.supplierAddress || '').trim(),
          remark: (this.form.remark || '').trim(),
          items: this.buildItemsPayload(),
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
      getPurchaseOrderPage(this.$httpUrl, {
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
            this.$message.error((res && res.msg) || '获取采购数据失败')
          }
        })
        .catch(() => {
          this.tableData = []
          this.total = 0
          this.$message.warning('采购接口未就绪，当前展示为空列表')
        })
    }
  },
  beforeMount() {
    this.loadPost()
  }
}
</script>

<style scoped>
.pm-page {
  --pm-cyan: #22d3ee;
  --pm-indigo: #6366f1;
  text-align: left;
  position: relative;
  overflow: hidden;
}

.pm-page::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  opacity: 0.38;
  background-image:
    linear-gradient(rgba(99, 102, 241, 0.07) 1px, transparent 1px),
    linear-gradient(90deg, rgba(99, 102, 241, 0.07) 1px, transparent 1px);
  background-size: 44px 44px;
  z-index: 0;
}

.pm-head {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 20px;
  padding: 22px 24px 20px;
  border-radius: 16px;
  overflow: hidden;
  background: linear-gradient(125deg, #0f172a 0%, #1d4ed8 48%, #4f46e5 100%);
  box-shadow:
    0 16px 48px rgba(59, 130, 246, 0.25),
    0 0 0 1px rgba(255, 255, 255, 0.08) inset;
}

.pm-head-glow {
  position: absolute;
  right: -20%;
  top: -60%;
  width: 55%;
  height: 200%;
  background: radial-gradient(circle, rgba(34, 211, 238, 0.35) 0%, transparent 65%);
  pointer-events: none;
}

.pm-head-text {
  position: relative;
  z-index: 1;
}

.pm-eyebrow {
  margin: 0 0 8px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.2em;
  color: rgba(226, 232, 240, 0.85);
  display: flex;
  align-items: center;
  gap: 8px;
}

.pm-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--pm-cyan);
  box-shadow: 0 0 12px var(--pm-cyan);
}

.pm-title {
  margin: 0 0 6px;
  font-size: 24px;
  font-weight: 700;
  letter-spacing: -0.03em;
  color: #f8fafc;
}

.pm-desc {
  margin: 0;
  font-size: 13px;
  color: rgba(226, 232, 240, 0.82);
}

.pm-toolbar {
  position: relative;
  z-index: 1;
  margin-bottom: 18px;
  padding: 1px;
  border-radius: 14px;
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.45), rgba(34, 211, 238, 0.25));
  box-shadow: 0 8px 32px rgba(15, 23, 42, 0.08);
}

.pm-toolbar-inner {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  padding: 14px 16px;
  border-radius: 13px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(12px);
}

.pm-input {
  width: 260px;
}

.pm-select {
  width: 170px;
}

.pm-btn-search,
.pm-btn-add {
  border-radius: 10px;
  font-weight: 600;
}

.pm-btn-search {
  box-shadow: 0 4px 14px rgba(79, 70, 229, 0.35);
}

.pm-btn-add {
  border-color: rgba(99, 102, 241, 0.45);
  color: #4f46e5;
}

.pm-table-shell {
  position: relative;
  z-index: 1;
  border-radius: 16px;
  padding: 1px;
  background: linear-gradient(160deg, rgba(99, 102, 241, 0.12), rgba(14, 116, 144, 0.1));
  box-shadow: 0 12px 40px rgba(15, 23, 42, 0.07);
}

.pm-table {
  width: 100%;
  border-radius: 15px;
}

.pm-table >>> .el-table__row--striped td {
  background: rgba(248, 250, 252, 0.85);
}

.pm-pagination {
  padding: 14px 16px 16px;
  display: flex;
  justify-content: flex-end;
  background: #fff;
  border-radius: 0 0 15px 15px;
}

.pm-status {
  display: inline-block;
  font-size: 12px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 999px;
}

.pm-status--draft {
  color: #4338ca;
  background: rgba(224, 231, 255, 0.9);
}

.pm-status--pending {
  color: #b45309;
  background: rgba(254, 240, 138, 0.5);
}

.pm-status--completed {
  color: #047857;
  background: rgba(167, 243, 208, 0.55);
}

.pm-status--cancelled {
  color: #b91c1c;
  background: rgba(254, 226, 226, 0.85);
}

.pm-op {
  border-radius: 8px;
  font-weight: 600;
}

.pm-op--edit {
  color: #4f46e5;
  border: 1px solid rgba(99, 102, 241, 0.45);
  background: rgba(255, 255, 255, 0.92);
}

.pm-op--del {
  margin-left: 8px;
  color: #dc2626;
  border: 1px solid rgba(248, 113, 113, 0.5);
  background: rgba(255, 255, 255, 0.95);
}

.pm-op--approve {
  margin-right: 6px;
  color: #047857;
  border: 1px solid rgba(16, 185, 129, 0.5);
  background: rgba(255, 255, 255, 0.95);
}

.pm-op--approve:hover {
  color: #fff;
  background: linear-gradient(135deg, #34d399, #059669);
  border-color: transparent;
}

.pm-items-head {
  margin: 4px 0 8px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pm-items-title {
  font-weight: 700;
  color: #1e293b;
}

.pm-items-table {
  margin-bottom: 10px;
}

.pm-item-del {
  color: #dc2626;
  padding: 0;
}

.pm-summary {
  margin-bottom: 14px;
  display: flex;
  justify-content: flex-end;
  gap: 18px;
  color: #334155;
}

.pm-summary b {
  color: #0f172a;
}
</style>

<style>
.pm-dialog.el-dialog {
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid rgba(99, 102, 241, 0.2);
  box-shadow: 0 24px 64px rgba(15, 23, 42, 0.2), 0 0 0 1px rgba(255, 255, 255, 0.06) inset;
}

.pm-dialog .el-dialog__header {
  background: linear-gradient(125deg, #1e3a8a, #4f46e5);
  border-bottom: none;
  padding: 18px 20px;
}

.pm-dialog .el-dialog__title {
  color: #f8fafc;
  font-weight: 700;
}

.pm-dialog .el-dialog__headerbtn .el-dialog__close {
  color: rgba(248, 250, 252, 0.85);
}

.pm-dialog .el-dialog__body {
  padding: 20px 22px 8px;
  background: #fafbfc;
}

.pm-form .el-input__inner {
  border-radius: 10px;
}
</style>
