<!-- 库存盘点调整：选定商品、仓库、库位后，把账面数量调整为实际数量 -->
<script setup lang="ts">
  import { computed, onMounted, reactive, ref } from 'vue';
  import { Message, Modal } from '@arco-design/web-vue';
  import { getInventoryPage, adjustInventory, type InventoryItem } from '@/api/inventory';
  import { getProductOptions, type ProductItem } from '@/api/product';
  import { getWarehouseOptions, type WarehouseItem } from '@/api/warehouse';
  import { getLocationsByWarehouse, type LocationItem } from '@/api/location';

  const toNum = (value: unknown): number | undefined =>
    value === undefined || value === null || value === '' ? undefined : Number(value);

  const products = ref<ProductItem[]>([]);
  const warehouses = ref<WarehouseItem[]>([]);
  const locations = ref<LocationItem[]>([]);

  const productOptions = computed(() =>
    products.value.map((item) => ({
      label: item.sku ? `${item.name}（${item.sku}）` : item.name,
      value: item.id,
    }))
  );
  const warehouseOptions = computed(() =>
    warehouses.value.map((item) => ({ label: item.name, value: item.id }))
  );
  const locationOptions = computed(() =>
    locations.value.map((item) => ({
      label: item.zone ? `${item.code}（${item.zone}）` : item.code,
      value: item.id,
    }))
  );

  const form = reactive<{
    productId?: number;
    warehouseId?: number;
    locationId?: number;
    actualQuantity?: number;
    remark: string;
  }>({
    productId: undefined,
    warehouseId: undefined,
    locationId: undefined,
    actualQuantity: undefined,
    remark: '',
  });

  const inventory = ref<InventoryItem | null>(null);
  const notFound = ref(false);

  const loadCurrent = async () => {
    inventory.value = null;
    notFound.value = false;
    if (!form.productId || !form.warehouseId || !form.locationId) {
      return;
    }
    const res = await getInventoryPage({
      pageNum: 1,
      pageSize: 1,
      productId: form.productId,
      warehouseId: form.warehouseId,
      locationId: form.locationId,
    });
    const record = res.list[0];
    if (record) {
      inventory.value = record;
    } else {
      notFound.value = true;
      Message.warning('该商品在此库位暂无库存记录');
    }
  };

  const handleProductChange = (value: unknown) => {
    form.productId = toNum(value);
    loadCurrent();
  };

  const handleWarehouseChange = async (value: unknown) => {
    form.warehouseId = toNum(value);
    form.locationId = undefined;
    locations.value = [];
    if (form.warehouseId) {
      locations.value = await getLocationsByWarehouse(form.warehouseId);
    }
    loadCurrent();
  };

  const handleLocationChange = (value: unknown) => {
    form.locationId = toNum(value);
    loadCurrent();
  };

  const handleSubmit = () => {
    if (!form.productId) {
      Message.warning('请选择商品');
      return;
    }
    if (!form.warehouseId) {
      Message.warning('请选择仓库');
      return;
    }
    if (!form.locationId) {
      Message.warning('请选择库位');
      return;
    }
    if (form.actualQuantity === undefined || form.actualQuantity === null) {
      Message.warning('请输入调整后数量');
      return;
    }
    if (!Number.isInteger(form.actualQuantity) || form.actualQuantity < 0) {
      Message.warning('调整后数量必须为非负整数');
      return;
    }
    const before = inventory.value?.quantity;
    const after = form.actualQuantity;
    Modal.warning({
      title: '盘点调整确认',
      content: `确定将账面数量 ${before ?? '-'} 调整为 ${after} 吗？`,
      hideCancel: true,
      onOk: async () => {
        await adjustInventory({
          productId: form.productId as number,
          locationId: form.locationId as number,
          actualQuantity: after,
          remark: form.remark || undefined,
        });
        Message.success('盘点调整成功');
        form.actualQuantity = undefined;
        form.remark = '';
        loadCurrent();
      },
    });
  };

  onMounted(async () => {
    products.value = await getProductOptions();
    warehouses.value = await getWarehouseOptions();
  });
</script>

<template>
  <div class="stock-adjust">
    <a-card title="库存盘点调整">
      <a-form :model="form" layout="vertical" class="stock-adjust-form">
        <a-form-item label="商品" required>
          <a-select
            :model-value="form.productId"
            :options="productOptions"
            allow-clear
            allow-search
            placeholder="请选择商品"
            @change="handleProductChange"
          />
        </a-form-item>
        <a-form-item label="仓库" required>
          <a-select
            :model-value="form.warehouseId"
            :options="warehouseOptions"
            allow-clear
            allow-search
            placeholder="请选择仓库"
            @change="handleWarehouseChange"
          />
        </a-form-item>
        <a-form-item label="库位" required>
          <a-select
            :model-value="form.locationId"
            :options="locationOptions"
            :disabled="!form.warehouseId"
            allow-clear
            allow-search
            placeholder="请先选择仓库"
            @change="handleLocationChange"
          />
        </a-form-item>

        <a-form-item v-if="inventory" label="当前库存">
          <a-descriptions :column="2" bordered size="medium">
            <a-descriptions-item label="账面数量">{{ inventory.quantity }}</a-descriptions-item>
            <a-descriptions-item label="可用数量">{{ inventory.availableQuantity }}</a-descriptions-item>
          </a-descriptions>
        </a-form-item>
        <a-form-item v-if="notFound">
          <a-alert type="warning">该商品在此库位暂无库存记录</a-alert>
        </a-form-item>

        <a-form-item label="调整后数量" required>
          <a-input-number
            v-model="form.actualQuantity"
            :min="0"
            :precision="0"
            placeholder="请输入调整后数量"
          />
        </a-form-item>
        <a-form-item label="调整原因">
          <a-textarea
            v-model="form.remark"
            placeholder="请输入调整原因"
            :auto-size="{ minRows: 2, maxRows: 4 }"
          />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="handleSubmit">提交调整</a-button>
        </a-form-item>
      </a-form>
    </a-card>
  </div>
</template>

<style scoped lang="less">
  .stock-adjust {
    padding: 16px;

    &-form {
      max-width: 720px;
    }
  }
</style>
