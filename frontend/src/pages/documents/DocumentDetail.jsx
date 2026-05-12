import { useState } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { documentsApi } from '@/api/documents'
import { useAuth } from '@/context/AuthContext'
import { formatFileSize, formatDateTime, timeAgo, getRoleBadge, getErrorMessage } from '@/utils/helpers'
import FileIcon from '@/components/common/FileIcon'
import Spinner from '@/components/common/Spinner'
import { ConfirmDialog, StatusBadge, Avatar } from '@/components/common/index'
import DocumentPreview from '@/components/documents/DocumentPreview'
import VersionHistory from '@/components/documents/VersionHistory'
import ShareModal from '@/components/documents/ShareModal'
import toast from 'react-hot-toast'
import clsx from 'clsx'
import {
  HiOutlineArrowLeft, HiOutlineDownload, HiOutlineTrash, HiOutlineShare,
  HiOutlinePencil, HiOutlineClock, HiOutlineTag, HiOutlineUser,
  HiOutlineDocumentText, HiOutlineCheckCircle,
} from 'react-icons/hi'

const TABS = ['Preview','Versions','Details']

export default function DocumentDetail() {
  const { id } = useParams()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const { isEditor, isAdmin } = useAuth()

  const [tab,        setTab]       = useState('Preview')
  const [shareOpen,  setShareOpen] = useState(false)
  const [deleteOpen, setDeleteOpen]= useState(false)
  const [deleting,   setDeleting]  = useState(false)
  const [editing,    setEditing]   = useState(false)
  const [editTitle,  setEditTitle] = useState('')
  const [editDesc,   setEditDesc]  = useState('')

  const { data: doc, isLoading, isError } = useQuery({
    queryKey: ['document', id],
    queryFn:  () => documentsApi.get(id),
  })

  const startEdit = () => { setEditTitle(doc.title); setEditDesc(doc.description ?? ''); setEditing(true) }
  const saveEdit  = async () => {
    try {
      await documentsApi.update(id, { title: editTitle, description: editDesc })
      qc.invalidateQueries({ queryKey: ['document', id] })
      toast.success('Document updated')
      setEditing(false)
    } catch (e) { toast.error(getErrorMessage(e)) }
  }

  const handleDelete = async () => {
    setDeleting(true)
    try {
      await documentsApi.delete(id)
      qc.invalidateQueries({ queryKey: ['documents'] })
      toast.success('Document moved to archive')
      navigate('/documents')
    } catch (e) { toast.error(getErrorMessage(e)) }
    finally { setDeleting(false) }
  }

  if (isLoading) return <div className="flex justify-center py-20"><Spinner size="lg" /></div>
  if (isError || !doc) return (
    <div className="text-center py-20">
      <p className="text-red-500 mb-4">Document not found</p>
      <Link to="/documents" className="btn-secondary">← Back</Link>
    </div>
  )

  return (
    <div className="animate-fade-in max-w-6xl mx-auto">
      {/* Breadcrumb */}
      <div className="flex items-center gap-2 text-sm text-surface-500 mb-4">
        <Link to="/documents" className="hover:text-surface-800 flex items-center gap-1">
          <HiOutlineArrowLeft className="w-4 h-4" /> Documents
        </Link>
        <span>/</span>
        <span className="text-surface-800 font-medium truncate max-w-xs">{doc.title}</span>
      </div>

      {/* Header */}
      <div className="card p-5 mb-5">
        <div className="flex items-start gap-4">
          <FileIcon mimeType={doc.mimeType} fileName={doc.originalFileName} size="lg" />
          <div className="flex-1 min-w-0">
            {editing ? (
              <div className="space-y-2">
                <input value={editTitle} onChange={(e) => setEditTitle(e.target.value)} className="input text-lg font-semibold" />
                <textarea value={editDesc} onChange={(e) => setEditDesc(e.target.value)} rows={2} placeholder="Description…" className="input resize-none" />
                <div className="flex gap-2">
                  <button onClick={saveEdit} className="btn-primary btn-sm gap-1"><HiOutlineCheckCircle className="w-4 h-4" /> Save</button>
                  <button onClick={() => setEditing(false)} className="btn-secondary btn-sm">Cancel</button>
                </div>
              </div>
            ) : (
              <>
                <h1 className="text-xl font-semibold text-surface-900 truncate">{doc.title}</h1>
                {doc.description && <p className="text-sm text-surface-500 mt-1">{doc.description}</p>}
                <div className="flex flex-wrap items-center gap-3 mt-2">
                  <StatusBadge status={doc.status} />
                  <span className="text-xs text-surface-400">v{doc.currentVersion}</span>
                  <span className="text-xs text-surface-400">{formatFileSize(doc.fileSize)}</span>
                  <span className="text-xs text-surface-400">{timeAgo(doc.createdAt)}</span>
                </div>
              </>
            )}
          </div>

          {/* Actions */}
          {!editing && (
            <div className="flex items-center gap-2 flex-shrink-0">
              <button onClick={() => documentsApi.download(doc.id, doc.originalFileName)} className="btn-secondary btn-sm gap-1.5">
                <HiOutlineDownload className="w-4 h-4" /> Download
              </button>
              {isEditor() && (
                <>
                  <button onClick={() => setShareOpen(true)} className="btn-secondary btn-sm gap-1.5">
                    <HiOutlineShare className="w-4 h-4" /> Share
                  </button>
                  <button onClick={startEdit} className="btn-ghost p-2 rounded-lg"><HiOutlinePencil className="w-4 h-4" /></button>
                  <button onClick={() => setDeleteOpen(true)} className="btn-ghost p-2 rounded-lg text-red-400 hover:text-red-600 hover:bg-red-50">
                    <HiOutlineTrash className="w-4 h-4" />
                  </button>
                </>
              )}
            </div>
          )}
        </div>
      </div>

      {/* Tabs */}
      <div className="flex gap-1 border-b border-surface-200 mb-5">
        {TABS.map((t) => (
          <button key={t} onClick={() => setTab(t)}
            className={clsx('px-4 py-2.5 text-sm font-medium border-b-2 -mb-px transition-colors',
              tab === t ? 'border-primary-600 text-primary-700' : 'border-transparent text-surface-500 hover:text-surface-800')}>
            {t}
          </button>
        ))}
      </div>

      {/* Tab content */}
      {tab === 'Preview' && <DocumentPreview document={doc} />}
      {tab === 'Versions' && <VersionHistory document={doc} />}
      {tab === 'Details' && <DetailsTab doc={doc} />}

      <ShareModal open={shareOpen} onClose={() => setShareOpen(false)} document={doc} />
      <ConfirmDialog
        open={deleteOpen} onClose={() => setDeleteOpen(false)}
        onConfirm={handleDelete} loading={deleting}
        title="Delete document"
        message={`Are you sure you want to delete "${doc.title}"? This action cannot be undone.`}
      />
    </div>
  )
}

function DetailsTab({ doc }) {
  const rows = [
    { label: 'Owner',       value: `${doc.owner?.firstName} ${doc.owner?.lastName}`, icon: HiOutlineUser },
    { label: 'File name',   value: doc.originalFileName,                              icon: HiOutlineDocumentText },
    { label: 'File size',   value: formatFileSize(doc.fileSize),                      icon: HiOutlineDocumentText },
    { label: 'MIME type',   value: doc.mimeType,                                      icon: HiOutlineDocumentText },
    { label: 'Uploaded',    value: formatDateTime(doc.createdAt),                     icon: HiOutlineClock },
    { label: 'Last updated',value: formatDateTime(doc.updatedAt),                     icon: HiOutlineClock },
    { label: 'Tags',        value: doc.tags || '—',                                   icon: HiOutlineTag },
    { label: 'Downloads',   value: doc.downloadCount ?? 0,                            icon: HiOutlineDownload },
    { label: 'Views',       value: doc.viewCount ?? 0,                                icon: HiOutlineDocumentText },
  ]
  return (
    <div className="card">
      <dl className="divide-y divide-surface-100">
        {rows.map(({ label, value, icon: Icon }) => (
          <div key={label} className="flex items-center gap-4 px-5 py-3.5">
            <Icon className="w-4 h-4 text-surface-400 flex-shrink-0" />
            <dt className="text-sm text-surface-500 w-32 flex-shrink-0">{label}</dt>
            <dd className="text-sm text-surface-800 font-medium flex-1">{value}</dd>
          </div>
        ))}
      </dl>
    </div>
  )
}
