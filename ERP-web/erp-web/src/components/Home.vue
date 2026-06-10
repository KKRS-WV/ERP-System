<template>
  <div class="home-page">
    <section class="home-hero">
      <p class="home-eyebrow">工作台</p>
      <h1 class="home-title">你好，{{ user.name }}</h1>
      <p class="home-lead">这是您的个人概览，信息一目了然。</p>
    </section>

    <el-row :gutter="20" class="home-row">
      <el-col :xs="24" :md="14">
        <div class="home-card">
          <div class="home-card-head">
            <h2 class="home-card-title">个人信息</h2>
          </div>
          <el-descriptions :column="1" border size="medium" class="home-desc">
            <el-descriptions-item>
              <template slot="label">
                <span class="desc-label"><i class="el-icon-s-custom"></i> 账号</span>
              </template>
              {{ user.no }}
            </el-descriptions-item>
            <el-descriptions-item>
              <template slot="label">
                <span class="desc-label"><i class="el-icon-mobile-phone"></i> 电话</span>
              </template>
              {{ user.phone }}
            </el-descriptions-item>
            <el-descriptions-item>
              <template slot="label">
                <span class="desc-label"><i class="el-icon-male"></i> 性别</span>
              </template>
              <el-tag :type="user.sex === '1' ? 'primary' : 'success'" effect="plain" size="medium">
                {{ user.sex == 1 ? '男' : '女' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item>
              <template slot="label">
                <span class="desc-label"><i class="el-icon-s-flag"></i> 角色</span>
              </template>
              <el-tag type="info" effect="plain" size="medium">
                {{ user.roleId == 0 ? '超级管理员' : user.roleId == 1 ? '管理员' : '用户' }}
              </el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </el-col>
      <el-col :xs="24" :md="10">
        <div class="home-card home-card--clock">
          <DateUtils />
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import DateUtils from './DateUtils'
export default {
  name: 'HomeS',
  components: { DateUtils },
  data() {
    return {
      user: {}
    }
  },
  methods: {
    init() {
      this.user = JSON.parse(sessionStorage.getItem('CurUser') || '{}')
    }
  },
  created() {
    this.init()
  }
}
</script>

<style scoped>
.home-page {
  text-align: left;
}

.home-hero {
  margin-bottom: 24px;
  padding: 28px 28px 26px;
  border-radius: 16px;
  background: linear-gradient(125deg, #312e81 0%, #4f46e5 45%, #6366f1 100%);
  color: #fff;
  box-shadow: 0 16px 40px rgba(79, 70, 229, 0.35);
}

.home-eyebrow {
  margin: 0 0 8px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  opacity: 0.85;
}

.home-title {
  margin: 0 0 8px;
  font-size: 28px;
  font-weight: 700;
  letter-spacing: -0.02em;
  line-height: 1.2;
}

.home-lead {
  margin: 0;
  font-size: 14px;
  line-height: 1.5;
  opacity: 0.92;
  max-width: 480px;
}

.home-row {
  align-items: stretch;
}

.home-card {
  height: 100%;
  background: #fff;
  border-radius: 16px;
  padding: 22px 24px 24px;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.06), 0 12px 32px rgba(15, 23, 42, 0.06);
  border: 1px solid #e2e8f0;
}

.home-card--clock {
  display: flex;
  align-items: stretch;
  min-height: 200px;
}

.home-card-head {
  margin-bottom: 16px;
}

.home-card-title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.desc-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #64748b;
  font-weight: 500;
}

.home-desc >>> .el-descriptions-item__label {
  width: 100px;
}
</style>
