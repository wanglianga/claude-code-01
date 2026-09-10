<template>
  <el-card v-if="show" shadow="never" style="margin-bottom:14px">
    <!-- 核验补贴资格与房屋信息 -->
    <template v-if="app.status === 'SUBMITTED'">
      <div class="section-title" style="margin-top:0">社区核验：补贴资格 + 房屋信息</div>
      <el-form label-width="110px">
        <el-form-item label="核验结论">
          <el-radio-group v-model="eligible">
            <el-radio :value="true">核验通过，符合高龄适老化改造补贴对象</el-radio>
            <el-radio :value="false">不通过</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="核验备注">
          <el-input v-model="remark" type="textarea" :rows="2"
                    placeholder="户籍/房屋权属核对、补贴档次说明；不通过需写明原因" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="submit">提交核验结论</el-button>
        </el-form-item>
      </el-form>
    </template>

    <!-- 派单给评估师 -->
    <template v-else-if="app.status === 'VERIFIED'">
      <div class="section-title" style="margin-top:0">派单评估师</div>
      <el-form label-width="110px">
        <el-form-item label="选择评估师">
          <el-select v-model="assessorId" placeholder="请选择" style="width:360px">
            <el-option v-for="a in assessors" :key="a.id"
                       :label="`${a.realName}（${a.organization} ${a.phone || ''}）`" :value="a.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="assign">派单</el-button>
        </el-form-item>
      </el-form>
    </template>
  </el-card>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'

const props = defineProps({ app: Object, role: String })
const emit = defineEmits(['done'])

const show = computed(() =>
  props.role === 'COMMUNITY' && (props.app.status === 'SUBMITTED' || props.app.status === 'VERIFIED'))

const eligible = ref(true)
const remark = ref('户籍与房屋信息一致，符合补贴条件')
const assessorId = ref(null)
const assessors = ref([])
const loading = ref(false)

async function submit() {
  loading.value = true
  try {
    await api.post(`/applications/${props.app.id}/verify`, { eligible: eligible.value, remark: remark.value })
    ElMessage.success('核验完成')
    emit('done')
  } finally {
    loading.value = false
  }
}

async function assign() {
  if (!assessorId.value) {
    ElMessage.warning('请选择评估师')
    return
  }
  loading.value = true
  try {
    await api.post(`/applications/${props.app.id}/assign`, { assessorId: assessorId.value })
    ElMessage.success('派单成功')
    emit('done')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  if (show.value) {
    assessors.value = await api.get('/users', { params: { role: 'ASSESSOR' } })
  }
})
</script>
