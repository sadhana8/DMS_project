import clsx from 'clsx'
import { getFileIcon, getFileColor } from '@/utils/helpers'

export default function FileIcon({ mimeType, fileName, size = 'md' }) {
  const sizes  = { sm: 'w-8 h-8 text-base', md: 'w-10 h-10 text-xl', lg: 'w-14 h-14 text-3xl' }
  const icon   = getFileIcon(mimeType, fileName)
  const color  = getFileColor(mimeType)
  return (
    <div className={clsx('rounded-xl flex items-center justify-center flex-shrink-0', sizes[size], color)}>
      <span>{icon}</span>
    </div>
  )
}
