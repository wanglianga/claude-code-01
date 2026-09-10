<template>
  <div class="login-wrap">
    <div class="login-card">
      <div class="brand">
        <el-icon :size="34" color="#2f6f5e"><HomeFilled /></el-icon>
        <div>
          <h1>适老化改造入户评估与施工交付平台</h1>
          <p>申请 · 评估 · 方案 · 施工 · 补贴 · 质保 全流程协同</p>
        </div>
      </div>

      <el-form :model="form" @keyup.enter="submit" class="login-form">
        <el-form-item>
          <el-input v-model="form.username" size="large" placeholder="用户名" :prefix-icon="User" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" size="large" type="password" placeholder="密码"
                    :prefix-icon="Lock" show-password />
        </el-form-item>
        <el-button type="primary" size="large" style="width:100%" :loading="loading" @click="submit">
          登 录
        </el-button>
      </el-form>

      <el-divider>演示账号（密码均为 123456）</el-divider>
      <div class="accounts">
        <el-tag v-for="acc in accounts" :key="acc.u" class="acc-tag"
                :type="acc.type" effect="plain" @click="quick(acc.u)">
          {{ acc.label }}：{{ acc.u }}
        </el-tag>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import api from '../api'
import { useAuthStore } from '../store'

const router = useRouter()
const auth = useAuthStore()
const form = reactive({ username: 'zhangwei', password: '123456' })
const loading = ref(false)

const accounts = [
  { u: 'zhangwei', label: '家属(张伟)', type: 'success' },
  { u: 'wangli', label: '社区(王丽)', type: 'warning' },
  { u: 'lizhigu', label: '评估师(李智固)', type: 'primary' },
  { u: 'chenjianguo', label: '施工队(陈建国)', type: 'info' },
  { u: 'sunjiwei', label: '街道(孙纪伟)', type: 'danger' }
]

function quick(u) {
  form.username = u
  form.password = '123456'
}

async function submit() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const res = await api.post('/auth/login', form)
    auth.setSession(res.token, res.user)
    ElMessage.success(`欢迎，${res.user.realName}`)
    router.push('/dashboard')
  } catch (e) {
    // 全局拦截器已提示
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrap {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #2f6f5e 0%, #4a8f7a 55%, #79b49f 100%);
  padding: 20px;
}
.login-card {
  width: 460px;
  background: #fff;
  border-radius: 16px;
  padding: 34px 38px 28px;
  box-shadow: 0 18px 50px rgba(0, 0, 0, 0.22);
}
.brand {
  display: flex;
  gap: 14px;
  align-items: flex-start;
  margin-bottom: 22px;
}
.brand h1 {
  font-size: 19px;
  margin: 2px 0 6px;
  color: #1f3d35;
}
.brand p {
  margin: 0;
  font-size: 13px;
  color: #7a8580;
}
.accounts {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.acc-tag {
  cursor: pointer;
  font-size: 12px;
}
.login-form {
  margin-top: 8px;
}
</style>
