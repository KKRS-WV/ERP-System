<script>
/**
 * 兼容旧菜单：后端 menu 表里若 menucomponent 仍为 Main.vue，动态路由会加载本文件。
 * 实际页面与「用户管理」共用同一实现。
 */
import UserManage from './user/UserManage.vue'
export default UserManage
</script>
