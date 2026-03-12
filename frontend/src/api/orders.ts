import { apiClient } from './client'
import type {
  ApiResponse,
  CreateOrderInput,
  Order,
  RefundRequest,
  RefundRequestInput,
  Shipment,
} from './contracts'

export async function getOrders(): Promise<Order[]> {
  const { data } = await apiClient.get<ApiResponse<Order[]>>('/api/orders')

  return data.data
}

export async function createOrder(payload?: CreateOrderInput): Promise<Order> {
  const { data } = payload?.addressId
    ? await apiClient.post<ApiResponse<Order>>('/api/orders', payload)
    : await apiClient.post<ApiResponse<Order>>('/api/orders')

  return data.data
}

export async function getOrderShipments(orderId: number): Promise<Shipment[]> {
  const { data } = await apiClient.get<ApiResponse<Shipment[]>>(
    `/api/orders/${orderId}/shipments`,
  )

  return data.data
}

export async function getOrderRefundRequests(
  orderId: number,
): Promise<RefundRequest[]> {
  const { data } = await apiClient.get<ApiResponse<RefundRequest[]>>(
    `/api/orders/${orderId}/refund-requests`,
  )

  return data.data
}

export async function createRefundRequest(
  orderId: number,
  payload: RefundRequestInput,
): Promise<RefundRequest> {
  const { data } = await apiClient.post<ApiResponse<RefundRequest>>(
    `/api/orders/${orderId}/refund-requests`,
    payload,
  )

  return data.data
}
