import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { documentSearchApi } from '@/api/documentSearch'
import { formatFileSize, timeAgo, getErrorMessage } from '@/utils/helpers'
import FileIcon from '@/components/common/FileIcon'
import Spinner from '@/components/common/Spinner'
import { EmptyState, Pagination, StatusBadge } from '@/components/common/index'
import {
  HiOutlineSearch, HiOutlineDocumentText, HiOutlineFilter,
  HiOutlineX, HiOutlineCalendar,
} from 'react-icons/hi'

const PAGE_SIZE = 12

const DEPARTMENTS = ['HR', 'ACCOUNT', 'ENGINEERING', 'SALES', 'OPERATIONS', 'OTHER']

const INITIAL_FILTERS = {
  name: '',
  tag: '',
  department: '',
  ownerEmail: '',
  dateFrom: '',
  dateTo: '',
}

/**
 * Multi-criteria document search. Hits the backend
 * /document-search/advanced endpoint added alongside the basic
 * /documents/search. Filters: name, tag, department, owner, date range.
 */
export default function AdvancedSearchPage() {
  const [filters, setFilters] = useState(INITIAL_FILTERS)
  const [applied, setApplied] = useState(null)
  const [page,    setPage]    = useState(1)

  const onChange = (k) => (e) => setFilters(f => ({ ...f, [k]: e.target.value }))

  const onSubmit = (e) => {
    e.preventDefault()
    const cleaned = Object.fromEntries(
      Object.entries(filters).filter(([, v]) => v !== '' && v != null)
    )
    setApplied(cleaned)
    setPage(1)
  }

  const onReset = () => { setFilters(INITIAL_FILTERS); setApplied(null); setPage(1) }

  const { data, isLoading, isError, error } = useQuery({
    queryKey: ['advanced-search', applied, page],
    queryFn:  () => documentSearchApi.advanced({
      ...applied, page: page - 1, size: PAGE_SIZE,
    }),
    enabled:  !!applied,
    keepPreviousData: true,
  })

  const docs       = data?.content ?? []
  const totalPages = data?.totalPages ?? 1
  const total      = data?.totalElements ?? 0

  const activeFilters = applied ? Object.entries(applied) : []

  return (
    <div className="animate-fade-in">
      <div className="page-header mb-5">
        <div>
          <h1 className="page-title">Advanced search</h1>
          <p className="page-subtitle">Filter documents by name, tag, department, owner, or date range</p>
        </div>
      </div>

      {/* Filter panel */}
      <form onSubmit={onSubmit} className="card p-5 mb-5">
        <div className="flex items-center gap-2 mb-4">
          <HiOutlineFilter className="w-4 h-4 text-surface-500 dark:text-gray-400" />
          <h2 className="text-sm font-semibold text-surface-800 dark:text-gray-200">Filters</h2>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          <div>
            <label className="label">Document name</label>
            <input className="input" placeholder="e.g. salary report"
              value={filters.name} onChange={onChange('name')} />
          </div>

          <div>
            <label className="label">Tag</label>
            <input className="input" placeholder="e.g. april"
              value={filters.tag} onChange={onChange('tag')} />
          </div>

          <div>
            <label className="label">Department</label>
            <select className="input" value={filters.department} onChange={onChange('department')}>
              <option value="">Any department</option>
              {DEPARTMENTS.map(d => <option key={d} value={d}>{d}</option>)}
            </select>
          </div>

          <div>
            <label className="label">Owner email</label>
            <input className="input" placeholder="ram@company.com"
              value={filters.ownerEmail} onChange={onChange('ownerEmail')} />
          </div>

          <div>
            <label className="label flex items-center gap-1">
              <HiOutlineCalendar className="w-3.5 h-3.5" /> Date from
            </label>
            <input type="date" className="input"
              value={filters.dateFrom} onChange={onChange('dateFrom')} />
          </div>

          <div>
            <label className="label flex items-center gap-1">
              <HiOutlineCalendar className="w-3.5 h-3.5" /> Date to
            </label>
            <input type="date" className="input"
              value={filters.dateTo} onChange={onChange('dateTo')} />
          </div>
        </div>

        <div className="flex items-center gap-2 mt-5">
          <button type="submit" className="btn-primary gap-2">
            <HiOutlineSearch className="w-4 h-4" /> Search
          </button>
          <button type="button" onClick={onReset} className="btn-secondary">Reset</button>
          {applied && (
            <span className="ml-auto text-xs text-surface-500 dark:text-gray-400">
              {total} result{total !== 1 ? 's' : ''}
            </span>
          )}
        </div>
      </form>

      {/* Active filter chips */}
      {activeFilters.length > 0 && (
        <div className="flex flex-wrap gap-1.5 mb-4">
          {activeFilters.map(([k, v]) => (
            <span key={k} className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full bg-primary-50 text-primary-700 text-xs">
              <span className="font-medium">{prettyKey(k)}:</span> {v}
              <button type="button"
                onClick={() => {
                  const next = { ...applied }
                  delete next[k]
                  setFilters(f => ({ ...f, [k]: '' }))
                  setApplied(Object.keys(next).length ? next : null)
                  setPage(1)
                }}
                className="hover:text-primary-900">
                <HiOutlineX className="w-3 h-3" />
              </button>
            </span>
          ))}
        </div>
      )}

      {/* Results */}
      {!applied ? (
        <EmptyState
          icon={HiOutlineSearch}
          title="Start a search"
          description="Use the filters above to narrow down documents. All fields are optional."
        />
      ) : isLoading ? (
        <div className="flex justify-center py-16"><Spinner size="lg" /></div>
      ) : isError ? (
        <div className="card p-6 text-red-600 text-sm">{getErrorMessage(error)}</div>
      ) : docs.length === 0 ? (
        <EmptyState
          icon={HiOutlineDocumentText}
          title="No matching documents"
          description="Try removing some filters or widening the date range"
        />
      ) : (
        <>
          <div className="card overflow-hidden">
            <div className="table-wrapper !rounded-none !border-none">
              <table className="table">
                <thead>
                  <tr>
                    <th>Name</th>
                    <th>Owner</th>
                    <th>Department</th>
                    <th>Tags</th>
                    <th>Size</th>
                    <th>Status</th>
                    <th>Created</th>
                  </tr>
                </thead>
                <tbody>
                  {docs.map(d => (
                    <tr key={d.id}>
                      <td>
                        <Link to={`/documents/${d.id}`} className="flex items-center gap-2.5 hover:text-primary-700">
                          <FileIcon mimeType={d.mimeType} fileName={d.originalFileName} size="sm" />
                          <span className="font-medium line-clamp-1">{d.title}</span>
                        </Link>
                      </td>
                      <td className="text-sm">
                        {d.owner?.firstName} {d.owner?.lastName}
                        <p className="text-xs text-surface-400 dark:text-gray-500">{d.owner?.email}</p>
                      </td>
                      <td className="text-sm text-surface-600 dark:text-gray-400">{d.owner?.department ?? '—'}</td>
                      <td className="text-xs text-surface-500 dark:text-gray-400 line-clamp-1 max-w-xs">{d.tags || '—'}</td>
                      <td className="text-sm text-surface-600 dark:text-gray-400">{formatFileSize(d.fileSize)}</td>
                      <td><StatusBadge status={d.status} /></td>
                      <td className="text-xs text-surface-500 dark:text-gray-400">{timeAgo(d.createdAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          <div className="mt-4">
            <Pagination page={page} totalPages={totalPages} onChange={setPage} />
          </div>
        </>
      )}
    </div>
  )
}

function prettyKey(k) {
  return ({
    name: 'Name',
    tag: 'Tag',
    department: 'Department',
    ownerEmail: 'Owner',
    dateFrom: 'From',
    dateTo: 'To',
  }[k]) ?? k
}
