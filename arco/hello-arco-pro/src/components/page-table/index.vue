<script lang="ts" setup>
  /**
   * 列表页通用表格：统一"查询区 + 表格 + 分页"结构，减少各页面重复代码。
   * 用法：
   *   <page-table :data="list" :loading="loading" :total="total"
   *               v-model:page="query.pageNum" v-model:page-size="query.pageSize"
   *               @change="load">
   *     <template #toolbar> ...查询表单/新增按钮... </template>
   *     <template #columns> <a-table-column .../> </template>
   *   </page-table>
   */
  defineProps<{
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    data: any[];
    loading?: boolean;
    total?: number;
    page?: number;
    pageSize?: number;
    /** 横向滚动宽度，列多时传入，如 { x: 1200 } */
    scroll?: { x?: number; y?: number };
    /** 行样式：可传函数按行返回类名（如库存低于安全库存整行标红） */
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    rowClass?: string | ((record: any, rowIndex: number) => string);
  }>();

  const emit = defineEmits<{
    (e: 'update:page', page: number): void;
    (e: 'update:pageSize', size: number): void;
    (e: 'change'): void;
  }>();

  const handlePageChange = (page: number) => {
    emit('update:page', page);
    emit('change');
  };
  const handleSizeChange = (size: number) => {
    emit('update:pageSize', size);
    emit('update:page', 1);
    emit('change');
  };
</script>

<template>
  <div class="page-table">
    <div v-if="$slots.toolbar" class="page-table-toolbar">
      <slot name="toolbar" />
    </div>
    <a-table
      :data="data"
      :loading="loading"
      :pagination="false"
      :bordered="{ wrapper: true, cell: true }"
      :scroll="scroll"
      :row-class="rowClass"
      row-key="id"
      column-resizable
    >
      <template #columns>
        <slot name="columns" />
      </template>
    </a-table>
    <div class="page-table-pager">
      <a-pagination
        :total="total || 0"
        :current="page || 1"
        :page-size="pageSize || 10"
        show-total
        show-jumper
        show-page-size
        @change="handlePageChange"
        @page-size-change="handleSizeChange"
      />
    </div>
  </div>
</template>

<style scoped lang="less">
  .page-table {
    padding: 16px;
    background-color: var(--color-bg-2);
    border-radius: 8px;

    &-toolbar {
      margin-bottom: 16px;
    }

    &-pager {
      display: flex;
      justify-content: flex-end;
      margin-top: 16px;
    }
  }
</style>
