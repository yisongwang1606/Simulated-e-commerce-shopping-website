import { apiClient } from './client'
import type {
  ApiResponse,
  CreateOrderInput,
  Order,
  RefundRequest,
  RefundRequestInput,
  Shipment,
  SupportTicket,
  SupportTicketInput,
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

export async function getOrderSupportTickets(
  orderId: number,
): Promise<SupportTicket[]> {
  const { data } = await apiClient.get<ApiResponse<SupportTicket[]>>(
    `/api/orders/${orderId}/support-tickets`,
  )

  return data.data
}

export async function createOrderSupportTicket(
  orderId: number,
  payload: SupportTicketInput,
): Promise<SupportTicket> {
  const { data } = await apiClient.post<ApiResponse<SupportTicket>>(
    `/api/orders/${orderId}/support-tickets`,
    payload,
  )

  return data.data
}
