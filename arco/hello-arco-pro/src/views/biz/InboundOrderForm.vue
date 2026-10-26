<!-- 入库单制单页：新建（/inbound-orders/new）与编辑（/inbound-orders/edit/:id）共用。 -->
<template>
  <div class="order-form">
    <a-card :bordered="false">
      <template #title>
        {{ isEdit ? '编辑入库单' : '新建入库单' }}
      </template>
      <template #extra>
        <a-space>
          <a-button :disabled="saving" @click="goBack">返回列表</a-button>
          <a-button :loading="saving" @click="save(false)">保存草稿</a-button>
          <a-button type="primary" :loading="saving" @click="save(true)">
            提交审核
          </a-button>
        </a-space>
      </template>

      <a-spin :loading="loading" style="width: 100%">
        <a-form :model="form" layout="vertical">
          <a-row :gutter="16">
            <a-col :span="6">
              <a-form-item label="入库类型" required>
                <a-select v-model="form.type" placeholder="请选择">
                  <a-option
                    v-for="(item, key) in INBOUND_TYPE_DICT"
                    :key="key"
                    :value="key"
                  >
                    {{ item.label }}
                  </a-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item label="入库仓库" required>
                <a-select
                  v-model="form.warehouseId"
                  placeholder="请选择仓库"
                  allow-search
                  @change="onWarehouseChange"
                >
                  <a-option
                    v-for="w in warehouses"
                    :key="w.id"
                    :value="w.id"
                    :label="`${w.code} ${w.name}`"
                  />
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item
                :label="form.type === 'PURCHASE' ? '供应商（采购必选）' : '供应商'"
              >
                <a-select
                  v-model="form.supplierId"
                  placeholder="请选择供应商"
                  allow-search
                  allow-clear
                >
                  <a-option
                    v-for="s in suppliers"
                    :key="s.id"
                    :value="s.id"
                    :label="s.name"
                  />
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item label="备注">
                <a-input v-model="form.remark" placeholder="选填" allow-clear />
              </a-form-item>
            </a-col>
          </a-row>
        </a-form>

        <order-item-editor
          v-model="items"
          :products="products"
          :locations="locations"
          mode="inbound"
          :disabled="saving"
        />
      </a-spin>
    </a-card>
  </div>
</template>

<script lang="ts" setup>
  import { computed, onMounted, reactive, ref } from 'vue';
  import { useRoute, useRouter } from 'vue-router';
  import { Message } from '@arco-design/web-vue';
  import OrderItemEditor from './components/OrderItemEditor.vue';
  import {
    createInboundOrder,
    getInboundOrder,
    submitInboundOrder,
    updateInboundOrder,
    type InboundOrderSaveData,
  } from '@/api/inbound';
  import { getWarehouseOptions, type WarehouseItem } from '@/api/warehouse';
  import { getSupplierOptions, type SupplierItem } from '@/api/supplier';
  import { getProductOptions, type ProductItem } from '@/api/product';
  import {
    getLocationsByWarehouse,
    type LocationItem,
  } from '@/api/location';
  import { INBOUND_TYPE_DICT } from '@/utils/dict';

  const route = useRoute();
  const router = useRouter();

  const editId = computed(() => {
    const raw = route.params.id;
    return raw ? Number(raw) : undefined;
  });
  const isEdit = computed(() => editId.value !== undefined);

  const loading = ref(false);
  const saving = ref(false);
  const warehouses = ref<WarehouseItem[]>([]);
  const suppliers = ref<SupplierItem[]>([]);
  const products = ref<ProductItem[]>([]);
  const locations = ref<LocationItem[]>([]);

  const form = reactive({
    type: 'PURCHASE',
    warehouseId: undefined as number | undefined,
    supplierId: undefined as number | undefined,
    remark: '',
  });

  // 明细行：{ productId, locationId, quantity, price }
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const items = ref<any[]>([{ rowKey: 'new-0', quantity: 1, price: 0 }]);

  /** 仓库变化：重新拉库位，并清掉已选库位（避免残留别的仓库的库位） */
  const onWarehouseChange = async (warehouseId: unknown) => {
    items.value.forEach((row) => {
      row.locationId = undefined;
      row.availableStock = undefined;
    });
    locations.value = [];
    if (warehouseId) {
      locations.value = await getLocationsByWarehouse(Number(warehouseId));
    }
  };

  /** 基础下拉：仓库/供应商/商品一次拉齐（后端只对 options 接口放开了普通用户） */
  const loadOptions = async () => {
    const [wh, sup, prod] = await Promise.all([
      getWarehouseOptions(),
      getSupplierOptions(),
      getProductOptions(),
    ]);
    warehouses.value = wh;
    suppliers.value = sup;
    products.value = prod;
  };

  /** 编辑态：回填主表与明细 */
  const loadOrder = async (id: number) => {
    const detail = await getInboundOrder(id);
    form.type = detail.type;
    form.warehouseId = detail.warehouseId;
    form.supplierId = detail.supplierId;
    form.remark = detail.remark ?? '';
    locations.value = await getLocationsByWarehouse(detail.warehouseId);
    items.value = (detail.items || []).map((i) => ({
      rowKey: `old-${i.id ?? i.productId}`,
      productId: i.productId,
      locationId: i.locationId,
      quantity: i.quantity,
      price: i.price ?? 0,
    }));
  };

  onMounted(async () => {
    loading.value = true;
    try {
      await loadOptions();
      if (editId.value !== undefined) {
        await loadOrder(editId.value);
      }
    } catch {
      /* 错误提示已由响应拦截器统一处理 */
    } finally {
      loading.value = false;
    }
  });

  /** 前端校验：明细至少一行、数量 > 0、采购必选供应商 */
  const validate = () => {
    if (!form.type) {
      Message.warning('请选择入库类型');
      return false;
    }
    if (!form.warehouseId) {
      Message.warning('请选择入库仓库');
      return false;
    }
    if (form.type === 'PURCHASE' && !form.supplierId) {
      Message.warning('采购入库必须选择供应商');
      return false;
    }
    if (!items.value.length) {
      Message.warning('请至少添加一行明细');
      return false;
    }
    for (let i = 0; i < items.value.length; i += 1) {
      const row = items.value[i];
      if (!row.productId) {
        Message.warning(`第 ${i + 1} 行：请选择商品`);
        return false;
      }
      if (!row.locationId) {
        Message.warning(`第 ${i + 1} 行：请选择库位`);
        return false;
      }
      if (!row.quantity || Number(row.quantity) <= 0) {
        Message.warning(`第 ${i + 1} 行：数量必须大于 0`);
        return false;
      }
    }
    return true;
  };

  const buildPayload = (): InboundOrderSaveData => ({
    type: form.type,
    warehouseId: form.warehouseId as number,
    supplierId: form.supplierId,
    remark: form.remark,
    items: items.value.map((row) => ({
      productId: row.productId,
      locationId: row.locationId,
      quantity: Number(row.quantity),
      price: Number(row.price || 0),
    })),
  });

  /** 保存草稿 / 提交审核：统一走一个 saving 标记防重复点击 */
  const save = async (submit: boolean) => {
    if (saving.value || !validate()) return;
    saving.value = true;
    try {
      const payload = buildPayload();
      let orderId = editId.value;
      if (orderId !== undefined) {
        await updateInboundOrder(orderId, payload);
      } else {
        const created = await createInboundOrder(payload);
        orderId = created.id;
      }
      if (submit) {
        await submitInboundOrder(orderId);
        Message.success('已提交审核');
      } else {
        Message.success('草稿已保存');
      }
      goBack();
    } catch {
      /* 错误提示已由响应拦截器统一处理 */
    } finally {
      saving.value = false;
    }
  };

  const goBack = () => {
    router.push({ name: 'InboundOrder' });
  };
</script>

<style scoped lang="less">
  .order-form {
    :deep(.arco-card-body) {
      padding-top: 8px;
    }
  }
</style>
