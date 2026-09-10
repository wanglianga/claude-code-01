<template>
  <div v-loading="loading">
    <el-page-header v-if="d.application" @back="$router.push('/applications')" class="back">
      <template #content>
        <span>工单 #{{ d.application.id }} · {{ d.application.elderName }}</span>
      </template>
    </el-page-header>

    <template v-if="d.application">
      <StatusSteps :app="d.application" :labels="meta.statusLabels" />

      <ApplicationInfo :app="d.application" />

      <VerifyPanel :app="d.application" :role="auth.role" @done="load" />

      <AssessmentPanel v-if="d.assessment || d.application.status === 'ASSIGNED'"
                       :app="d.application" :role="auth.role" :d="d" @done="load" />

      <PlanPanel v-if="d.planItems?.length"
                 :app="d.application" :role="auth.role" :d="d" :meta="meta" @done="load" />

      <ConstructionPanel :app="d.application" :role="auth.role" :d="d" :meta="meta"
                         @done="load" @want-complete="scrollComplete" />

      <div id="completion-anchor">
        <CompletionPanel :app="d.application" :role="auth.role" :d="d" @done="load" />
      </div>

      <SubsidyPanel :app="d.application" :role="auth.role" :d="d" @done="load" />

      <TimelineLog :logs="d.logs || []" :labels="meta.statusLabels" />
    </template>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import api from '../api'
import { useAuthStore } from '../store'
import StatusSteps from '../components/StatusSteps.vue'
import ApplicationInfo from '../components/ApplicationInfo.vue'
import VerifyPanel from '../components/VerifyPanel.vue'
import AssessmentPanel from '../components/AssessmentPanel.vue'
import PlanPanel from '../components/PlanPanel.vue'
import ConstructionPanel from '../components/ConstructionPanel.vue'
import CompletionPanel from '../components/CompletionPanel.vue'
import SubsidyPanel from '../components/SubsidyPanel.vue'
import TimelineLog from '../components/TimelineLog.vue'

const route = useRoute()
const auth = useAuthStore()
const d = ref({})
const meta = ref({ statusLabels: {}, changeReasons: {} })
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    // 路由参数变化（组件复用）时重置数据，避免短暂展示上一张工单
    d.value = {}
    d.value = await api.get(`/applications/${route.params.id}`)
  } finally {
    loading.value = false
  }
}

function scrollComplete() {
  setTimeout(() => {
    document.getElementById('completion-anchor')?.scrollIntoView({ behavior: 'smooth' })
  }, 300)
}

onMounted(async () => {
  meta.value = await api.get('/meta')
})

// 同组件复用时 :id 变化（如 #/applications/4 → #/applications/9），重新加载工单
watch(
  () => route.params.id,
  (newId, oldId) => {
    if (newId && newId !== oldId) {
      window.scrollTo({ top: 0 })
      load()
    }
  },
  { immediate: true }
)
</script>

<style scoped>
.back {
  margin-bottom: 12px;
}
</style>
