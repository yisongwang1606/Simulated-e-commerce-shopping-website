import { create } from 'zustand'
import { createJSONStorage, persist } from 'zustand/middleware'

import type { AuthPayload, UserProfile } from '../api/contracts'

interface SessionState {
  token: string | null
  expiresAt: string | null
  user: UserProfile | null
  setSession: (payload: AuthPayload) => void
  setUser: (user: UserProfile | null) => void
  clearSession: () => void
}

export const useSessionStore = create<SessionState>()(
  persist(
    (set) => ({
      token: null,
      expiresAt: null,
      user: null,
      setSession: (payload) =>
        set({
          token: payload.token,
          expiresAt: payload.expiresAt,
          user: payload.user,
        }),
      setUser: (user) => set({ user }),
      clearSession: () =>
        set({
          token: null,
          expiresAt: null,
          user: null,
        }),
    }),
    {
      name: 'ecom-customer-session',
      storage: createJSONStorage(() => localStorage),
    },
  ),
)

export function isAdmin(user: UserProfile | null): boolean {
  return user?.role === 'ADMIN'
}
