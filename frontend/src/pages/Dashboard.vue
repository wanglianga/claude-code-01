<template>
  <div>
    <h2 class="page-title">工作台 · {{ ROLE_LABEL[auth.role] }}</h2>

    <el-row :gutter="16">
      <el-col :span="6">
        <el-card shadow="hover" class="stat">
          <div class="stat-num">{{ stats.total ?? 0 }}</div>
          <div class="stat-label">可见工单总数</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat stat-todo">
          <div class="stat-num">{{ stats.todo ?? 0 }}</div>
          <div class="stat-label">我的待办</div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <div class="section-title" style="margin-top:0">按状态分布</div>
          <div class="dist">
            <el-tag v-for="(cnt, st) in (stats.byStatus || {})" :key="st"
                    :type="STATUS_TYPE[st] || 'info'" class="dist-tag"
                    @click="$router.push(`/applications?status=${st}`)">
              {{ statusLabel(st) }} × {{ cnt }}
            </el-tag>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top:16px">
      <div class="section-title" style="margin-top:0">
        我的待办工单
        <el-button link type="primary" @click="$router.push('/applications')">全部工单 →</el-button>
      </div>
      <el-table :data="todos" stripe size="default">
        <el-table-column prop="id" label="工单号" width="90" />
        <el-table-column prop="elderName" label="老人" width="100" />
        <el-table-column prop="address" label="地址" min-width="220" show-overflow-tooltip />
        <el-table-column label="当前状态" width="170">
          <template #default="{ row }">
            <el-tag :type="STATUS_TYPE[row.status]">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="待办环节" min-width="200">
          <template #default="{ row }">{{ todoHint(row.status) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="110">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="$router.push(`/applications/${row.id}`)">
              去处理
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!todos.length" description="暂无待办，所有工单都在正常推进" />
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import api from '../api'
import { useAuthStore, ROLE_LABEL, STATUS_TYPE } from '../store'

const auth = useAuthStore()
const stats = ref({})
const all = ref([])

const meta = ref({ statusLabels: {} })
const statusLabel = (s) => meta.value.statusLabels[s] || s

const TODO_STATUS = {
  FAMILY: ['PLAN_REVIEW', 'CHANGE_PENDING_FAMILY'],
  COMMUNITY: ['SUBMITTED', 'VERIFIED', 'PLAN_FAMILY_CONFIRMED', 'CHANGE_PENDING_COMMUNITY'],
  ASSESSOR: ['ASSIGNED'],
  TEAM: ['PLAN_APPROVED', 'SCHEDULED', 'IN_CONSTRUCTION'],
  STREET: ['COMPLETED']
}

const HINTS = {
  PLAN_REVIEW: '查看方案改造原因/费用/补贴/施工影响，增删项目并签字确认',
  CHANGE_PENDING_FAMILY: '施工队提交现场变更，等待家属确认',
  SUBMITTED: '核验补贴资格与房屋信息',
  VERIFIED: '选择评估师派单',
  PLAN_FAMILY_CONFIRMED: '家属增删过项目，重新复核补贴规则',
  CHANGE_PENDING_COMMUNITY: '变更单等待社区复核',
  ASSIGNED: '入户采集门槛/卫生间/墙体/照明等评估数据',
  PLAN_APPROVED: '查看排期约束并接单',
  SCHEDULED: '按排期开工',
  IN_CONSTRUCTION: '推进施工；如有现场异常可提交变更；完工后提交竣工验收',
  COMPLETED: '审核前后照片、签字、材料费用明细，核准补贴'
}

const todos = computed(() => {
  const wanted = TODO_STATUS[auth.role] || []
  return all.value.filter((a) => wanted.includes(a.status))
})

function todoHint(s) {
  return HINTS[s] || ''
}

onMounted(async () => {
  const [m, st, list] = await Promise.all([
    api.get('/meta'),
    api.get('/stats'),
    api.get('/applications')
  ])
  meta.value = m
  stats.value = st
  all.value = list
})
</script>

<style scoped>
.stat {
  text-align: center;
}
.stat-num {
  font-size: 34px;
  font-weight: 700;
  color: #2f6f5e;
}
.stat-todo .stat-num {
  color: #c4561e;
}
.stat-label {
  color: #7a8580;
  margin-top: 6px;
}
.dist {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.dist-tag {
  cursor: pointer;
}
</style>
