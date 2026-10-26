<template>
  <!--
    页面容器：外层 div 保证 Transition 始终只有单一元素根节点。
    （各业务页面模板多为「表格 + 弹窗」的多根 Fragment，直接放在 Transition 下会导致
      非元素根节点无法参与过渡，进而出现页面内容不渲染的问题。）
  -->
  <router-view v-slot="{ Component, route }">
    <transition name="page-fade" mode="out-in">
      <div :key="route.fullPath" class="page-wrapper">
        <component :is="Component" />
      </div>
    </transition>
  </router-view>
</template>

<script lang="ts" setup></script>

<style scoped lang="less">
  .page-fade-enter-active,
  .page-fade-leave-active {
    transition: opacity 0.2s ease;
  }

  .page-fade-enter-from,
  .page-fade-leave-to {
    opacity: 0;
  }
</style>
