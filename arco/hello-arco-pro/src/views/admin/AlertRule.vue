<!-- 预警规则：库存预警规则的查询、新增、编辑、启停与删除 -->
<script setup lang="ts">
  import { computed, onMounted, reactive, ref } from 'vue';
  import { Message, Modal } from '@arco-design/web-vue';
  import {
    getAlertRulePage,
    createAlertRule,
    updateAlertRule,
    deleteAlertRule,
    toggleAlertRule,
    type AlertRuleItem,
    type AlertRuleQuery,
    type AlertRuleSaveData,
  } from '@/api/alertRule';
  import { getProductOptions, type ProductItem } from '@/api/product';
  import { getWarehouseOptions, type WarehouseItem } from '@/api/warehouse';
  import { formatDateTime } from '@/utils/dict';

  const enabledOptions = [
    { label: '启用', value: 1 },
    { label: '停用', value: 0 },
  ];

  const toNum = (value: unknown): number | undefined =>
    value === undefined || value === null || value === '' ? undefined : Number(value);

  const loading = ref(false);
  const list = ref<AlertRuleItem[]>([]);
  const total = ref(0);
  const query = reactive<AlertRuleQuery>({
    pageNum: 1,
    pageSize: 10,
    productId: undefined,
    enabled: undefined,
  });

  const products = ref<ProductItem[]>([]);
  const warehouses = ref<WarehouseItem[]>([]);
  const productOptions = computed(() =>
    products.value.map((item) => ({
      label: item.sku ? `${item.name}（${item.sku}）` : item.name,
      value: item.id,
    }))
  );
  const warehouseOptions = computed(() =>
    warehouses.value.map((item) => ({ label: item.name, value: item.id }))
  );

  const load = async () => {
    loading.value = true;
    try {
      const res = await getAlertRulePage(query);
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
    query.productId = undefined;
    query.enabled = undefined;
    query.pageNum = 1;
    load();
  };

  const handleFilterProductChange = (value: unknown) => {
    query.productId = toNum(value);
  };

  const handleFilterEnabledChange = (value: unknown) => {
    query.enabled = toNum(value);
  };

  const productText = (record: AlertRuleItem) => {
    if (!record.productName) {
      return '全局';
    }
    return record.sku ? `${record.productName}（${record.sku}）` : record.productName;
  };

  const visible = ref(false);
  const currentId = ref<number>();
  const formRef = ref();
  const form = reactive<AlertRuleSaveData>({
    productId: undefined,
    warehouseId: undefined,
    safetyStock: undefined,
    maxStock: undefined,
    deadStockDays: undefined,
    enabled: 1,
  });

  const rules = {
    safetyStock: [{ required: true, message: '请输入安全库存' }],
  };

  const resetForm = () => {
    Object.assign(form, {
      productId: undefined,
      warehouseId: undefined,
      safetyStock: undefined,
      maxStock: undefined,
      deadStockDays: undefined,
      enabled: 1,
    });
  };

  const openCreate = () => {
    currentId.value = undefined;
    resetForm();
    visible.value = true;
  };

  const openEdit = (record: AlertRuleItem) => {
    currentId.value = record.id;
    Object.assign(form, {
      productId: record.productId,
      warehouseId: record.warehouseId,
      safetyStock: record.safetyStock,
      maxStock: record.maxStock,
      deadStockDays: record.deadStockDays,
      enabled: record.enabled,
    });
    visible.value = true;
  };

  const handleFormProductChange = (value: unknown) => {
    form.productId = toNum(value);
  };

  const handleFormWarehouseChange = (value: unknown) => {
    form.warehouseId = toNum(value);
  };

  const handleEnabledChange = (value: unknown) => {
    form.enabled = Number(value);
  };

  const handleBeforeOk = async () => {
    const errors = await formRef.value?.validate();
    if (errors) {
      return false;
    }
    const payload: AlertRuleSaveData = {
      productId: form.productId,
      warehouseId: form.warehouseId,
      safetyStock: form.safetyStock,
      maxStock: form.maxStock,
      deadStockDays: form.deadStockDays,
      enabled: form.enabled,
    };
    if (currentId.value) {
      await updateAlertRule(currentId.value, payload);
      Message.success('修改成功');
    } else {
      await createAlertRule(payload);
      Message.success('新增成功');
    }
    visible.value = false;
    load();
    return true;
  };

  const handleToggle = async (record: AlertRuleItem, value: unknown) => {
    const enabled = Number(value);
    await toggleAlertRule(record.id, enabled);
    Message.success(enabled === 1 ? '启用成功' : '停用成功');
    load();
  };

  const handleDelete = (record: AlertRuleItem) => {
    Modal.warning({
      title: '删除确认',
      content: '确定要删除该预警规则吗？',
      hideCancel: true,
      onOk: async () => {
        await deleteAlertRule(record.id);
        Message.success('删除成功');
        load();
      },
    });
  };

  onMounted(async () => {
    load();
    products.value = await getProductOptions();
    warehouses.value = await getWarehouseOptions();
  });
</script>

<template>
  <page-table
    :data="list"
    :loading="loading"
    :total="total"
    v-model:page="query.pageNum"
    v-model:page-size="query.pageSize"
    :scroll="{ x: 1500 }"
    @change="load"
  >
    <template #toolbar>
      <a-form :model="query" layout="inline">
        <a-form-item field="productId" label="商品">
          <a-select
            :model-value="query.productId"
            :options="productOptions"
            allow-clear
            allow-search
            placeholder="全部商品"
            style="width: 200px"
            @change="handleFilterProductChange"
          />
        </a-form-item>
        <a-form-item field="enabled" label="状态">
          <a-select
            :model-value="query.enabled"
            :options="enabledOptions"
            allow-clear
            placeholder="全部状态"
            style="width: 140px"
            @change="handleFilterEnabledChange"
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
      <a-table-column title="商品" :width="200">
        <template #cell="{ record }">{{ productText(record) }}</template>
      </a-table-column>
      <a-table-column title="仓库" :width="150">
        <template #cell="{ record }">{{ record.warehouseName || '全局' }}</template>
      </a-table-column>
      <a-table-column title="安全库存" data-index="safetyStock" :width="110" />
      <a-table-column title="库存上限" data-index="maxStock" :width="110" />
      <a-table-column title="呆滞天数" data-index="deadStockDays" :width="110" />
      <a-table-column title="是否启用" :width="110">
        <template #cell="{ record }">
          <a-switch
            :model-value="record.enabled"
            :checked-value="1"
            :unchecked-value="0"
            @change="(value) => handleToggle(record, value)"
          />
        </template>
      </a-table-column>
      <a-table-column title="创建时间" :width="180">
        <template #cell="{ record }">{{ formatDateTime(record.createTime) }}</template>
      </a-table-column>
      <a-table-column title="操作" :width="180" fixed="right">
        <template #cell="{ record }">
          <a-space>
            <a-button type="text" size="small" @click="openEdit(record)"><icon-edit /> 编辑</a-button>
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
    :title="currentId ? '编辑预警规则' : '新增预警规则'"
    unmount-on-close
    @before-ok="handleBeforeOk"
  >
    <a-form ref="formRef" :model="form" :rules="rules">
      <a-form-item field="productId" label="商品">
        <a-select
          :model-value="form.productId"
          :options="productOptions"
          allow-clear
          allow-search
          placeholder="不选表示全局"
          @change="handleFormProductChange"
        />
      </a-form-item>
      <a-form-item field="warehouseId" label="仓库">
        <a-select
          :model-value="form.warehouseId"
          :options="warehouseOptions"
          allow-clear
          allow-search
          placeholder="不选表示全局"
          @change="handleFormWarehouseChange"
        />
      </a-form-item>
      <a-form-item field="safetyStock" label="安全库存">
        <a-input-number v-model="form.safetyStock" :min="0" placeholder="请输入安全库存" />
      </a-form-item>
      <a-form-item field="maxStock" label="库存上限">
        <a-input-number v-model="form.maxStock" :min="0" placeholder="请输入库存上限" />
      </a-form-item>
      <a-form-item field="deadStockDays" label="呆滞天数">
        <a-input-number v-model="form.deadStockDays" :min="0" placeholder="请输入呆滞天数" />
      </a-form-item>
      <a-form-item field="enabled" label="是否启用">
        <a-switch
          :model-value="form.enabled"
          :checked-value="1"
          :unchecked-value="0"
          @change="handleEnabledChange"
        />
      </a-form-item>
    </a-form>
  </a-modal>
</template>
