import { useState, useEffect, useCallback } from 'react'
import { getRepos, syncRepos } from '../api/repos'
import type { Repository, PagedResponse, SyncSummary } from '../api/repos'
import Navbar from '../components/Navbar'
import RepoCard from '../components/RepoCard'
import './DashboardPage.css'

const PAGE_SIZE = 18

export default function DashboardPage() {
  const [repos, setRepos] = useState<Repository[]>([])
  const [totalPages, setTotalPages] = useState(1)
  const [currentPage, setCurrentPage] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [loading, setLoading] = useState(true)
  const [syncing, setSyncing] = useState(false)
  const [syncMsg, setSyncMsg] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [search, setSearch] = useState('')

  const load = useCallback(async (page: number) => {
    setLoading(true)
    setError(null)
    try {
      const data = await getRepos(page, PAGE_SIZE)
      setRepos(data.content)
      setTotalPages(data.totalPages)
      setTotalElements(data.totalElements)
      setCurrentPage(data.number)
    } catch {
      setError('Failed to load repositories. Please try again.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { load(0) }, [load])

  const handleSync = async () => {
    setSyncing(true)
    setSyncMsg(null)
    try {
      const summary = await syncRepos()
      setSyncMsg(
        `Sync complete — ${summary.addedCount} added, ${summary.updatedCount} updated, ${summary.deletedCount} removed`
      )
      load(currentPage)
    } catch {
      setSyncMsg('Sync failed. Please try again.')
    } finally {
      setSyncing(false)
      setTimeout(() => setSyncMsg(null), 5000)
    }
  }

  const handleIndexStart = (repoId: string) => {
    setRepos(prev =>
      prev.map(r => r.id === repoId ? { ...r, indexStatus: 'INDEXING' } : r)
    )
    // Poll for status update
    const interval = setInterval(async () => {
      try {
        const fresh = await getRepos(currentPage, PAGE_SIZE)
        const updated = fresh.content.find(r => r.id === repoId)
        if (updated && updated.indexStatus !== 'INDEXING') {
          setRepos(prev => prev.map(r => r.id === repoId ? updated : r))
          clearInterval(interval)
        }
      } catch { clearInterval(interval) }
    }, 3000)
  }

  const filtered = repos.filter(r =>
    search === '' ||
    r.name.toLowerCase().includes(search.toLowerCase()) ||
    (r.description || '').toLowerCase().includes(search.toLowerCase()) ||
    (r.language || '').toLowerCase().includes(search.toLowerCase())
  )

  return (
    <div className="dashboard-page">
      <Navbar />

      <main className="dashboard-main">
        {/* Header */}
        <div className="dashboard-header">
          <div className="dashboard-header-left">
            <h1 className="dashboard-title">Your Repositories</h1>
            <span className="dashboard-count">{totalElements} repos</span>
          </div>
          <div className="dashboard-header-right">
            <button
              id="sync-btn"
              className="btn btn-secondary"
              onClick={handleSync}
              disabled={syncing}
            >
              {syncing ? (
                <>
                  <span className="btn-spinner" style={{ borderColor: 'rgba(255,255,255,.3)', borderTopColor: '#fff' }} />
                  Syncing…
                </>
              ) : (
                <>
                  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <polyline points="1 4 1 10 7 10"/>
                    <path d="M3.51 15a9 9 0 1 0 .49-4.7"/>
                  </svg>
                  Sync Repos
                </>
              )}
            </button>
          </div>
        </div>

        {syncMsg && (
          <div className={`sync-banner ${syncMsg.includes('failed') ? 'sync-banner-error' : 'sync-banner-ok'}`}>
            {syncMsg}
          </div>
        )}

        {/* Search */}
        <div className="dashboard-search-row">
          <div className="search-wrapper">
            <svg className="search-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
            </svg>
            <input
              id="repo-search"
              className="input search-input"
              placeholder="Filter repositories…"
              value={search}
              onChange={e => setSearch(e.target.value)}
            />
          </div>
        </div>

        {/* Grid */}
        {loading ? (
          <div className="dashboard-grid">
            {Array.from({ length: 6 }).map((_, i) => (
              <div key={i} className="repo-card-skeleton card" />
            ))}
          </div>
        ) : error ? (
          <div className="dashboard-error">
            <p>{error}</p>
            <button className="btn btn-secondary btn-sm" onClick={() => load(currentPage)}>Retry</button>
          </div>
        ) : filtered.length === 0 ? (
          <div className="dashboard-empty">
            <p>{search ? 'No repositories match your search.' : 'No repositories found. Try syncing!'}</p>
          </div>
        ) : (
          <div className="dashboard-grid">
            {filtered.map(repo => (
              <RepoCard key={repo.id} repo={repo} onIndexStart={handleIndexStart} />
            ))}
          </div>
        )}

        {/* Pagination */}
        {!loading && totalPages > 1 && (
          <div className="dashboard-pagination">
            <button
              className="btn btn-ghost btn-sm"
              disabled={currentPage === 0}
              onClick={() => load(currentPage - 1)}
              id="page-prev"
            >← Prev</button>

            {Array.from({ length: totalPages }).map((_, i) => (
              <button
                key={i}
                className={`btn btn-sm ${i === currentPage ? 'btn-primary' : 'btn-ghost'}`}
                onClick={() => load(i)}
                id={`page-${i}`}
              >{i + 1}</button>
            ))}

            <button
              className="btn btn-ghost btn-sm"
              disabled={currentPage >= totalPages - 1}
              onClick={() => load(currentPage + 1)}
              id="page-next"
            >Next →</button>
          </div>
        )}
      </main>
    </div>
  )
}
