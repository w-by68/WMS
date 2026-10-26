<!--
  出库单页面：
  - 状态页签（全部/草稿/待审核/已通过/已驳回）+ 列表查询；
  - 操作列按状态动态显示：DRAFT 可编辑/提交/删除，REJECTED 可编辑/删除，
    PENDING 仅作废，APPROVED/CANCELED 仅查看详情；
  - 管理员额外有"审核通过/驳回"；普通用户只看自己的单据（后端按 applicantId 过滤）。
-->
<template>
  <page-table
    :data="list"
    :loading="loading"
    :total="total"
    :scroll="{ x: 1300 }"
    v-model:page="query.pageNum"
    v-model:page-size="query.pageSize"
    @change="load"
  >
    <template #toolbar>
      <div class="toolbar-wrap">
        <a-tabs :active-key="activeStatus" type="line" @change="onTabChange">
          <a-tab-pane key="ALL" title="全部" />
          <a-tab-pane key="DRAFT" title="草稿" />
          <a-tab-pane key="PENDING" :title="`待审核 ${pendingCount}`" />
          <a-tab-pane key="APPROVED" title="已通过" />
          <a-tab-pane key="REJECTED" title="已驳回" />
        </a-tabs>

        <div class="toolbar">
          <a-form :model="query" layout="inline">
            <a-form-item field="keyword" label="关键词">
              <a-input
                v-model="query.keyword"
                placeholder="单号 / 客户"
                allow-clear
                style="width: 180px"
                @press-enter="search"
              />
            </a-form-item>
            <a-form-item field="type" label="类型">
              <a-select
                v-model="query.type"
                placeholder="全部"
                allow-clear
                style="width: 140px"
              >
                <a-option
                  v-for="(item, key) in OUTBOUND_TYPE_DICT"
                  :key="key"
                  :value="key"
                >
                  {{ item.label }}
                </a-option>
              </a-select>
            </a-form-item>
            <a-form-item>
              <a-space>
                <a-button type="primary" @click="search">
                  <template #icon><icon-search /></template>
                  查询
                </a-button>
                <a-button @click="reset">
                  <template #icon><icon-refresh /></template>
                  重置
                </a-button>
              </a-space>
            </a-form-item>
          </a-form>
          <a-space>
            <a-button type="primary" @click="createOrder">
              <template #icon><icon-plus /></template>
              新建出库单
            </a-button>
            <a-button @click="load">
              <template #icon><icon-refresh /></template>
              刷新
            </a-button>
          </a-space>
        </div>
      </div>
    </template>

    <template #columns>
      <a-table-column title="单号" data-index="orderNo" :width="180" />
      <a-table-column title="类型" :width="120">
        <template #cell="{ record }">
          <dict-tag :dict="OUTBOUND_TYPE_DICT" :value="record.type" />
        </template>
      </a-table-column>
      <a-table-column title="仓库" data-index="warehouseName" :width="140" />
      <a-table-column title="客户" data-index="customerName" :width="180" />
      <a-table-column title="状态" :width="100">
        <template #cell="{ record }">
          <dict-tag :dict="ORDER_STATUS_DICT" :value="record.status" />
        </template>
      </a-table-column>
      <a-table-column title="总数量" data-index="totalQuantity" :width="100" />
      <a-table-column title="总金额" data-index="totalAmount" :width="120" />
      <a-table-column title="申请人" data-index="applicantName" :width="120" />
      <a-table-column title="提交时间" :width="180">
        <template #cell="{ record }">{{ formatDateTime(record.createTime) }}</template>
      </a-table-column>
      <a-table-column title="审核时间" :width="180">
        <template #cell="{ record }">{{ formatDateTime(record.auditTime) }}</template>
      </a-table-column>
      <a-table-column title="审核备注" :width="160">
        <template #cell="{ record }">{{ record.auditRemark || '-' }}</template>
      </a-table-column>
      <a-table-column title="操作" :width="240" fixed="right">
        <template #cell="{ record }">
          <a-space wrap>
            <a-button type="text" size="small" @click="openDetail(record)">详情</a-button>

            <!-- 草稿：可编辑 / 提交 / 删除 -->
            <template v-if="record.status === 'DRAFT'">
              <a-button type="text" size="small" @click="editOrder(record)">编辑</a-button>
              <a-button type="text" size="small" @click="submitOrder(record)">提交</a-button>
              <a-button
                type="text"
                size="small"
                status="danger"
                @click="deleteOrder(record)"
              >
                删除
              </a-button>
            </template>

            <!-- 已驳回：编辑后会自动回到草稿，再提交；此处不直接给提交按钮 -->
            <template v-else-if="record.status === 'REJECTED'">
              <a-button type="text" size="small" @click="editOrder(record)">编辑</a-button>
              <a-button
                type="text"
                size="small"
                status="danger"
                @click="deleteOrder(record)"
              >
                删除
              </a-button>
            </template>

            <!-- 待审核：仅可作废；管理员可审核 -->
            <template v-else-if="record.status === 'PENDING'">
              <a-button
                type="text"
                size="small"
                status="warning"
                @click="cancelOrder(record)"
              >
                作废
              </a-button>
              <a-button
                v-if="isAdmin"
                type="text"
                size="small"
                @click="approve(record)"
              >
                审核通过
              </a-button>
              <a-button
                v-if="isAdmin"
                type="text"
                size="small"
                status="danger"
                @click="openReject(record)"
              >
                驳回
              </a-button>
            </template>
          </a-space>
        </template>
      </a-table-column>
    </template>
  </page-table>

  <!-- 详情抽屉 -->
  <a-drawer
    v-model:visible="detailVisible"
    :width="720"
    placement="right"
    title="出库单详情"
    :footer="canAuditDetail"
    unmount-on-close
  >
    <a-spin :loading="detailLoading" style="width: 100%">
      <template v-if="detail">
        <a-descriptions :column="2" bordered>
          <a-descriptions-item label="单号">{{ detail.orderNo }}</a-descriptions-item>
          <a-descriptions-item label="类型">
            <dict-tag :dict="OUTBOUND_TYPE_DICT" :value="detail.type" />
          </a-descriptions-item>
          <a-descriptions-item label="仓库">{{ detail.warehouseName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="客户">{{ detail.customerName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="状态">
            <dict-tag :dict="ORDER_STATUS_DICT" :value="detail.status" />
          </a-descriptions-item>
          <a-descriptions-item label="总数量">{{ detail.totalQuantity ?? '-' }}</a-descriptions-item>
          <a-descriptions-item label="总金额">{{ detail.totalAmount ?? '-' }}</a-descriptions-item>
          <a-descriptions-item label="申请人">{{ detail.applicantName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ formatDateTime(detail.createTime) }}</a-descriptions-item>
          <a-descriptions-item label="审核时间">{{ formatDateTime(detail.auditTime) }}</a-descriptions-item>
          <a-descriptions-item label="审核备注">{{ detail.auditRemark || '-' }}</a-descriptions-item>
          <a-descriptions-item label="备注">{{ detail.remark || '-' }}</a-descriptions-item>
        </a-descriptions>

        <div class="detail-subtitle">明细</div>
        <a-table
          :data="detail.items || []"
          :pagination="false"
          size="small"
          :scroll="{ x: 800 }"
          row-key="id"
        >
          <template #columns>
            <a-table-column title="商品名称" data-index="productName" :width="180" />
            <a-table-column title="SKU" data-index="sku" :width="140" />
            <a-table-column title="库位" data-index="locationCode" :width="140" />
            <a-table-column title="数量" data-index="quantity" :width="100" />
            <a-table-column title="单价" data-index="price" :width="120" />
            <a-table-column title="金额" data-index="amount" :width="120" />
          </template>
        </a-table>
      </template>
    </a-spin>

    <template #footer>
      <a-space>
        <a-button type="primary" @click="approve(detail!)">审核通过</a-button>
        <a-button status="danger" @click="openReject(detail!)">驳回</a-button>
      </a-space>
    </template>
  </a-drawer>

  <!-- 驳回原因输入弹窗 -->
  <a-modal
    v-model:visible="rejectVisible"
    title="驳回出库单"
    :on-before-ok="confirmReject"
    @cancel="rejectRemark = ''"
  >
    <a-form :model="{ remark: rejectRemark }" layout="vertical">
      <a-form-item label="驳回原因" required>
        <a-textarea
          v-model="rejectRemark"
          placeholder="请输入驳回原因"
          :auto-size="{ minRows: 3, maxRows: 5 }"
          allow-clear
        />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script lang="ts" setup>
  import { computed, onMounted, reactive, ref } from 'vue';
  import { useRouter } from 'vue-router';
  import { Message, Modal } from '@arco-design/web-vue';
  import {
    getOutboundOrderPage,
    getOutboundOrder,
    approveOutboundOrder,
    rejectOutboundOrder,
    submitOutboundOrder,
    cancelOutboundOrder,
    deleteOutboundOrder,
    type OutboundOrderItem,
  } from '@/api/outbound';
  import {
    ORDER_STATUS_DICT,
    OUTBOUND_TYPE_DICT,
    formatDateTime,
  } from '@/utils/dict';
  import { useUserStore } from '@/store';

  const router = useRouter();
  const userStore = useUserStore();
  /** 仅管理员可见审核相关操作 */
  const isAdmin = computed(() => userStore.role === 'ADMIN');

  const query = reactive({
    pageNum: 1,
    pageSize: 10,
    keyword: '',
    type: undefined as string | undefined,
  });

  /** 状态页签：ALL 表示"全部" */
  const activeStatus = ref('ALL');

  const list = ref<OutboundOrderItem[]>([]);
  const total = ref(0);
  const loading = ref(false);
  const pendingCount = ref(0);

  /** 列表刷新（请求失败提示由拦截器统一处理） */
  const load = async () => {
    loading.value = true;
    try {
      const res = await getOutboundOrderPage({
        ...query,
        status: activeStatus.value === 'ALL' ? undefined : activeStatus.value,
      });
      list.value = res.list;
      total.value = res.total;
    } catch {
      /* 错误提示已由响应拦截器统一处理 */
    } finally {
      loading.value = false;
      loadPending();
    }
  };

  /** 待审核角标：只取总数，单独请求一次 */
  const loadPending = async () => {
    try {
      const res = await getOutboundOrderPage({ pageNum: 1, pageSize: 1, status: 'PENDING' });
      pendingCount.value = res.total;
    } catch {
      /* 忽略角标请求失败 */
    }
  };

  const onTabChange = (key: string | number) => {
    activeStatus.value = String(key);
    query.pageNum = 1;
    load();
  };

  const search = () => {
    query.pageNum = 1;
    load();
  };

  const reset = () => {
    query.keyword = '';
    query.type = undefined;
    query.pageNum = 1;
    load();
  };

  // ---- 制单 ----
  const createOrder = () => {
    router.push({ name: 'OutboundOrderCreate' });
  };

  const editOrder = (record: OutboundOrderItem) => {
    router.push({ name: 'OutboundOrderEdit', params: { id: record.id } });
  };

  // ---- 详情 ----
  const detailVisible = ref(false);
  const detailLoading = ref(false);
  const detail = ref<OutboundOrderItem | null>(null);
  const canAuditDetail = computed(
    () => isAdmin.value && !!detail.value && detail.value.status === 'PENDING'
  );

  const openDetail = async (record: OutboundOrderItem) => {
    detailVisible.value = true;
    detailLoading.value = true;
    detail.value = null;
    try {
      detail.value = await getOutboundOrder(record.id);
    } catch {
      /* 错误提示已由响应拦截器统一处理 */
    } finally {
      detailLoading.value = false;
    }
  };

  // ---- 提交（提交时会锁定库存） ----
  const submitOrder = (record: OutboundOrderItem) => {
    Modal.warning({
      title: '提交审核',
      content: `确认提交出库单 ${record.orderNo} 吗？提交后将锁定库存且不可再编辑。`,
      hideCancel: true,
      onOk: async () => {
        await submitOutboundOrder(record.id);
        Message.success('提交成功');
        load();
      },
    });
  };

  // ---- 审核 ----
  const approve = (record: OutboundOrderItem) => {
    Modal.warning({
      title: '审核通过',
      content: `确认通过出库单 ${record.orderNo} 吗？`,
      hideCancel: true,
      onOk: async () => {
        await approveOutboundOrder(record.id);
        Message.success('审核通过成功');
        detailVisible.value = false;
        load();
      },
    });
  };

  // ---- 驳回 ----
  const rejectVisible = ref(false);
  const rejectRemark = ref('');
  const rejectTarget = ref<OutboundOrderItem | null>(null);

  const openReject = (record: OutboundOrderItem) => {
    rejectTarget.value = record;
    rejectRemark.value = '';
    rejectVisible.value = true;
  };

  const confirmReject = async () => {
    if (!rejectRemark.value.trim()) {
      Message.warning('请填写驳回原因');
      return false;
    }
    if (!rejectTarget.value) return false;
    await rejectOutboundOrder(rejectTarget.value.id, rejectRemark.value.trim());
    Message.success('驳回成功');
    rejectVisible.value = false;
    detailVisible.value = false;
    load();
    return true;
  };

  // ---- 作废（会释放已锁定库存） ----
  const cancelOrder = (record: OutboundOrderItem) => {
    Modal.warning({
      title: '作废出库单',
      content: `确认作废出库单 ${record.orderNo} 吗？作废后已锁定的库存会释放。`,
      hideCancel: true,
      onOk: async () => {
        await cancelOutboundOrder(record.id);
        Message.success('作废成功');
        load();
      },
    });
  };

  // ---- 删除 ----
  const deleteOrder = (record: OutboundOrderItem) => {
    Modal.warning({
      title: '删除出库单',
      content: `确认删除出库单 ${record.orderNo} 吗？删除后不可恢复。`,
      hideCancel: true,
      onOk: async () => {
        await deleteOutboundOrder(record.id);
        Message.success('删除成功');
        load();
      },
    });
  };

  onMounted(load);
</script>

<style scoped lang="less">
  .toolbar-wrap {
    width: 100%;
  }

  .toolbar {
    display: flex;
    flex-wrap: wrap;
    align-items: flex-start;
    justify-content: space-between;
    gap: 12px;
  }

  .detail-subtitle {
    margin: 16px 0 8px;
    font-weight: 600;
  }
</style>
