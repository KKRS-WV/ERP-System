<template>
  <div class="app-header">
    <button type="button" class="collapse-btn" @click="collapse" :aria-label="isCollapsed ? '展开菜单' : '收起菜单'">
      <i :class="icon"></i>
    </button>
    <div class="header-title">
      <span class="header-title-main">ERP 控制台</span>
      <span class="header-title-sub">资源管理</span>
    </div>
    <div class="header-right">
      <span class="header-user">{{ user.name }}</span>
      <el-dropdown trigger="click" placement="bottom-end">
        <span class="header-avatar-wrap">
          <span class="header-avatar">{{ userInitial }}</span>
          <i class="el-icon-arrow-down header-caret"></i>
        </span>
        <el-dropdown-menu slot="dropdown" class="header-dropdown">
          <el-dropdown-item icon="el-icon-user" @click.native="toUser">个人中心</el-dropdown-item>
          <el-dropdown-item divided icon="el-icon-switch-button" @click.native="toLogout">退出登录</el-dropdown-item>
        </el-dropdown-menu>
      </el-dropdown>
    </div>
  </div>
</template>

<script>
export default {
  name: 'HeaderA',
  data() {
    return {
      user: JSON.parse(sessionStorage.getItem('CurUser') || '{}')
    }
  },
  computed: {
    userInitial() {
      const n = this.user && this.user.name
      return n ? String(n).charAt(0).toUpperCase() : '?'
    },
    isCollapsed() {
      return this.icon === 'el-icon-arrow-right'
    }
  },
  props: {
    icon: String
  },
  methods: {
    toUser() {
      this.$router.push('/Home')
    },
    toLogout() {
      this.$confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
        .then(() => {
          this.$message.success('已安全退出')
          sessionStorage.clear()
          this.$router.push('/')
        })
        .catch(() => {})
    },
    collapse() {
      this.$emit('doCollapse')
    }
  },
  created() {
    this.$router.push('/Home')
  }
}
</script>

<style scoped>
.app-header {
  display: flex;
  align-items: center;
  height: 64px;
  gap: 16px;
}

.collapse-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  padding: 0;
  border: none;
  border-radius: 10px;
  background: #f1f5f9;
  color: #475569;
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
}

.collapse-btn:hover {
  background: #e2e8f0;
  color: #0f172a;
}

.collapse-btn i {
  font-size: 18px;
}

.header-title {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  min-width: 0;
}

.header-title-main {
  font-size: 17px;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: -0.02em;
  line-height: 1.25;
}

.header-title-sub {
  font-size: 12px;
  color: #64748b;
  margin-top: 2px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-user {
  font-size: 14px;
  color: #475569;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.header-avatar-wrap {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  padding: 4px 4px 4px 0;
  border-radius: 999px;
  transition: background 0.2s;
}

.header-avatar-wrap:hover {
  background: #f1f5f9;
}

.header-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, #6366f1, #4f46e5);
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.35);
}

.header-caret {
  font-size: 12px;
  color: #94a3b8;
}
</style>
