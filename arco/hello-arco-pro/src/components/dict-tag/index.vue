<script lang="ts" setup>
  /**
   * 状态字典标签（DictTag）：按 00-全局约定 第七节的状态字典渲染不同颜色的 Tag。
   * 用法：<dict-tag :dict="ORDER_STATUS_DICT" :value="record.status" />
   */
  import { computed } from 'vue';
  import { dictLabel, dictColor, DictItem } from '@/utils/dict';

  const props = defineProps<{
    dict: Record<string, DictItem>;
    value?: string | number;
  }>();

  const label = computed(() => dictLabel(props.dict, props.value));
  const color = computed(() => dictColor(props.dict, props.value));
</script>

<template>
  <a-tag :color="color" size="small">{{ label }}</a-tag>
</template>
