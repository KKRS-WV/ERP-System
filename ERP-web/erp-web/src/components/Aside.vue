<template>
  <div class="aside-root">
    <div class="aside-brand" v-show="!isCollapse">
      <span class="aside-brand-mark">E</span>
      <span class="aside-brand-text">ERP</span>
    </div>
    <div class="aside-brand aside-brand--mini" v-show="isCollapse">
      <span class="aside-brand-mark">E</span>
    </div>
    <el-menu
      class="app-sidebar-menu"
      background-color="transparent"
      text-color="rgba(255,255,255,0.82)"
      active-text-color="#fff"
      :default-active="activeMenu"
      :collapse="isCollapse"
      :collapse-transition="false"
      router
    >
      <el-menu-item index="/Home">
        <i class="el-icon-s-home"></i>
        <span slot="title">首页</span>
      </el-menu-item>
      <el-menu-item index="/Purchase">
        <i class="el-icon-s-goods"></i>
        <span slot="title">采购订单</span>
      </el-menu-item>
      <el-menu-item index="/Sales">
        <i class="el-icon-s-order"></i>
        <span slot="title">销售订单</span>
      </el-menu-item>
      <el-menu-item index="/Warehouse">
        <i class="el-icon-box"></i>
        <span slot="title">仓库管理</span>
      </el-menu-item>
      <el-menu-item v-if="canViewFinance" index="/Finance">
        <i class="el-icon-data-line"></i>
        <span slot="title">财务报表</span>
      </el-menu-item>

      <el-menu-item :index="'/' + item.menuclick" v-for="(item, i) in filteredMenu" :key="i">
        <i :class="item.menuicon"></i>
        <span slot="title">{{ item.menuname }}</span>
      </el-menu-item>
    </el-menu>
  </div>
</template>

<script>
export default {
  name: 'AsideS',
  computed: {
    menu: {
      get() {
        return this.$store.state.menu
      }
    },
    roleId() {
      try {
        const u = JSON.parse(sessionStorage.getItem('CurUser') || '{}')
        const r = Number(u.roleId)
        return Number.isFinite(r) ? r : 2
      } catch (e) {
        return 2
      }
    },
    canViewFinance() {
      return this.roleId <= 1
    },
    filteredMenu() {
      return (this.menu || []).filter((item) => {
        const click = String((item && item.menuclick) || '').toLowerCase()
        if (click === 'user') return this.roleId === 0
        if (click === 'finance') return false
        return true
      })
    },
    activeMenu() {
      return this.$route.path || '/Home'
    }
  },
  props: {
    isCollapse: Boolean
  }
}
</script>

<style scoped>
.aside-root {
  display: flex;
  flex-direction: column;
  height: 100vh;
  min-height: 100%;
  background: linear-gradient(180deg, #0f172a 0%, #1e293b 55%, #0f172a 100%);
  border-right: 1px solid rgba(255, 255, 255, 0.06);
}

.aside-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px 18px 16px;
  flex-shrink: 0;
}

.aside-brand--mini {
  justify-content: center;
  padding-left: 0;
  padding-right: 0;
}

.aside-brand-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  font-size: 16px;
  font-weight: 800;
  color: #fff;
  background: linear-gradient(135deg, #38bdf8, #6366f1);
  border-radius: 10px;
  box-shadow: 0 8px 20px rgba(56, 189, 248, 0.25);
}

.aside-brand-text {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0.06em;
  color: #f8fafc;
}

.app-sidebar-menu {
  flex: 1;
  border-right: none !important;
  padding: 8px 10px 24px;
  background: transparent !important;
}

.app-sidebar-menu >>> .el-menu-item {
  height: 48px;
  line-height: 48px;
  margin-bottom: 4px;
  border-radius: 10px;
  font-size: 14px;
}

.app-sidebar-menu >>> .el-menu-item i {
  color: rgba(255, 255, 255, 0.55);
}

.app-sidebar-menu >>> .el-menu-item:hover {
  background: rgba(255, 255, 255, 0.08) !important;
}

.app-sidebar-menu >>> .el-menu-item.is-active {
  background: linear-gradient(90deg, rgba(99, 102, 241, 0.45), rgba(56, 189, 248, 0.15)) !important;
  color: #fff !important;
  box-shadow: inset 3px 0 0 #38bdf8;
}

.app-sidebar-menu >>> .el-menu-item.is-active i {
  color: #e0e7ff;
}
</style>
