<template>
  <!-- 首页数据看板：6 张统计卡片 + 近 30 天出入库趋势折线 + 分类库存占比饼图 + 未处理预警列表 -->
  <div class="container">
    <a-spin :loading="loading" style="width: 100%">
      <a-grid :cols="24" :col-gap="16" :row-gap="16">
        <a-grid-item
          v-for="card in statCards"
          :key="card.title"
          :span="{ xs: 24, sm: 12, md: 8, lg: 4, xl: 4, xxl: 4 }"
        >
          <div class="stat-card">
            <div class="stat-card__title">
              <component :is="card.icon" class="stat-card__icon" />
              <span>{{ card.title }}</span>
            </div>
            <div class="stat-card__value">{{ card.value }}</div>
          </div>
        </a-grid-item>
      </a-grid>

      <a-grid :cols="24" :col-gap="16" :row-gap="16" style="margin-top: 16px">
        <a-grid-item
          :span="{ xs: 24, sm: 24, md: 24, lg: 16, xl: 16, xxl: 16 }"
        >
          <div class="panel">
            <div class="panel__header">近 30 天出入库趋势</div>
            <Chart :options="trendOption" height="320px" />
          </div>
        </a-grid-item>
        <a-grid-item
          :span="{ xs: 24, sm: 24, md: 24, lg: 8, xl: 8, xxl: 8 }"
        >
          <div class="panel">
            <div class="panel__header">分类库存占比</div>
            <Chart :options="categoryOption" height="320px" />
          </div>
        </a-grid-item>
      </a-grid>

      <div class="panel" style="margin-top: 16px">
        <div class="panel__header">
          未处理预警
          <a-button
            size="mini"
            type="text"
            style="margin-left: 8px"
            @click="loadAlerts"
          >
            刷新
          </a-button>
        </div>
        <a-table
          :data="alerts"
          :loading="alertLoading"
          :pagination="false"
          :bordered="false"
          row-key="id"
        >
          <template #columns>
            <a-table-column title="预警单号" data-index="alertNo" :width="160" />
            <a-table-column title="商品" :width="200">
              <template #cell="{ record }">
                {{ record.productName || '-' }}
                <span v-if="record.sku" class="sub-text">
                  （{{ record.sku }}）
                </span>
              </template>
            </a-table-column>
            <a-table-column title="仓库" data-index="warehouseName" :width="140" />
            <a-table-column title="类型" :width="100">
              <template #cell="{ record }">
                <DictTag :dict="ALERT_TYPE_DICT" :value="record.alertType" />
              </template>
            </a-table-column>
            <a-table-column title="级别" :width="100">
              <template #cell="{ record }">
                <DictTag :dict="ALERT_LEVEL_DICT" :value="record.level" />
              </template>
            </a-table-column>
            <a-table-column title="当前数量" data-index="currentQuantity" :width="100" />
            <a-table-column title="阈值" data-index="thresholdValue" :width="100" />
            <a-table-column title="产生时间" :width="180">
              <template #cell="{ record }">
                {{ formatDateTime(record.createTime) }}
              </template>
            </a-table-column>
            <a-table-column v-if="isAdmin" title="操作" :width="100" fixed="right">
              <template #cell="{ record }">
                <a-button
                  type="text"
                  size="mini"
                  @click="openHandleModal(record)"
                >
                  处理
                </a-button>
              </template>
            </a-table-column>
          </template>
        </a-table>
      </div>
    </a-spin>

    <a-modal
      v-model:visible="handleVisible"
      title="处理预警"
      :ok-loading="handleLoading"
      @ok="submitHandle"
    >
      <a-form :model="handleForm" layout="vertical">
        <a-form-item field="status" label="处理结果">
          <a-radio-group v-model="handleForm.status">
            <a-radio value="RESOLVED">标记已处理</a-radio>
            <a-radio value="IGNORED">忽略该预警</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item field="handleRemark" label="处理说明">
          <a-textarea
            v-model="handleForm.handleRemark"
            placeholder="请输入处理说明"
            :auto-size="{ minRows: 3 }"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
  import { computed, onMounted, reactive, ref } from 'vue';
  import { Message } from '@arco-design/web-vue';
  import {
    getDashboardOverview,
    getDashboardTrend,
    getCategoryStock,
    DashboardOverview,
    TrendPoint,
    CategoryStockItem,
  } from '@/api/dashboard';
  import { getAlertPage, handleAlert, AlertItem } from '@/api/alert';
  import { useUserStore } from '@/store';
  import {
    ALERT_TYPE_DICT,
    ALERT_LEVEL_DICT,
    formatDateTime,
  } from '@/utils/dict';

  const userStore = useUserStore();
  const isAdmin = computed(() => userStore.role === 'ADMIN');

  const loading = ref(false);
  const overview = ref<DashboardOverview | null>(null);
  const trend = ref<TrendPoint[]>([]);

  const formatNumber = (value?: number) =>
    value === undefined || value === null ? '0' : String(value);

  const formatMoney = (value?: number) =>
    value === undefined || value === null
      ? '0.00'
      : Number(value).toLocaleString('zh-CN', {
          minimumFractionDigits: 2,
          maximumFractionDigits: 2,
        });

  /** 6 张统计卡片：商品种类 / 库存总量 / 库存总值 / 未处理预警 / 待审入库 / 待审出库 */
  const statCards = computed(() => [
    {
      title: '商品种类',
      icon: 'icon-apps',
      value: formatNumber(overview.value?.totalSku),
    },
    {
      title: '库存总量',
      icon: 'icon-storage',
      value: formatNumber(overview.value?.totalQuantity),
    },
    {
      title: '库存总值(元)',
      icon: 'icon-money-collect',
      value: formatMoney(overview.value?.totalValue),
    },
    {
      title: '未处理预警',
      icon: 'icon-exclamation-circle',
      value: formatNumber(overview.value?.alertStats?.unresolved),
    },
    {
      title: '待审入库单',
      icon: 'icon-import',
      value: formatNumber(overview.value?.inboundStats?.pending),
    },
    {
      title: '待审出库单',
      icon: 'icon-export',
      value: formatNumber(overview.value?.outboundStats?.pending),
    },
  ]);

  const trendOption = computed(() => ({
    tooltip: { trigger: 'axis' },
    legend: { data: ['入库数量', '出库数量'] },
    grid: { left: 40, right: 20, top: 40, bottom: 30 },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: trend.value.map((item) => item.date.slice(5)),
    },
    yAxis: { type: 'value' },
    series: [
      {
        name: '入库数量',
        type: 'line',
        smooth: true,
        data: trend.value.map((item) => item.inboundQty),
      },
      {
        name: '出库数量',
        type: 'line',
        smooth: true,
        data: trend.value.map((item) => item.outboundQty),
      },
    ],
  }));

  const categoryStock = ref<CategoryStockItem[]>([]);
  const categoryOption = computed(() => ({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0 },
    series: [
      {
        name: '分类库存',
        type: 'pie',
        radius: ['40%', '68%'],
        avoidLabelOverlap: true,
        label: { formatter: '{b}\n{d}%' },
        data: categoryStock.value.map((item) => ({
          name: item.categoryName,
          value: item.quantity,
        })),
      },
    ],
  }));

  const alerts = ref<AlertItem[]>([]);
  const alertLoading = ref(false);

  const handleVisible = ref(false);
  const handleLoading = ref(false);
  const handleForm = reactive({
    id: 0,
    status: 'RESOLVED',
    handleRemark: '',
  });

  const loadOverview = async () => {
    overview.value = await getDashboardOverview();
  };

  const loadTrend = async () => {
    trend.value = await getDashboardTrend(30);
  };

  const loadCategoryStock = async () => {
    categoryStock.value = await getCategoryStock();
  };

  const loadAlerts = async () => {
    alertLoading.value = true;
    try {
      const res = await getAlertPage({
        pageNum: 1,
        pageSize: 10,
        status: 'UNRESOLVED',
      });
      alerts.value = res.list;
    } finally {
      alertLoading.value = false;
    }
  };

  const openHandleModal = (record: AlertItem) => {
    handleForm.id = record.id;
    handleForm.status = 'RESOLVED';
    handleForm.handleRemark = '';
    handleVisible.value = true;
  };

  const submitHandle = async () => {
    handleLoading.value = true;
    try {
      await handleAlert(handleForm.id, {
        status: handleForm.status,
        handleRemark: handleForm.handleRemark,
      });
      Message.success('处理成功');
      handleVisible.value = false;
      await Promise.all([loadAlerts(), loadOverview()]);
    } finally {
      handleLoading.value = false;
    }
  };

  onMounted(async () => {
    loading.value = true;
    try {
      await Promise.all([loadOverview(), loadTrend(), loadCategoryStock(), loadAlerts()]);
    } finally {
      loading.value = false;
    }
  });
</script>

<script lang="ts">
  export default {
    // 与 DEFAULT_ROUTE_NAME 保持一致，便于 keep-alive 缓存
    name: 'Workplace',
  };
</script>

<style scoped lang="less">
  .container {
    padding: 16px 20px;
    background-color: var(--color-fill-2);
  }

  .stat-card {
    padding: 16px;
    background-color: var(--color-bg-2);
    border-radius: 4px;
  }

  .stat-card__title {
    display: flex;
    align-items: center;
    color: var(--color-text-2);
    font-size: 13px;
  }

  .stat-card__icon {
    margin-right: 6px;
    font-size: 16px;
  }

  .stat-card__value {
    margin-top: 8px;
    color: var(--color-text-1);
    font-weight: 600;
    font-size: 24px;
  }

  .panel {
    height: 100%;
    padding: 16px;
    background-color: var(--color-bg-2);
    border-radius: 4px;
  }

  .panel__header {
    margin-bottom: 12px;
    color: var(--color-text-1);
    font-weight: 600;
    font-size: 15px;
  }

  .sub-text {
    color: var(--color-text-3);
    font-size: 12px;
  }
</style>
