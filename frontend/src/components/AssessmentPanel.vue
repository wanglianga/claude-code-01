<template>
  <el-card shadow="never" style="margin-bottom:14px">
    <!-- 评估师入户采集 -->
    <template v-if="app.status === 'ASSIGNED' && role === 'ASSESSOR'">
      <div class="section-title" style="margin-top:0">入户评估采集（五维度将由平台自动生成风险等级）</div>
      <el-form :model="f" label-width="150px">
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="门槛高度(cm)">
              <el-input-number v-model="f.thresholdHeight" :min="0" :max="30" :step="0.5"
                               :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="卫生间宽(cm)">
              <el-input-number v-model="f.bathroomWidth" :min="50" :max="500" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="卫生间深(cm)">
              <el-input-number v-model="f.bathroomDepth" :min="50" :max="500" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="墙体材质">
              <el-select v-model="f.wallMaterial" style="width:100%">
                <el-option v-for="w in meta.wallMaterial" :key="w" :label="w" :value="w" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="夜间照明">
              <el-select v-model="f.nightLighting" style="width:100%">
                <el-option v-for="w in meta.nightLighting" :key="w" :label="w" :value="w" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="卫生间湿滑程度">
              <el-radio-group v-model="f.wetness">
                <el-radio v-for="w in meta.wetness" :key="w" :value="w">{{ w }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>

        <div class="sub-title">风险分级五维度现场记录</div>
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="现场行动能力">
              <el-select v-model="f.mobilityObserved" style="width:100%">
                <el-option v-for="m in meta.mobilityObserved" :key="m" :label="m" :value="m" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="16">
            <el-form-item label="床边起身难度">
              <el-radio-group v-model="f.bedDifficultyScore">
                <el-radio :value="0">0 自如</el-radio>
                <el-radio :value="1">1 略困难</el-radio>
                <el-radio :value="2">2 明显困难</el-radio>
                <el-radio :value="3">3 无法独立起身</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="紧急呼叫条件">
          <el-input v-model="f.emergencyCondition"
                    placeholder="同住人情况、是否有手机并随身、与邻居距离、有无呼叫器，如：独居、无手机、邻居距离远" />
        </el-form-item>
        <el-form-item label="床边起身困难点">
          <el-input v-model="f.bedTransferDifficulty" type="textarea" :rows="2"
                    placeholder="如：床高偏低、上肢力量弱、独立起身需20秒以上" />
        </el-form-item>
        <el-form-item label="老人试行动作">
          <el-input v-model="f.trialActions" type="textarea" :rows="2"
                    placeholder="如：坐位→站立需扶持、行走5米需停顿、试蹲无法自主起立" />
        </el-form-item>
        <el-form-item label="评估小结">
          <el-input v-model="f.summary" type="textarea" :rows="2"
                    placeholder="风险等级将由平台依据上述五维度自动判定，无需手填" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="submit">提交评估，平台生成风险等级与改造方案</el-button>
        </el-form-item>
      </el-form>
    </template>

    <!-- 评估结果只读展示 -->
    <template v-else-if="d.assessment">
      <RiskCard :assessment="d.assessment" />
      <div class="section-title">现场采集明细</div>
      <el-descriptions :column="3" border>
        <el-descriptions-item label="门槛高度">{{ d.assessment.thresholdHeight }} cm</el-descriptions-item>
        <el-descriptions-item label="卫生间尺寸">{{ d.assessment.bathroomWidth }} × {{ d.assessment.bathroomDepth }} cm</el-descriptions-item>
        <el-descriptions-item label="墙体材质">{{ d.assessment.wallMaterial }}</el-descriptions-item>
        <el-descriptions-item label="夜间照明">
          <el-tag size="small" :type="d.assessment.nightLighting === '昏暗' ? 'danger' : 'info'">
            {{ d.assessment.nightLighting }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="湿滑程度">
          <el-tag size="small" :type="wetType(d.assessment.wetness)">{{ d.assessment.wetness || '—' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="评估时间">{{ fmt(d.assessment.assessedAt) }}</el-descriptions-item>
        <el-descriptions-item label="现场行动能力">{{ d.assessment.mobilityObserved || '—' }}</el-descriptions-item>
        <el-descriptions-item label="床边起身难度">{{ bedText(d.assessment.bedDifficultyScore) }}</el-descriptions-item>
        <el-descriptions-item label="紧急呼叫条件">{{ d.assessment.emergencyCondition || '—' }}</el-descriptions-item>
        <el-descriptions-item label="床边起身困难点" :span="3">{{ d.assessment.bedTransferDifficulty }}</el-descriptions-item>
        <el-descriptions-item label="老人试行动作" :span="3">{{ d.assessment.trialActions }}</el-descriptions-item>
        <el-descriptions-item label="评估小结" :span="3">{{ d.assessment.summary }}</el-descriptions-item>
      </el-descriptions>
    </template>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'
import RiskCard from './RiskCard.vue'

const props = defineProps({ app: Object, role: String, d: Object })
const emit = defineEmits(['done'])

const meta = ref({ wallMaterial: [], nightLighting: [], wetness: [], mobilityObserved: [] })
const loading = ref(false)

const f = reactive({
  thresholdHeight: 2.5,
  bathroomWidth: 150,
  bathroomDepth: 180,
  wallMaterial: '实心砖墙',
  nightLighting: '一般',
  wetness: '一般',
  mobilityObserved: '拄拐',
  bedDifficultyScore: 1,
  emergencyCondition: '',
  bedTransferDifficulty: '',
  trialActions: '',
  summary: ''
})

function wetType(w) {
  return w === '积水' ? 'danger' : w === '较湿' ? 'warning' : 'info'
}
function bedText(s) {
  return ['自如（0分）', '略困难（1分）', '明显困难（2分）', '无法独立起身（3分）'][s] || '—'
}
function fmt(t) {
  return t ? t.replace('T', ' ').substring(0, 16) : '—'
}

async function submit() {
  loading.value = true
  try {
    await api.post(`/applications/${props.app.id}/assessment`, f)
    ElMessage.success('评估已提交，平台已生成风险等级并据此生成改造方案')
    emit('done')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  meta.value = await api.get('/meta')
})
</script>

<style scoped>
.sub-title {
  font-weight: 600;
  color: #1f3d35;
  margin: 4px 0 12px;
  padding-left: 10px;
  border-left: 3px solid #79b49f;
}
</style>
