<template>
  <el-card shadow="never">
    <div class="section-title" style="margin-top:0">流程时间线</div>
    <el-timeline>
      <el-timeline-item v-for="l in logs" :key="l.id" :timestamp="fmt(l.createdAt)" placement="top">
        <el-card shadow="never" class="log-card">
          <div class="log-head">
            <b>{{ l.action }}</b>
            <el-tag v-if="l.toStatus" size="small" effect="plain" type="success">
              → {{ labels[l.toStatus] || l.toStatus }}
            </el-tag>
            <span class="operator">{{ l.operatorName || '系统' }}</span>
          </div>
          <div v-if="l.note" class="log-note">{{ l.note }}</div>
        </el-card>
      </el-timeline-item>
    </el-timeline>
  </el-card>
</template>

<script setup>
defineProps({
  logs: { type: Array, default: () => [] },
  labels: { type: Object, default: () => ({}) }
})

function fmt(t) {
  return t ? t.replace('T', ' ').substring(0, 16) : ''
}
</script>

<style scoped>
.log-card {
  background: #fafcfb;
}
.log-head {
  display: flex;
  align-items: center;
  gap: 8px;
}
.operator {
  margin-left: auto;
  color: #7a8580;
  font-size: 12px;
}
.log-note {
  margin-top: 4px;
  font-size: 13px;
  color: #5a6661;
  line-height: 1.6;
}
</style>
