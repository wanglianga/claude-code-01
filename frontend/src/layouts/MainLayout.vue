<template>
  <el-container class="layout">
    <el-aside width="218px" class="aside">
      <div class="logo">
        <el-icon :size="22"><HomeFilled /></el-icon>
        <span>适老化改造平台</span>
      </div>
      <el-menu :default-active="$route.path" router class="menu"
               background-color="#1f3d35" text-color="#cfe0da" active-text-color="#ffd98a">
        <el-menu-item index="/dashboard">
          <el-icon><DataBoard /></el-icon><span>工作台</span>
        </el-menu-item>
        <el-menu-item index="/applications">
          <el-icon><Document /></el-icon><span>工单列表</span>
        </el-menu-item>
        <el-menu-item v-if="auth.role === 'FAMILY'" index="/applications/new">
          <el-icon><EditPen /></el-icon><span>提交改造申请</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="title">城市社区适老化改造入户评估与施工交付平台</div>
        <el-dropdown @command="onCommand">
          <span class="user">
            <el-avatar :size="30" class="avatar">{{ auth.user?.realName?.charAt(0) }}</el-avatar>
            {{ auth.user?.realName }}
            <el-tag size="small" type="success" effect="dark" class="role-tag">
              {{ ROLE_LABEL[auth.role] }}
            </el-tag>
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>

      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore, ROLE_LABEL } from '../store'

const auth = useAuthStore()
const router = useRouter()

function onCommand(cmd) {
  if (cmd === 'logout') {
    auth.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.layout {
  height: 100vh;
}
.aside {
  background: #1f3d35;
}
.logo {
  height: 60px;
  display: flex;
  align-items: center;
  gap: 10px;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  padding: 0 18px;
  white-space: nowrap;
}
.menu {
  border-right: none;
}
.header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e6ebe9;
}
.header .title {
  font-size: 16px;
  font-weight: 600;
  color: #1f3d35;
}
.user {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;
}
.avatar {
  background: #2f6f5e;
}
.role-tag {
  margin-right: 2px;
}
.main {
  background: #f4f6f8;
  padding: 18px 22px;
}
</style>
