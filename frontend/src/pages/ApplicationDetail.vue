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
import { onMounted, ref } from 'vue'
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
  await load()
})
</script>

<style scoped>
.back {
  margin-bottom: 12px;
}
</style>
