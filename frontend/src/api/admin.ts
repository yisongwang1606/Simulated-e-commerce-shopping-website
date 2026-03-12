import { apiClient } from './client'
import type {
  AdminDashboardSummary,
  ApiResponse,
  Order,
  OrderTag,
  PagedResponse,
  Product,
  ProductPayload,
  RefundRequest,
  RefundReviewInput,
  RefundSummary,
  SupportTicket,
  SupportTicketUpdateInput,
} from './contracts'

interface AdminOrderSearchQuery {
  status?: string
  customer?: string
  dateFrom?: string
  dateTo?: string
  page?: number
  size?: number
}

interface AdminRefundSearchQuery {
  status?: string
  page?: number
  size?: number
}

interface AdminSupportTicketSearchQuery {
  status?: string
  priority?: string
  assignedTeam?: string
  page?: number
  size?: number
}

export async function searchAdminOrders(
  query: AdminOrderSearchQuery = {},
): Promise<PagedResponse<Order>> {
  const { data } = await apiClient.get<ApiResponse<PagedResponse<Order>>>(
    '/api/admin/orders/search',
    {
      params: {
        status: query.status || undefined,
        customer: query.customer || undefined,
        dateFrom: query.dateFrom || undefined,
        dateTo: query.dateTo || undefined,
        page: query.page ?? 0,
        size: query.size ?? 8,
      },
    },
  )

  return data.data
}

export async function getAdminDashboardSummary(): Promise<AdminDashboardSummary> {
  const { data } = await apiClient.get<ApiResponse<AdminDashboardSummary>>(
    '/api/admin/dashboard/summary',
  )

  return data.data
}

export async function getAdminRefundRequests(
  query: AdminRefundSearchQuery = {},
): Promise<PagedResponse<RefundRequest>> {
  const { data } = await apiClient.get<ApiResponse<PagedResponse<RefundRequest>>>(
    '/api/admin/refund-requests',
    {
      params: {
        status: query.status || undefined,
        page: query.page ?? 0,
        size: query.size ?? 6,
      },
    },
  )

  return data.data
}

export async function getRefundSummary(
  dateFrom?: string,
  dateTo?: string,
): Promise<RefundSummary> {
  const { data } = await apiClient.get<ApiResponse<RefundSummary>>(
    '/api/admin/refund-requests/summary',
    {
      params: {
        dateFrom: dateFrom || undefined,
        dateTo: dateTo || undefined,
      },
    },
  )

  return data.data
}

export async function reviewRefundRequest(
  refundRequestId: number,
  payload: RefundReviewInput,
): Promise<RefundRequest> {
  const { data } = await apiClient.put<ApiResponse<RefundRequest>>(
    `/api/admin/refund-requests/${refundRequestId}/review`,
    payload,
  )

  return data.data
}

export async function getOrderTagCatalog(): Promise<OrderTag[]> {
  const { data } = await apiClient.get<ApiResponse<OrderTag[]>>(
    '/api/admin/order-tags',
  )

  return data.data
}

export async function assignOrderTag(
  orderId: number,
  orderTagId: number,
): Promise<OrderTag[]> {
  const { data } = await apiClient.post<ApiResponse<OrderTag[]>>(
    `/api/admin/orders/${orderId}/tags`,
    { orderTagId },
  )

  return data.data
}

export async function removeOrderTag(
  orderId: number,
  orderTagId: number,
): Promise<OrderTag[]> {
  const { data } = await apiClient.delete<ApiResponse<OrderTag[]>>(
    `/api/admin/orders/${orderId}/tags/${orderTagId}`,
  )

  return data.data
}

export async function getAdminSupportTickets(
  query: AdminSupportTicketSearchQuery = {},
): Promise<PagedResponse<SupportTicket>> {
  const { data } = await apiClient.get<ApiResponse<PagedResponse<SupportTicket>>>(
    '/api/admin/support-tickets',
    {
      params: {
        status: query.status || undefined,
        priority: query.priority || undefined,
        assignedTeam: query.assignedTeam || undefined,
        page: query.page ?? 0,
        size: query.size ?? 6,
      },
    },
  )

  return data.data
}

export async function updateSupportTicket(
  ticketId: number,
  payload: SupportTicketUpdateInput,
): Promise<SupportTicket> {
  const { data } = await apiClient.put<ApiResponse<SupportTicket>>(
    `/api/admin/support-tickets/${ticketId}`,
    payload,
  )

  return data.data
}

export async function createProduct(payload: ProductPayload): Promise<Product> {
  const { data } = await apiClient.post<ApiResponse<Product>>(
    '/api/admin/products',
    payload,
  )

  return data.data
}
