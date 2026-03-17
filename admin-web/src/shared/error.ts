import axios from 'axios'

import type { ApiResponse } from '../api/contracts'

export function extractErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const response = error.response?.data as Partial<ApiResponse<unknown>> | undefined

    if (
      typeof response?.message === 'string' &&
      response.message.trim().length > 0
    ) {
      return response.message
    }

    return error.message
  }

  if (error instanceof Error) {
    return error.message
  }

  return 'Unexpected error'
}
