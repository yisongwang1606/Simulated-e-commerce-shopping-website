import { apiClient } from './client'
import type {
  ApiResponse,
  AuthPayload,
  LoginInput,
  RegisterInput,
  UserProfile,
} from './contracts'

export async function login(input: LoginInput): Promise<AuthPayload> {
  const { data } = await apiClient.post<ApiResponse<AuthPayload>>(
    '/api/auth/login',
    input,
  )

  return data.data
}

export async function register(input: RegisterInput): Promise<UserProfile> {
  const { data } = await apiClient.post<ApiResponse<UserProfile>>(
    '/api/auth/register',
    input,
  )

  return data.data
}

export async function logout(): Promise<void> {
  await apiClient.post<ApiResponse<null>>('/api/auth/logout')
}

export async function fetchCurrentUser(): Promise<UserProfile> {
  const { data } = await apiClient.get<ApiResponse<UserProfile>>('/api/users/me')

  return data.data
}
