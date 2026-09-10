<template>
  <div>
    <div class="head">
      <h2 class="page-title" style="margin:0">工单列表</h2>
      <el-button v-if="auth.role === 'FAMILY'" type="primary" @click="$router.push('/applications/new')">
        <el-icon><Plus /></el-icon> 提交新申请
      </el-button>
    </div>

    <el-card>
      <div class="filters">
        <el-radio-group v-model="statusFilter" @change="load">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button v-for="(label, st) in meta.statusLabels" :key="st" :value="st">
            {{ label }}
          </el-radio-button>
        </el-radio-group>
        <el-input v-model="keyword" placeholder="搜索老人/地址" clearable style="width:220px" />
      </div>

      <el-table :data="filtered" stripe @row-click="(r) => $router.push(`/applications/${r.id}`)"
                :row-style="{ cursor: 'pointer' }">
        <el-table-column prop="id" label="工单号" width="80" />
        <el-table-column prop="elderName" label="老人" width="90" />
        <el-table-column prop="elderAge" label="年龄" width="70" />
        <el-table-column label="楼层/电梯" width="100">
          <template #default="{ row }">{{ row.floor }}层{{ row.hasElevator ? '/有梯' : '/无梯' }}</template>
        </el-table-column>
        <el-table-column prop="mobility" label="行动能力" width="90" />
        <el-table-column prop="address" label="地址" min-width="210" show-overflow-tooltip />
        <el-table-column prop="phone" label="家属电话" width="130" />
        <el-table-column label="当前状态" width="180">
          <template #default="{ row }">
            <el-tag :type="STATUS_TYPE[row.status]">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button size="small" link type="primary" @click.stop="$router.push(`/applications/${row.id}`)">
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import api from '../api'
import { useAuthStore, STATUS_TYPE } from '../store'

const auth = useAuthStore()
const route = useRoute()
const list = ref([])
const statusFilter = ref(route.query.status || '')
const keyword = ref('')
const meta = ref({ statusLabels: {} })

const statusLabel = (s) => meta.value.statusLabels[s] || s

const filtered = computed(() => {
  if (!keyword.value) return list.value
  const k = keyword.value.trim()
  return list.value.filter(
    (a) => (a.elderName || '').includes(k) || (a.address || '').includes(k)
  )
})

async function load() {
  list.value = await api.get('/applications', {
    params: statusFilter.value ? { status: statusFilter.value } : {}
  })
}

onMounted(async () => {
  meta.value = await api.get('/meta')
  await load()
})
</script>

<style scoped>
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}
.filters {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}
</style>
