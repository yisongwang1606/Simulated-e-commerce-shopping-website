import { apiClient } from './client'
import type { ApiResponse, Order } from './contracts'

export async function getOrders(): Promise<Order[]> {
  const { data } = await apiClient.get<ApiResponse<Order[]>>('/api/orders')

  return data.data
}

export async function createOrder(): Promise<Order> {
  const { data } = await apiClient.post<ApiResponse<Order>>('/api/orders')

  return data.data
}
