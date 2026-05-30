import { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { approvalsApi } from '@/api/approvals'
import { timeAgo, getErrorMessage } from '@/utils/helpers'
import Spinner from '@/components/common/Spinner'
import { Pagination } from '@/components/common/index'
import Modal from '@/components/common/Modal'
import toast from 'react-hot-toast'
import clsx from 'clsx'
import { HiOutlineUserGroup, HiOutlineCheck, HiOutlineX, HiOutlineClock } from 'react-icons/hi'

const STATUS_STYLES = { PENDING:'badge-yellow', APPROVED:'badge-green', REJECTED:'badge-red' }

export default function ApprovalsPage() {
  const qc = useQueryClient()
  const [page,       setPage]       = useState(1)
  const [statusTab,  setStatusTab]  = useState('PENDING')
  const [reviewItem, setReviewItem] = useState(null)
  const [note,       setNote]       = useState('')
  const [loading,    setLoading]    = useState(false)

  const { data, isLoading } = useQuery({
    queryKey: ['approvals', page, statusTab],
    queryFn:  () => approvalsApi.list({ status: statusTab, page: page - 1, size: 20 }),
  })
  const { data: pendingCount = 0 } = useQuery({ queryKey: ['approval-count'], queryFn: approvalsApi.count })

  const approvals  = data?.content ?? []
  const totalPages = data?.totalPages ?? 1

  const handleReview = async (status) => {
    setLoading(true)
    try {
      await approvalsApi.review(reviewItem.id, { status, note })
      qc.invalidateQueries({ queryKey: ['approvals'] })
      qc.invalidateQueries({ queryKey: ['approval-count'] })
      toast.success(`User ${status === 'APPROVED' ? 'approved' : 'rejected'} successfully`)
      setReviewItem(null); setNote('')
    } catch (e) { toast.error(getErrorMessage(e)) }
    finally { setLoading(false) }
  }

  return (
    <div className="animate-fade-in">
      <div className="page-header mb-5">
        <div>
          <div className="flex items-center gap-2 mb-1"><HiOutlineUserGroup className="w-5 h-5 text-primary-600" /><h1 className="page-title">User Approvals</h1></div>
          <p className="page-subtitle">{pendingCount} pending approval{pendingCount !== 1 ? 's' : ''}</p>
        </div>
      </div>

      {/* Status tabs */}
      <div className="flex gap-1 border-b border-surface-200 dark:border-gray-700 mb-5">
        {['PENDING','APPROVED','REJECTED'].map(s => (
          <button key={s} onClick={() => { setStatusTab(s); setPage(1) }}
            className={clsx('px-4 py-2.5 text-sm font-medium border-b-2 -mb-px transition-colors',
              statusTab === s ? 'border-primary-600 text-primary-700 dark:text-primary-400' : 'border-transparent text-surface-500 dark:text-gray-400 hover:text-surface-800 dark:hover:text-gray-100')}>
            {s} {s === 'PENDING' && pendingCount > 0 && <span className="ml-1.5 px-1.5 py-0.5 rounded-full bg-amber-100 dark:bg-amber-900/30 text-amber-800 dark:text-amber-300 text-xs font-bold">{pendingCount}</span>}
          </button>
        ))}
      </div>

      {isLoading ? (
        <div className="flex justify-center py-16"><Spinner size="lg" /></div>
      ) : approvals.length === 0 ? (
        <div className="text-center py-16"><HiOutlineClock className="w-12 h-12 mx-auto mb-3 text-surface-300 dark:text-gray-600" /><p className="text-surface-400 dark:text-gray-500">No {statusTab.toLowerCase()} approvals</p></div>
      ) : (
        <div className="table-wrapper">
          <table className="table">
            <thead><tr><th>User</th><th>Registered</th><th>Status</th>{statusTab !== 'PENDING' && <th>Reviewed by</th>}{statusTab !== 'PENDING' && <th>Note</th>}{statusTab !== 'PENDING' && <th>Reviewed</th>}{statusTab === 'PENDING' && <th className="text-right">Actions</th>}</tr></thead>
            <tbody>
              {approvals.map(a => (
                <tr key={a.id}>
                  <td>
                    <div className="flex items-center gap-3">
                      <div className="w-8 h-8 rounded-full bg-primary-100 dark:bg-primary-900/40 text-primary-700 dark:text-primary-400 flex items-center justify-center text-xs font-bold flex-shrink-0">
                        {a.user?.firstName?.charAt(0)}{a.user?.lastName?.charAt(0)}
                      </div>
                      <div>
                        <p className="text-sm font-medium text-surface-800 dark:text-gray-200 dark:text-gray-200">{a.user?.firstName} {a.user?.lastName}</p>
                        <p className="text-xs text-surface-400 dark:text-gray-500 dark:text-gray-500">{a.user?.email}</p>
                      </div>
                    </div>
                  </td>
                  <td className="text-xs text-surface-500 dark:text-gray-400">{timeAgo(a.createdAt)}</td>
                  <td><span className={clsx('badge', STATUS_STYLES[a.status] ?? 'badge-gray')}>{a.status}</span></td>
                  {statusTab !== 'PENDING' && <td className="text-xs text-surface-600 dark:text-gray-300">{a.reviewedBy ?? '—'}</td>}
                  {statusTab !== 'PENDING' && <td className="text-xs text-surface-500 dark:text-gray-400 max-w-xs truncate">{a.reviewNote ?? '—'}</td>}
                  {statusTab !== 'PENDING' && <td className="text-xs text-surface-400 dark:text-gray-500 dark:text-gray-500">{a.reviewedAt ? timeAgo(a.reviewedAt) : '—'}</td>}
                  {statusTab === 'PENDING' && (
                    <td>
                      <div className="flex items-center justify-end gap-1">
                        <button onClick={() => { setReviewItem(a); setNote('') }}
                          className="btn-sm gap-1.5 bg-green-600 text-white hover:bg-green-700 focus-visible:ring-green-500 shadow-sm">
                          <HiOutlineCheck className="w-3.5 h-3.5" /> Review
                        </button>
                      </div>
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      <Pagination page={page} totalPages={totalPages} onChange={setPage} />

      {/* Review modal */}
      <Modal open={!!reviewItem} onClose={() => setReviewItem(null)} title="Review Registration"
        footer={<>
          <button className="btn-secondary" onClick={() => setReviewItem(null)}>Cancel</button>
          <button className="btn-danger gap-1.5" onClick={() => handleReview('REJECTED')} disabled={loading}><HiOutlineX className="w-4 h-4" /> Reject</button>
          <button className="btn-success gap-1.5" onClick={() => handleReview('APPROVED')} disabled={loading}><HiOutlineCheck className="w-4 h-4" /> Approve</button>
        </>}>
        {reviewItem && (
          <div className="space-y-4">
            <div className="flex items-center gap-3 p-3 bg-surface-50 dark:bg-gray-800 rounded-xl">
              <div className="w-10 h-10 rounded-full bg-primary-100 dark:bg-primary-900/40 text-primary-700 dark:text-primary-400 flex items-center justify-center font-bold">
                {reviewItem.user?.firstName?.charAt(0)}{reviewItem.user?.lastName?.charAt(0)}
              </div>
              <div>
                <p className="font-medium text-surface-800 dark:text-gray-200">{reviewItem.user?.firstName} {reviewItem.user?.lastName}</p>
                <p className="text-sm text-surface-500 dark:text-gray-400">{reviewItem.user?.email}</p>
                <p className="text-xs text-surface-400 dark:text-gray-500 dark:text-gray-500 mt-0.5">Registered {timeAgo(reviewItem.createdAt)}</p>
              </div>
            </div>
            <div>
              <label className="label">Review note <span className="text-surface-400 dark:text-gray-500 font-normal">(optional)</span></label>
              <textarea value={note} onChange={e => setNote(e.target.value)} rows={3}
                placeholder="Reason for approval or rejection…" className="input resize-none" />
            </div>
          </div>
        )}
      </Modal>
    </div>
  )
}
