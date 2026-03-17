type PaginationControlsProps = {
  page: number
  totalPages: number
  disabled?: boolean
  selectId: string
  label?: string
  onPageChange: (page: number) => void
}

function buildPageOptions(totalPages: number) {
  return Array.from({ length: totalPages }, (_, index) => index)
}

export function PaginationControls({
  page,
  totalPages,
  disabled = false,
  selectId,
  label = 'Page',
  onPageChange,
}: PaginationControlsProps) {
  if (totalPages <= 0) {
    return null
  }

  return (
    <div className="toolbar">
      <button
        className="button-outline"
        disabled={page === 0 || disabled}
        onClick={() => onPageChange(Math.max(page - 1, 0))}
        type="button"
      >
        Previous
      </button>
      <div className="page-jump">
        <label htmlFor={selectId}>{label}</label>
        <select
          id={selectId}
          onChange={(event) => onPageChange(Number(event.target.value))}
          value={page}
        >
          {buildPageOptions(totalPages).map((option) => (
            <option key={option} value={option}>
              {option + 1}
            </option>
          ))}
        </select>
      </div>
      <button
        className="button-outline"
        disabled={page >= totalPages - 1 || disabled}
        onClick={() => onPageChange(Math.min(page + 1, totalPages - 1))}
        type="button"
      >
        Next
      </button>
    </div>
  )
}
