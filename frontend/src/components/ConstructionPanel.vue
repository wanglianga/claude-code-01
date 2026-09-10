<template>
  <el-card v-if="show" shadow="never" style="margin-bottom:14px">
    <!-- 施工队接单排期 -->
    <template v-if="app.status === 'PLAN_APPROVED' && role === 'TEAM'">
      <div class="section-title" style="margin-top:0">施工队接单 · 上门排期</div>
      <el-form :model="s" label-width="130px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="计划开始日期">
              <el-date-picker v-model="s.scheduledStart" type="date" value-format="YYYY-MM-DD" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="计划结束日期">
              <el-date-picker v-model="s.scheduledEnd" type="date" value-format="YYYY-MM-DD" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="楼栋通行">
          <el-input v-model="s.buildingAccess" placeholder="单元门宽、搬运路线、物业报备" />
        </el-form-item>
        <el-form-item label="电梯使用">
          <el-input v-model="s.elevatorPlan" placeholder="无电梯时的楼层搬运、错峰安排" />
        </el-form-item>
        <el-form-item label="材料到货">
          <el-input v-model="s.materialArrival" placeholder="材料到场/暂存安排" />
        </el-form-item>
        <el-form-item label="老人作息">
          <el-input v-model="s.elderSchedule" placeholder="如午休12:30-14:30，期间只做无噪音工序" />
        </el-form-item>
        <el-form-item label="邻里噪音限制">
          <el-input v-model="s.noiseRestriction" placeholder="如电锤仅限9:00-11:30、15:00-17:30" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="accept">接单并提交排期</el-button>
        </el-form-item>
      </el-form>
    </template>

    <!-- 排期只读 + 开工 -->
    <template v-if="d.schedule && app.status !== 'PLAN_APPROVED'">
      <div class="section-title" style="margin-top:0">
        施工排期
        <el-button v-if="role === 'TEAM' && app.status === 'SCHEDULED'" type="primary"
                   size="small" style="margin-left:12px" :loading="loading" @click="start">按排期开工</el-button>
      </div>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="计划工期">{{ d.schedule.scheduledStart }} 至 {{ d.schedule.scheduledEnd }}</el-descriptions-item>
        <el-descriptions-item label="材料到货">{{ d.schedule.materialArrival }}</el-descriptions-item>
        <el-descriptions-item label="楼栋通行" :span="2">{{ d.schedule.buildingAccess }}</el-descriptions-item>
        <el-descriptions-item label="电梯使用" :span="2">{{ d.schedule.elevatorPlan }}</el-descriptions-item>
        <el-descriptions-item label="老人作息" :span="2">{{ d.schedule.elderSchedule }}</el-descriptions-item>
        <el-descriptions-item label="噪音限制" :span="2">{{ d.schedule.noiseRestriction }}</el-descriptions-item>
        <el-descriptions-item v-if="d.schedule.remark" label="备注" :span="2">{{ d.schedule.remark }}</el-descriptions-item>
      </el-descriptions>
    </template>

    <!-- 施工队提交变更 -->
    <template v-if="role === 'TEAM' && app.status === 'IN_CONSTRUCTION'">
      <div class="action-inline">
        <el-button type="warning" :icon="Warning" @click="showChange = true">现场异常 · 提交变更</el-button>
        <el-button type="primary" :icon="CircleCheck" @click="$emit('wantComplete')">施工完成 · 提交竣工验收</el-button>
      </div>
    </template>

    <!-- 变更单列表 -->
    <template v-if="d.changes?.length">
      <div class="section-title">现场变更记录（{{ d.changes.length }}）</div>
      <el-timeline>
        <el-timeline-item v-for="c in d.changes" :key="c.id" :type="changeColor(c.status)"
                          :timestamp="fmt(c.createdAt)">
          <el-card shadow="never">
            <div class="change-head">
              <el-tag size="small" type="warning">{{ reasonLabel(c.reasonType) }}</el-tag>
              <el-tag size="small" :type="changeColor(c.status)">{{ changeStatus(c.status) }}</el-tag>
              <span class="delta" :class="Number(c.costDelta) > 0 ? 'up' : Number(c.costDelta) < 0 ? 'down' : ''">
                费用变化 ¥{{ c.costDelta }}
              </span>
            </div>
            <div class="change-desc">{{ c.description }}</div>
            <div v-if="c.familyOpinion" class="change-op">家属意见：{{ c.familyOpinion }}（{{ fmt(c.familyConfirmedAt) }}）</div>
            <div v-if="c.communityRemark" class="change-op">社区意见：{{ c.communityRemark }}（{{ fmt(c.communityReviewedAt) }}）</div>

            <div v-if="app.status === 'CHANGE_PENDING_FAMILY' && role === 'FAMILY'" class="change-actions">
              <el-input v-model="opinionMap[c.id]" placeholder="家属意见（必填）" style="max-width:420px" />
              <el-button type="primary" size="small" @click="family(c, true)">同意变更</el-button>
              <el-button type="danger" size="small" @click="family(c, false)">不同意</el-button>
            </div>
            <div v-if="app.status === 'CHANGE_PENDING_COMMUNITY' && role === 'COMMUNITY'" class="change-actions">
              <el-input v-model="remarkMap[c.id]" placeholder="社区复核意见" style="max-width:420px" />
              <el-button type="success" size="small" @click="community(c, true)">核准变更</el-button>
              <el-button type="danger" size="small" @click="community(c, false)">驳回</el-button>
            </div>
          </el-card>
        </el-timeline-item>
      </el-timeline>
    </template>

    <!-- 提交变更对话框 -->
    <el-dialog v-model="showChange" title="提交现场变更" width="600px">
      <el-form :model="cf" label-width="120px">
        <el-form-item label="异常类型" required>
          <el-select v-model="cf.reasonType" style="width:100%">
            <el-option v-for="(label, key) in meta.changeReasons" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="情况说明" required>
          <el-input v-model="cf.description" type="textarea" :rows="3"
                    placeholder="描述现场情况、替代方案，如改用穿墙背板、材料替代型号等" />
        </el-form-item>
        <el-form-item label="费用变化(元)">
          <el-input-number v-model="cf.costDelta" :step="20" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showChange = false">取消</el-button>
        <el-button type="warning" :loading="loading" @click="submitChange">提交（回家属确认+社区复核）</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Warning, CircleCheck } from '@element-plus/icons-vue'
import api from '../api'

const props = defineProps({ app: Object, role: String, d: Object, meta: Object })
const emit = defineEmits(['done', 'wantComplete'])

const loading = ref(false)
const showChange = ref(false)
const opinionMap = reactive({})
const remarkMap = reactive({})

const today = new Date().toISOString().slice(0, 10)
const s = reactive({
  scheduledStart: today,
  scheduledEnd: today,
  buildingAccess: '提前与物业报备，材料经单元门搬运',
  elevatorPlan: '',
  materialArrival: '主要材料开工前一天送达社区暂存点',
  elderSchedule: '老人午休 12:30-14:30，午休期间仅安排无噪音工序',
  noiseRestriction: '电锤作业限 9:00-11:30、15:00-17:30'
})

const cf = reactive({ reasonType: 'WALL_UNDRILLABLE', description: '', costDelta: 0 })

const show = computed(() =>
  ['PLAN_APPROVED', 'SCHEDULED', 'IN_CONSTRUCTION', 'CHANGE_PENDING_FAMILY', 'CHANGE_PENDING_COMMUNITY',
   'COMPLETED', 'SETTLED', 'VISITED'].includes(props.app.status))

function reasonLabel(k) {
  return props.meta.changeReasons?.[k] || k
}
function changeStatus(s) {
  return { SUBMITTED: '待家属确认', FAMILY_CONFIRMED: '待社区复核', COMMUNITY_APPROVED: '已核准', REJECTED: '已驳回' }[s] || s
}
function changeColor(s) {
  return { SUBMITTED: 'warning', FAMILY_CONFIRMED: 'primary', COMMUNITY_APPROVED: 'success', REJECTED: 'danger' }[s] || 'info'
}
function fmt(t) {
  return t ? t.replace('T', ' ').substring(0, 16) : ''
}

async function accept() {
  if (!s.scheduledStart || !s.scheduledEnd) {
    ElMessage.warning('请选择计划工期')
    return
  }
  loading.value = true
  try {
    await api.post(`/applications/${props.app.id}/schedule`, s)
    ElMessage.success('已接单，排期已通知社区与家属')
    emit('done')
  } finally {
    loading.value = false
  }
}

async function start() {
  loading.value = true
  try {
    await api.post(`/applications/${props.app.id}/start`, {})
    ElMessage.success('已开工')
    emit('done')
  } finally {
    loading.value = false
  }
}

async function submitChange() {
  if (!cf.description) {
    ElMessage.warning('请填写现场情况说明')
    return
  }
  loading.value = true
  try {
    await api.post(`/applications/${props.app.id}/changes`, cf)
    ElMessage.success('变更已提交，等待家属确认与社区复核')
    showChange.value = false
    emit('done')
  } finally {
    loading.value = false
  }
}

async function family(c, agree) {
  if (agree && !opinionMap[c.id]) {
    ElMessage.warning('请填写家属意见')
    return
  }
  await api.post(`/changes/${c.id}/family`, { agree, opinion: opinionMap[c.id] || '不同意，请施工队按原方案处理' })
  ElMessage.success(agree ? '已同意，流转社区复核' : '已驳回，回到施工环节')
  emit('done')
}

async function community(c, approved) {
  await api.post(`/changes/${c.id}/community`, {
    approved,
    remark: remarkMap[c.id] || (approved ? '变更合理，核准' : '变更不符合补贴/施工规则')
  })
  ElMessage.success(approved ? '变更已核准' : '已驳回')
  emit('done')
}
</script>

<style scoped>
.action-inline {
  display: flex;
  gap: 12px;
  margin: 4px 0 10px;
}
.change-head {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 6px;
}
.delta {
  margin-left: auto;
  font-weight: 600;
}
.delta.up {
  color: #c4561e;
}
.delta.down {
  color: #2f6f5e;
}
.change-desc {
  font-size: 13px;
  line-height: 1.6;
}
.change-op {
  font-size: 13px;
  color: #5a6661;
  margin-top: 4px;
}
.change-actions {
  margin-top: 10px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
</style>
