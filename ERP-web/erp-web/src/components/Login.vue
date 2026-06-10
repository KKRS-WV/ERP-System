<template>
  <div class="loginBody">
    <div class="login-bg" aria-hidden="true"></div>
    <div class="loginDiv">
      <div class="login-brand">
        <div class="login-logo">ERP</div>
        <p class="login-tagline">资源管理系统</p>
      </div>
      <div class="login-content">
        <h1 class="login-title">登录</h1>
        <p class="login-sub">使用您的账号进入控制台</p>
        <el-form
          :model="loginForm"
          label-width="72px"
          :rules="rules"
          ref="loginForm"
          class="login-form"
        >
          <el-form-item label="账号" prop="no">
            <el-input
              v-model="loginForm.no"
              autocomplete="off"
              placeholder="请输入账号"
              prefix-icon="el-icon-user"
            />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              show-password
              autocomplete="off"
              placeholder="请输入密码"
              prefix-icon="el-icon-lock"
              @keyup.enter.native="confirm"
            />
          </el-form-item>
          <el-form-item class="login-actions">
            <el-button type="primary" class="login-btn" @click="confirm" :loading="confirm_disabled">
              登 录
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'LoginS',
  data() {
    return {
      confirm_disabled: false,
      loginForm: {
        no: '',
        password: ''
      },
      rules: {
        no: [{ required: true, message: '请输入账号', trigger: 'blur' }],
        password: [{ required: true, message: '请输密码', trigger: 'blur' }]
      }
    }
  },
  methods: {
    normalizeLoginError(msg) {
      if (!msg) return ''
      const raw = String(msg)
      if (
        raw.indexOf('Communications link failure') !== -1 ||
        raw.indexOf('Connection refused') !== -1 ||
        raw.indexOf('No operations allowed after connection closed') !== -1
      ) {
        return '数据库连接失败，请确认数据库服务已启动'
      }
      return raw
    },
    confirm() {
      this.confirm_disabled = true
      this.$refs.loginForm.validate((valid) => {
        if (valid) {
          this.$axios
            .post(this.$httpUrl + '/user/login', this.loginForm)
            .then((res) => res.data)
            .then((res) => {
              console.log(res)
              if (res.code == 200) {
                if (!res.data || !res.data.token) {
                  this.confirm_disabled = false
                  this.$message.error('登录失败：未获取到令牌')
                  return false
                }
                sessionStorage.setItem('CurToken', res.data.token)
                sessionStorage.setItem('CurUser', JSON.stringify(res.data.user))
                this.$store.commit('setMenu', res.data.menu || [])
                // 持久化已过滤后的菜单（与侧栏一致，不含已废弃项）
                sessionStorage.setItem('CurMenu', JSON.stringify(this.$store.state.menu))
                this.$router.replace('/Index')
              } else {
                this.confirm_disabled = false
                this.$message.error(this.normalizeLoginError((res && res.msg) || '用户名或密码错误'))
                return false
              }
            })
            .catch((err) => {
              this.confirm_disabled = false
              const msg =
                (err.response && err.response.data && err.response.data.msg) ||
                (err.message && err.message.indexOf('Network Error') !== -1
                  ? '无法连接服务器，请确认后端已启动（如 localhost:8090）且允许跨域'
                  : err.message)
              this.$message.error(this.normalizeLoginError(msg) || '请求失败，请稍后重试')
            })
        } else {
          this.confirm_disabled = false
          console.log('校验失败')
          return false
        }
      })
    }
  }
}
</script>

<style scoped>
.loginBody {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 16px;
  overflow: hidden;
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 45%, #312e81 100%);
}

.login-bg {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse 80% 50% at 20% 10%, rgba(56, 189, 248, 0.25), transparent 50%),
    radial-gradient(ellipse 60% 40% at 80% 90%, rgba(99, 102, 241, 0.35), transparent 45%);
  pointer-events: none;
}

.loginDiv {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 420px;
  padding: 36px 36px 32px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-radius: 20px;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.35), 0 0 0 1px rgba(255, 255, 255, 0.2) inset;
}

.login-brand {
  text-align: center;
  margin-bottom: 28px;
}

.login-logo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 52px;
  height: 52px;
  margin-bottom: 12px;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0.04em;
  color: #fff;
  background: linear-gradient(135deg, #6366f1, #4f46e5);
  border-radius: 14px;
  box-shadow: 0 10px 28px rgba(79, 70, 229, 0.45);
}

.login-tagline {
  margin: 0;
  font-size: 13px;
  color: #64748b;
  letter-spacing: 0.02em;
}

.login-title {
  margin: 0 0 6px;
  font-size: 22px;
  font-weight: 700;
  color: #0f172a;
  text-align: center;
}

.login-sub {
  margin: 0 0 24px;
  font-size: 13px;
  color: #64748b;
  text-align: center;
}

.login-form >>> .el-form-item__label {
  color: #475569;
  font-weight: 500;
}

.login-form >>> .el-input__inner {
  height: 42px;
  line-height: 42px;
}

.login-actions {
  margin-bottom: 0;
}

.login-actions >>> .el-form-item__content {
  margin-left: 72px !important;
}

.login-btn {
  width: 100%;
  height: 42px;
  font-size: 15px;
  font-weight: 600;
  border-radius: 10px;
  letter-spacing: 0.06em;
}
</style>
