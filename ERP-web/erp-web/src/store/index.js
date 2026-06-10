import vue from 'vue'
import Vuex from 'vuex'
import router, { resetRouter } from '../router'
vue.use(Vuex)

/** 登录接口返回但无对应前端路由/已废弃的菜单，不展示、不注册动态路由 */
const EXCLUDED_MENU_NAMES = ['物品分类管理', '物品管理', '记录管理', '仓库管理', '管理员']

function filterSidebarMenu(menuList) {
  if (!menuList || !Array.isArray(menuList)) return []
  return menuList.filter((m) => {
    if (!m || !m.menuname) return false
    const name = String(m.menuname).trim()
    const click = String(m.menuclick || '').trim().toLowerCase()
    if (click === 'admin') return false
    return !EXCLUDED_MENU_NAMES.includes(name)
  })
}

function addNewRoute(menuList) {
  if (!menuList || !Array.isArray(menuList) || menuList.length === 0) {
    resetRouter()
    return
  }

  const routes = router.options.routes
  routes.forEach((routeItem) => {
    if (routeItem.path === '/Index') {
      menuList.forEach((menu) => {
        if (!menu || !menu.menuclick || !menu.menucomponent) {
          return
        }
        const childPath = '/' + menu.menuclick
        const hasSamePath = Array.isArray(routeItem.children) && routeItem.children.some((c) => c.path === childPath)
        if (hasSamePath) {
          return
        }
        const childRoute = {
          path: childPath,
          name: menu.menuname,
          meta: {
            title: menu.menuname
          },
          component: () => import('../components/' + menu.menucomponent)
        }
        routeItem.children.push(childRoute)
      })
    }
  })

  resetRouter()
  router.addRoutes(routes)
}

export default new Vuex.Store({
    state: {
        menu: []
    },
    mutations: {
        setMenu(state, menuList) {
            const filtered = filterSidebarMenu(menuList)
            state.menu = filtered
            addNewRoute(filtered)
        }
    },
    getters: {
        getMenu(state) {
            return state.menu
        }
    }
})