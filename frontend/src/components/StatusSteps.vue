<template>
  <el-card shadow="never" style="margin-bottom:14px">
    <div class="head">
      <div>
        <span class="id">工单 #{{ app.id }}</span>
        <span class="elder">{{ app.elderName }}（{{ app.elderAge }}岁）</span>
        <el-tag :type="STATUS_TYPE[app.status]" size="large" style="margin-left:10px">
          {{ labels[app.status] || app.status }}
        </el-tag>
        <el-tag v-if="riskLevel" :type="riskLevel === '高' ? 'danger' : riskLevel === '中' ? 'warning' : 'success'"
                effect="dark" size="large" style="margin-left:8px">
          {{ riskLevel }}风险
        </el-tag>
      </div>
      <div class="addr">
        <el-icon><Location /></el-icon>{{ app.address }} ｜ 家属电话：{{ app.phone }}
      </div>
    </div>
    <el-steps :active="stepIndex" :process-status="app.status === 'VERIFY_REJECTED' ? 'error' : 'process'"
              align-center finish-status="success" style="margin-top:16px">
      <el-step v-for="s in STEP_LABELS" :key="s" :title="s" />
    </el-steps>
    <el-alert v-if="app.status === 'VERIFY_REJECTED'" type="error" :closable="false"
              :title="`核验不通过：${app.verifyRemark || '请联系社区工作人员'}`" style="margin-top:12px" />
    <el-alert v-if="app.status === 'CHANGE_PENDING_FAMILY' || app.status === 'CHANGE_PENDING_COMMUNITY'"
              type="warning" :closable="false" style="margin-top:12px"
              title="施工中产生现场变更，流程已回到家属确认 / 社区复核节点，核准后方可继续施工" />
  </el-card>
</template>

<script setup>
import { computed } from 'vue'
import { STATUS_TYPE } from '../store'

const props = defineProps({
  app: { type: Object, required: true },
  labels: { type: Object, default: () => ({}) },
  riskLevel: { type: String, default: '' }
})

const STEP_LABELS = ['提交申请', '社区核验', '派单评估', '入户评估', '方案确认', '补贴复核', '接单排期', '施工/变更', '竣工验收', '街道审核', '质保回访']

const MAP = {
  SUBMITTED: 0, VERIFY_REJECTED: 0, VERIFIED: 1, ASSIGNED: 2,
  PLAN_REVIEW: 3, PLAN_FAMILY_CONFIRMED: 4, PLAN_APPROVED: 5,
  SCHEDULED: 6, IN_CONSTRUCTION: 7, CHANGE_PENDING_FAMILY: 7, CHANGE_PENDING_COMMUNITY: 7,
  COMPLETED: 8, SETTLED: 9, VISITED: 10
}
const stepIndex = computed(() => MAP[props.app.status] ?? 0)
</script>

<style scoped>
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}
.id {
  font-size: 16px;
  font-weight: 700;
  color: #2f6f5e;
}
.elder {
  font-size: 15px;
  font-weight: 600;
  margin-left: 12px;
}
.addr {
  color: #7a8580;
  font-size: 13px;
}
</style>
