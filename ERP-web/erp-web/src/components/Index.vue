<template>
  <el-container class="app-shell">
    <el-aside :width="aside_width" class="app-aside-wrap">
      <Aside :isCollapse="isCollapse"></Aside>
    </el-aside>

    <el-container class="app-main-wrap">
      <el-header height="64px" class="app-header-wrap">
        <Header @doCollapse="doCollaspe" :icon="icon"></Header>
      </el-header>

      <el-main class="app-main">
        <div class="app-main-inner">
          <router-view></router-view>
        </div>
      </el-main>
    </el-container>
  </el-container>
</template>

<script>
import Aside from './Aside.vue'
import Header from './Header.vue'

export default {
  name: 'IndexView',
  components: { Aside, Header },
  data() {
    return {
      isCollapse: false,
      aside_width: '220px',
      icon: 'el-icon-arrow-left'
    }
  },
  methods: {
    doCollaspe() {
      this.isCollapse = !this.isCollapse
      if (!this.isCollapse) {
        this.aside_width = '220px'
        this.icon = 'el-icon-arrow-left'
      } else {
        this.aside_width = '72px'
        this.icon = 'el-icon-arrow-right'
      }
    }
  }
}
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
  background: var(--app-bg, #f0f4f8);
}

.app-aside-wrap {
  overflow: hidden;
  box-shadow: 4px 0 24px rgba(15, 23, 42, 0.06);
  z-index: 2;
}

.app-main-wrap {
  min-height: 100vh;
  flex-direction: column;
}

.app-header-wrap {
  padding: 0 20px;
  background: var(--app-surface, #fff);
  border-bottom: 1px solid var(--app-border, #e2e8f0);
  box-shadow: 0 1px 0 rgba(15, 23, 42, 0.04);
}

.app-main {
  padding: 0;
  background: var(--app-bg, #f0f4f8);
  flex: 1;
}

.app-main-inner {
  max-width: 1280px;
  margin: 0 auto;
  padding: 20px 24px 32px;
  min-height: calc(100vh - 64px);
}
</style>
