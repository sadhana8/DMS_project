import { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useSearchParams } from 'react-router-dom'
import { documentsApi } from '@/api/documents'
import { useAuth } from '@/context/AuthContext'
import { formatFileSize, timeAgo, getErrorMessage } from '@/utils/helpers'
import FileIcon from '@/components/common/FileIcon'
import Spinner from '@/components/common/Spinner'
import { EmptyState, Pagination, ConfirmDialog, StatusBadge } from '@/components/common/index'
import UploadModal from '@/components/documents/UploadModal'
import toast from 'react-hot-toast'
import clsx from 'clsx'
import {
  HiOutlineSearch, HiOutlineUpload, HiOutlineDocumentText,
  HiOutlineDownload, HiOutlineTrash, HiOutlineEye,
  HiOutlineViewGrid, HiOutlineViewList, HiOutlineAdjustments,
} from 'react-icons/hi'

const PAGE_SIZE = 12

export default function DocumentsPage() {
  const { canUpload, isAdmin } = useAuth()
  const qc = useQueryClient()
  const [searchParams, setSearchParams] = useSearchParams()

  const [page,          setPage]         = useState(1)
  const [view,          setView]         = useState('grid')   // grid | list
  const [uploadOpen,    setUploadOpen]   = useState(false)
  const [deleteDoc,     setDeleteDoc]    = useState(null)
  const [deleting,      setDeleting]     = useState(false)

  const q      = searchParams.get('q') ?? ''
  const status = searchParams.get('status') ?? ''

  const setFilter = (key, val) => {
    setSearchParams(prev => { val ? prev.set(key, val) : prev.delete(key); return prev })
    setPage(1)
  }

  const { data, isLoading, isError } = useQuery({
    queryKey: ['documents', page, q, status],
    queryFn:  () => q
      ? documentsApi.search(q, { page: page - 1, size: PAGE_SIZE, status: status || undefined })
      : documentsApi.list({ page: page - 1, size: PAGE_SIZE, status: status || undefined }),
    keepPreviousData: true,
  })

  const docs       = data?.content ?? []
  const totalPages = data?.totalPages ?? 1
  const total      = data?.totalElements ?? 0

  const confirmDelete = async () => {
    setDeleting(true)
    try {
      await documentsApi.delete(deleteDoc.id)
      qc.invalidateQueries({ queryKey: ['documents'] })
      toast.success('Document moved to archive')
      setDeleteDoc(null)
    } catch (e) { toast.error(getErrorMessage(e)) }
    finally { setDeleting(false) }
  }

  return (
    <div className="animate-fade-in">
      {/* Header */}
      <div className="page-header mb-5">
        <div>
          <h1 className="page-title">Documents</h1>
          <p className="page-subtitle">{total} document{total !== 1 ? 's' : ''} total</p>
        </div>
        {canUpload() && (
          <div className="flex items-center gap-2">
            <Link to="/documents/search/advanced" className="btn-secondary gap-2">
              <HiOutlineAdjustments className="w-4 h-4" /> Advanced
            </Link>
            <button onClick={() => setUploadOpen(true)} className="btn-primary gap-2">
              <HiOutlineUpload className="w-4 h-4" /> Upload
            </button>
          </div>
        )}
        {!canUpload() && (
          <Link to="/documents/search/advanced" className="btn-secondary gap-2">
            <HiOutlineAdjustments className="w-4 h-4" /> Advanced search
          </Link>
        )}
      </div>

      {/* Toolbar */}
      <div className="flex flex-wrap items-center gap-3 mb-5">
        {/* Search */}
        <div className="relative flex-1 min-w-56">
          <HiOutlineSearch className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-surface-400" />
          <input
            defaultValue={q}
            onKeyDown={(e) => e.key === 'Enter' && setFilter('q', e.target.value)}
            placeholder="Search documents…"
            className="input pl-9"
          />
        </div>

        {/* Status filter */}
        <select value={status} onChange={(e) => setFilter('status', e.target.value)} className="input w-40">
          <option value="">All status</option>
          <option value="ACTIVE">Active</option>
          <option value="ARCHIVED">Archived</option>
          <option value="PENDING_REVIEW">Pending review</option>
        </select>

        {/* View toggle */}
        <div className="flex rounded-lg border border-surface-200 overflow-hidden">
          <button onClick={() => setView('grid')} className={clsx('p-2.5 transition-colors', view === 'grid' ? 'bg-primary-50 text-primary-700' : 'bg-white text-surface-500 hover:bg-surface-50')}>
            <HiOutlineViewGrid className="w-4 h-4" />
          </button>
          <button onClick={() => setView('list')} className={clsx('p-2.5 border-l border-surface-200 transition-colors', view === 'list' ? 'bg-primary-50 text-primary-700' : 'bg-white text-surface-500 hover:bg-surface-50')}>
            <HiOutlineViewList className="w-4 h-4" />
          </button>
        </div>
      </div>

      {/* Content */}
      {isLoading ? (
        <div className="flex justify-center py-16"><Spinner size="lg" /></div>
      ) : isError ? (
        <div className="text-center py-16 text-red-500">Failed to load documents</div>
      ) : docs.length === 0 ? (
        <EmptyState
          icon={HiOutlineDocumentText}
          title={q ? `No results for "${q}"` : 'No documents yet'}
          description={q ? 'Try different keywords or clear the search' : canUpload() ? 'Upload your first document to get started' : 'No documents available'}
          action={canUpload() && <button onClick={() => setUploadOpen(true)} className="btn-primary gap-2"><HiOutlineUpload className="w-4 h-4" /> Upload document</button>}
        />
      ) : view === 'grid' ? (
        <GridView docs={docs} onDelete={setDeleteDoc} />
      ) : (
        <ListView docs={docs} onDelete={setDeleteDoc} />
      )}

      <Pagination page={page} totalPages={totalPages} onChange={setPage} />

      <UploadModal open={uploadOpen} onClose={() => setUploadOpen(false)} />

      <ConfirmDialog
        open={!!deleteDoc}
        onClose={() => setDeleteDoc(null)}
        onConfirm={confirmDelete}
        loading={deleting}
        title="Archive document"
        confirmLabel="Archive"
        message={`Move "${deleteDoc?.title}" to the archive? It will be hidden from listings but an admin can restore it later.`}
      />
    </div>
  )
}

/* ── Grid View ───────────────────────────────────────────────── */
function GridView({ docs, onDelete }) {
  return (
    <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-4">
      {docs.map(doc => (
        <div key={doc.id} className="card group hover:shadow-md transition-shadow">
          <Link to={`/documents/${doc.id}`} className="block p-4">
            <div className="flex justify-center mb-3">
              <FileIcon mimeType={doc.mimeType} fileName={doc.originalFileName} size="lg" />
            </div>
            <p className="text-sm font-medium text-surface-800 truncate text-center" title={doc.title}>{doc.title}</p>
            <p className="text-xs text-surface-400 text-center mt-0.5">{formatFileSize(doc.fileSize)}</p>
            <p className="text-xs text-surface-400 text-center">{timeAgo(doc.createdAt)}</p>
          </Link>
          <div className="flex border-t border-surface-100">
            <button onClick={() => documentsApi.download(doc.id, doc.originalFileName)}
              className="flex-1 flex items-center justify-center p-2.5 text-surface-400 hover:text-primary-600 hover:bg-primary-50 transition-colors">
              <HiOutlineDownload className="w-4 h-4" />
            </button>
            <button onClick={() => onDelete(doc)}
              className="flex-1 flex items-center justify-center p-2.5 text-surface-400 hover:text-red-600 hover:bg-red-50 transition-colors border-l border-surface-100">
              <HiOutlineTrash className="w-4 h-4" />
            </button>
          </div>
        </div>
      ))}
    </div>
  )
}

/* ── List View ───────────────────────────────────────────────── */
function ListView({ docs, onDelete }) {
  return (
    <div className="table-wrapper">
      <table className="table">
        <thead>
          <tr>
            <th>Document</th>
            <th>Owner</th>
            <th>Size</th>
            <th>Status</th>
            <th>Uploaded</th>
            <th className="text-right">Actions</th>
          </tr>
        </thead>
        <tbody>
          {docs.map(doc => (
            <tr key={doc.id}>
              <td>
                <div className="flex items-center gap-3">
                  <FileIcon mimeType={doc.mimeType} fileName={doc.originalFileName} size="sm" />
                  <div>
                    <Link to={`/documents/${doc.id}`} className="text-sm font-medium text-surface-800 hover:text-primary-600 truncate max-w-xs block">
                      {doc.title}
                    </Link>
                    <p className="text-xs text-surface-400">{doc.originalFileName}</p>
                  </div>
                </div>
              </td>
              <td className="text-xs">{doc.owner?.firstName} {doc.owner?.lastName}</td>
              <td className="text-xs">{formatFileSize(doc.fileSize)}</td>
              <td><StatusBadge status={doc.status} /></td>
              <td className="text-xs">{timeAgo(doc.createdAt)}</td>
              <td>
                <div className="flex items-center justify-end gap-1">
                  <Link to={`/documents/${doc.id}`} className="btn-ghost p-1.5 rounded-lg"><HiOutlineEye className="w-4 h-4" /></Link>
                  <button onClick={() => documentsApi.download(doc.id, doc.originalFileName)} className="btn-ghost p-1.5 rounded-lg"><HiOutlineDownload className="w-4 h-4" /></button>
                  <button onClick={() => onDelete(doc)} className="btn-ghost p-1.5 rounded-lg text-red-400 hover:text-red-600 hover:bg-red-50"><HiOutlineTrash className="w-4 h-4" /></button>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
