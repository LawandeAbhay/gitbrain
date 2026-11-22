import api from './client'

export interface Repository {
  id: string
  githubId: number
  name: string
  fullName: string
  description: string | null
  htmlUrl: string
  defaultBranch: string
  language: string | null
  privateRepository: boolean
  fork: boolean
  visibility: string
  githubCreatedAt: string
  githubUpdatedAt: string
  githubPushedAt: string
  lastSyncedAt: string | null
  hasUnsyncedChanges: boolean
  indexStatus: 'NONE' | 'INDEXING' | 'INDEXED' | 'FAILED'
  indexedAt: string | null
}

export interface PagedResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

export interface SyncSummary {
  addedCount: number
  updatedCount: number
  deletedCount: number
}

export interface IndexStatus {
  status: 'NONE' | 'INDEXING' | 'INDEXED' | 'FAILED'
  indexedAt: string | null
  chunkCount: number
  error: string | null
}

export const getRepos = (page = 0, size = 20): Promise<PagedResponse<Repository>> =>
  api.get('/repos', { params: { page, size } }).then(r => r.data.data)

export const syncRepos = (): Promise<SyncSummary> =>
  api.get('/repos/sync').then(r => r.data.data)

export const triggerIndex = (repoId: string): Promise<void> =>
  api.post(`/repos/${repoId}/index`).then(() => undefined)

export const getIndexStatus = (repoId: string): Promise<IndexStatus> =>
  api.get(`/repos/${repoId}/index-status`).then(r => r.data.data)
