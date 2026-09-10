import axios from 'axios'
import { ElMessage } from 'element-plus'

const api = axios.create({ baseURL: '/api', timeout: 20000 })

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (resp) => resp.data,
  (error) => {
    const status = error.response?.status
    const msg = error.response?.data?.message || error.message || '请求失败'
    if (status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      if (location.hash !== '#/login') {
        location.hash = '#/login'
      }
    }
    ElMessage.error(msg)
    return Promise.reject(error)
  }
)

export default api
