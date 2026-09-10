<template>
  <el-card v-if="show" shadow="never" style="margin-bottom:14px">
    <div class="section-title" style="margin-top:0">竣工验收材料</div>

    <!-- 施工队填报 -->
    <el-form v-if="role === 'TEAM' && app.status === 'IN_CONSTRUCTION'" :model="f" label-width="140px">
      <el-form-item label="改造前照片" required>
        <el-input v-model="f.beforePhotos" type="textarea" :rows="2"
                  placeholder="照片档案编号/说明（演示环境以文字代替上传），如：卫生间旧照2张 WP-B1/B2" />
      </el-form-item>
      <el-form-item label="改造后照片" required>
        <el-input v-model="f.afterPhotos" type="textarea" :rows="2"
                  placeholder="如：扶手完工照、夜灯夜间实拍等3张 WP-A1/A2/A3" />
      </el-form-item>
      <el-form-item label="老人试用记录" required>
        <el-input v-model="f.trialRecord" type="textarea" :rows="2"
                  placeholder="现场试用各设施的动作与反应" />
      </el-form-item>
      <el-form-item label="家属签字" required>
        <el-input v-model="f.familySigner" placeholder="确认验收的家属姓名" style="max-width:320px" />
      </el-form-item>
      <el-form-item label="施工材料明细" required>
        <el-input v-model="f.materialsDetail" type="textarea" :rows="2" />
      </el-form-item>
      <el-form-item label="费用明细" required>
        <el-input v-model="f.costDetail" type="textarea" :rows="2" placeholder="产品费 + 人工费说明" />
      </el-form-item>
      <el-form-item label="竣工总费用(元)" required>
        <el-input-number v-model="f.totalCost" :min="0" :step="100" style="width:200px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" size="large" :loading="loading" @click="submit">提交竣工验收（进入街道补贴审核）</el-button>
      </el-form-item>
    </el-form>

    <!-- 只读展示 -->
    <template v-else-if="d.completion">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="家属签字">{{ d.completion.familySigner }}（{{ fmt(d.completion.signedAt) }}）</el-descriptions-item>
        <el-descriptions-item label="竣工总费用">
          <span class="cost-money">¥{{ d.completion.totalCost }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="改造前照片" :span="2">{{ d.completion.beforePhotos }}</el-descriptions-item>
        <el-descriptions-item label="改造后照片" :span="2">{{ d.completion.afterPhotos }}</el-descriptions-item>
        <el-descriptions-item label="老人试用记录" :span="2">{{ d.completion.trialRecord }}</el-descriptions-item>
        <el-descriptions-item label="施工材料明细" :span="2">{{ d.completion.materialsDetail }}</el-descriptions-item>
        <el-descriptions-item label="费用明细" :span="2">{{ d.completion.costDetail }}</el-descriptions-item>
      </el-descriptions>
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

const f = reactive({
  beforePhotos: '',
  afterPhotos: '',
  trialRecord: '',
  familySigner: '',
  materialsDetail: '',
  costDetail: '',
  totalCost: 3000
})

const show = computed(() =>
  ['IN_CONSTRUCTION', 'COMPLETED', 'SETTLED', 'VISITED'].includes(props.app.status))

function fmt(t) {
  return t ? t.replace('T', ' ').substring(0, 16) : ''
}

async function submit() {
  for (const k of ['beforePhotos', 'afterPhotos', 'trialRecord', 'familySigner', 'materialsDetail', 'costDetail']) {
    if (!f[k]) {
      ElMessage.warning('请完整填写竣工验收材料')
      return
    }
  }
  loading.value = true
  try {
    await api.post(`/applications/${props.app.id}/complete`, f)
    ElMessage.success('竣工材料已提交，进入街道补贴审核')
    emit('done')
  } finally {
    loading.value = false
  }
}
</script>
