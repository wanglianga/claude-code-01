import { defineStore } from 'pinia'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    user: JSON.parse(localStorage.getItem('user') || 'null')
  }),
  getters: {
    role: (s) => s.user?.role || '',
    isLogin: (s) => !!s.token
  },
  actions: {
    setSession(token, user) {
      this.token = token
      this.user = user
      localStorage.setItem('token', token)
      localStorage.setItem('user', JSON.stringify(user))
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem('token')
      localStorage.removeItem('user')
    }
  }
})

export const ROLE_LABEL = {
  FAMILY: '老人/家属',
  COMMUNITY: '社区工作人员',
  ASSESSOR: '评估师',
  TEAM: '施工队',
  STREET: '街道审核员'
}

export const STATUS_STEPS = [
  'SUBMITTED', 'VERIFIED', 'ASSIGNED', 'PLAN_REVIEW', 'PLAN_FAMILY_CONFIRMED',
  'PLAN_APPROVED', 'SCHEDULED', 'IN_CONSTRUCTION', 'COMPLETED', 'SETTLED', 'VISITED'
]

export const STATUS_TYPE = {
  SUBMITTED: 'info',
  VERIFY_REJECTED: 'danger',
  VERIFIED: 'warning',
  ASSIGNED: 'warning',
  PLAN_REVIEW: 'primary',
  PLAN_FAMILY_CONFIRMED: 'primary',
  PLAN_APPROVED: 'success',
  SCHEDULED: 'warning',
  IN_CONSTRUCTION: '',
  CHANGE_PENDING_FAMILY: 'danger',
  CHANGE_PENDING_COMMUNITY: 'danger',
  CHANGE_COORDINATING: 'warning',
  COMPLETED: 'warning',
  SETTLED: 'success',
  VISITED: 'success'
}
