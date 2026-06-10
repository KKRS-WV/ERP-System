import VueRouter from 'vue-router'

/** 与初始路由一致，供 resetRouter 重置 matcher，避免动态路由残留 */
function createRoutes() {
  return [
    {
      path: '/',
      name: 'login',
      component: () => import('../components/Login')
    },
    {
      path: '/Index',
      name: 'index',
      component: () => import('../components/Index'),
      children: [
        {
          path: '/Home',
          name: 'home',
          meta: { title: '首页' },
          component: () => import('../components/Home')
        },
        {
          path: '/User',
          name: 'user',
          meta: { title: '用户', maxRoleId: 0 },
          component: () => import('../components/user/UserManage.vue')
        },
        {
          path: '/Purchase',
          name: 'purchase',
          meta: { title: '采购订单' },
          component: () => import('../components/purchase/PurchaseManage.vue')
        },
        {
          path: '/Sales',
          name: 'sales',
          meta: { title: '销售订单' },
          component: () => import('../components/sales/SalesManage.vue')
        },
        {
          path: '/Warehouse',
          name: 'warehouse',
          meta: { title: '仓库管理' },
          component: () => import('../components/warehouse/WarehouseManage.vue')
        },
        {
          path: '/Finance',
          name: 'finance',
          meta: { title: '财务报表', maxRoleId: 1 },
          component: () => import('../components/finance/FinanceReport.vue')
        }
      ]
    }
  ]
}

const router = new VueRouter({
  mode: 'history',
  routes: createRoutes()
})

const VueRouterPush = VueRouter.prototype.push
VueRouter.prototype.push = function push(to) {
  return VueRouterPush.call(this, to).catch((err) => err)
}

function currentRoleId() {
  try {
    const raw = sessionStorage.getItem('CurUser') || '{}'
    const u = JSON.parse(raw)
    const role = Number(u.roleId)
    return Number.isFinite(role) ? role : 2
  } catch (e) {
    return 2
  }
}

router.beforeEach((to, from, next) => {
  const needLogin = to.path !== '/'
  const hasLogin = !!sessionStorage.getItem('CurUser') && !!sessionStorage.getItem('CurToken')
  if (needLogin && !hasLogin) {
    next('/')
    return
  }
  const limit = to && to.meta ? to.meta.maxRoleId : null
  if (limit != null && currentRoleId() > Number(limit)) {
    next('/Home')
    return
  }
  next()
})

/**
 * 重置路由 matcher，配合动态菜单 addRoutes 使用。
 * store 里 setMenu 会调用，未导出时会导致 resetRouter is not a function，登录后无法跳转。
 */
export function resetRouter() {
  const next = new VueRouter({
    mode: 'history',
    routes: createRoutes()
  })
  router.matcher = next.matcher
}

export default router
