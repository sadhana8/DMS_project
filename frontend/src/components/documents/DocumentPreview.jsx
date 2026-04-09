import { useState, useEffect } from 'react'
import { documentsApi } from '@/api/documents'
import Spinner from '@/components/common/Spinner'
import FileIcon from '@/components/common/FileIcon'
import { formatFileSize } from '@/utils/helpers'
import { HiOutlineDownload, HiOutlineExternalLink } from 'react-icons/hi'

export default function DocumentPreview({ document }) {
  const [url,     setUrl]     = useState(null)
  const [loading, setLoading] = useState(false)
  const [error,   setError]   = useState(null)

  const mime = document?.mimeType ?? ''
  const isPdf   = mime === 'application/pdf'
  const isImage = mime.startsWith('image/')
  const isText  = mime.startsWith('text/')
  const canPreview = isPdf || isImage || isText

  useEffect(() => {
    if (!canPreview || !document?.id) return
    let objectUrl
    setLoading(true)
    setError(null)
    documentsApi.preview(document.id)
      .then((blob) => {
        objectUrl = URL.createObjectURL(blob)
        setUrl(objectUrl)
      })
      .catch(() => setError('Could not load preview'))
      .finally(() => setLoading(false))
    return () => { if (objectUrl) URL.revokeObjectURL(objectUrl) }
  }, [document?.id, canPreview])

  if (!canPreview) {
    return (
      <div className="flex flex-col items-center justify-center py-16 text-center bg-surface-50 rounded-xl">
        <FileIcon mimeType={mime} fileName={document?.originalFileName} size="lg" />
        <p className="text-sm font-medium text-surface-700 mt-4">{document?.originalFileName}</p>
        <p className="text-xs text-surface-400 mt-1">{formatFileSize(document?.fileSize)} · Preview not available for this file type</p>
        <button
          onClick={() => documentsApi.download(document.id, document.originalFileName)}
          className="btn-primary btn-sm mt-5 gap-1.5"
        >
          <HiOutlineDownload className="w-4 h-4" /> Download to view
        </button>
      </div>
    )
  }

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center py-16 bg-surface-50 rounded-xl gap-3">
        <Spinner size="lg" />
        <p className="text-sm text-surface-400">Loading preview…</p>
      </div>
    )
  }

  if (error) {
    return (
      <div className="flex flex-col items-center justify-center py-16 bg-surface-50 rounded-xl gap-3">
        <p className="text-sm text-red-500">{error}</p>
        <button onClick={() => documentsApi.download(document.id, document.originalFileName)} className="btn-secondary btn-sm">
          <HiOutlineDownload className="w-4 h-4" /> Download instead
        </button>
      </div>
    )
  }

  return (
    <div className="rounded-xl overflow-hidden border border-surface-200 bg-surface-50">
      {isPdf && (
        <iframe
          src={`${url}#toolbar=1&navpanes=0`}
          title={document.title}
          className="w-full"
          style={{ height: '70vh' }}
        />
      )}
      {isImage && (
        <div className="flex items-center justify-center p-4 min-h-64">
          <img src={url} alt={document.title} className="max-w-full max-h-96 rounded-lg object-contain" />
        </div>
      )}
      {isText && (
        <TextPreview url={url} />
      )}
    </div>
  )
}

function TextPreview({ url }) {
  const [text, setText] = useState('')
  useEffect(() => {
    fetch(url).then(r => r.text()).then(setText)
  }, [url])
  return (
    <pre className="p-5 text-xs text-surface-700 font-mono overflow-auto max-h-96 whitespace-pre-wrap break-all leading-relaxed">
      {text}
    </pre>
  )
}
