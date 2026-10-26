<!-- 商品分类：以树形展示分类，支持新增一级/子分类、编辑与删除 -->
<template>
  <div class="category-page">
    <div class="category-page-toolbar">
      <a-button type="primary" @click="handleAddRoot">
        <icon-plus />
        新增一级分类
      </a-button>
      <a-button @click="load">
        <icon-refresh />
        刷新
      </a-button>
    </div>
    <a-table
      :data="tree"
      :loading="loading"
      :pagination="false"
      :bordered="{ wrapper: true, cell: true }"
      row-key="id"
      default-expand-all-rows
    >
      <template #columns>
        <a-table-column title="名称" data-index="name" />
        <a-table-column title="排序" data-index="sort" :width="100" />
        <a-table-column title="创建时间" :width="180">
          <template #cell="{ record }">
            {{ formatDateTime(record.createTime) }}
          </template>
        </a-table-column>
        <a-table-column title="操作" :width="240">
          <template #cell="{ record }">
            <a-space>
              <a-button
                type="text"
                size="mini"
                @click="handleAddChild(record)"
              >
                新增子分类
              </a-button>
              <a-button type="text" size="mini" @click="handleEdit(record)">
                编辑
              </a-button>
              <a-button
                type="text"
                size="mini"
                status="danger"
                @click="handleDelete(record)"
              >
                删除
              </a-button>
            </a-space>
          </template>
        </a-table-column>
      </template>
    </a-table>
  </div>

  <a-modal
    v-model:visible="formVisible"
    :title="formMode === 'add' ? '新增分类' : '编辑分类'"
    :width="480"
    :mask-closable="false"
    @before-ok="handleBeforeOk"
  >
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <a-form-item field="parentId" label="上级分类">
        <a-input :model-value="parentName" disabled />
      </a-form-item>
      <a-form-item field="name" label="分类名称">
        <a-input v-model="form.name" placeholder="请输入分类名称" />
      </a-form-item>
      <a-form-item field="sort" label="排序">
        <a-input-number v-model="form.sort" :min="0" placeholder="请输入排序" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
  import { computed, nextTick, onMounted, reactive, ref } from 'vue';
  import { Message, Modal, type FormInstance } from '@arco-design/web-vue';
  import {
    createCategory,
    deleteCategory,
    getCategoryTree,
    updateCategory,
  } from '@/api/category';
  import type { CategoryItem, CategorySaveData } from '@/api/category';
  import { formatDateTime } from '@/utils/dict';

  const tree = ref<CategoryItem[]>([]);
  const loading = ref(false);

  /** 分类 id -> 名称，用于展示上级分类 */
  const nameMap = computed(() => {
    const map = new Map<number, string>();
    const walk = (nodes: CategoryItem[]) => {
      nodes.forEach((node) => {
        map.set(node.id, node.name);
        if (node.children && node.children.length) {
          walk(node.children);
        }
      });
    };
    walk(tree.value);
    return map;
  });

  async function load() {
    loading.value = true;
    try {
      tree.value = await getCategoryTree();
    } catch {
      // 请求失败提示由 axios 拦截器统一处理
    } finally {
      loading.value = false;
    }
  }

  const formVisible = ref(false);
  const formMode = ref<'add' | 'edit'>('add');
  const currentId = ref<number>();
  const parentName = ref('顶级分类');
  const formRef = ref<FormInstance>();
  const form = reactive<CategorySaveData>({
    name: '',
    parentId: undefined,
    sort: 0,
  });

  const rules = {
    name: [{ required: true, message: '请输入分类名称' }],
  };

  function handleAddRoot() {
    formMode.value = 'add';
    currentId.value = undefined;
    form.name = '';
    form.parentId = undefined;
    form.sort = 0;
    parentName.value = '顶级分类';
    formVisible.value = true;
    nextTick(() => formRef.value?.clearValidate());
  }

  function handleAddChild(record: CategoryItem) {
    formMode.value = 'add';
    currentId.value = undefined;
    form.name = '';
    form.parentId = record.id;
    form.sort = 0;
    parentName.value = record.name;
    formVisible.value = true;
    nextTick(() => formRef.value?.clearValidate());
  }

  function handleEdit(record: CategoryItem) {
    formMode.value = 'edit';
    currentId.value = record.id;
    form.name = record.name;
    form.parentId = record.parentId;
    form.sort = record.sort ?? 0;
    parentName.value = record.parentId
      ? nameMap.value.get(record.parentId) ?? '顶级分类'
      : '顶级分类';
    formVisible.value = true;
    nextTick(() => formRef.value?.clearValidate());
  }

  async function handleBeforeOk() {
    const errors = await formRef.value?.validate();
    if (errors) {
      Message.warning('请完善表单信息');
      return false;
    }
    try {
      if (formMode.value === 'add') {
        await createCategory({ ...form });
        Message.success('新增成功');
      } else if (currentId.value !== undefined) {
        await updateCategory(currentId.value, { ...form });
        Message.success('更新成功');
      }
      load();
      return true;
    } catch {
      return false;
    }
  }

  function handleDelete(record: CategoryItem) {
    Modal.warning({
      title: '删除确认',
      content: `确定要删除分类「${record.name}」吗？`,
      hideCancel: true,
      onOk: async () => {
        try {
          await deleteCategory(record.id);
          Message.success('删除成功');
          load();
        } catch {
          // 请求失败提示由 axios 拦截器统一处理
        }
      },
    });
  }

  onMounted(load);
</script>

<style scoped lang="less">
  .category-page {
    padding: 16px;
    background-color: var(--color-bg-2);
    border-radius: 8px;

    &-toolbar {
      display: flex;
      gap: 8px;
      margin-bottom: 16px;
    }
  }
</style>
