<template>
  <div class="um-page">
    <header class="um-head">
      <div class="um-head-text">
        <p class="um-eyebrow"><span class="um-dot" /> USER · REGISTRY</p>
        <h1 class="um-title">用户管理</h1>
        <p class="um-desc">检索、维护系统账户与角色权限数据</p>
      </div>
      <div class="um-head-glow" aria-hidden="true" />
    </header>

    <div class="um-toolbar">
      <div class="um-toolbar-inner">
        <el-input
          v-model="name"
          placeholder="姓名 / 关键词"
          suffix-icon="el-icon-search"
          class="um-input"
          clearable
          @keyup.enter.native="applySearch"
          @clear="onFilterCleared"
        />
        <el-select
          v-model="roleId"
          filterable
          clearable
          placeholder="筛选角色"
          class="um-select"
          @clear="onFilterCleared"
          @change="onRoleChange"
        >
          <el-option v-for="item in roleIds" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-button type="primary" class="um-btn-search" icon="el-icon-search" @click="applySearch">搜索</el-button>
        <el-button type="primary" plain class="um-btn-add" icon="el-icon-plus" @click="add">新增用户</el-button>
      </div>
    </div>

    <div class="um-table-shell">
      <el-table
        :data="tableData"
        :header-cell-style="tableHeaderStyle"
        stripe
        class="um-table"
        :row-class-name="tableRowClass"
      >
        <el-table-column prop="id" label="ID" width="72" align="center" />
        <el-table-column prop="no" label="账号" min-width="110" show-overflow-tooltip />
        <el-table-column prop="name" label="姓名" min-width="100" show-overflow-tooltip />
        <el-table-column prop="age" label="年龄" width="80" align="center" />
        <el-table-column prop="sex" label="性别" width="96" align="center">
          <template slot-scope="scope">
            <span class="um-pill" :class="scope.row.sex === 1 ? 'um-pill--male' : 'um-pill--female'">
              {{ scope.row.sex === 1 ? '男' : '女' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="roleId" label="角色" width="120" align="center">
          <template slot-scope="scope">
            <span
              class="um-role"
              :class="{
                'um-role--0': scope.row.roleId === 0,
                'um-role--1': scope.row.roleId === 1,
                'um-role--2': scope.row.roleId === 2
              }"
            >
              {{
                scope.row.roleId === 0 ? '超级管理员' : scope.row.roleId === 1 ? '管理员' : '普通用户'
              }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="电话" min-width="120" show-overflow-tooltip />
        <el-table-column label="操作" width="168" align="center" fixed="right">
          <template slot-scope="scope">
            <el-button size="mini" class="um-op um-op--edit" @click="mod(scope.row)">编辑</el-button>
            <el-popconfirm title="确定删除该用户？" @confirm="del(scope.row.id)">
              <el-button slot="reference" size="mini" class="um-op um-op--del">删除</el-button>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div class="um-pagination">
        <el-pagination
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          :current-page="pageNum"
          :page-sizes="[2, 5, 10, 20]"
          :page-size="pageSize"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          background
        />
      </div>
    </div>

    <el-dialog
      :title="form.id ? '编辑用户' : '新增用户'"
      :visible.sync="centerDialogVisible"
      width="500px"
      custom-class="um-dialog"
      :close-on-click-modal="false"
      append-to-body
    >
      <el-form ref="form" :rules="rules" :model="form" label-width="80px" class="um-form">
        <el-form-item label="账号" prop="no">
          <el-input v-model="form.no" placeholder="登录账号" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" show-password placeholder="密码" />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="姓名" />
        </el-form-item>
        <el-form-item label="年龄" prop="age">
          <el-input v-model="form.age" placeholder="年龄" />
        </el-form-item>
        <el-form-item label="性别" prop="sex">
          <el-radio-group v-model="form.sex">
            <el-radio label="1">男</el-radio>
            <el-radio label="0">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="角色" prop="roleId">
          <el-select v-model="form.roleId" placeholder="请选择角色" style="width: 100%;">
            <el-option v-for="item in roleIds" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="电话" prop="phone">
          <el-input v-model="form.phone" placeholder="手机号" maxlength="11" />
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="centerDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="save">确 定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: 'UserManage',
  data() {
    const checkDuplicate = (rule, value, callback) => {
      if (this.form.id) {
        return callback()
      }
      this.$axios.get(this.$httpUrl + '/user/findByNo?no=' + this.form.no).then((res) => res.data).then((res) => {
        if (res.code != 200) {
          callback()
        } else {
          callback(new Error('账号已经存在'))
        }
      })
    }
    return {
      tableData: [],
      pageSize: 10,
      pageNum: 1,
      total: 0,
      name: '',
      roleId: '',
      roleIds: [
        { value: '0', label: '超级管理员' },
        { value: '1', label: '管理员' },
        { value: '2', label: '普通用户' }
      ],
      centerDialogVisible: false,
      tableHeaderStyle: {
        background: 'linear-gradient(180deg, #f1f5f9 0%, #e8eef5 100%)',
        color: '#0f172a',
        fontWeight: '600',
        borderBottom: '1px solid rgba(99, 102, 241, 0.12)'
      },
      form: {
        id: '',
        no: '',
        name: '',
        password: '',
        age: '',
        phone: '',
        sex: '0',
        roleId: '2'
      },
      rules: {
        no: [
          { required: true, message: '请输入账号', trigger: 'blur' },
          { min: 3, max: 15, message: '长度在 3 到 15 个字符', trigger: 'blur' },
          { validator: checkDuplicate, trigger: 'blur' }
        ],
        name: [
          { required: true, message: '请输入名称', trigger: 'blur' },
          { min: 3, max: 15, message: '长度在 3 到 15 个字符', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' },
          { min: 3, max: 15, message: '长度在 3 到 15 个字符', trigger: 'blur' }
        ],
        phone: [{ pattern: /^1[3|4|5|6|7|8|9][0-9]\d{8}$/, message: '请输入正确的手机号码', trigger: 'blur' }],
        roleId: [{ required: true, message: '请选择角色', trigger: 'change' }]
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
    isSuperAdmin() {
      return Number(this.curUser.roleId) === 0
    }
  },
  methods: {
    extractErrorMsg(err, fallback = '请求失败，请稍后重试') {
      const resMsg = err && err.response && err.response.data && err.response.data.msg
      if (resMsg) return resMsg
      const msg = err && err.message
      if (msg && msg.indexOf('Network Error') !== -1) {
        return '无法连接服务器，请确认后端和数据库服务已启动'
      }
      return msg || fallback
    },
    tableRowClass() {
      return 'um-table-row'
    },
    /** 清空搜索框或角色筛选后：回到第 1 页并重新拉取列表 */
    onFilterCleared() {
      this.pageNum = 1
      this.loadPost()
    },
    /** 下拉变更时立即按条件筛选（含清空为「全部」） */
    onRoleChange() {
      this.pageNum = 1
      this.loadPost()
    },
    applySearch() {
      this.pageNum = 1
      this.loadPost()
    },
    resetForm() {
      this.$refs.form.resetFields()
    },
    del(id) {
      if (!this.isSuperAdmin) {
        this.$message.error('仅超级管理员可管理用户')
        return
      }
      this.$axios
        .delete(this.$httpUrl + '/user/del?id=' + id + '&operatorId=' + (this.curUser.id || ''))
        .then((res) => res.data)
        .then((res) => {
          if (res.code == 200) {
            this.$message({ message: '操作成功', type: 'success' })
            this.loadPost()
          } else {
            this.$message({ message: '操作失败', type: 'error' })
          }
        })
    },
    mod(row) {
      if (!this.isSuperAdmin) {
        this.$message.error('仅超级管理员可管理用户')
        return
      }
      this.centerDialogVisible = true
      this.$nextTick(() => {
        this.form.id = row.id
        this.form.no = row.no
        this.form.name = row.name
        this.form.password = row.password
        this.form.age = row.age + ''
        this.form.sex = row.sex + ''
        this.form.phone = row.phone
        this.form.roleId = row.roleId + ''
      })
    },
    add() {
      if (!this.isSuperAdmin) {
        this.$message.error('仅超级管理员可管理用户')
        return
      }
      this.centerDialogVisible = true
      this.$nextTick(() => {
        this.resetForm()
      })
    },
    doSave() {
      const payload = {
        ...this.form,
        age: this.form.age === '' ? null : Number(this.form.age),
        sex: Number(this.form.sex),
        roleId: Number(this.form.roleId),
        operatorId: this.curUser.id
      }
      this.$axios
        .post(this.$httpUrl + '/user/save', payload)
        .then((res) => res.data)
        .then((res) => {
          if (res.code == 200) {
            this.$message({ message: '操作成功', type: 'success' })
            this.centerDialogVisible = false
            this.loadPost()
            this.resetForm()
          } else {
            this.$message.error((res && res.msg) || '操作失败')
          }
        })
        .catch((err) => {
          this.$message.error(this.extractErrorMsg(err, '新增失败，请稍后重试'))
        })
    },
    doMod() {
      const payload = {
        ...this.form,
        age: this.form.age === '' ? null : Number(this.form.age),
        sex: Number(this.form.sex),
        roleId: Number(this.form.roleId),
        operatorId: this.curUser.id
      }
      this.$axios
        .post(this.$httpUrl + '/user/update', payload)
        .then((res) => res.data)
        .then((res) => {
          if (res.code == 200) {
            this.$message({ message: '操作成功', type: 'success' })
            this.centerDialogVisible = false
            this.loadPost()
          } else {
            this.$message.error((res && res.msg) || '操作失败')
          }
        })
        .catch((err) => {
          this.$message.error(this.extractErrorMsg(err, '编辑失败，请稍后重试'))
        })
    },
    save() {
      if (!this.isSuperAdmin) {
        this.$message.error('仅超级管理员可管理用户')
        return
      }
      this.$refs.form.validate((valid) => {
        if (valid) {
          if (this.form.id) {
            this.doMod()
          } else {
            this.doSave()
          }
        } else {
          return false
        }
      })
    },
    handleSizeChange(val) {
      this.pageNum = 1
      this.pageSize = val
      this.loadPost()
    },
    handleCurrentChange(val) {
      this.pageNum = val
      this.loadPost()
    },
    loadPost() {
      if (!this.isSuperAdmin) {
        this.tableData = []
        this.total = 0
        return
      }
      this.$axios
        .post(this.$httpUrl + '/user/listPageC1', {
          pageSize: this.pageSize,
          pageNum: this.pageNum,
          param: {
            name: this.name,
            roleId: this.roleId,
            operatorId: this.curUser.id
          }
        })
        .then((res) => res.data)
        .then((res) => {
          if (res.code == 200) {
            this.tableData = res.data
            this.total = res.total
          } else {
            this.$message.error((res && res.msg) || '获取数据失败')
          }
        })
        .catch((err) => {
          this.$message.error(this.extractErrorMsg(err, '获取数据失败，请稍后重试'))
        })
    }
  },
  beforeMount() {
    if (!this.isSuperAdmin) {
      this.$message.error('仅超级管理员可访问用户管理')
      this.$router.replace('/Home')
      return
    }
    this.loadPost()
  }
}
</script>

<style scoped>
.um-page {
  --um-cyan: #22d3ee;
  --um-indigo: #6366f1;
  --um-violet: #8b5cf6;
  text-align: left;
  position: relative;
  overflow: hidden;
}

/* subtle tech grid (scoped to this page only) */
.um-page::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  opacity: 0.4;
  background-image:
    linear-gradient(rgba(99, 102, 241, 0.07) 1px, transparent 1px),
    linear-gradient(90deg, rgba(99, 102, 241, 0.07) 1px, transparent 1px);
  background-size: 44px 44px;
  z-index: 0;
}

.um-head {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 20px;
  padding: 22px 24px 20px;
  border-radius: 16px;
  overflow: hidden;
  background: linear-gradient(125deg, #1e1b4b 0%, #312e81 38%, #4f46e5 72%, #0e7490 100%);
  box-shadow:
    0 16px 48px rgba(79, 70, 229, 0.28),
    0 0 0 1px rgba(255, 255, 255, 0.08) inset;
}

.um-head-glow {
  position: absolute;
  right: -20%;
  top: -60%;
  width: 55%;
  height: 200%;
  background: radial-gradient(circle, rgba(34, 211, 238, 0.35) 0%, transparent 65%);
  pointer-events: none;
}

.um-head-text {
  position: relative;
  z-index: 1;
}

.um-eyebrow {
  margin: 0 0 8px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.2em;
  color: rgba(226, 232, 240, 0.85);
  display: flex;
  align-items: center;
  gap: 8px;
}

.um-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--um-cyan);
  box-shadow: 0 0 12px var(--um-cyan);
  animation: um-pulse 2.2s ease-in-out infinite;
}

@keyframes um-pulse {
  0%,
  100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.65;
    transform: scale(0.92);
  }
}

.um-title {
  margin: 0 0 6px;
  font-size: 24px;
  font-weight: 700;
  letter-spacing: -0.03em;
  color: #f8fafc;
  text-shadow: 0 2px 24px rgba(15, 23, 42, 0.4);
}

.um-desc {
  margin: 0;
  font-size: 13px;
  color: rgba(226, 232, 240, 0.78);
  max-width: 360px;
  line-height: 1.5;
}

.um-toolbar {
  position: relative;
  z-index: 1;
  margin-bottom: 18px;
  padding: 1px;
  border-radius: 14px;
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.45), rgba(34, 211, 238, 0.25));
  box-shadow: 0 8px 32px rgba(15, 23, 42, 0.08);
}

.um-toolbar-inner {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 13px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(12px);
}

.um-input {
  width: 220px;
}

.um-select {
  width: 170px;
}

.um-input >>> .el-input__inner,
.um-select >>> .el-input__inner {
  border-color: rgba(99, 102, 241, 0.2);
  transition: border-color 0.2s, box-shadow 0.2s;
}

.um-input >>> .el-input__inner:focus,
.um-select >>> .el-input__inner:focus {
  border-color: var(--um-indigo);
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.15);
}

.um-btn-search {
  border-radius: 10px;
  font-weight: 600;
  box-shadow: 0 4px 14px rgba(79, 70, 229, 0.35);
}

.um-btn-add {
  border-radius: 10px;
  font-weight: 600;
  border-color: rgba(99, 102, 241, 0.45);
  color: #4f46e5;
}

.um-btn-add:hover {
  background: rgba(99, 102, 241, 0.08);
}

.um-table-shell {
  position: relative;
  z-index: 1;
  border-radius: 16px;
  padding: 1px;
  background: linear-gradient(160deg, rgba(99, 102, 241, 0.12), rgba(14, 116, 144, 0.1));
  box-shadow: 0 12px 40px rgba(15, 23, 42, 0.07);
}

.um-table-shell > .um-table {
  border-radius: 15px;
}

.um-table {
  width: 100%;
}

.um-table >>> .el-table__header-wrapper th {
  font-size: 13px;
}

.um-table >>> .um-table-row:hover > td {
  background: rgba(99, 102, 241, 0.06) !important;
}

.um-table >>> td {
  border-color: rgba(226, 232, 240, 0.9);
}

.um-table >>> .el-table__row--striped td {
  background: rgba(248, 250, 252, 0.85);
}

.um-pagination {
  padding: 14px 16px 16px;
  display: flex;
  justify-content: flex-end;
  background: #fff;
  border-radius: 0 0 15px 15px;
}

.um-pill {
  display: inline-block;
  min-width: 36px;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.um-pill--male {
  color: #1d4ed8;
  background: linear-gradient(180deg, #dbeafe, #eff6ff);
  box-shadow: 0 0 0 1px rgba(59, 130, 246, 0.25);
}

.um-pill--female {
  color: #be185d;
  background: linear-gradient(180deg, #fce7f3, #fdf2f8);
  box-shadow: 0 0 0 1px rgba(236, 72, 153, 0.22);
}

.um-role {
  display: inline-block;
  font-size: 12px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 6px;
  letter-spacing: 0.02em;
}

.um-role--0 {
  color: #b91c1c;
  background: rgba(254, 226, 226, 0.85);
  box-shadow: 0 0 12px rgba(239, 68, 68, 0.12);
}

.um-role--1 {
  color: #4338ca;
  background: rgba(224, 231, 255, 0.9);
  box-shadow: 0 0 12px rgba(99, 102, 241, 0.15);
}

.um-role--2 {
  color: #047857;
  background: rgba(209, 250, 229, 0.85);
  box-shadow: 0 0 12px rgba(16, 185, 129, 0.12);
}

.um-op {
  border-radius: 8px;
  font-weight: 600;
}

.um-op--edit {
  color: #4f46e5;
  border: 1px solid rgba(99, 102, 241, 0.45);
  background: rgba(255, 255, 255, 0.9);
}

.um-op--edit:hover {
  color: #fff;
  background: linear-gradient(135deg, #6366f1, #4f46e5);
  border-color: transparent;
}

.um-op--del {
  margin-left: 8px;
  color: #dc2626;
  border: 1px solid rgba(248, 113, 113, 0.5);
  background: rgba(255, 255, 255, 0.95);
}

.um-op--del:hover {
  color: #fff;
  background: linear-gradient(135deg, #f87171, #dc2626);
  border-color: transparent;
}
</style>

<style>
/* 非 scoped：对话框挂载在 body 下 */
.um-dialog.el-dialog {
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid rgba(99, 102, 241, 0.2);
  box-shadow: 0 24px 64px rgba(15, 23, 42, 0.2), 0 0 0 1px rgba(255, 255, 255, 0.06) inset;
}

.um-dialog .el-dialog__header {
  background: linear-gradient(125deg, #312e81, #4f46e5);
  border-bottom: none;
  padding: 18px 20px;
}

.um-dialog .el-dialog__title {
  color: #f8fafc;
  font-weight: 700;
  letter-spacing: 0.02em;
}

.um-dialog .el-dialog__headerbtn .el-dialog__close {
  color: rgba(248, 250, 252, 0.85);
}

.um-dialog .el-dialog__body {
  padding: 20px 22px 8px;
  background: #fafbfc;
}

.um-form .el-input__inner {
  border-radius: 10px;
}
</style>
