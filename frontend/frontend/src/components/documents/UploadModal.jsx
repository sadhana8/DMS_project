import { useState, useCallback } from 'react'
import { useDropzone } from 'react-dropzone'
import { useQueryClient } from '@tanstack/react-query'
import { documentsApi } from '@/api/documents'
import { formatFileSize, getErrorMessage } from '@/utils/helpers'
import Modal from '@/components/common/Modal'
import FileIcon from '@/components/common/FileIcon'
import toast from 'react-hot-toast'
import clsx from 'clsx'
import { HiOutlineCloudUpload, HiOutlineX, HiOutlineCheckCircle } from 'react-icons/hi'

export default function UploadModal({ open, onClose }) {
  const qc = useQueryClient()
  const [files,     setFiles]     = useState([])
  const [uploading, setUploading] = useState(false)
  const [progress,  setProgress]  = useState({})  // { filename: % }

  const onDrop = useCallback((accepted) => {
    setFiles(prev => [...prev, ...accepted.map(f => ({ file: f, title: f.name.replace(/\.[^.]+$/, ''), tags: '' }))])
  }, [])

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    maxSize: 50 * 1024 * 1024,
    multiple: true,
  })

  const removeFile = (idx) => setFiles(prev => prev.filter((_, i) => i !== idx))

  const updateMeta = (idx, field, val) =>
    setFiles(prev => prev.map((f, i) => i === idx ? { ...f, [field]: val } : f))

  const handleUpload = async () => {
    if (!files.length) return
    setUploading(true)
    let successCount = 0
    for (const { file, title, tags } of files) {
      const fd = new FormData()
      fd.append('file',  file)
      fd.append('title', title || file.name)
      fd.append('tags',  tags)
      try {
        await documentsApi.upload(fd, (pct) =>
          setProgress(prev => ({ ...prev, [file.name]: pct }))
        )
        successCount++
      } catch (e) {
        toast.error(`Failed to upload ${file.name}: ${getErrorMessage(e)}`)
      }
    }
    if (successCount > 0) {
      toast.success(`${successCount} file${successCount > 1 ? 's' : ''} uploaded!`)
      qc.invalidateQueries({ queryKey: ['documents'] })
      qc.invalidateQueries({ queryKey: ['dashboard-stats'] })
    }
    setFiles([])
    setProgress({})
    setUploading(false)
    onClose()
  }

  const handleClose = () => {
    if (!uploading) { setFiles([]); setProgress({}); onClose() }
  }

  return (
    <Modal open={open} onClose={handleClose} title="Upload Documents" size="lg"
      footer={<>
        <button className="btn-secondary" onClick={handleClose} disabled={uploading}>Cancel</button>
        <button className="btn-primary" onClick={handleUpload} disabled={!files.length || uploading}>
          {uploading ? 'Uploading…' : `Upload ${files.length ? `${files.length} file${files.length > 1 ? 's' : ''}` : ''}`}
        </button>
      </>}>

      {/* Drop zone */}
      <div {...getRootProps()} className={clsx(
        'border-2 border-dashed rounded-xl p-8 text-center cursor-pointer transition-all duration-150 mb-4',
        isDragActive ? 'border-primary-500 bg-primary-50' : 'border-surface-200 hover:border-primary-300 hover:bg-surface-50'
      )}>
        <input {...getInputProps()} />
        <HiOutlineCloudUpload className={clsx('w-10 h-10 mx-auto mb-3', isDragActive ? 'text-primary-500' : 'text-surface-300')} />
        <p className="text-sm font-medium text-surface-700">
          {isDragActive ? 'Drop files here' : 'Drag & drop files, or click to browse'}
        </p>
        <p className="text-xs text-surface-400 mt-1">Any file type · Max 50 MB per file</p>
      </div>

      {/* File list */}
      {files.length > 0 && (
        <div className="space-y-3 max-h-72 overflow-y-auto pr-1">
          {files.map(({ file, title, tags }, idx) => {
            const pct = progress[file.name]
            const done = pct === 100
            return (
              <div key={idx} className="rounded-xl border border-surface-200 p-3">
                <div className="flex items-start gap-3 mb-2">
                  <FileIcon mimeType={file.type} fileName={file.name} size="sm" />
                  <div className="flex-1 min-w-0">
                    <p className="text-xs text-surface-400 truncate">{file.name} · {formatFileSize(file.size)}</p>
                    <input
                      value={title}
                      onChange={(e) => updateMeta(idx, 'title', e.target.value)}
                      placeholder="Document title"
                      className="input mt-1 text-xs py-1"
                    />
                    <input
                      value={tags}
                      onChange={(e) => updateMeta(idx, 'tags', e.target.value)}
                      placeholder="Tags (comma separated)"
                      className="input mt-1 text-xs py-1"
                    />
                  </div>
                  {!uploading && (
                    <button onClick={() => removeFile(idx)} className="btn-ghost p-1 rounded-lg flex-shrink-0">
                      <HiOutlineX className="w-4 h-4" />
                    </button>
                  )}
                  {done && <HiOutlineCheckCircle className="w-5 h-5 text-green-500 flex-shrink-0" />}
                </div>
                {pct !== undefined && (
                  <div className="h-1.5 bg-surface-100 rounded-full overflow-hidden">
                    <div className={clsx('h-full rounded-full transition-all duration-300', done ? 'bg-green-500' : 'bg-primary-500')}
                      style={{ width: `${pct}%` }} />
                  </div>
                )}
              </div>
            )
          })}
        </div>
      )}
    </Modal>
  )
}
