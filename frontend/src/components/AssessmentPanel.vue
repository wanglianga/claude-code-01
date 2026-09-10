<template>
  <el-card shadow="never" style="margin-bottom:14px">
    <!-- 评估师入户采集 -->
    <template v-if="app.status === 'ASSIGNED' && role === 'ASSESSOR'">
      <div class="section-title" style="margin-top:0">入户评估采集（评估结果将直接生成改造方案）</div>
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
            <el-form-item label="跌倒风险等级">
              <el-radio-group v-model="f.fallRiskLevel">
                <el-radio value="高">高</el-radio>
                <el-radio value="中">中</el-radio>
                <el-radio value="低">低</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="床边起身困难点">
          <el-input v-model="f.bedTransferDifficulty" type="textarea" :rows="2"
                    placeholder="如：床高偏低、上肢力量弱、独立起身需20秒以上" />
        </el-form-item>
        <el-form-item label="老人试行动作">
          <el-input v-model="f.trialActions" type="textarea" :rows="2"
                    placeholder="如：坐位→站立需扶持、行走5米需停顿、试蹲无法自主起立" />
        </el-form-item>
        <el-form-item label="评估小结">
          <el-input v-model="f.summary" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="submit">提交评估并生成改造方案</el-button>
        </el-form-item>
      </el-form>
    </template>

    <!-- 评估结果只读展示 -->
    <template v-else-if="d.assessment">
      <div class="section-title" style="margin-top:0">入户评估结果（评估师 {{ assessorName }}）</div>
      <el-descriptions :column="3" border>
        <el-descriptions-item label="门槛高度">{{ d.assessment.thresholdHeight }} cm</el-descriptions-item>
        <el-descriptions-item label="卫生间尺寸">{{ d.assessment.bathroomWidth }} × {{ d.assessment.bathroomDepth }} cm</el-descriptions-item>
        <el-descriptions-item label="墙体材质">{{ d.assessment.wallMaterial }}</el-descriptions-item>
        <el-descriptions-item label="夜间照明">
          <el-tag size="small" :type="d.assessment.nightLighting === '昏暗' ? 'danger' : 'info'">
            {{ d.assessment.nightLighting }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="跌倒风险">
          <el-tag size="small" :type="riskType(d.assessment.fallRiskLevel)">{{ d.assessment.fallRiskLevel }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="评估时间">{{ fmt(d.assessment.assessedAt) }}</el-descriptions-item>
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

const props = defineProps({ app: Object, role: String, d: Object })
const emit = defineEmits(['done'])

const meta = ref({ wallMaterial: [], nightLighting: [] })
const loading = ref(false)
const assessorName = ref('')

const f = reactive({
  thresholdHeight: 2.5,
  bathroomWidth: 150,
  bathroomDepth: 180,
  wallMaterial: '实心砖墙',
  nightLighting: '一般',
  fallRiskLevel: '中',
  bedTransferDifficulty: '',
  trialActions: '',
  summary: ''
})

function riskType(r) {
  return r === '高' ? 'danger' : r === '中' ? 'warning' : 'success'
}
function fmt(t) {
  return t ? t.replace('T', ' ').substring(0, 16) : '—'
}

async function submit() {
  loading.value = true
  try {
    await api.post(`/applications/${props.app.id}/assessment`, f)
    ElMessage.success('评估已提交，系统已据此生成改造方案')
    emit('done')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  meta.value = await api.get('/meta')
})
</script>
