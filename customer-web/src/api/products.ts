import { apiClient } from './client'
import type {
  ApiResponse,
  PagedResponse,
  PopularProduct,
  Product,
} from './contracts'

interface ProductQuery {
  keyword?: string
  category?: string
  page?: number
  size?: number
}

export async function getProducts(
  query: ProductQuery = {},
): Promise<PagedResponse<Product>> {
  const { data } = await apiClient.get<ApiResponse<PagedResponse<Product>>>(
    '/api/products',
    {
      params: {
        keyword: query.keyword || undefined,
        category: query.category || undefined,
        page: query.page ?? 0,
        size: query.size ?? 9,
      },
    },
  )

  return data.data
}

export async function getCategories(): Promise<string[]> {
  const { data } = await apiClient.get<ApiResponse<string[]>>(
    '/api/products/categories',
  )

  return data.data
}

export async function getPopularProducts(limit = 6): Promise<PopularProduct[]> {
  const { data } = await apiClient.get<ApiResponse<PopularProduct[]>>(
    '/api/products/popular',
    {
      params: { limit },
    },
  )

  return data.data
}

export async function getProduct(productId: number): Promise<Product> {
  const { data } = await apiClient.get<ApiResponse<Product>>(
    `/api/products/${productId}`,
  )

  return data.data
}
