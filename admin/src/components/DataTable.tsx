import type { ReactNode } from 'react'

export interface Column<T> {
  key: string
  header: string
  render: (row: T) => ReactNode
  /** CSS 너비 (예: "120px", "30%") — 없으면 남는 폭을 나눠 가진다 */
  width?: string
}

interface DataTableProps<T> {
  columns: Column<T>[]
  rows: T[]
  rowKey: (row: T) => string | number
  /** 행 클릭 — 주면 행이 눌리는 모양이 된다 (목록 + 상세 화면) */
  onRowClick?: (row: T) => void
  selectedKey?: string | number | null
  /** 행이 없을 때 문구 (예: "해당하는 서류가 없어요") */
  emptyMessage: string
  /** 행을 흐리게 (예: 숨긴 물품) */
  dimmed?: (row: T) => boolean
}

/** 관리자 공통 표. 헤더 table-head, 행 구분 row-line, 선택 행 row-selected */
export default function DataTable<T>({
  columns,
  rows,
  rowKey,
  onRowClick,
  selectedKey,
  emptyMessage,
  dimmed,
}: DataTableProps<T>) {
  return (
    <table className="w-full table-fixed border-collapse text-left">
      <thead>
        <tr className="bg-table-head">
          {columns.map((column) => (
            <th
              key={column.key}
              scope="col"
              style={{ width: column.width }}
              className="h-10 px-4 text-[12px] font-bold text-label-alt"
            >
              {column.header}
            </th>
          ))}
        </tr>
      </thead>
      <tbody>
        {rows.length === 0 ? (
          <tr>
            <td colSpan={columns.length} className="px-4 py-12 text-center text-[14px] font-medium text-label-alt">
              {emptyMessage}
            </td>
          </tr>
        ) : (
          rows.map((row) => {
            const key = rowKey(row)
            const selected = selectedKey !== undefined && selectedKey === key
            return (
              <tr
                key={key}
                onClick={onRowClick ? () => onRowClick(row) : undefined}
                aria-selected={onRowClick ? selected : undefined}
                className={`border-t border-row-line text-[14px] text-label ${selected ? 'bg-row-selected' : ''} ${
                  onRowClick ? 'cursor-pointer hover:bg-row-selected' : ''
                } ${dimmed?.(row) ? 'opacity-60' : ''}`}
              >
                {columns.map((column) => (
                  <td key={column.key} className="truncate px-4 py-3.5">
                    {column.render(row)}
                  </td>
                ))}
              </tr>
            )
          })
        )}
      </tbody>
    </table>
  )
}
