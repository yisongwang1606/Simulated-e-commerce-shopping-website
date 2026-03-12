import { apiClient } from './client'
import type {
  ApiResponse,
  Order,
  PagedResponse,
  Product,
  ProductPayload,
  RefundRequest,
  RefundReviewInput,
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

export async function createProduct(payload: ProductPayload): Promise<Product> {
  const { data } = await apiClient.post<ApiResponse<Product>>(
    '/api/admin/products',
    payload,
  )

  return data.data
}
