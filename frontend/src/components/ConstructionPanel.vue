<template>
  <el-card v-if="show" shadow="never" style="margin-bottom:14px">
    <!-- 施工队接单排期 -->
    <template v-if="app.status === 'PLAN_APPROVED' && role === 'TEAM'">
      <div class="section-title" style="margin-top:0">施工队接单 · 上门排期</div>
      <el-alert v-if="highRisk" type="error" :closable="false" show-icon style="margin-bottom:14px"
                title="该家庭评估为高风险，平台要求优先排期"
                :description="d.assessment?.careRecommendation" />
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

        <div class="sub-title">施工期间安全照护</div>
        <el-form-item label="陪同/照护安排" :required="highRisk">
          <el-radio-group v-model="s.careRequired">
            <el-radio v-for="o in meta.careOptions || []" :key="o.value" :value="o.value">{{ o.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="s.careRequired && s.careRequired !== 'NONE'" label="照护安排详情" :required="highRisk">
          <el-input v-model="s.careArrangement" type="textarea" :rows="2"
                    :placeholder="highRisk
                      ? '高风险必填：由谁陪同/照护、关键拆改工序谁在场、临时照护机构与联系人'
                      : '陪同人、时间段、联系方式（选填）'" />
        </el-form-item>
        <el-form-item>
          <el-button :type="highRisk ? 'danger' : 'primary'" :loading="loading" @click="accept">
            {{ highRisk ? '接单（高风险优先排期，确认照护安排）' : '接单并提交排期' }}
          </el-button>
        </el-form-item>
      </el-form>
    </template>

    <!-- 排期只读 + 开工 -->
    <template v-if="d.schedule && app.status !== 'PLAN_APPROVED'">
      <div class="section-title" style="margin-top:0">
        施工排期
        <el-tag v-if="d.schedule.priority === 1" type="danger" effect="dark" size="small" style="margin-left:8px">
          高风险优先排期
        </el-tag>
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
        <el-descriptions-item v-if="d.schedule.careRequired && d.schedule.careRequired !== 'NONE'"
                              label="施工陪同/临时照护" :span="2">
          <el-tag size="small" type="danger">
            {{ d.schedule.careRequired === 'TEMP_CARE' ? '临时照护/日间托管' : '家属全程陪同' }}
          </el-tag>
          <span style="margin-left:8px">{{ d.schedule.careArrangement }}</span>
        </el-descriptions-item>
        <el-descriptions-item v-if="d.schedule.remark" label="备注" :span="2">{{ d.schedule.remark }}</el-descriptions-item>
      </el-descriptions>
    </template>

    <!-- 施工队操作 -->
    <template v-if="role === 'TEAM' && app.status === 'IN_CONSTRUCTION'">
      <div class="action-inline">
        <el-button type="warning" :icon="Warning" @click="openChangeDialog">现场异常/加项 · 提报变更</el-button>
        <el-button type="primary" :icon="CircleCheck" @click="$emit('wantComplete')">施工完成 · 提交竣工验收</el-button>
      </div>
      <el-alert type="info" :closable="false"
                title="所有现场变更必须上传照片、说明和新材料需求，经家属确认、社区核定补贴后才生效；未通过前按原方案计价，禁止口头加价。" />
    </template>

    <!-- 协调中提示 -->
    <el-alert v-if="app.status === 'CHANGE_COORDINATING'" type="warning" :closable="false" show-icon
              title="变更正在社区协调中，施工暂停按变更内容施工，不得加价" style="margin:10px 0" />

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
              <el-tag v-if="c.subsidyAffected === true" size="small" type="success">影响补贴</el-tag>
              <el-tag v-else-if="c.status === 'COMMUNITY_APPROVED'" size="small" type="info">不新增补贴</el-tag>
              <span class="delta">
                材料 <b :class="Number(c.materialFeeDelta) > 0 ? 'up' : ''">¥{{ c.materialFeeDelta }}</b>
                ＋ 人工 <b :class="Number(c.laborFeeDelta) > 0 ? 'up' : ''">¥{{ c.laborFeeDelta }}</b>
                ＝ <b class="up">¥{{ c.costDelta }}</b>
              </span>
            </div>
            <div class="change-desc">{{ c.description }}</div>

            <el-descriptions :column="1" size="small" border style="margin-top:8px">
              <el-descriptions-item label="现场照片">
                <el-icon><Picture /></el-icon> {{ c.sitePhotos || '—' }}
              </el-descriptions-item>
              <el-descriptions-item label="新材料需求">{{ c.materialRequirements || '—' }}</el-descriptions-item>
            </el-descriptions>

            <!-- 变更明细 -->
            <el-table v-if="itemsOf(c.id).length" :data="itemsOf(c.id)" size="small" border style="margin-top:8px">
              <el-table-column label="类型" width="86">
                <template #default="{ row }">
                  <el-tag size="small" :type="row.itemType === 'ADD' ? 'warning' : 'info'">
                    {{ row.itemType === 'ADD' ? '新增项目' : '材料/工艺替代' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="category" label="类别" width="80" />
              <el-table-column prop="name" label="项目/材料" min-width="160" />
              <el-table-column label="数量" width="80">
                <template #default="{ row }">{{ row.quantity }}{{ row.unit }}</template>
              </el-table-column>
              <el-table-column label="材料费" width="90">
                <template #default="{ row }">¥{{ row.materialFee }}</template>
              </el-table-column>
              <el-table-column label="人工费" width="90">
                <template #default="{ row }">¥{{ row.laborFee }}</template>
              </el-table-column>
              <el-table-column label="可报销" width="80">
                <template #default="{ row }">
                  <el-icon v-if="row.subsidyEligible" color="#2f6f5e"><CircleCheckFilled /></el-icon>
                  <span v-else style="color:#b7bfbc">—</span>
                </template>
              </el-table-column>
              <el-table-column prop="reason" label="现场原因" min-width="140" />
            </el-table>

            <!-- 社区核准后的重算结果 -->
            <div v-if="c.status === 'COMMUNITY_APPROVED' && c.newTotalCost != null" class="recalc">
              <el-icon color="#2f6f5e"><RefreshRight /></el-icon>
              <b>核准后重算：</b>
              材料费 ¥{{ c.newMaterialFee }} ＋ 人工费 ¥{{ c.newLaborFee }}
              ＝ 总费用 <b class="cost-money">¥{{ c.newTotalCost }}</b>，
              可报销金额 <b style="color:#2f6f5e">¥{{ c.newSubsidyAmount }}</b>
              <el-tag v-if="Number(c.reimbursementDelta) > 0" type="success" size="small">
                本次新增可报销 ¥{{ c.reimbursementDelta }}
              </el-tag>
            </div>

            <div v-if="c.familyOpinion" class="change-op">家属意见：{{ c.familyOpinion }}（{{ fmt(c.familyConfirmedAt) }}）</div>
            <div v-if="c.communityRemark" class="change-op">社区意见：{{ c.communityRemark }}（{{ fmt(c.communityReviewedAt) }}）</div>
            <div v-if="c.coordinationNote" class="change-op" style="color:#b88230">协调记录：{{ c.coordinationNote }}</div>

            <!-- 家属确认 -->
            <div v-if="app.status === 'CHANGE_PENDING_FAMILY' && role === 'FAMILY'" class="change-actions">
              <el-input v-model="opinionMap[c.id]" placeholder="家属意见（同意或不同意均可补充）" style="max-width:420px" />
              <el-button type="primary" size="small" @click="family(c, true)">同意变更，提交社区核定</el-button>
              <el-button type="danger" size="small" @click="family(c, false)">不同意，维持原方案</el-button>
            </div>

            <!-- 社区三分支决策 -->
            <div v-if="app.status === 'CHANGE_PENDING_COMMUNITY' && role === 'COMMUNITY'" class="community-review">
              <div class="sub-title" style="margin:6px 0">判断是否影响补贴资格（可报销明细勾选）</div>
              <el-checkbox v-for="it in eligibleCandidates(c.id)" :key="it.id"
                           v-model="eligibleMap[it.id]" style="display:block;margin-bottom:4px">
                纳入可报销：{{ it.name }}（{{ it.quantity }}{{ it.unit }}，
                材料 ¥{{ it.materialFee }}＋人工 ¥{{ it.laborFee }}）
              </el-checkbox>
              <el-input v-model="remarkMap[c.id]" placeholder="社区意见：补贴规则判断/驳回原因/协调说明"
                        type="textarea" :rows="2" style="max-width:640px;margin:6px 0" />
              <div>
                <el-button type="success" size="small" @click="community(c, 'APPROVED')">
                  核准通过并重算费用
                </el-button>
                <el-button type="danger" size="small" @click="community(c, 'REJECTED')">
                  未通过·保留原方案
                </el-button>
                <el-button type="warning" size="small" @click="community(c, 'COORDINATING')">
                  转社区协调
                </el-button>
              </div>
            </div>

            <!-- 社区协调处理 -->
            <div v-if="app.status === 'CHANGE_COORDINATING' && role === 'COMMUNITY'" class="change-actions">
              <el-input v-model="coordRemark" placeholder="协调结果说明" style="max-width:420px" />
              <el-button type="primary" size="small" @click="coordinate(c, true)">协调采纳（费用自理）</el-button>
              <el-button size="small" @click="coordinate(c, false)">维持原方案</el-button>
            </div>
          </el-card>
        </el-timeline-item>
      </el-timeline>
    </template>

    <!-- 提报变更对话框 -->
    <el-dialog v-model="showChange" title="提报现场变更" width="820px" top="6vh">
      <el-alert type="warning" :closable="false" show-icon style="margin-bottom:12px"
                title="变更须经家属确认、社区核定补贴后方可执行；禁止仅凭口头约定加价" />
      <el-form :model="cf" label-width="120px">
        <el-form-item label="异常/加项类型" required>
          <el-select v-model="cf.reasonType" style="width:100%">
            <el-option v-for="(label, key) in meta.changeReasons" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="变更说明" required>
          <el-input v-model="cf.description" type="textarea" :rows="2"
                    placeholder="现场发现的问题、替代工艺或老人加项诉求" />
        </el-form-item>
        <el-form-item label="现场照片" required>
          <el-input v-model="cf.sitePhotos" type="textarea" :rows="2"
                    placeholder="照片档案编号/说明（演示环境以文字代替上传），如：空鼓墙面近景2张、测距1张" />
        </el-form-item>
        <el-form-item label="新材料需求" required>
          <el-input v-model="cf.materialRequirements" type="textarea" :rows="2"
                    placeholder="如：304不锈钢背板2块、M8对穿螺栓4套；不涉及新材料请填“不涉及”" />
        </el-form-item>
      </el-form>

      <div class="sub-title">变更项目明细（材料费/人工费）</div>
      <el-table :data="cf.items" size="small" border>
        <el-table-column label="类型" width="120">
          <template #default="{ row }">
            <el-select v-model="row.itemType" size="small">
              <el-option label="新增项目" value="ADD" />
              <el-option label="材料/工艺替代" value="REPLACE" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="类别" width="100">
          <template #default="{ row }">
            <el-select v-model="row.category" size="small">
              <el-option v-for="c in (meta.planCategories || [])" :key="c" :label="c" :value="c" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="名称/规格" min-width="180">
          <template #default="{ row }">
            <el-input v-model="row.name" size="small" placeholder="项目名称" style="margin-bottom:2px" />
            <el-input v-model="row.spec" size="small" placeholder="规格型号" />
          </template>
        </el-table-column>
        <el-table-column label="单位/数量" width="120">
          <template #default="{ row }">
            <el-input v-model="row.unit" size="small" placeholder="单位" style="margin-bottom:2px" />
            <el-input-number v-model="row.quantity" :min="1" size="small" controls-position="right" style="width:90px" />
          </template>
        </el-table-column>
        <el-table-column label="材料费(元)" width="110">
          <template #default="{ row }">
            <el-input-number v-model="row.materialFee" :min="0" :step="10" size="small" controls-position="right" style="width:100px" />
          </template>
        </el-table-column>
        <el-table-column label="人工费(元)" width="110">
          <template #default="{ row }">
            <el-input-number v-model="row.laborFee" :min="0" :step="10" size="small" controls-position="right" style="width:100px" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="60">
          <template #default="{ $index }">
            <el-button link type="danger" size="small" @click="cf.items.splice($index, 1)">删</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-button plain size="small" style="margin:8px 0" @click="addRow">+ 添加明细行</el-button>
      <div class="sum-bar">
        材料费合计 <b>¥{{ sumFee('materialFee') }}</b>
        ＋ 人工费合计 <b>¥{{ sumFee('laborFee') }}</b>
        ＝ 变更增加 <b class="up">¥{{ sumFee('materialFee') + sumFee('laborFee') }}</b>
      </div>
      <template #footer>
        <el-button @click="showChange = false">取消</el-button>
        <el-button type="warning" :loading="loading" @click="submitChange">
          提交（家属确认 → 社区核定补贴）
        </el-button>
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
const eligibleMap = reactive({})
const coordRemark = ref('')

const today = new Date().toISOString().slice(0, 10)
const s = reactive({
  scheduledStart: today,
  scheduledEnd: today,
  buildingAccess: '提前与物业报备，材料经单元门搬运',
  elevatorPlan: '',
  materialArrival: '主要材料开工前一天送达社区暂存点',
  elderSchedule: '老人午休 12:30-14:30，午休期间仅安排无噪音工序',
  noiseRestriction: '电锤作业限 9:00-11:30、15:00-17:30',
  careRequired: 'NONE',
  careArrangement: ''
})

const emptyRow = () => ({
  itemType: 'ADD', category: '扶手', name: '', spec: '', unit: '处', quantity: 1,
  materialFee: 100, laborFee: 50, reason: ''
})
const cf = reactive({
  reasonType: 'WALL_UNDRILLABLE',
  description: '',
  sitePhotos: '',
  materialRequirements: '',
  materialFeeDelta: 0,
  laborFeeDelta: 0,
  costDelta: 0,
  items: [emptyRow()]
})

const highRisk = computed(() => props.d.assessment?.fallRiskLevel === '高')

const show = computed(() =>
  ['PLAN_APPROVED', 'SCHEDULED', 'IN_CONSTRUCTION', 'CHANGE_PENDING_FAMILY', 'CHANGE_PENDING_COMMUNITY',
   'CHANGE_COORDINATING', 'COMPLETED', 'SETTLED', 'VISITED'].includes(props.app.status))

function reasonLabel(k) {
  return props.meta.changeReasons?.[k] || k
}
function changeStatus(s) {
  return {
    SUBMITTED: '待家属确认', FAMILY_CONFIRMED: '待社区复核', COMMUNITY_APPROVED: '已核准并重算',
    REJECTED: '未通过·保留原方案', COORDINATING: '社区协调中'
  }[s] || s
}
function changeColor(s) {
  return { SUBMITTED: 'warning', FAMILY_CONFIRMED: 'primary', COMMUNITY_APPROVED: 'success',
    REJECTED: 'danger', COORDINATING: 'warning' }[s] || 'info'
}
function fmt(t) {
  return t ? t.replace('T', ' ').substring(0, 16) : ''
}
function itemsOf(changeId) {
  return props.d.changeItems?.[changeId] || []
}
function eligibleCandidates(changeId) {
  return itemsOf(changeId).filter((i) => i.itemType === 'ADD')
}

function openChangeDialog() {
  cf.reasonType = 'WALL_UNDRILLABLE'
  cf.description = ''
  cf.sitePhotos = ''
  cf.materialRequirements = ''
  cf.items = [emptyRow()]
  showChange.value = true
}
function addRow() {
  cf.items.push(emptyRow())
}
function sumFee(key) {
  return cf.items.reduce((sum, r) => sum + (Number(r[key]) || 0), 0)
}

async function accept() {
  if (!s.scheduledStart || !s.scheduledEnd) {
    ElMessage.warning('请选择计划工期')
    return
  }
  if (highRisk.value && (!s.careRequired || s.careRequired === 'NONE')) {
    ElMessage.warning('高风险家庭必须选择家属陪同或临时照护安排')
    return
  }
  if (highRisk.value && !s.careArrangement?.trim()) {
    ElMessage.warning('请填写陪同/临时照护的具体安排')
    return
  }
  loading.value = true
  try {
    await api.post(`/applications/${props.app.id}/schedule`, s)
    ElMessage.success(highRisk.value ? '已接单（高风险优先排期）' : '已接单，排期已通知社区与家属')
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
  if (!cf.description) return ElMessage.warning('请填写变更说明')
  if (!cf.sitePhotos) return ElMessage.warning('请上传现场照片或填写照片档案编号')
  if (!cf.materialRequirements) return ElMessage.warning('请填写新材料需求（无则填“不涉及”）')
  if (!cf.items.length) return ElMessage.warning('请至少添加一条变更明细')
  for (const it of cf.items) {
    if (!it.name) return ElMessage.warning('请完善每条明细的名称')
    if (!it.reason) it.reason = cf.description
  }
  const material = sumFee('materialFee')
  const labor = sumFee('laborFee')
  const payload = {
    reasonType: cf.reasonType,
    description: cf.description,
    sitePhotos: cf.sitePhotos,
    materialRequirements: cf.materialRequirements,
    materialFeeDelta: material,
    laborFeeDelta: labor,
    costDelta: material + labor,
    items: cf.items.map(({ itemType, category, name, spec, unit, quantity, materialFee, laborFee, reason }) =>
      ({ itemType, category, name, spec, unit, quantity, materialFee, laborFee, reason }))
  }
  loading.value = true
  try {
    await api.post(`/applications/${props.app.id}/changes`, payload)
    ElMessage.success('变更已提报：等待家属确认与社区补贴核定')
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
  await api.post(`/changes/${c.id}/family`, { agree, opinion: opinionMap[c.id] || '不同意，按原方案执行' })
  ElMessage.success(agree ? '已同意，流转社区核定补贴' : '已驳回，按原方案继续施工')
  emit('done')
}

async function community(c, decision) {
  const eligibleIds = eligibleCandidates(c.id).filter((i) => eligibleMap[i.id]).map((i) => i.id)
  const remark = remarkMap[c.id]
    || (decision === 'APPROVED' ? '变更合理，按勾选项核定可报销'
      : decision === 'REJECTED' ? '不予认可，维持原方案与原费用'
      : '存在争议，转社区协调处理')
  await api.post(`/changes/${c.id}/community`, { decision, remark, eligibleItemIds: eligibleIds })
  ElMessage.success({ APPROVED: '已核准并重算费用', REJECTED: '已驳回，保留原方案', COORDINATING: '已转入社区协调' }[decision])
  emit('done')
}

async function coordinate(c, adopted) {
  if (!coordRemark.value) {
    ElMessage.warning('请填写协调结果')
    return
  }
  await api.post(`/changes/${c.id}/coordination`, { adopted, remark: coordRemark.value })
  ElMessage.success('协调结果已记录，恢复施工')
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
  flex-wrap: wrap;
}
.delta {
  margin-left: auto;
  font-size: 13px;
}
.delta .up, .up {
  color: #c4561e;
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
  align-items: center;
}
.community-review {
  margin-top: 10px;
  padding: 10px;
  background: #fbf8f1;
  border-radius: 6px;
}
.recalc {
  margin-top: 8px;
  padding: 8px 10px;
  background: #f1f7f4;
  border-radius: 6px;
  font-size: 13px;
  display: flex;
  gap: 6px;
  align-items: center;
  flex-wrap: wrap;
}
.sub-title {
  font-weight: 600;
  color: #1f3d35;
  margin: 6px 0 12px;
  padding-left: 10px;
  border-left: 3px solid #79b49f;
}
.sum-bar {
  text-align: right;
  padding: 8px 4px;
  font-size: 14px;
}
</style>
