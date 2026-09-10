<template>
  <el-card v-if="a" shadow="never" class="risk-card" :class="'risk-' + levelKey">
    <div class="head">
      <div class="section-title" style="margin:0">入户评估风险分级（平台五维度评分）</div>
      <div class="level-badge">
        <span class="score">{{ a.riskScore ?? '—' }}<i>/15</i></span>
        <el-tag :type="levelType" size="large" effect="dark">{{ a.fallRiskLevel }}风险</el-tag>
      </div>
    </div>

    <div class="dimensions">
      <div v-for="dim in dimensions" :key="dim.key" class="dim">
        <div class="dim-head">
          <span>{{ dim.label }}</span>
          <span class="dim-score">{{ dim.value }}<i>/{{ dim.max }}</i></span>
        </div>
        <el-progress :percentage="dim.percent" :stroke-width="10"
                     :color="dimColor(dim.percent)" :show-text="false" />
        <div class="dim-note">{{ dim.note }}</div>
      </div>
    </div>

    <div v-if="factors.length" class="factors">
      <div class="factor-title">主要风险因子：</div>
      <el-tag v-for="(f, i) in factors" :key="i" class="factor-tag"
              :type="levelKey === '高' ? 'danger' : levelKey === '中' ? 'warning' : 'info'">
        {{ f }}
      </el-tag>
    </div>

    <el-alert v-if="a.careRecommendation"
              :type="levelKey === '高' ? 'error' : 'warning'"
              :closable="false" show-icon style="margin-top:12px"
              :title="levelKey === '高' ? '高风险：优先排期' : '施工安全提示'"
              :description="a.careRecommendation" />
  </el-card>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({ assessment: Object })
const a = computed(() => props.assessment)

const levelKey = computed(() => a.value?.fallRiskLevel || '低')
const levelType = computed(() => levelKey.value === '高' ? 'danger' : levelKey.value === '中' ? 'warning' : 'success')

const mobilityNotes = { 0: '可独立行动', 1: '拄拐慢行', 2: '需搀扶', 3: '轮椅转移困难', 4: '卧床、完全依赖照护' }
const wetnessNotes = { 0: '地面干燥', 1: '洗漱后偏湿', 2: '较湿、无防滑', 3: '积水、打滑严重' }
const bedNotes = { 0: '起身自如', 1: '略有困难', 2: '明显困难', 3: '无法独立起身' }
const lightNotes = { 0: '照明充足', 1: '局部看不清', 2: '动线昏暗无灯' }
const emergencyNotes = { 0: '呼叫有保障', 1: '有手机但不随身/操作难', 2: '仅能呼救、独居响应无保障', 3: '缺乏任何有效呼救手段' }

const dimensions = computed(() => {
  const x = a.value || {}
  const dims = [
    { key: 'mobility', label: '行动能力', value: n(x.mobilityScore), max: 4, note: mobilityNotes[n(x.mobilityScore)] || (x.mobilityObserved || '—') },
    { key: 'wetness', label: '卫生间湿滑', value: n(x.wetnessScore), max: 3, note: wetnessNotes[n(x.wetnessScore)] || x.wetness || '—' },
    { key: 'bed', label: '床边起身难度', value: n(x.bedDifficultyScore), max: 3, note: bedNotes[n(x.bedDifficultyScore)] || '—' },
    { key: 'lighting', label: '夜间照明', value: n(x.lightingScore), max: 2, note: lightNotes[n(x.lightingScore)] || x.nightLighting || '—' },
    { key: 'emergency', label: '紧急呼叫条件', value: n(x.emergencyScore), max: 3, note: emergencyNotes[n(x.emergencyScore)] || '—' }
  ]
  dims.forEach((d) => { d.percent = Math.round((d.value / d.max) * 100) })
  return dims
})

const factors = computed(() => {
  const raw = a.value?.riskFactors
  if (!raw) return []
  return raw.split('；').map((s) => s.trim()).filter(Boolean)
})

function n(v) {
  return v == null ? 0 : Number(v)
}
function dimColor(pct) {
  if (pct >= 75) return '#c4561e'
  if (pct >= 50) return '#e6a23c'
  return '#2f6f5e'
}
</script>

<style scoped>
.risk-card {
  margin-bottom: 14px;
  border-left-width: 4px;
}
.risk-高 { border-left-color: #c4561e; }
.risk-中 { border-left-color: #e6a23c; }
.risk-低 { border-left-color: #2f6f5e; }
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}
.level-badge {
  display: flex;
  align-items: center;
  gap: 10px;
}
.score {
  font-size: 26px;
  font-weight: 700;
  color: #2f6f5e;
}
.score i {
  font-style: normal;
  font-size: 13px;
  color: #98a29e;
}
.risk-高 .score { color: #c4561e; }
.dimensions {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 14px;
}
.dim-head {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  margin-bottom: 4px;
}
.dim-score {
  font-weight: 600;
}
.dim-score i {
  font-style: normal;
  color: #98a29e;
  font-size: 11px;
}
.dim-note {
  font-size: 12px;
  color: #7a8580;
  margin-top: 4px;
  min-height: 32px;
  line-height: 1.4;
}
.factors {
  margin-top: 12px;
}
.factor-title {
  font-size: 13px;
  color: #5a6661;
  margin-bottom: 6px;
}
.factor-tag {
  margin: 0 8px 8px 0;
}
</style>
