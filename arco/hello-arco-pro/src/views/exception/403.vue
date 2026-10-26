<template>
  <!-- 403 无权限页：普通用户越权访问管理员路由时进入 -->
  <div class="content">
    <a-result class="result" status="403" subtitle="没有访问该页面的权限">
    </a-result>
    <div class="operation-row">
      <a-button key="back" type="primary" @click="back">返回首页</a-button>
    </div>
  </div>
</template>

<script lang="ts" setup>
  import { useRouter } from 'vue-router';

  const router = useRouter();
  const back = () => {
    router.push({ name: 'Workplace' });
  };
</script>

<style scoped lang="less">
  .content {
    position: absolute;
    top: 50%;
    left: 50%;
    margin-left: -95px;
    margin-top: -121px;
    text-align: center;
  }
</style>
