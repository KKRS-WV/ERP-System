<template>
  <div class="wm-page">
    <header class="wm-head">
      <div class="wm-head-text">
        <p class="wm-eyebrow"><span class="wm-dot" /> WMS · INVENTORY</p>
        <h1 class="wm-title">仓库管理</h1>
        <p class="wm-desc">产品编号与库存台账，入库、出库登记及库存警戒，接口与后端 `/warehouse` 模块对齐。</p>
      </div>
      <div class="wm-head-glow" aria-hidden="true" />
    </header>

    <el-tabs v-model="activeTab" class="wm-tabs" @tab-click="onTabClick">
      <el-tab-pane label="库存管理" name="stock">
        <div class="wm-toolbar">
          <div class="wm-toolbar-inner">
            <el-input
              v-model="keyword"
              class="wm-input"
              clearable
              placeholder="产品编号 / 名称 / 规格"
              suffix-icon="el-icon-search"
              @keyup.enter.native="applySearch"
              @clear="onFilterCleared"
            />
            <el-checkbox v-model="alertOnly" class="wm-check" @change="applySearch">仅显示库存警戒</el-checkbox>
            <el-button type="primary" icon="el-icon-search" class="wm-btn-search" @click="applySearch">搜索</el-button>
            <el-button type="primary" plain icon="el-icon-document-add" class="wm-btn-plain" @click="openArchive">新建档案</el-button>
            <el-button type="primary" plain icon="el-icon-bottom" class="wm-btn-plain" @click="openInbound()">入库</el-button>
            <el-button type="primary" plain icon="el-icon-top" class="wm-btn-plain" @click="openOutbound()">出库</el-button>
          </div>
        </div>

        <div class="wm-alert-strip" v-if="alertPreview.length">
          <span class="wm-alert-label"><i class="el-icon-warning-outline" /> 警戒预览（前 {{ alertPreview.length }} 条）</span>
          <span v-for="(a, i) in alertPreview" :key="i" class="wm-alert-chip">{{ a.productCode }} · 余 {{ a.quantity }} / 阈 {{ a.minStock }}</span>
        </div>

        <div class="wm-table-shell">
          <el-table :data="tableData" stripe class="wm-table" :header-cell-style="tableHeaderStyle">
            <el-table-column prop="productCode" label="产品编号" min-width="130" show-overflow-tooltip />
            <el-table-column prop="productName" label="产品名称" min-width="140" show-overflow-tooltip />
            <el-table-column prop="specification" label="规格" min-width="110" show-overflow-tooltip />
            <el-table-column prop="unit" label="单位" width="72" align="center" />
            <el-table-column prop="quantity" label="当前库存" width="100" align="center" />
            <el-table-column prop="minStock" label="警戒数量" width="100" align="center" />
            <el-table-column label="状态" width="100" align="center">
              <template slot-scope="scope">
                <span class="wm-status" :class="'wm-status--' + stockStatus(scope.row).key">{{ stockStatus(scope.row).label }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="warehouseLocation" label="仓位" min-width="100" show-overflow-tooltip />
            <el-table-column label="操作" width="220" fixed="right" align="center">
              <template slot-scope="scope">
                <el-button size="mini" class="wm-op wm-op--edit" @click="openArchive(scope.row)">编辑</el-button>
                <el-button size="mini" class="wm-op wm-op--in" @click="openInbound(scope.row)">入库</el-button>
                <el-button size="mini" class="wm-op wm-op--out" @click="openOutbound(scope.row)">出库</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="wm-pagination">
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
      </el-tab-pane>

      <el-tab-pane label="出入库流水" name="flow">
        <div class="wm-toolbar">
          <div class="wm-toolbar-inner">
            <el-input
              v-model="flowKeyword"
              class="wm-input"
              clearable
              placeholder="单号 / 产品编号 / 备注"
              suffix-icon="el-icon-search"
              @keyup.enter.native="applyFlowSearch"
              @clear="onFlowFilterCleared"
            />
            <el-select v-model="flowType" class="wm-select" clearable placeholder="类型" @change="applyFlowSearch">
              <el-option label="全部" value="" />
              <el-option label="待审批" value="pending" />
              <el-option label="入库" value="in" />
              <el-option label="出库" value="out" />
              <el-option label="已驳回" value="rejected" />
            </el-select>
            <el-button type="primary" icon="el-icon-search" class="wm-btn-search" @click="applyFlowSearch">搜索</el-button>
          </div>
        </div>

        <div class="wm-table-shell">
          <el-table :data="flowData" stripe class="wm-table" :header-cell-style="tableHeaderStyle">
            <el-table-column prop="flowNo" label="流水单号" min-width="150" show-overflow-tooltip />
            <el-table-column label="类型" width="96" align="center">
              <template slot-scope="scope">
                <span :class="flowTypeClass(scope.row.flowType)">
                  {{ flowTypeLabel(scope.row.flowType) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="productCode" label="产品编号" min-width="120" show-overflow-tooltip />
            <el-table-column prop="productName" label="产品名称" min-width="120" show-overflow-tooltip />
            <el-table-column prop="quantity" label="数量" width="88" align="center" />
            <el-table-column prop="unit" label="单位" width="72" align="center" />
            <el-table-column prop="bizDate" label="业务日期" width="118" align="center" />
            <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
            <el-table-column v-if="canApprove" label="审批" width="178" align="center" fixed="right">
              <template slot-scope="scope">
                <template v-if="isPendingFlow(scope.row)">
                  <el-button size="mini" class="wm-op wm-op--approve" @click="approveFlowRow(scope.row)">通过</el-button>
                  <el-button size="mini" class="wm-op wm-op--reject" @click="rejectFlowRow(scope.row)">驳回</el-button>
                </template>
                <span v-else class="wm-muted">—</span>
              </template>
            </el-table-column>
          </el-table>

          <div class="wm-pagination">
            <el-pagination
              background
              layout="total, sizes, prev, pager, next, jumper"
              :current-page="flowPageNum"
              :page-sizes="[5, 10, 20, 50]"
              :page-size="flowPageSize"
              :total="flowTotal"
              @size-change="handleFlowSizeChange"
              @current-change="handleFlowCurrentChange"
            />
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 入库 -->
    <el-dialog
      title="产品入库"
      :visible.sync="inboundVisible"
      width="520px"
      custom-class="wm-dialog"
      :close-on-click-modal="false"
      append-to-body
      @closed="resetInboundForm"
    >
      <el-form ref="inboundForm" :model="inboundForm" :rules="inboundRules" label-width="96px" class="wm-form">
        <el-form-item label="产品编号" prop="productCode">
          <el-input v-model="inboundForm.productCode" placeholder="必填，与档案一致" />
        </el-form-item>
        <el-form-item label="产品名称">
          <el-input v-model="inboundForm.productName" placeholder="可选，首次建档时建议填写" />
        </el-form-item>
        <el-form-item label="入库数量" prop="quantity">
          <el-input-number v-model="inboundForm.quantity" :min="1" :step="1" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="单位">
          <el-input v-model="inboundForm.unit" placeholder="如 件、箱" />
        </el-form-item>
        <el-form-item label="业务日期" prop="bizDate">
          <el-date-picker v-model="inboundForm.bizDate" type="date" value-format="yyyy-MM-dd" placeholder="选择日期" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="inboundForm.remark" type="textarea" :rows="2" placeholder="可选" />
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="inboundVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitInbound">确 定</el-button>
      </span>
    </el-dialog>

    <!-- 出库 -->
    <el-dialog
      title="产品出库"
      :visible.sync="outboundVisible"
      width="520px"
      custom-class="wm-dialog"
      :close-on-click-modal="false"
      append-to-body
      @closed="resetOutboundForm"
    >
      <el-form ref="outboundForm" :model="outboundForm" :rules="outboundRules" label-width="96px" class="wm-form">
        <el-form-item label="产品编号" prop="productCode">
          <el-input v-model="outboundForm.productCode" placeholder="必填" />
        </el-form-item>
        <el-form-item v-if="outboundContextQty !== null" label="当前库存">
          <span class="wm-muted">{{ outboundContextQty }}</span>
        </el-form-item>
        <el-form-item label="出库数量" prop="quantity">
          <el-input-number v-model="outboundForm.quantity" :min="1" :step="1" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="业务日期" prop="bizDate">
          <el-date-picker v-model="outboundForm.bizDate" type="date" value-format="yyyy-MM-dd" placeholder="选择日期" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="outboundForm.remark" type="textarea" :rows="2" placeholder="可选" />
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="outboundVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitOutbound">确 定</el-button>
      </span>
    </el-dialog>

    <!-- 档案：新建 / 编辑（含警戒） -->
    <el-dialog
      :title="archiveForm.id ? '编辑库存档案' : '新建库存档案'"
      :visible.sync="archiveVisible"
      width="560px"
      custom-class="wm-dialog"
      :close-on-click-modal="false"
      append-to-body
      @closed="resetArchiveForm"
    >
      <el-form ref="archiveFormRef" :model="archiveForm" :rules="archiveRules" label-width="96px" class="wm-form">
        <el-form-item label="产品编号" prop="productCode">
          <el-input v-model="archiveForm.productCode" :disabled="!!archiveForm.id" placeholder="唯一编号" />
        </el-form-item>
        <el-form-item label="产品名称" prop="productName">
          <el-input v-model="archiveForm.productName" placeholder="名称" />
        </el-form-item>
        <el-form-item label="规格">
          <el-input v-model="archiveForm.specification" placeholder="规格型号" />
        </el-form-item>
        <el-form-item label="单位">
          <el-input v-model="archiveForm.unit" placeholder="计量单位" />
        </el-form-item>
        <el-form-item label="警戒数量" prop="minStock">
          <el-input-number v-model="archiveForm.minStock" :min="0" :step="1" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="仓位">
          <el-input v-model="archiveForm.warehouseLocation" placeholder="库位 / 仓位" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="archiveForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="archiveVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitArchive">保 存</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import {
  applyInboundStock,
  applyOutboundStock,
  approveFlow,
  getFlowPage,
  getStockPage,
  rejectFlow,
  saveStock,
  updateStock
} from '../../api/warehouse'

function todayStr() {
  const d = new Date()
  const pad = (n) => (n < 10 ? '0' + n : '' + n)
  return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate())
}

function defaultInbound() {
  return {
    productCode: '',
    productName: '',
    quantity: 1,
    unit: '',
    bizDate: todayStr(),
    remark: ''
  }
}

function defaultOutbound() {
  return {
    productCode: '',
    quantity: 1,
    bizDate: todayStr(),
    remark: ''
  }
}

function defaultArchive() {
  return {
    id: '',
    productCode: '',
    productName: '',
    specification: '',
    unit: '',
    minStock: 0,
    warehouseLocation: '',
    remark: ''
  }
}

export default {
  name: 'WarehouseManage',
  data() {
    return {
      activeTab: 'stock',
      keyword: '',
      alertOnly: false,
      tableData: [],
      pageNum: 1,
      pageSize: 10,
      total: 0,
      alertPreview: [],
      tableHeaderStyle: {
        background: 'linear-gradient(180deg, #fff7ed 0%, #ffedd5 100%)',
        color: '#7c2d12',
        fontWeight: '600',
        borderBottom: '1px solid rgba(234, 88, 12, 0.15)'
      },
      flowKeyword: '',
      flowType: '',
      flowData: [],
      flowPageNum: 1,
      flowPageSize: 10,
      flowTotal: 0,
      inboundVisible: false,
      inboundForm: defaultInbound(),
      inboundRules: {
        productCode: [{ required: true, message: '请输入产品编号', trigger: 'blur' }],
        quantity: [{ required: true, message: '请输入数量', trigger: 'change' }],
        bizDate: [{ required: true, message: '请选择日期', trigger: 'change' }]
      },
      outboundVisible: false,
      outboundForm: defaultOutbound(),
      outboundContextQty: null,
      outboundRules: {
        productCode: [{ required: true, message: '请输入产品编号', trigger: 'blur' }],
        quantity: [{ required: true, message: '请输入数量', trigger: 'change' }],
        bizDate: [{ required: true, message: '请选择日期', trigger: 'change' }]
      },
      archiveVisible: false,
      archiveForm: defaultArchive(),
      archiveRules: {
        productCode: [{ required: true, message: '请输入产品编号', trigger: 'blur' }],
        productName: [{ required: true, message: '请输入产品名称', trigger: 'blur' }],
        minStock: [{ required: true, message: '请设置警戒数量', trigger: 'change' }]
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
    canApprove() {
      const r = Number(this.curUser.roleId)
      return r === 0 || r === 1
    }
  },
  methods: {
    stockStatus(row) {
      const q = Number(row.quantity || 0)
      const min = Number(row.minStock || 0)
      if (q <= 0) return { key: 'empty', label: '缺货' }
      if (min > 0 && q <= min) return { key: 'warn', label: '警戒' }
      return { key: 'ok', label: '正常' }
    },
    flowTypeLabel(type) {
      if (type === 'in') return '入库'
      if (type === 'out') return '出库'
      if (type === 'pin') return '待审入库'
      if (type === 'pout') return '待审出库'
      if (type === 'rin') return '已驳回入库'
      if (type === 'rout') return '已驳回出库'
      return type || '未知'
    },
    flowTypeClass(type) {
      if (type === 'in') return 'wm-flow-in'
      if (type === 'out') return 'wm-flow-out'
      if (type === 'pin' || type === 'pout') return 'wm-flow-pending'
      if (type === 'rin' || type === 'rout') return 'wm-flow-reject'
      return 'wm-muted'
    },
    isPendingFlow(row) {
      const t = row && row.flowType
      return t === 'pin' || t === 'pout'
    },
    onTabClick(tab) {
      if (tab.name === 'flow' && !this.flowData.length) {
        this.loadFlow()
      }
    },
    onFilterCleared() {
      this.pageNum = 1
      this.loadStock()
    },
    applySearch() {
      this.pageNum = 1
      this.loadStock()
    },
    handleSizeChange(val) {
      this.pageSize = val
      this.pageNum = 1
      this.loadStock()
    },
    handleCurrentChange(val) {
      this.pageNum = val
      this.loadStock()
    },
    onFlowFilterCleared() {
      this.flowPageNum = 1
      this.loadFlow()
    },
    applyFlowSearch() {
      this.flowPageNum = 1
      this.loadFlow()
    },
    handleFlowSizeChange(val) {
      this.flowPageSize = val
      this.flowPageNum = 1
      this.loadFlow()
    },
    handleFlowCurrentChange(val) {
      this.flowPageNum = val
      this.loadFlow()
    },
    openInbound(row) {
      this.inboundForm = defaultInbound()
      if (row) {
        this.inboundForm.productCode = row.productCode || ''
        this.inboundForm.productName = row.productName || ''
        this.inboundForm.unit = row.unit || ''
      }
      this.inboundVisible = true
      this.$nextTick(() => this.$refs.inboundForm && this.$refs.inboundForm.clearValidate())
    },
    resetInboundForm() {
      this.inboundForm = defaultInbound()
    },
    submitInbound() {
      this.$refs.inboundForm.validate((valid) => {
        if (!valid) return
        applyInboundStock(this.$httpUrl, {
          ...this.inboundForm,
          operatorId: this.curUser.id
        })
          .then((res) => res.data)
          .then((res) => {
            if (res.code === 200) {
              this.$message.success('已提交入库审批')
              this.inboundVisible = false
              this.activeTab = 'flow'
              this.flowPageNum = 1
              this.loadFlow()
            } else {
              this.$message.error((res && res.msg) || '提交失败')
            }
          })
          .catch(() => {
            this.$message.error('提交失败，接口未就绪')
          })
      })
    },
    openOutbound(row) {
      this.outboundForm = defaultOutbound()
      this.outboundContextQty = row && row.quantity != null ? Number(row.quantity) : null
      if (row) {
        this.outboundForm.productCode = row.productCode || ''
      }
      this.outboundVisible = true
      this.$nextTick(() => this.$refs.outboundForm && this.$refs.outboundForm.clearValidate())
    },
    resetOutboundForm() {
      this.outboundForm = defaultOutbound()
      this.outboundContextQty = null
    },
    submitOutbound() {
      this.$refs.outboundForm.validate((valid) => {
        if (!valid) return
        const qty = Number(this.outboundForm.quantity || 0)
        if (this.outboundContextQty !== null && qty > this.outboundContextQty) {
          this.$message.warning('出库数量不能大于当前库存')
          return
        }
        applyOutboundStock(this.$httpUrl, {
          ...this.outboundForm,
          operatorId: this.curUser.id
        })
          .then((res) => res.data)
          .then((res) => {
            if (res.code === 200) {
              this.$message.success('已提交出库审批')
              this.outboundVisible = false
              this.activeTab = 'flow'
              this.flowPageNum = 1
              this.loadFlow()
            } else {
              this.$message.error((res && res.msg) || '提交失败')
            }
          })
          .catch(() => {
            this.$message.error('提交失败，接口未就绪')
          })
      })
    },
    approveFlowRow(row) {
      if (!this.canApprove) {
        this.$message.error('仅管理员或超级管理员可审批')
        return
      }
      this.$confirm('确认审批通过该流水？通过后将执行真实库存变更。', '审批', {
        type: 'warning'
      })
        .then(() => approveFlow(this.$httpUrl, { id: row.id, operatorId: this.curUser.id }))
        .then((res) => res.data)
        .then((res) => {
          if (res.code === 200) {
            this.$message.success('审批通过')
            this.loadFlow()
            this.loadStock()
          } else {
            this.$message.error((res && res.msg) || '审批失败')
          }
        })
        .catch(() => {})
    },
    rejectFlowRow(row) {
      if (!this.canApprove) {
        this.$message.error('仅管理员或超级管理员可审批')
        return
      }
      this.$confirm('确认驳回该待审批流水？', '审批', { type: 'warning' })
        .then(() => rejectFlow(this.$httpUrl, { id: row.id, operatorId: this.curUser.id }))
        .then((res) => res.data)
        .then((res) => {
          if (res.code === 200) {
            this.$message.success('已驳回')
            this.loadFlow()
          } else {
            this.$message.error((res && res.msg) || '驳回失败')
          }
        })
        .catch(() => {})
    },
    openArchive(row) {
      if (row && row.id) {
        this.archiveForm = {
          id: row.id,
          productCode: row.productCode || '',
          productName: row.productName || '',
          specification: row.specification || '',
          unit: row.unit || '',
          minStock: Number(row.minStock || 0),
          warehouseLocation: row.warehouseLocation || '',
          remark: row.remark || ''
        }
      } else {
        this.archiveForm = defaultArchive()
      }
      this.archiveVisible = true
      this.$nextTick(() => this.$refs.archiveFormRef && this.$refs.archiveFormRef.clearValidate())
    },
    resetArchiveForm() {
      this.archiveForm = defaultArchive()
    },
    submitArchive() {
      this.$refs.archiveFormRef.validate((valid) => {
        if (!valid) return
        const req = this.archiveForm.id
          ? updateStock(this.$httpUrl, { ...this.archiveForm })
          : saveStock(this.$httpUrl, { ...this.archiveForm })
        req
          .then((res) => res.data)
          .then((res) => {
            if (res.code === 200) {
              this.$message.success('保存成功')
              this.archiveVisible = false
              this.loadStock()
            } else {
              this.$message.error((res && res.msg) || '保存失败')
            }
          })
          .catch(() => {
            this.$message.error('保存失败，接口未就绪')
          })
      })
    },
    loadStock() {
      getStockPage(this.$httpUrl, {
        pageSize: this.pageSize,
        pageNum: this.pageNum,
        param: {
          keyword: this.keyword,
          alertOnly: this.alertOnly
        }
      })
        .then((res) => res.data)
        .then((res) => {
          if (res.code === 200) {
            this.tableData = (res.data || []).map((row) => ({
              ...row,
              quantity: Number(row.quantity != null ? row.quantity : 0),
              minStock: Number(row.minStock != null ? row.minStock : 0)
            }))
            this.total = Number(res.total || 0)
            this.refreshAlertPreview()
          } else {
            this.$message.error((res && res.msg) || '获取库存失败')
          }
        })
        .catch(() => {
          this.tableData = []
          this.total = 0
          this.alertPreview = []
          this.$message.warning('库存接口未就绪，当前展示为空列表')
        })
    },
    refreshAlertPreview() {
      const list = this.tableData.filter((r) => {
        const st = this.stockStatus(r)
        return st.key === 'warn' || st.key === 'empty'
      })
      this.alertPreview = list.slice(0, 5)
    },
    loadFlow() {
      getFlowPage(this.$httpUrl, {
        pageSize: this.flowPageSize,
        pageNum: this.flowPageNum,
        param: {
          keyword: this.flowKeyword,
          flowType: this.flowType
        }
      })
        .then((res) => res.data)
        .then((res) => {
          if (res.code === 200) {
            this.flowData = res.data || []
            this.flowTotal = Number(res.total || 0)
          } else {
            this.$message.error((res && res.msg) || '获取流水失败')
          }
        })
        .catch(() => {
          this.flowData = []
          this.flowTotal = 0
          this.$message.warning('流水接口未就绪，当前展示为空列表')
        })
    }
  },
  beforeMount() {
    this.loadStock()
  }
}
</script>

<style scoped>
.wm-page {
  --wm-amber: #fbbf24;
  --wm-orange: #ea580c;
  text-align: left;
  position: relative;
  overflow: hidden;
}

.wm-page::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  opacity: 0.35;
  background-image:
    linear-gradient(rgba(234, 88, 12, 0.07) 1px, transparent 1px),
    linear-gradient(90deg, rgba(234, 88, 12, 0.07) 1px, transparent 1px);
  background-size: 44px 44px;
  z-index: 0;
}

.wm-head {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 16px;
  padding: 22px 24px 20px;
  border-radius: 16px;
  overflow: hidden;
  background: linear-gradient(125deg, #7c2d12 0%, #c2410c 50%, #ea580c 100%);
  box-shadow:
    0 16px 48px rgba(234, 88, 12, 0.22),
    0 0 0 1px rgba(255, 255, 255, 0.08) inset;
}

.wm-head-glow {
  position: absolute;
  right: -16%;
  top: -50%;
  width: 50%;
  height: 180%;
  background: radial-gradient(circle, rgba(251, 191, 36, 0.35) 0%, transparent 65%);
  pointer-events: none;
}

.wm-head-text {
  position: relative;
  z-index: 1;
}

.wm-eyebrow {
  margin: 0 0 8px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.2em;
  color: rgba(255, 247, 237, 0.88);
  display: flex;
  align-items: center;
  gap: 8px;
}

.wm-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--wm-amber);
  box-shadow: 0 0 12px var(--wm-amber);
}

.wm-title {
  margin: 0 0 6px;
  font-size: 24px;
  font-weight: 700;
  letter-spacing: -0.03em;
  color: #fffbeb;
}

.wm-desc {
  margin: 0;
  font-size: 13px;
  color: rgba(255, 247, 237, 0.85);
}

.wm-tabs {
  position: relative;
  z-index: 1;
}

.wm-tabs >>> .el-tabs__header {
  margin-bottom: 14px;
}

.wm-tabs >>> .el-tabs__item {
  font-weight: 600;
  color: #57534e;
}

.wm-tabs >>> .el-tabs__item.is-active {
  color: #c2410c;
}

.wm-tabs >>> .el-tabs__active-bar {
  background-color: #ea580c;
}

.wm-toolbar {
  margin-bottom: 14px;
  padding: 1px;
  border-radius: 14px;
  background: linear-gradient(135deg, rgba(234, 88, 12, 0.4), rgba(251, 191, 36, 0.25));
  box-shadow: 0 8px 32px rgba(124, 45, 18, 0.08);
}

.wm-toolbar-inner {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  padding: 14px 16px;
  border-radius: 13px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(12px);
}

.wm-input {
  width: 240px;
}

.wm-select {
  width: 140px;
}

.wm-check {
  color: #44403c;
  font-weight: 500;
}

.wm-btn-search {
  border-radius: 10px;
  font-weight: 600;
  box-shadow: 0 4px 14px rgba(194, 65, 12, 0.35);
}

.wm-btn-plain {
  border-radius: 10px;
  font-weight: 600;
  border-color: rgba(234, 88, 12, 0.45);
  color: #c2410c;
}

.wm-alert-strip {
  margin-bottom: 12px;
  padding: 10px 14px;
  border-radius: 12px;
  background: rgba(254, 243, 199, 0.6);
  border: 1px solid rgba(251, 191, 36, 0.45);
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  font-size: 12px;
  color: #78350f;
}

.wm-alert-label {
  font-weight: 700;
  margin-right: 4px;
}

.wm-alert-chip {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.75);
  border: 1px solid rgba(234, 88, 12, 0.2);
}

.wm-table-shell {
  position: relative;
  z-index: 1;
  border-radius: 16px;
  padding: 1px;
  background: linear-gradient(160deg, rgba(251, 191, 36, 0.15), rgba(234, 88, 12, 0.1));
  box-shadow: 0 12px 40px rgba(124, 45, 18, 0.06);
}

.wm-table {
  width: 100%;
  border-radius: 15px;
}

.wm-table >>> .el-table__row--striped td {
  background: rgba(255, 251, 235, 0.75);
}

.wm-pagination {
  padding: 14px 16px 16px;
  display: flex;
  justify-content: flex-end;
  background: #fff;
  border-radius: 0 0 15px 15px;
}

.wm-status {
  display: inline-block;
  font-size: 12px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 999px;
}

.wm-status--ok {
  color: #047857;
  background: rgba(167, 243, 208, 0.55);
}

.wm-status--warn {
  color: #b45309;
  background: rgba(254, 240, 138, 0.55);
}

.wm-status--empty {
  color: #b91c1c;
  background: rgba(254, 226, 226, 0.85);
}

.wm-op {
  border-radius: 8px;
  font-weight: 600;
}

.wm-op--edit {
  color: #c2410c;
  border: 1px solid rgba(234, 88, 12, 0.4);
  background: rgba(255, 255, 255, 0.92);
}

.wm-op--in {
  color: #047857;
  border: 1px solid rgba(16, 185, 129, 0.45);
  background: rgba(255, 255, 255, 0.92);
}

.wm-op--out {
  color: #b45309;
  border: 1px solid rgba(245, 158, 11, 0.5);
  background: rgba(255, 255, 255, 0.92);
}

.wm-table .wm-op + .wm-op {
  margin-left: 6px;
}

.wm-flow-in {
  color: #047857;
  font-weight: 700;
}

.wm-flow-out {
  color: #c2410c;
  font-weight: 700;
}

.wm-flow-pending {
  color: #1d4ed8;
  font-weight: 700;
}

.wm-flow-reject {
  color: #b91c1c;
  font-weight: 700;
}

.wm-muted {
  color: #57534e;
  font-weight: 600;
}

.wm-op--approve {
  color: #047857;
  border: 1px solid rgba(16, 185, 129, 0.45);
  background: rgba(255, 255, 255, 0.92);
}

.wm-op--reject {
  margin-left: 6px;
  color: #b91c1c;
  border: 1px solid rgba(239, 68, 68, 0.45);
  background: rgba(255, 255, 255, 0.92);
}
</style>

<style>
.wm-dialog.el-dialog {
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid rgba(234, 88, 12, 0.22);
  box-shadow: 0 24px 64px rgba(124, 45, 18, 0.15), 0 0 0 1px rgba(255, 255, 255, 0.06) inset;
}

.wm-dialog .el-dialog__header {
  background: linear-gradient(125deg, #9a3412, #ea580c);
  border-bottom: none;
  padding: 18px 20px;
}

.wm-dialog .el-dialog__title {
  color: #fffbeb;
  font-weight: 700;
}

.wm-dialog .el-dialog__headerbtn .el-dialog__close {
  color: rgba(255, 251, 235, 0.85);
}

.wm-dialog .el-dialog__body {
  padding: 20px 22px 8px;
  background: #fafbfc;
}

.wm-form .el-input__inner {
  border-radius: 10px;
}
</style>
