import { apiClient } from './client'
import type {
  ApiResponse,
  CustomerAddress,
  CustomerAddressInput,
} from './contracts'

export async function getAddresses(): Promise<CustomerAddress[]> {
  const { data } = await apiClient.get<ApiResponse<CustomerAddress[]>>(
    '/api/addresses',
  )

  return data.data
}

export async function createAddress(
  payload: CustomerAddressInput,
): Promise<CustomerAddress> {
  const { data } = await apiClient.post<ApiResponse<CustomerAddress>>(
    '/api/addresses',
    payload,
  )

  return data.data
}

export async function setDefaultAddress(
  addressId: number,
): Promise<CustomerAddress> {
  const { data } = await apiClient.put<ApiResponse<CustomerAddress>>(
    `/api/addresses/${addressId}/default`,
  )

  return data.data
}
