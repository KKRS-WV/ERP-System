import Vue from 'vue'
import App from './App.vue'
import ElementUI from 'element-ui'; 
import 'element-ui/lib/theme-chalk/index.css';
import './assets/globe.css';
import axios from 'axios';
import VueRouter from 'vue-router';
import { Message } from 'element-ui'
import router from './router';
import store from './store'

const TOKEN_KEY = 'CurToken'
let authNotified = false

/**
 * 刷新后 Vuex 会清空，菜单仅来自登录接口。
 * 已登录时从 sessionStorage 恢复菜单并重新 addRoutes，避免侧栏为空、动态页 404。
 */
function restoreSessionMenu() {
  if (!sessionStorage.getItem('CurUser') || !sessionStorage.getItem(TOKEN_KEY)) return
  const raw = sessionStorage.getItem('CurMenu')
  if (raw == null || raw === '') return
  try {
    const menu = JSON.parse(raw)
    if (Array.isArray(menu)) {
      store.commit('setMenu', menu)
    }
  } catch (e) {
    console.warn('[erp] restore CurMenu failed', e)
  }
}
restoreSessionMenu()

Vue.prototype.$httpUrl='http://localhost:8090'
Vue.prototype.$axios=axios;

axios.interceptors.request.use((config) => {
  const token = sessionStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers = config.headers || {}
    config.headers.Authorization = 'Bearer ' + token
  }
  return config
})

axios.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error && error.response ? error.response.status : 0
    if (status === 401) {
      sessionStorage.removeItem(TOKEN_KEY)
      sessionStorage.removeItem('CurUser')
      sessionStorage.removeItem('CurMenu')
      if (!authNotified) {
        authNotified = true
        Message.error('登录已过期，请重新登录')
        setTimeout(() => {
          authNotified = false
        }, 1000)
      }
      if (router.currentRoute.path !== '/') {
        router.replace('/')
      }
    }
    return Promise.reject(error)
  }
)

Vue.config.productionTip = false
Vue.use(ElementUI);
Vue.use(VueRouter);
new Vue({
  router,
  store,
  render: h => h(App),
}).$mount('#app')
