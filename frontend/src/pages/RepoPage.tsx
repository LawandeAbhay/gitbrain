import { useState, useEffect, useCallback } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { getRepos, triggerIndex, getIndexStatus } from '../api/repos'
import type { Repository, IndexStatus } from '../api/repos'
import Navbar from '../components/Navbar'
import ChatPanel from '../components/ChatPanel'
import './RepoPage.css'

const LANGUAGE_COLORS: Record<string, string> = {
  JavaScript: '#f7df1e', TypeScript: '#3178c6', Python: '#3572a5',
  Java: '#b07219', Kotlin: '#a97bff', Go: '#00add8', Rust: '#dea584',
  Ruby: '#701516', PHP: '#4f5d95', 'C#': '#239120', Swift: '#fa7343',
  C: '#555555', 'C++': '#f34b7d',
}

export default function RepoPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [repo, setRepo] = useState<Repository | null>(null)
  const [indexStatus, setIndexStatus] = useState<IndexStatus | null>(null)
  const [loading, setLoading] = useState(true)
  const [indexing, setIndexing] = useState(false)

  // Load repo by id from the repos list
  const loadRepo = useCallback(async () => {
    if (!id) return
    try {
      // Fetch all repos pages to find this one (simple approach for demo)
      const data = await getRepos(0, 100)
      const found = data.content.find(r => r.id === id)
      if (found) setRepo(found)
    } finally {
      setLoading(false)
    }
  }, [id])

  const refreshStatus = useCallback(async () => {
    if (!id) return
    try {
      const status = await getIndexStatus(id)
      setIndexStatus(status)
      // Update local repo state
      setRepo(prev => prev ? { ...prev, indexStatus: status.status, indexedAt: status.indexedAt } : prev)
    } catch {}
  }, [id])

  useEffect(() => {
    loadRepo()
    refreshStatus()
  }, [loadRepo, refreshStatus])

  // Poll while indexing
  useEffect(() => {
    if (!repo || repo.indexStatus !== 'INDEXING') return
    const interval = setInterval(refreshStatus, 3000)
    return () => clearInterval(interval)
  }, [repo?.indexStatus, refreshStatus])

  const handleIndex = async () => {
    if (!id) return
    setIndexing(true)
    try {
      await triggerIndex(id)
      setRepo(prev => prev ? { ...prev, indexStatus: 'INDEXING' } : prev)
    } catch {} finally {
      setIndexing(false)
    }
  }

  if (loading) {
    return (
      <div className="repo-page">
        <Navbar />
        <div className="repo-loading">
          <div className="spinner" style={{ width: '36px', height: '36px', border: '3px solid var(--bg-3)', borderTopColor: 'var(--accent)', borderRadius: '50%', animation: 'spin 0.8s linear infinite' }} />
        </div>
      </div>
    )
  }

  if (!repo) {
    return (
      <div className="repo-page">
        <Navbar />
        <div className="repo-not-found">
          <h2>Repository not found</h2>
          <button className="btn btn-secondary" onClick={() => navigate('/dashboard')}>← Back to Dashboard</button>
        </div>
      </div>
    )
  }

  const langColor = repo.language ? LANGUAGE_COLORS[repo.language] : undefined
  const isIndexed  = repo.indexStatus === 'INDEXED'
  const isIndexing = repo.indexStatus === 'INDEXING'
  const isFailed   = repo.indexStatus === 'FAILED'

  return (
    <div className="repo-page">
      <Navbar />

      <main className="repo-main">
        {/* Back */}
        <button className="btn btn-ghost btn-sm repo-back" onClick={() => navigate('/dashboard')} id="back-btn">
          ← Dashboard
        </button>

        <div className="repo-layout">
          {/* Left panel — repo info + index control */}
          <aside className="repo-sidebar">
            <div className="card repo-info-card">
              {/* Header */}
              <div className="repo-info-header">
                <div className="repo-info-icon">
                  <svg width="20" height="20" viewBox="0 0 16 16" fill="currentColor">
                    <path d="M2 2.5A2.5 2.5 0 0 1 4.5 0h8.75a.75.75 0 0 1 .75.75v12.5a.75.75 0 0 1-.75.75h-2.5a.75.75 0 0 1 0-1.5h1.75v-2h-8a1 1 0 0 0-.714 1.7.75.75 0 1 1-1.072 1.05A2.495 2.495 0 0 1 2 11.5Zm10.5-1h-8a1 1 0 0 0-1 1v6.708A2.486 2.486 0 0 1 4.5 9h8Z"/>
                  </svg>
                </div>
                <div>
                  <h1 className="repo-info-name">{repo.name}</h1>
                  <span className="repo-info-full">{repo.fullName}</span>
                </div>
              </div>

              {repo.description && (
                <p className="repo-info-desc">{repo.description}</p>
              )}

              {/* Meta */}
              <div className="repo-meta-list">
                {repo.language && (
                  <div className="repo-meta-row">
                    <span className="lang-dot" style={{ background: langColor || 'var(--text-muted)', width: '10px', height: '10px', borderRadius: '50%', display: 'inline-block' }} />
                    <span>{repo.language}</span>
                  </div>
                )}
                <div className="repo-meta-row">
                  <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/>
                  </svg>
                  <span>{repo.defaultBranch}</span>
                </div>
                <div className="repo-meta-row">
                  {repo.privateRepository ? (
                    <><svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg><span>Private</span></>
                  ) : (
                    <><svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="12" cy="12" r="10"/><path d="M2 12h20M12 2a15 15 0 0 1 0 20M12 2a15 15 0 0 0 0 20"/></svg><span>Public</span></>
                  )}
                </div>
              </div>

              <a
                href={repo.htmlUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="btn btn-secondary"
                style={{ justifyContent: 'center' }}
                id="view-on-github"
              >
                <svg width="15" height="15" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0 0 24 12c0-6.63-5.37-12-12-12z"/>
                </svg>
                View on GitHub
              </a>
            </div>

            {/* Index Status Card */}
            <div className="card index-card">
              <div className="index-card-header">
                <h2 className="index-card-title">RAG Index</h2>
                <div className={`index-status-chip status-chip-${repo.indexStatus.toLowerCase()}`}>
                  {isIndexing && <span className="chip-spinner" />}
                  {repo.indexStatus}
                </div>
              </div>

              {isIndexed && indexStatus && (
                <div className="index-info">
                  <div className="index-stat">
                    <span className="index-stat-val">{indexStatus.chunkCount.toLocaleString()}</span>
                    <span className="index-stat-lbl">code chunks</span>
                  </div>
                  {repo.indexedAt && (
                    <div className="index-stat">
                      <span className="index-stat-val">{new Date(repo.indexedAt).toLocaleDateString()}</span>
                      <span className="index-stat-lbl">last indexed</span>
                    </div>
                  )}
                </div>
              )}

              {isFailed && indexStatus?.error && (
                <p className="index-error-msg">{indexStatus.error.slice(0, 120)}</p>
              )}

              {isIndexing && (
                <p className="index-progress-msg">
                  Cloning, chunking, and embedding your repository…<br />
                  This may take a few minutes for large repos.
                </p>
              )}

              <button
                id="index-repo-btn"
                className={`btn ${isIndexed ? 'btn-secondary' : 'btn-primary'}`}
                style={{ width: '100%', justifyContent: 'center' }}
                onClick={handleIndex}
                disabled={indexing || isIndexing}
              >
                {isIndexing ? (
                  <><span className="btn-spinner" /> Indexing…</>
                ) : isIndexed ? (
                  <>
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                      <polyline points="1 4 1 10 7 10"/><path d="M3.51 15a9 9 0 1 0 .49-4.7"/>
                    </svg>
                    Re-index
                  </>
                ) : (
                  <>
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                      <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/>
                    </svg>
                    {isFailed ? 'Retry Index' : 'Index Repository'}
                  </>
                )}
              </button>
            </div>
          </aside>

          {/* Right panel — Chat */}
          <section className="repo-chat-section">
            <div className="chat-section-header">
              <h2>Chat with codebase</h2>
              {!isIndexed && (
                <span className="chat-locked-hint">
                  {isIndexing ? '⏳ Indexing in progress…' : '⚠️ Index this repository first to chat'}
                </span>
              )}
            </div>

            {isIndexed ? (
              <div className="chat-container">
                <ChatPanel repoId={repo.id} repoName={repo.name} />
              </div>
            ) : (
              <div className="chat-placeholder card">
                <div className="chat-placeholder-inner">
                  {isIndexing ? (
                    <>
                      <div className="placeholder-spinner" />
                      <h3>Indexing in progress…</h3>
                      <p>We're cloning and embedding your repository. This page will update automatically.</p>
                    </>
                  ) : (
                    <>
                      <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1" style={{ color: 'var(--text-muted)', marginBottom: '8px' }}>
                        <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
                      </svg>
                      <h3>Repository not indexed yet</h3>
                      <p>Click <strong>Index Repository</strong> to enable AI-powered chat for this codebase.</p>
                    </>
                  )}
                </div>
              </div>
            )}
          </section>
        </div>
      </main>
    </div>
  )
}
