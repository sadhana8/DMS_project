import { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { profileChangesApi } from '@/api/profileChanges'
import Spinner from '@/components/common/Spinner'
import toast from 'react-hot-toast'
import clsx from 'clsx'
import { HiOutlineDocumentText, HiOutlineCheck, HiOutlineX } from 'react-icons/hi'

const STATUSES = ['PENDING', 'APPROVED', 'REJECTED']

/**
 * Admin/HR review queue for employee profile-change requests.
 *
 * - Defaults to PENDING (the things needing action)
 * - Tabs to view APPROVED / REJECTED history
 * - Each pending row has Approve / Reject buttons
 * - Reject opens a small note prompt
 */
export default function ChangeRequestsPage() {
  const qc = useQueryClient()
  const [status, setStatus] = useState('PENDING')
  const [reviewing, setReviewing] = useState(null)   // request being acted on
  const [note, setNote] = useState('')
  const [busy, setBusy] = useState(false)

  const { data, isLoading } = useQuery({
    queryKey: ['change-requests', status],
    queryFn: () => profileChangesApi.listForReview({ status, page: 0, size: 20 }),
  })

  const review = async (request, approve) => {
    if (!approve && (!note || note.trim().length < 3)) {
      toast.error('Please provide a short note explaining the rejection')
      return
    }
    setBusy(true)
    try {
      await profileChangesApi.review(request.id, { approve, note: note.trim() || null })
      toast.success(approve ? 'Request approved' : 'Request rejected')
      qc.invalidateQueries({ queryKey: ['change-requests'] })
      qc.invalidateQueries({ queryKey: ['change-requests-count'] })
      setReviewing(null)
      setNote('')
    } catch (e) {
      toast.error(e?.response?.data?.message || 'Failed to record review')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="animate-fade-in">
      <div className="page-header mb-5">
        <div>
          <h1 className="page-title flex items-center gap-2">
            <HiOutlineDocumentText className="w-6 h-6" /> Profile Change Requests
          </h1>
          <p className="page-subtitle">Review changes employees have requested to their profile</p>
        </div>
      </div>

      {/* Status tabs */}
      <div className="flex border-b border-surface-200 mb-4">
        {STATUSES.map(s => (
          <button key={s} onClick={() => setStatus(s)}
            className={clsx('px-4 py-2 text-sm font-medium border-b-2 -mb-px transition-colors',
              status === s
                ? 'border-primary-600 text-primary-700'
                : 'border-transparent text-surface-500 hover:text-surface-800')}>
            {s.charAt(0) + s.slice(1).toLowerCase()}
          </button>
        ))}
      </div>

      {/* List */}
      <div className="card overflow-hidden">
        {isLoading ? (
          <div className="flex justify-center py-12"><Spinner /></div>
        ) : !data || data.content.length === 0 ? (
          <div className="py-16 text-center">
            <HiOutlineDocumentText className="w-10 h-10 text-surface-300 mx-auto mb-2" />
            <p className="text-sm text-surface-500">No {status.toLowerCase()} requests</p>
          </div>
        ) : (
          <div className="divide-y divide-surface-100">
            {data.content.map(r => (
              <div key={r.id} className="px-5 py-4">
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-medium text-surface-900">
                      {r.userFirstName} {r.userLastName}{' '}
                      <span className="text-surface-400 font-normal text-xs">· {r.userEmail}</span>
                    </p>
                    <p className="text-sm text-surface-700 mt-1">
                      Wants to change <strong>{r.fieldName === 'phoneNumber' ? 'phone number' : 'address'}</strong>
                    </p>
                    <div className="mt-2 grid grid-cols-1 md:grid-cols-2 gap-2 text-xs">
                      <div className="px-3 py-2 rounded-lg bg-surface-50 border border-surface-100">
                        <p className="text-surface-400 mb-0.5">Current</p>
                        <p className="text-surface-700 break-words">{r.oldValue || '(empty)'}</p>
                      </div>
                      <div className="px-3 py-2 rounded-lg bg-blue-50 border border-blue-100">
                        <p className="text-blue-500 mb-0.5">Requested</p>
                        <p className="text-blue-900 font-medium break-words">{r.newValue}</p>
                      </div>
                    </div>
                    {r.reason && (
                      <p className="text-xs text-surface-500 mt-2 italic">"{r.reason}"</p>
                    )}
                    {r.reviewNote && (
                      <p className="text-xs text-surface-500 mt-1">
                        <span className="text-surface-400">Reviewer note:</span> {r.reviewNote}
                      </p>
                    )}
                    <p className="text-xs text-surface-400 mt-2">
                      Submitted {new Date(r.createdAt).toLocaleString()}
                      {r.reviewedBy && <> · Reviewed by {r.reviewedBy} · {new Date(r.reviewedAt).toLocaleString()}</>}
                    </p>
                  </div>
                  <div className="flex flex-col items-end gap-2 flex-shrink-0">
                    <span className={clsx('badge',
                      r.status === 'APPROVED' && 'badge-green',
                      r.status === 'REJECTED' && 'badge-red',
                      r.status === 'PENDING'  && 'badge-amber')}>
                      {r.status}
                    </span>
                    {r.status === 'PENDING' && (
                      <div className="flex gap-2">
                        <button onClick={() => { setReviewing({ ...r, action: 'approve' }); setNote('') }}
                          className="btn-sm gap-1.5 bg-green-600 text-white hover:bg-green-700"
                          title="Approve">
                          <HiOutlineCheck className="w-4 h-4" /> Approve
                        </button>
                        <button onClick={() => { setReviewing({ ...r, action: 'reject' }); setNote('') }}
                          className="btn-sm gap-1.5 bg-red-600 text-white hover:bg-red-700"
                          title="Reject">
                          <HiOutlineX className="w-4 h-4" /> Reject
                        </button>
                      </div>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Review modal */}
      {reviewing && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-black/50" onClick={() => !busy && setReviewing(null)} />
          <div className="relative bg-white rounded-2xl shadow-modal p-6 w-full max-w-md animate-slide-up">
            <h2 className="text-lg font-semibold mb-1">
              {reviewing.action === 'approve' ? 'Approve' : 'Reject'} change request
            </h2>
            <p className="text-sm text-surface-500 mb-4">
              <strong>{reviewing.userFirstName} {reviewing.userLastName}</strong> wants to change their{' '}
              {reviewing.fieldName === 'phoneNumber' ? 'phone number' : 'address'} to{' '}
              <strong>{reviewing.newValue}</strong>.
              {reviewing.action === 'approve' && ' This will update their profile immediately.'}
            </p>
            <label className="label">
              {reviewing.action === 'approve' ? 'Note ' : 'Reason for rejection '}
              <span className={reviewing.action === 'approve' ? 'text-surface-400 font-normal' : 'text-red-500'}>
                {reviewing.action === 'approve' ? '(optional)' : '*'}
              </span>
            </label>
            <textarea value={note} onChange={e => setNote(e.target.value)}
              rows={3} className="input mb-5" autoFocus
              placeholder={reviewing.action === 'approve' ? 'Anything to add' : 'Explain to the employee why'} />
            <div className="flex gap-3 justify-end">
              <button className="btn-secondary" onClick={() => setReviewing(null)} disabled={busy}>Cancel</button>
              <button onClick={() => review(reviewing, reviewing.action === 'approve')}
                disabled={busy}
                className={clsx('btn',
                  reviewing.action === 'approve'
                    ? 'bg-green-600 text-white hover:bg-green-700'
                    : 'bg-red-600 text-white hover:bg-red-700')}>
                {busy
                  ? (reviewing.action === 'approve' ? 'Approving…' : 'Rejecting…')
                  : (reviewing.action === 'approve' ? 'Approve' : 'Reject')}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}