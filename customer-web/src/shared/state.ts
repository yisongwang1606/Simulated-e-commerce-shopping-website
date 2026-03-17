export function setObjectField<T extends object, K extends keyof T>(
  current: T,
  field: K,
  value: T[K],
): T {
  return {
    ...current,
    [field]: value,
  }
}

export function setIndexedValue<T>(
  current: Record<number, T>,
  key: number,
  value: T,
): Record<number, T> {
  return {
    ...current,
    [key]: value,
  }
}

export function patchIndexedValue<T extends object>(
  current: Record<number, T>,
  key: number,
  fallback: T,
  patch: Partial<T>,
): Record<number, T> {
  return {
    ...current,
    [key]: {
      ...fallback,
      ...current[key],
      ...patch,
    },
  }
}
