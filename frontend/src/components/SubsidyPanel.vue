<template>
  <el-card v-if="show" shadow="never" style="margin-bottom:14px">
    <div class="section-title" style="margin-top:0">街道补贴审核 → 回写施工队结算</div>

    <!-- 风险判断如何影响最终方案（街道必看） -->
    <el-descriptions v-if="d.assessment" :column="3" border class="risk-box" size="small">
      <el-descriptions-item label="评估风险等级">
        <el-tag :type="riskType" size="small" effect="dark">
          {{ d.assessment.fallRiskLevel }}风险 · {{ d.assessment.riskScore }}/15
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="行动/湿滑/起身">
        {{ d.assessment.mobilityScore }}+{{ d.assessment.wetnessScore }}+{{ d.assessment.bedDifficultyScore }}
      </el-descriptions-item>
      <el-descriptions-item label="照明/呼叫">
        {{ d.assessment.lightingScore }}+{{ d.assessment.emergencyScore }}
      </el-descriptions-item>
      <el-descriptions-item label="主要风险因子" :span="3">{{ riskFactors }}</el-descriptions-item>
      <el-descriptions-item v-if="d.schedule?.priority === 1" label="高风险排期/照护" :span="3">
        已优先排期；{{ d.schedule.careRequired === 'TEMP_CARE' ? '临时照护' : '家属陪同' }}：{{ d.schedule.careArrangement }}
      </el-descriptions-item>
    </el-descriptions>

    <div v-if="d.rejections?.length" class="reject-box">
      <el-alert type="warning" :closable="false" show-icon
                title="以下高风险相关改造项目被家属拒绝，请结合评估师说明与家属原因审慎核定补贴与责任"
                style="margin-bottom:8px" />
      <el-table :data="d.rejections" border size="small">
        <el-table-column label="拒绝项目" min-width="170">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.category }}</el-tag>
            <b style="margin-left:6px">{{ row.itemName }}</b>
            <el-tag v-if="row.riskLevel === '高'" size="small" type="danger" style="margin-left:6px">高风险时建议</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="评估师说明（风险依据）" min-width="230">
          <template #default="{ row }">{{ row.assessorNote }}</template>
        </el-table-column>
        <el-table-column label="家属拒绝原因" min-width="200">
          <template #default="{ row }"><span style="color:#c4561e">{{ row.familyReason }}</span></template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 街道审核操作 -->
    <el-form v-if="role === 'STREET' && app.status === 'COMPLETED'" :model="f" label-width="140px">
      <el-alert type="info" :closable="false" style="margin-bottom:12px"
                title="请核对前后对比照片、老人试用记录、家属签字、材料与费用明细，给出补贴结论。结论将自动生成施工队结算单。" />
      <el-form-item label="审核结论">
        <el-radio-group v-model="f.conclusion">
          <el-radio value="APPROVED">准予补贴</el-radio>
          <el-radio value="REJECTED">不予补贴</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="f.conclusion === 'APPROVED'" label="核准补贴金额(元)">
        <el-input-number v-model="f.approvedSubsidy" :min="0" :max="d.completion?.totalCost || 0"
                         :step="100" style="width:200px" />
        <span class="hint">竣工总费用 ¥{{ d.completion?.totalCost }}，补贴不超过总额及方案封顶</span>
      </el-form-item>
      <el-form-item label="审核意见">
        <el-input v-model="f.remark" type="textarea" :rows="2"
                  placeholder="材料完整性、费用合理性、签字与照片核对情况" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" size="large" :loading="loading" @click="submit">出具审核结论并回写结算</el-button>
      </el-form-item>
    </el-form>

    <!-- 审核结论与结算 -->
    <template v-if="d.subsidyReview">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="审核结论">
          <el-tag :type="d.subsidyReview.conclusion === 'APPROVED' ? 'success' : 'danger'">
            {{ d.subsidyReview.conclusion === 'APPROVED' ? '准予补贴' : '不予补贴' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="审核时间">{{ fmt(d.subsidyReview.reviewedAt) }}</el-descriptions-item>
        <el-descriptions-item label="核准补贴">
          <b style="color:#2f6f5e">¥{{ d.subsidyReview.approvedSubsidy }}</b>
        </el-descriptions-item>
        <el-descriptions-item label="家属自付">
          <b class="cost-money">¥{{ d.subsidyReview.selfPay }}</b>
        </el-descriptions-item>
        <el-descriptions-item label="审核意见" :span="2">{{ d.subsidyReview.remark }}</el-descriptions-item>
      </el-descriptions>

      <div v-if="d.settlement" class="settle">
        <el-tag type="success" size="large">施工队结算单（{{ d.settlement.status === 'SETTLED' ? '已回写' : '待回写' }}）</el-tag>
        <div class="settle-grid">
          <div><span>工程总额</span><b>¥{{ d.settlement.totalAmount }}</b></div>
          <div><span>街道补贴拨付</span><b>¥{{ d.settlement.subsidyAmount }}</b></div>
          <div><span>家属支付</span><b>¥{{ d.settlement.familyPayAmount }}</b></div>
          <div><span>应付施工队</span><b class="cost-money">¥{{ d.settlement.teamPayAmount }}</b></div>
        </div>
      </div>
    </template>

    <!-- 质保回访（施工队，结算后） -->
    <template v-if="role === 'TEAM' && ['SETTLED', 'VISITED'].includes(app.status)">
      <div class="section-title">质保回访登记</div>
      <el-form :model="w" inline>
        <el-form-item label="回访结论">
          <el-radio-group v-model="w.result">
            <el-radio value="SATISFIED">老人满意</el-radio>
            <el-radio value="ISSUES">存在问题</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="回访内容">
          <el-input v-model="w.content" style="width:340px" placeholder="设施使用情况、问题与处理" />
        </el-form-item>
        <el-form-item label="下次回访">
          <el-date-picker v-model="w.nextVisitDate" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loadingW" @click="submitVisit">登记回访</el-button>
        </el-form-item>
      </el-form>
      <el-timeline v-if="d.warrantyVisits?.length" style="margin-top:6px">
        <el-timeline-item v-for="v in d.warrantyVisits" :key="v.id" :timestamp="fmt(v.visitTime)"
                          :type="v.result === 'SATISFIED' ? 'success' : 'warning'">
          <el-tag size="small" :type="v.result === 'SATISFIED' ? 'success' : 'warning'">
            {{ v.result === 'SATISFIED' ? '老人满意' : '存在问题' }}
          </el-tag>
          {{ v.content }}
          <span v-if="v.nextVisitDate" style="color:#98a29e">（计划下次：{{ v.nextVisitDate }}）</span>
        </el-timeline-item>
      </el-timeline>
    </template>
  </el-card>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'

const props = defineProps({ app: Object, role: String, d: Object })
const emit = defineEmits(['done'])
const loading = ref(false)
const loadingW = ref(false)

const riskType = computed(() => {
  const l = props.d.assessment?.fallRiskLevel
  return l === '高' ? 'danger' : l === '中' ? 'warning' : 'success'
})
const riskFactors = computed(() => (props.d.assessment?.riskFactors || '—').replaceAll('；', '；'))

const f = reactive({
  conclusion: 'APPROVED',
  approvedSubsidy: props.d.completion?.totalCost ? Number(props.d.completion.totalCost) : 3000,
  remark: '材料齐备、前后对比清晰、家属签字完整，费用合理'
})
const w = reactive({ result: 'SATISFIED', content: '', nextVisitDate: '' })

const show = computed(() => ['COMPLETED', 'SETTLED', 'VISITED'].includes(props.app.status))

function fmt(t) {
  return t ? t.replace('T', ' ').substring(0, 16) : ''
}

async function submit() {
  loading.value = true
  try {
    await api.post(`/applications/${props.app.id}/subsidy-review`, f)
    ElMessage.success('审核结论已出具，施工队结算单已生成')
    emit('done')
  } finally {
    loading.value = false
  }
}

async function submitVisit() {
  if (!w.content) {
    ElMessage.warning('请填写回访内容')
    return
  }
  loadingW.value = true
  try {
    await api.post(`/applications/${props.app.id}/warranty`, w)
    ElMessage.success('质保回访已登记')
    emit('done')
  } finally {
    loadingW.value = false
  }
}
</script>

<style scoped>
.hint {
  margin-left: 12px;
  color: #98a29e;
  font-size: 13px;
}
.risk-box {
  margin-bottom: 12px;
}
.reject-box {
  margin-bottom: 14px;
}
.settle {
  margin-top: 14px;
  padding: 14px 16px;
  background: #f3f8f6;
  border-radius: 8px;
}
.settle-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
  margin-top: 10px;
}
.settle-grid div {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.settle-grid span {
  color: #7a8580;
  font-size: 13px;
}
</style>
