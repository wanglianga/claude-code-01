<template>
  <div>
    <h2 class="page-title">提交适老化改造申请</h2>
    <el-card>
      <el-alert type="info" :closable="false" style="margin-bottom:16px"
                title="请如实填写老人居住与身体现状，信息将作为社区补贴核验和入户评估的基础材料。" />
      <el-form :model="form" label-width="130px" style="max-width:900px">
        <div class="section-title">基本信息</div>
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="申请人" required>
              <el-input v-model="form.applicantName" placeholder="老人本人或家属姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="老人姓名" required>
              <el-input v-model="form.elderName" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="老人年龄">
              <el-input-number v-model="form.elderAge" :min="50" :max="120" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="家属联系电话" required>
              <el-input v-model="form.phone" placeholder="评估与施工沟通使用" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属社区">
              <el-input v-model="form.community" placeholder="如：幸福里社区" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="居住地址" required>
          <el-input v-model="form.address" placeholder="小区、楼栋、单元、门牌" />
        </el-form-item>

        <div class="section-title">居住与身体现状</div>
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="居住楼层">
              <el-input-number v-model="form.floor" :min="1" :max="40" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="是否有电梯">
              <el-radio-group v-model="form.hasElevator">
                <el-radio :value="false">无</el-radio>
                <el-radio :value="true">有</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="是否独居">
              <el-radio-group v-model="form.livingAlone">
                <el-radio :value="false">否</el-radio>
                <el-radio :value="true">是</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="行动能力">
              <el-select v-model="form.mobility" placeholder="请选择" style="width:100%">
                <el-option v-for="m in meta.mobility" :key="m" :label="m" :value="m" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="16">
            <el-form-item label="房屋权属">
              <el-radio-group v-model="form.houseOwnership">
                <el-radio v-for="o in meta.ownership" :key="o" :value="o">{{ o }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="跌倒史">
          <el-input v-model="form.fallHistory" type="textarea" :rows="2"
                    placeholder="如：近半年在卫生间滑倒1次；无跌倒史" />
        </el-form-item>
        <el-form-item label="卫生间现状">
          <el-input v-model="form.bathroomStatus" type="textarea" :rows="2"
                    placeholder="如：蹲便、光面瓷砖、无扶手、洗澡时地面湿滑" />
        </el-form-item>
        <el-form-item label="卧室现状">
          <el-input v-model="form.bedroomStatus" type="textarea" :rows="2"
                    placeholder="如：床偏矮起身困难、夜间到卫生间通道无照明" />
        </el-form-item>

        <div class="section-title">改造诉求</div>
        <el-form-item label="期望改造项目">
          <el-checkbox-group v-model="expectList">
            <el-checkbox v-for="c in ['卫生间扶手','地面防滑','坐便改造','床边护栏','感应夜灯','紧急呼叫按钮','门槛坡道']"
                         :key="c" :value="c" :label="c" />
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="其他说明">
          <el-input v-model="form.expectedItems" type="textarea" :rows="2"
                    placeholder="补充期望与特殊情况，如老人午休时间、照护人情况" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" size="large" :loading="loading" @click="submit">提交申请</el-button>
          <el-button size="large" @click="$router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../api'

const router = useRouter()
const loading = ref(false)
const meta = ref({ mobility: [], ownership: [] })
const expectList = ref([])

const form = reactive({
  applicantName: '',
  elderName: '',
  elderAge: 80,
  phone: '',
  community: '',
  address: '',
  floor: 1,
  hasElevator: false,
  livingAlone: false,
  mobility: '独立',
  houseOwnership: '自有产权',
  fallHistory: '',
  bathroomStatus: '',
  bedroomStatus: '',
  expectedItems: ''
})

async function submit() {
  if (!form.applicantName || !form.elderName || !form.phone || !form.address) {
    ElMessage.warning('请填写申请人、老人姓名、联系电话和地址')
    return
  }
  const payload = { ...form, expectedItems: [...expectList.value, form.expectedItems].filter(Boolean).join('；') }
  loading.value = true
  try {
    const res = await api.post('/applications', payload)
    ElMessage.success(`申请已提交，工单号 #${res.id}，等待社区核验`)
    router.push(`/applications/${res.id}`)
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  meta.value = await api.get('/meta')
})
</script>
