<template>
  <el-card shadow="never" style="margin-bottom:14px">
    <div class="section-title" style="margin-top:0">申请信息（老人/家属填报）</div>
    <el-descriptions :column="3" border size="default">
      <el-descriptions-item label="申请人">{{ app.applicantName }}</el-descriptions-item>
      <el-descriptions-item label="老人姓名">{{ app.elderName }}</el-descriptions-item>
      <el-descriptions-item label="年龄">{{ app.elderAge }}</el-descriptions-item>
      <el-descriptions-item label="居住楼层">{{ app.floor }} 层（{{ app.hasElevator ? '有电梯' : '无电梯' }}）</el-descriptions-item>
      <el-descriptions-item label="是否独居">{{ app.livingAlone ? '是' : '否' }}</el-descriptions-item>
      <el-descriptions-item label="行动能力">
        <el-tag size="small" effect="plain">{{ app.mobility }}</el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="房屋权属">{{ app.houseOwnership }}</el-descriptions-item>
      <el-descriptions-item label="家属电话">{{ app.phone }}</el-descriptions-item>
      <el-descriptions-item label="所属社区">{{ app.community }}</el-descriptions-item>
      <el-descriptions-item label="居住地址" :span="3">{{ app.address }}</el-descriptions-item>
      <el-descriptions-item label="跌倒史" :span="3">{{ app.fallHistory || '—' }}</el-descriptions-item>
      <el-descriptions-item label="卫生间现状" :span="3">{{ app.bathroomStatus || '—' }}</el-descriptions-item>
      <el-descriptions-item label="卧室现状" :span="3">{{ app.bedroomStatus || '—' }}</el-descriptions-item>
      <el-descriptions-item label="期望改造项目" :span="3">{{ app.expectedItems || '—' }}</el-descriptions-item>
    </el-descriptions>
  </el-card>
</template>

<script setup>
defineProps({ app: { type: Object, required: true } })
</script>
