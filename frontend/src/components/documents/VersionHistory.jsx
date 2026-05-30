import { useRef } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { documentsApi } from '@/api/documents'
import { formatFileSize, formatDateTime, getErrorMessage } from '@/utils/helpers'
import { useAuth } from '@/context/AuthContext'
import Spinner from '@/components/common/Spinner'
import { Avatar } from '@/components/common/index'
import toast from 'react-hot-toast'
import { HiOutlineDownload, HiOutlineRefresh, HiOutlineCloudUpload } from 'react-icons/hi'

export default function VersionHistory({ document }) {
  const { isEditor } = useAuth()
  const qc = useQueryClient()
  const fileRef = useRef()

  const { data: versions = [], isLoading } = useQuery({
    queryKey: ['doc-versions', document?.id],
    queryFn:  () => documentsApi.getVersions(document.id),
    enabled:  !!document?.id,
  })

  const uploadNewVersion = async (e) => {
    const file = e.target.files?.[0]
    if (!file) return
    const changeSummary = prompt('Describe what changed in this version:') ?? ''
    const fd = new FormData()
    fd.append('file', file)
    try {
      await documentsApi.uploadVersion(document.id, fd, changeSummary)
      qc.invalidateQueries({ queryKey: ['doc-versions', document.id] })
      qc.invalidateQueries({ queryKey: ['document', document.id] })
      toast.success('New version uploaded!')
    } catch (e) { toast.error(getErrorMessage(e)) }
    e.target.value = ''
  }

  const restore = async (versionId) => {
    if (!confirm('Restore this version? The current version will be saved.')) return
    try {
      await documentsApi.restoreVersion(document.id, versionId)
      qc.invalidateQueries({ queryKey: ['doc-versions', document.id] })
      qc.invalidateQueries({ queryKey: ['document', document.id] })
      toast.success('Version restored!')
    } catch (e) { toast.error(getErrorMessage(e)) }
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-sm font-semibold text-surface-800 dark:text-gray-200">Version History</h3>
        {isEditor() && (
          <>
            <button onClick={() => fileRef.current?.click()} className="btn-secondary btn-sm gap-1.5">
              <HiOutlineCloudUpload className="w-4 h-4" /> New version
            </button>
            <input ref={fileRef} type="file" className="hidden" onChange={uploadNewVersion} />
          </>
        )}
      </div>

      {isLoading ? (
        <div className="flex justify-center py-6"><Spinner /></div>
      ) : (
        <div className="space-y-2">
          {versions.map((v) => (
            <div key={v.id} className="flex items-start gap-3 p-3.5 rounded-xl bg-surface-50 dark:bg-gray-800 border border-surface-100 dark:border-gray-700">
              <div className="w-8 h-8 rounded-lg bg-primary-100 dark:bg-primary-900/40 flex items-center justify-center text-primary-700 dark:text-primary-400 text-xs font-bold flex-shrink-0">
                v{v.versionNumber}
              </div>
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2">
                  <p className="text-sm font-medium text-surface-800 dark:text-gray-200">Version {v.versionNumber}</p>
                  {v.versionNumber === document.currentVersion && (
                    <span className="badge-green text-xs">Current</span>
                  )}
                </div>
                {v.changeSummary && <p className="text-xs text-surface-500 dark:text-gray-400 mt-0.5">{v.changeSummary}</p>}
                <div className="flex items-center gap-2 mt-1">
                  <Avatar user={v.uploadedBy} size="sm" />
                  <p className="text-xs text-surface-400 dark:text-gray-500">
                    {v.uploadedBy?.firstName} · {formatDateTime(v.createdAt)} · {formatFileSize(v.fileSize)}
                  </p>
                </div>
              </div>
              <div className="flex gap-1 flex-shrink-0">
                <button onClick={() => documentsApi.downloadVersion(document.id, v.id, v.fileName)}
                  className="btn-ghost p-1.5 rounded-lg" title="Download">
                  <HiOutlineDownload className="w-4 h-4" />
                </button>
                {isEditor() && v.versionNumber !== document.currentVersion && (
                  <button onClick={() => restore(v.id)} className="btn-ghost p-1.5 rounded-lg" title="Restore">
                    <HiOutlineRefresh className="w-4 h-4" />
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
