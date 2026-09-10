<template>
  <el-card v-if="items.length" shadow="never" style="margin-bottom:14px">
    <div class="section-title" style="margin-top:0">
      改造方案
      <el-tag v-if="app.status === 'PLAN_REVIEW'" type="danger" size="small" style="margin-left:8px">待家属确认</el-tag>
      <el-tag v-else-if="app.status === 'PLAN_FAMILY_CONFIRMED'" type="warning" size="small" style="margin-left:8px">待社区复核补贴</el-tag>
      <el-tag v-else-if="app.status === 'PLAN_APPROVED'" type="success" size="small" style="margin-left:8px">方案已核准</el-tag>
    </div>

    <el-table :data="items" border :row-class-name="rowClass">
      <el-table-column label="项目" min-width="200">
        <template #default="{ row }">
          <div class="item-name">
            <el-tag size="small" effect="plain">{{ row.category }}</el-tag>
            <span :class="{ removed: row.status === 'REMOVED' }">{{ row.name }}</span>
            <el-tag v-if="HIGH_RISK_CATEGORIES.includes(row.category)" size="small" type="danger" effect="dark">
              高风险项
            </el-tag>
            <el-tag v-if="row.status === 'ADDED'" size="small" type="warning">家属新增</el-tag>
            <el-tag v-if="row.status === 'REMOVED'" size="small" type="info">已删除</el-tag>
          </div>
          <div class="spec">{{ row.spec }}</div>
        </template>
      </el-table-column>
      <el-table-column label="改造原因（评估依据）" min-width="240">
        <template #default="{ row }"><span class="reason">{{ row.reason }}</span></template>
      </el-table-column>
      <el-table-column label="数量/单价" width="120">
        <template #default="{ row }">{{ row.quantity }}{{ row.unit }} × ¥{{ row.unitPrice }}</template>
      </el-table-column>
      <el-table-column label="小计" width="100">
        <template #default="{ row }"
          ><span class="cost-money">¥{{ lineCost(row) }}</span></template>
      </el-table-column>
      <el-table-column label="补贴范围" width="150">
        <template #default="{ row }">
          <template v-if="familyEditing && ['ADDED', 'PROPOSED'].includes(row.status)">
            <el-input-number v-model="capEdit[row.id]" :min="0" :max="lineCost(row)" :step="10"
                             :controls="false" size="small" style="width:100px" />
          </template>
          <span v-else :class="{ 'subsidy-zero': Number(row.subsidyCap) === 0 }">
            上限 ¥{{ capOf(row) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="施工影响" min-width="200">
        <template #default="{ row }">{{ row.constructionImpact }}</template>
      </el-table-column>
      <el-table-column v-if="canFamilyEdit" label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status !== 'REMOVED'" link type="danger" size="small"
                     @click="remove(row)">删除</el-button>
          <el-button v-else link type="primary" size="small" @click="undoRemove(row)">恢复</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 合计 -->
    <div class="cost-bar">
      <span>合计：<b class="cost-money">¥{{ cost.total }}</b></span>
      <span>拟补贴：<b style="color:#2f6f5e">¥{{ cost.subsidy }}</b>（总额封顶 ¥5000）</span>
      <span>家属自付：<b class="cost-money">¥{{ cost.selfPay }}</b></span>
    </div>

    <!-- 家属：新增项目 -->
    <div v-if="canFamilyEdit" style="margin-top:12px">
      <el-button :icon="Plus" plain @click="showAdd = !showAdd">增加改造项目</el-button>
      <el-form v-if="showAdd" inline style="margin-top:10px" class="add-form">
        <el-form-item label="类别">
          <el-select v-model="addForm.category" style="width:130px">
            <el-option v-for="c in meta.planCategories" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="addForm.name" style="width:200px" placeholder="如：厨房燃气报警器" />
        </el-form-item>
        <el-form-item label="单价">
          <el-input-number v-model="addForm.unitPrice" :min="1" style="width:130px" />
        </el-form-item>
        <el-form-item label="数量">
          <el-input-number v-model="addForm.quantity" :min="1" style="width:100px" />
        </el-form-item>
        <el-form-item label="原因/诉求" style="margin-right:0">
          <el-input v-model="addForm.reason" style="width:260px" placeholder="为什么要加" />
        </el-form-item>
        <el-form-item label="施工影响">
          <el-input v-model="addForm.constructionImpact" style="width:200px" placeholder="可留空" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="addItem">添加</el-button>
        </el-form-item>
      </el-form>
      <el-alert type="warning" :closable="false" style="margin-top:8px"
                title="家属新增项目是否纳入补贴、补贴多少，需要社区工作人员重新按补贴规则核定（非目录项目默认补贴为 0）。" />
    </div>

    <!-- 家属签字确认 -->
    <div v-if="canFamilyEdit" class="action-row">
      <el-input v-model="signer" placeholder="签字人（家属姓名）" style="width:240px" :prefix-icon="EditPen" />
      <el-button type="primary" size="large" :loading="loading" @click="familyConfirm">
        家属确认方案并签字
      </el-button>
    </div>

    <!-- 已拒绝项目留痕（家属确认后家属/社区/街道均可见） -->
    <div v-if="d.rejections?.length" class="rejections">
      <div class="section-title">家属拒绝项目留痕（提交街道补贴审核备查）</div>
      <el-table :data="d.rejections" border size="small">
        <el-table-column label="拒绝项目" min-width="180">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.category }}</el-tag>
            <b style="margin-left:6px">{{ row.itemName }}</b>
            <el-tag v-if="row.riskLevel === '高'" size="small" type="danger" style="margin-left:6px">评估为高风险时建议</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="评估师说明（建议依据）" min-width="240">
          <template #default="{ row }"><span class="assessor-note">{{ row.assessorNote }}</span></template>
        </el-table-column>
        <el-table-column label="家属拒绝原因" min-width="220">
          <template #default="{ row }"><span class="family-reason">{{ row.familyReason }}</span></template>
        </el-table-column>
        <el-table-column label="时间" width="150">
          <template #default="{ row }">{{ fmt(row.createdAt) }}</template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 社区复核 -->
    <div v-if="role === 'COMMUNITY' && app.status === 'PLAN_FAMILY_CONFIRMED'" class="action-row community">
      <el-input v-model="reviewRemark" type="textarea" :rows="2" style="max-width:520px"
                placeholder="复核意见：补贴目录、单项上限、总额封顶是否符合规则" />
      <div>
        <el-button type="success" size="large" :loading="loading" @click="community(true)">复核通过</el-button>
        <el-button type="danger" size="large" :loading="loading" @click="community(false)">退回家属调整</el-button>
      </div>
    </div>

    <!-- 历史确认轮次 -->
    <el-collapse v-if="d.confirmations?.length" style="margin-top:14px">
      <el-collapse-item v-for="c in d.confirmations" :key="c.id"
                        :title="`第 ${c.roundNo} 轮确认 · 合计 ¥${c.totalCost} / 补贴 ¥${c.subsidyAmount} / 自付 ¥${c.selfPay}`">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="家属签字">{{ c.familySigner }}（{{ fmt(c.familyConfirmedAt) }}）</el-descriptions-item>
          <el-descriptions-item label="社区复核">
            <el-tag size="small" :type="c.communityApproved === true ? 'success' : c.communityApproved === false ? 'danger' : 'warning'">
              {{ c.communityApproved === true ? '通过' : c.communityApproved === false ? '退回调整' : '待复核' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="社区意见" :span="2">{{ c.communityRemark || '—' }}</el-descriptions-item>
        </el-descriptions>
      </el-collapse-item>
    </el-collapse>
  </el-card>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, EditPen } from '@element-plus/icons-vue'
import api from '../api'

const HIGH_RISK_CATEGORIES = ['扶手', '坐便', '紧急呼叫', '床边护栏']

const props = defineProps({ app: Object, role: String, d: Object, meta: Object })
const emit = defineEmits(['done'])

const loading = ref(false)
const signer = ref('')
const reviewRemark = ref('')
const showAdd = ref(false)
const capEdit = reactive({})
// 项目id -> 家属删除原因
const removalReasons = reactive({})

const addForm = reactive({
  category: '其他', name: '', unitPrice: 200, quantity: 1, reason: '', constructionImpact: ''
})

const items = computed(() => props.d.planItems || [])
const activeItems = computed(() => items.value.filter((i) => i.status !== 'REMOVED'))
const canFamilyEdit = computed(
  () => props.role === 'FAMILY' && props.app.status === 'PLAN_REVIEW'
)
const familyEditing = computed(
  () => props.role === 'COMMUNITY' && props.app.status === 'PLAN_FAMILY_CONFIRMED'
)

watch(items, (list) => {
  list.forEach((i) => {
    if (capEdit[i.id] === undefined) capEdit[i.id] = Number(i.subsidyCap)
  })
}, { immediate: true, deep: false })

function lineCost(row) {
  return (Number(row.unitPrice) * row.quantity).toFixed(2)
}
function capOf(row) {
  return (Number(row.subsidyCap) * row.quantity).toFixed(2)
}
function rowClass({ row }) {
  return row.status === 'REMOVED' ? 'removed-row' : ''
}
function fmt(t) {
  return t ? t.replace('T', ' ').substring(0, 16) : ''
}

const cost = computed(() => {
  let total = 0
  let subsidy = 0
  for (const i of activeItems.value) {
    const line = Number(i.unitPrice) * i.quantity
    total += line
    if (familyEditing.value && capEdit[i.id] !== undefined) {
      subsidy += Math.min(Number(capEdit[i.id]), line)
    } else {
      subsidy += Number(i.subsidyCap) * i.quantity
    }
  }
  subsidy = Math.min(subsidy, 5000, total)
  return {
    total: total.toFixed(2),
    subsidy: subsidy.toFixed(2),
    selfPay: (total - subsidy).toFixed(2)
  }
})

async function remove(row) {
  const isHigh = HIGH_RISK_CATEGORIES.includes(row.category)
  try {
    const { value } = await ElMessageBox.prompt(
      (isHigh
        ? `【${row.name}】是针对${riskBasis(row)}的高风险建议项。删除前请填写家属拒绝原因，评估师说明与家属原因将随风险评估提交街道备查。`
        : `删除【${row.name}】前请填写原因，该说明将随风险评估提交街道备查。`),
      isHigh ? '删除高风险改造项目' : '删除改造项目',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '保留该项目',
        inputType: 'textarea',
        inputPlaceholder: '如：费用原因/老人拒绝/认为不需要/暂不具备施工条件等',
        inputValidator: (v) => (v && v.trim() ? true : '必须填写拒绝原因'),
        type: 'warning'
      }
    )
    removalReasons[row.id] = value.trim()
    row.status = 'REMOVED'
    ElMessage.success('已标记删除，原因已记录，签字确认后生效')
  } catch {
    // 家属取消，保留项目
  }
}
function riskBasis(row) {
  if (row.category === '紧急呼叫') return '突发意外呼救'
  if (row.category === '坐便') return '如厕转移安全'
  if (row.category === '床边护栏') return '床边起身防跌'
  return '跌倒防护'
}
function undoRemove(row) {
  delete removalReasons[row.id]
  row.status = row.id < 1000000 ? 'PROPOSED' : 'ADDED'
}
function addItem() {
  if (!addForm.name || !addForm.reason) {
    ElMessage.warning('请填写新增项目名称和改造原因')
    return
  }
  items.value.push({
    id: -Date.now() - Math.floor(Math.random() * 1000),
    applicationId: props.app.id,
    category: addForm.category,
    name: addForm.name,
    reason: addForm.reason,
    spec: '家属新增，规格以现场确认为准',
    unit: '项',
    quantity: addForm.quantity,
    unitPrice: addForm.unitPrice,
    subsidyCap: 0,
    constructionImpact: addForm.constructionImpact || '待评估施工影响',
    status: 'ADDED',
    _new: true
  })
  ElMessage.success('已加入方案，待社区核定补贴')
  addForm.name = ''
  addForm.reason = ''
}

async function familyConfirm() {
  if (!activeItems.value.length) {
    ElMessage.warning('至少保留一个项目')
    return
  }
  await ElMessageBox.confirm(
    `确认提交方案：合计 ¥${cost.value.total}，拟补贴 ¥${cost.value.subsidy}，自付 ¥${cost.value.selfPay}。提交后由社区复核补贴。`,
    '家属方案确认', { confirmButtonText: '签字确认', cancelButtonText: '再看看' }
  )
  loading.value = true
  try {
    // 已持久化项目（id>0）被删除时提交拒绝原因；本页新增后又删除的（负id）直接丢弃
    const removals = items.value
      .filter((i) => i.status === 'REMOVED' && i.id > 0)
      .map((i) => ({ itemId: i.id, familyReason: removalReasons[i.id] || '' }))
    const addedItems = items.value.filter((i) => i._new && i.status !== 'REMOVED')
      .map(({ category, name, reason, spec, unit, quantity, unitPrice, constructionImpact }) => ({
        category, name, reason, spec, unit, quantity, unitPrice, constructionImpact
      }))
    await api.post(`/applications/${props.app.id}/plan/family-confirm`, {
      signer: signer.value || undefined,
      removals,
      addedItems
    })
    ElMessage.success('方案已确认并提交社区复核')
    emit('done')
  } finally {
    loading.value = false
  }
}

async function community(approved) {
  const itemCaps = {}
  for (const i of activeItems.value) {
    itemCaps[i.id] = capEdit[i.id] ?? Number(i.subsidyCap)
  }
  loading.value = true
  try {
    await api.post(`/applications/${props.app.id}/plan/community-review`, {
      approved,
      remark: reviewRemark.value || (approved ? '符合补贴规则' : '请按补贴目录调整'),
      itemCaps
    })
    ElMessage.success(approved ? '复核通过，可交施工队接单' : '已退回家属重新调整')
    emit('done')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.item-name {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  flex-wrap: wrap;
}
.spec {
  color: #98a29e;
  font-size: 12px;
  margin-top: 2px;
}
.reason {
  font-size: 13px;
  color: #44504b;
}
.removed {
  text-decoration: line-through;
  color: #98a29e;
}
.subsidy-zero {
  color: #c4561e;
}
.cost-bar {
  display: flex;
  gap: 28px;
  justify-content: flex-end;
  margin-top: 12px;
  font-size: 15px;
}
.action-row {
  margin-top: 16px;
  display: flex;
  gap: 14px;
  align-items: center;
  flex-wrap: wrap;
}
.action-row.community {
  border-top: 1px dashed #d8dedb;
  padding-top: 14px;
  flex-direction: column;
  align-items: stretch;
}
.add-form {
  display: flex;
  flex-wrap: wrap;
}
:deep(.removed-row) {
  background: #faf7f2 !important;
}
.rejections {
  margin-top: 16px;
  padding-top: 6px;
  border-top: 1px dashed #d8dedb;
}
.assessor-note {
  font-size: 13px;
  color: #44504b;
}
.family-reason {
  font-size: 13px;
  color: #c4561e;
}
</style>
