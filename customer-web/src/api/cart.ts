import { apiClient } from './client'
import type {
  ApiResponse,
  Cart,
  CartItemInput,
  UpdateCartItemInput,
} from './contracts'

export async function getCart(): Promise<Cart> {
  const { data } = await apiClient.get<ApiResponse<Cart>>('/api/cart')

  return data.data
}

export async function addCartItem(input: CartItemInput): Promise<string> {
  const { data } = await apiClient.post<ApiResponse<null>>(
    '/api/cart/items',
    input,
  )

  return data.message
}

export async function updateCartItem(
  productId: number,
  input: UpdateCartItemInput,
): Promise<string> {
  const { data } = await apiClient.put<ApiResponse<null>>(
    `/api/cart/items/${productId}`,
    input,
  )

  return data.message
}

export async function removeCartItem(productId: number): Promise<string> {
  const { data } = await apiClient.delete<ApiResponse<null>>(
    `/api/cart/items/${productId}`,
  )

  return data.message
}
