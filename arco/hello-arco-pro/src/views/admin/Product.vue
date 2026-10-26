<!-- 商品管理：商品的分页查询与新增、编辑、删除、启用停用 -->
<script setup lang="ts">
  import { onMounted, reactive, ref } from 'vue';
  import { Message, Modal } from '@arco-design/web-vue';
  import {
    getProductPage,
    createProduct,
    updateProduct,
    deleteProduct,
    updateProductStatus,
    type ProductItem,
    type ProductQuery,
    type ProductSaveData,
  } from '@/api/product';
  import { getCategoryTree, type CategoryItem } from '@/api/category';
  import { ENABLE_DICT, formatDateTime } from '@/utils/dict';

  const statusOptions = Object.entries(ENABLE_DICT).map(([value, item]) => ({
    label: item.label,
    value,
  }));

  const categoryFieldNames = { key: 'id', title: 'name', children: 'children' };

  const loading = ref(false);
  const list = ref<ProductItem[]>([]);
  const total = ref(0);
  const query = reactive<ProductQuery>({
    pageNum: 1,
    pageSize: 10,
    keyword: '',
    categoryId: undefined,
    status: undefined,
  });

  const categoryTree = ref<CategoryItem[]>([]);

  const load = async () => {
    loading.value = true;
    try {
      const res = await getProductPage(query);
      list.value = res.list;
      total.value = res.total;
    } finally {
      loading.value = false;
    }
  };

  const handleSearch = () => {
    query.pageNum = 1;
    load();
  };

  const handleReset = () => {
    query.keyword = '';
    query.categoryId = undefined;
    query.status = undefined;
    query.pageNum = 1;
    load();
  };

  const handleCategoryChange = (value: unknown) => {
    query.categoryId = value === undefined || value === null || value === '' ? undefined : Number(value);
  };

  const handleStatusChange = (value: unknown) => {
    query.status = value === undefined || value === null || value === '' ? undefined : String(value);
  };

  const visible = ref(false);
  const isEdit = ref(false);
  const formRef = ref();
  const form = reactive<ProductSaveData & { id?: number }>({
    id: undefined,
    sku: '',
    name: '',
    categoryId: undefined,
    spec: '',
    unit: '',
    barcode: '',
    costPrice: undefined,
    salePrice: undefined,
    status: 'ENABLED',
  });

  const rules = {
    sku: [{ required: true, message: '请输入 SKU' }],
    name: [{ required: true, message: '请输入商品名称' }],
  };

  const resetForm = () => {
    Object.assign(form, {
      id: undefined,
      sku: '',
      name: '',
      categoryId: undefined,
      spec: '',
      unit: '',
      barcode: '',
      costPrice: undefined,
      salePrice: undefined,
      status: 'ENABLED',
    });
  };

  const openCreate = () => {
    isEdit.value = false;
    resetForm();
    visible.value = true;
  };

  const openEdit = (record: ProductItem) => {
    isEdit.value = true;
    Object.assign(form, {
      id: record.id,
      sku: record.sku,
      name: record.name,
      categoryId: record.categoryId,
      spec: record.spec,
      unit: record.unit,
      barcode: record.barcode,
      costPrice: record.costPrice,
      salePrice: record.salePrice,
      status: record.status,
    });
    visible.value = true;
  };

  const handleFormCategoryChange = (value: unknown) => {
    form.categoryId = value === undefined || value === null || value === '' ? undefined : Number(value);
  };

  const handleFormStatusChange = (value: unknown) => {
    form.status = value === undefined || value === null || value === '' ? undefined : String(value);
  };

  const handleBeforeOk = async () => {
    const errors = await formRef.value?.validate();
    if (errors) {
      return false;
    }
    const payload: ProductSaveData = {
      name: form.name,
      categoryId: form.categoryId,
      spec: form.spec,
      unit: form.unit,
      barcode: form.barcode,
      costPrice: form.costPrice,
      salePrice: form.salePrice,
      status: form.status,
    };
    if (isEdit.value) {
      await updateProduct(form.id as number, payload);
      Message.success('修改成功');
    } else {
      payload.sku = form.sku;
      await createProduct(payload);
      Message.success('新增成功');
    }
    visible.value = false;
    load();
    return true;
  };

  const handleToggleStatus = (record: ProductItem) => {
    const next = record.status === 'ENABLED' ? 'DISABLED' : 'ENABLED';
    const action = next === 'ENABLED' ? '启用' : '停用';
    Modal.warning({
      title: '操作确认',
      content: `确定要${action}商品「${record.name}」吗？`,
      hideCancel: true,
      onOk: async () => {
        await updateProductStatus(record.id, next);
        Message.success(`${action}成功`);
        load();
      },
    });
  };

  const handleDelete = (record: ProductItem) => {
    Modal.warning({
      title: '删除确认',
      content: `确定要删除商品「${record.name}」吗？`,
      hideCancel: true,
      onOk: async () => {
        await deleteProduct(record.id);
        Message.success('删除成功');
        load();
      },
    });
  };

  onMounted(async () => {
    load();
    categoryTree.value = await getCategoryTree();
  });
</script>

<template>
  <page-table
    :data="list"
    :loading="loading"
    :total="total"
    v-model:page="query.pageNum"
    v-model:page-size="query.pageSize"
    :scroll="{ x: 1700 }"
    @change="load"
  >
    <template #toolbar>
      <a-form :model="query" layout="inline">
        <a-form-item field="keyword" label="关键词">
          <a-input
            v-model="query.keyword"
            placeholder="SKU / 名称"
            allow-clear
            style="width: 200px"
            @press-enter="handleSearch"
          />
        </a-form-item>
        <a-form-item field="categoryId" label="分类">
          <a-tree-select
            :model-value="query.categoryId"
            :data="categoryTree"
            :field-names="categoryFieldNames"
            allow-clear
            placeholder="全部分类"
            style="width: 180px"
            @change="handleCategoryChange"
          />
        </a-form-item>
        <a-form-item field="status" label="状态">
          <a-select
            :model-value="query.status"
            :options="statusOptions"
            allow-clear
            placeholder="全部状态"
            style="width: 140px"
            @change="handleStatusChange"
          />
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" @click="handleSearch"><icon-search /> 查询</a-button>
            <a-button @click="handleReset"><icon-refresh /> 重置</a-button>
            <a-button type="primary" status="success" @click="openCreate">
              <icon-plus /> 新增
            </a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </template>

    <template #columns>
      <a-table-column title="ID" data-index="id" :width="80" />
      <a-table-column title="SKU" data-index="sku" :width="140" />
      <a-table-column title="名称" data-index="name" :width="180" />
      <a-table-column title="分类" data-index="categoryName" :width="140" />
      <a-table-column title="规格" data-index="spec" :width="140" />
      <a-table-column title="单位" data-index="unit" :width="90" />
      <a-table-column title="条码" data-index="barcode" :width="150" />
      <a-table-column title="成本价" :width="110">
        <template #cell="{ record }">{{ record.costPrice ?? '-' }}</template>
      </a-table-column>
      <a-table-column title="售价" :width="110">
        <template #cell="{ record }">{{ record.salePrice ?? '-' }}</template>
      </a-table-column>
      <a-table-column title="状态" :width="90">
        <template #cell="{ record }">
          <dict-tag :dict="ENABLE_DICT" :value="record.status" />
        </template>
      </a-table-column>
      <a-table-column title="创建时间" :width="180">
        <template #cell="{ record }">{{ formatDateTime(record.createTime) }}</template>
      </a-table-column>
      <a-table-column title="操作" :width="220" fixed="right">
        <template #cell="{ record }">
          <a-space>
            <a-button type="text" size="small" @click="openEdit(record)"><icon-edit /> 编辑</a-button>
            <a-button type="text" size="small" @click="handleToggleStatus(record)">
              {{ record.status === 'ENABLED' ? '停用' : '启用' }}
            </a-button>
            <a-button type="text" status="danger" size="small" @click="handleDelete(record)">
              <icon-delete /> 删除
            </a-button>
          </a-space>
        </template>
      </a-table-column>
    </template>
  </page-table>

  <a-modal
    v-model:visible="visible"
    :title="isEdit ? '编辑商品' : '新增商品'"
    unmount-on-close
    @before-ok="handleBeforeOk"
  >
    <a-form ref="formRef" :model="form" :rules="rules">
      <a-form-item v-if="!isEdit" field="sku" label="SKU">
        <a-input v-model="form.sku" placeholder="请输入 SKU" />
      </a-form-item>
      <a-form-item field="name" label="名称">
        <a-input v-model="form.name" placeholder="请输入商品名称" />
      </a-form-item>
      <a-form-item field="categoryId" label="分类">
        <a-tree-select
          :model-value="form.categoryId"
          :data="categoryTree"
          :field-names="categoryFieldNames"
          allow-clear
          placeholder="请选择分类"
          @change="handleFormCategoryChange"
        />
      </a-form-item>
      <a-form-item field="spec" label="规格">
        <a-input v-model="form.spec" placeholder="请输入规格" />
      </a-form-item>
      <a-form-item field="unit" label="单位">
        <a-input v-model="form.unit" placeholder="请输入单位" />
      </a-form-item>
      <a-form-item field="barcode" label="条码">
        <a-input v-model="form.barcode" placeholder="请输入条码" />
      </a-form-item>
      <a-form-item field="costPrice" label="成本价">
        <a-input-number v-model="form.costPrice" :min="0" :precision="2" placeholder="请输入成本价" />
      </a-form-item>
      <a-form-item field="salePrice" label="售价">
        <a-input-number v-model="form.salePrice" :min="0" :precision="2" placeholder="请输入售价" />
      </a-form-item>
      <a-form-item field="status" label="状态">
        <a-select
          :model-value="form.status"
          :options="statusOptions"
          placeholder="请选择状态"
          @change="handleFormStatusChange"
        />
      </a-form-item>
    </a-form>
  </a-modal>
</template>
