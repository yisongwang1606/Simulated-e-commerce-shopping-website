const currencyFormatter = new Intl.NumberFormat('en-CA', {
  style: 'currency',
  currency: 'CAD',
  maximumFractionDigits: 2,
})

const shortDateFormatter = new Intl.DateTimeFormat('en-CA', {
  year: 'numeric',
  month: 'short',
  day: 'numeric',
})

export function formatCurrency(value: number): string {
  return currencyFormatter.format(value)
}

export function formatDate(value: string): string {
  return shortDateFormatter.format(new Date(value))
}
