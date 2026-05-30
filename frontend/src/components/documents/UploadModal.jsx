import { useState, useCallback } from 'react'
import { useDropzone } from 'react-dropzone'
import { useQueryClient } from '@tanstack/react-query'
import { documentsApi } from '@/api/documents'
import { formatFileSize, getErrorMessage } from '@/utils/helpers'
import Modal from '@/components/common/Modal'
import FileIcon from '@/components/common/FileIcon'
import toast from 'react-hot-toast'
import clsx from 'clsx'
import { HiOutlineCloudUpload, HiOutlineX, HiOutlineCheckCircle, HiOutlineExclamation } from 'react-icons/hi'

const MAX_SIZE_MB = 5
const MAX_SIZE_BYTES = MAX_SIZE_MB * 1024 * 1024

// Regex: title must start with a letter, 3–120 chars, alphanumeric + spaces + common punctuation
const TITLE_REGEX = /^[A-Za-z][A-Za-z0-9 _\-().,']{2,119}$/
// Purpose: 10–500 chars
const PURPOSE_MIN = 10
const PURPOSE_MAX = 500

function validateTitle(title) {
  if (!title || !title.trim()) return 'Title is required'
  if (!TITLE_REGEX.test(title.trim())) return 'Title must start with a letter, 3–120 chars, letters/numbers/spaces/-_().,\' only'
  return null
}

function validatePurpose(purpose) {
  if (!purpose || !purpose.trim()) return 'Upload purpose is required'
  if (purpose.trim().length < PURPOSE_MIN) return `Purpose must be at least ${PURPOSE_MIN} characters`
  if (purpose.trim().length > PURPOSE_MAX) return `Purpose must be at most ${PURPOSE_MAX} characters`
  return null
}

export default function UploadModal({ open, onClose }) {
  const qc = useQueryClient()
  const [files,     setFiles]     = useState([])
  const [uploading, setUploading] = useState(false)
  const [progress,  setProgress]  = useState({})
  const [errors,    setErrors]    = useState({})

  const onDrop = useCallback((accepted, rejected) => {
    if (rejected?.length) {
      rejected.forEach(({ file, errors: errs }) => {
        if (errs.some(e => e.code === 'file-too-large')) {
          toast.error(`${file.name} exceeds the ${MAX_SIZE_MB} MB limit`)
        } else {
          toast.error(`${file.name}: ${errs[0]?.message ?? 'Rejected'}`)
        }
      })
    }
    setFiles(prev => [
      ...prev,
      ...accepted.map(f => ({
        file: f,
        title: f.name.replace(/\.[^.]+$/, ''),
        description: '',
        purpose: '',
        tags: '',
        uploadedAt: new Date().toISOString(),
      })),
    ])
  }, [])

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    maxSize: MAX_SIZE_BYTES,
    multiple: true,
  })

  const removeFile = (idx) => {
    setFiles(prev => prev.filter((_, i) => i !== idx))
    setErrors(prev => {
      const next = { ...prev }
      delete next[idx]
      return next
    })
  }

  const updateMeta = (idx, field, val) =>
    setFiles(prev => prev.map((f, i) => i === idx ? { ...f, [field]: val } : f))

  const validateAll = () => {
    const newErrors = {}
    files.forEach(({ title, purpose }, idx) => {
      const titleErr   = validateTitle(title)
      const purposeErr = validatePurpose(purpose)
      if (titleErr || purposeErr) {
        newErrors[idx] = { title: titleErr, purpose: purposeErr }
      }
    })
    setErrors(newErrors)
    return Object.keys(newErrors).length === 0
  }

  const handleUpload = async () => {
    if (!files.length) return
    if (!validateAll()) {
      toast.error('Please fix validation errors before uploading')
      return
    }
    setUploading(true)
    let successCount = 0
    for (const { file, title, description, purpose, tags, uploadedAt } of files) {
      const fd = new FormData()
      fd.append('file',        file)
      fd.append('title',       title || file.name)
      fd.append('description', description)
      fd.append('purpose',     purpose)
      fd.append('tags',        tags)
      fd.append('uploadedAt',  uploadedAt)
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
    setErrors({})
    setUploading(false)
    onClose()
  }

  const handleClose = () => {
    if (!uploading) { setFiles([]); setProgress({}); setErrors({}); onClose() }
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
        isDragActive ? 'border-primary-500 bg-primary-50 dark:bg-primary-900/20' : 'border-surface-200 dark:border-gray-700 hover:border-primary-300 hover:bg-surface-50 dark:hover:bg-gray-800/50'
      )}>
        <input {...getInputProps()} />
        <HiOutlineCloudUpload className={clsx('w-10 h-10 mx-auto mb-3', isDragActive ? 'text-primary-500' : 'text-surface-300 dark:text-gray-600')} />
        <p className="text-sm font-medium text-surface-700 dark:text-gray-300">
          {isDragActive ? 'Drop files here' : 'Drag & drop files, or click to browse'}
        </p>
        <p className="text-xs text-surface-400 dark:text-gray-500 mt-1">
          Any file type · Max <strong>{MAX_SIZE_MB} MB</strong> per file
        </p>
      </div>

      {/* File list */}
      {files.length > 0 && (
        <div className="space-y-4 max-h-80 overflow-y-auto pr-1">
          {files.map(({ file, title, description, purpose, tags, uploadedAt }, idx) => {
            const pct = progress[file.name]
            const done = pct === 100
            const fileErrors = errors[idx] || {}
            return (
              <div key={idx} className={clsx(
                'rounded-xl border p-3',
                Object.keys(fileErrors).length > 0
                  ? 'border-red-300 dark:border-red-700 bg-red-50/30 dark:bg-red-900/10'
                  : 'border-surface-200 dark:border-gray-700'
              )}>
                <div className="flex items-start gap-3 mb-3">
                  <FileIcon mimeType={file.type} fileName={file.name} size="sm" />
                  <div className="flex-1 min-w-0">
                    <p className="text-xs text-surface-400 dark:text-gray-500 truncate">
                      {file.name} · {formatFileSize(file.size)}
                    </p>
                    <p className="text-[10px] text-surface-300 dark:text-gray-600 mt-0.5">
                      Queued: {new Date(uploadedAt).toLocaleString()}
                    </p>
                  </div>
                  {!uploading && (
                    <button onClick={() => removeFile(idx)} className="btn-ghost p-1 rounded-lg flex-shrink-0">
                      <HiOutlineX className="w-4 h-4" />
                    </button>
                  )}
                  {done && <HiOutlineCheckCircle className="w-5 h-5 text-green-500 flex-shrink-0" />}
                </div>

                {/* Title */}
                <div className="mb-2">
                  <input
                    value={title}
                    onChange={(e) => updateMeta(idx, 'title', e.target.value)}
                    placeholder="Document title *"
                    className={clsx('input text-xs py-1.5', fileErrors.title && 'border-red-400 focus:ring-red-400')}
                  />
                  {fileErrors.title && (
                    <p className="mt-0.5 text-[10px] text-red-600 flex items-center gap-1">
                      <HiOutlineExclamation className="w-3 h-3" /> {fileErrors.title}
                    </p>
                  )}
                </div>

                {/* Description */}
                <div className="mb-2">
                  <input
                    value={description}
                    onChange={(e) => updateMeta(idx, 'description', e.target.value)}
                    placeholder="Description (optional)"
                    className="input text-xs py-1.5"
                  />
                </div>

                {/* Purpose (required) */}
                <div className="mb-2">
                  <textarea
                    value={purpose}
                    onChange={(e) => updateMeta(idx, 'purpose', e.target.value)}
                    placeholder="Purpose of uploading this document * (min 10 chars)"
                    rows={2}
                    className={clsx('input text-xs py-1.5 resize-none', fileErrors.purpose && 'border-red-400 focus:ring-red-400')}
                  />
                  <div className="flex items-center justify-between mt-0.5">
                    {fileErrors.purpose
                      ? <p className="text-[10px] text-red-600 flex items-center gap-1"><HiOutlineExclamation className="w-3 h-3" /> {fileErrors.purpose}</p>
                      : <span />
                    }
                    <p className="text-[10px] text-surface-400 dark:text-gray-600">
                      {purpose.length}/{PURPOSE_MAX}
                    </p>
                  </div>
                </div>

                {/* Tags */}
                <input
                  value={tags}
                  onChange={(e) => updateMeta(idx, 'tags', e.target.value)}
                  placeholder="Tags (comma separated)"
                  className="input text-xs py-1.5"
                />

                {pct !== undefined && (
                  <div className="h-1.5 bg-surface-100 dark:bg-gray-800 rounded-full overflow-hidden mt-2">
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
