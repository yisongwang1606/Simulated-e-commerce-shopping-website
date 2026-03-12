import { apiClient } from './client'
import type { ApiResponse, Order, Product, ProductPayload } from './contracts'

export async function getAdminOrders(): Promise<Order[]> {
  const { data } = await apiClient.get<ApiResponse<Order[]>>('/api/admin/orders')

  return data.data
}

export async function createProduct(
  payload: ProductPayload,
): Promise<Product> {
  const { data } = await apiClient.post<ApiResponse<Product>>(
    '/api/admin/products',
    payload,
  )

  return data.data
}
