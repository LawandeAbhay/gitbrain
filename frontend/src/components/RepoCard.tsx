import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { triggerIndex } from '../api/repos'
import type { Repository } from '../api/repos'
import './RepoCard.css'

const LANGUAGE_COLORS: Record<string, string> = {
  JavaScript: '#f7df1e', TypeScript: '#3178c6', Python: '#3572a5',
  Java: '#b07219', Kotlin: '#a97bff', Go: '#00add8', Rust: '#dea584',
  Ruby: '#701516', PHP: '#4f5d95', 'C#': '#239120', Swift: '#fa7343',
  C: '#555555', 'C++': '#f34b7d', HTML: '#e34c26', CSS: '#563d7c',
  Shell: '#89e051', Vue: '#42b883', Dart: '#00b4ab',
}

interface Props { repo: Repository; onIndexStart?: (id: string) => void }

export default function RepoCard({ repo, onIndexStart }: Props) {
  const navigate = useNavigate()
  const [indexing, setIndexing] = useState(false)

  const statusColor = {
    NONE:     undefined,
    INDEXING: 'amber',
    INDEXED:  'green',
    FAILED:   'red',
  }[repo.indexStatus]

  const handleIndex = async (e: React.MouseEvent) => {
    e.stopPropagation()
    setIndexing(true)
    try {
      await triggerIndex(repo.id)
      onIndexStart?.(repo.id)
    } catch {
      // conflict (already indexing) is fine
    } finally {
      setIndexing(false)
    }
  }

  const handleChat = (e: React.MouseEvent) => {
    e.stopPropagation()
    navigate(`/repo/${repo.id}`)
  }

  const langColor = repo.language ? LANGUAGE_COLORS[repo.language] : undefined

  return (
    <article
      className="repo-card card"
      onClick={() => navigate(`/repo/${repo.id}`)}
      role="button"
      tabIndex={0}
      onKeyDown={e => e.key === 'Enter' && navigate(`/repo/${repo.id}`)}
      id={`repo-${repo.id}`}
    >
      <div className="repo-card-header">
        <div className="repo-card-title-row">
          <svg className="repo-icon" width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
            <path d="M2 2.5A2.5 2.5 0 0 1 4.5 0h8.75a.75.75 0 0 1 .75.75v12.5a.75.75 0 0 1-.75.75h-2.5a.75.75 0 0 1 0-1.5h1.75v-2h-8a1 1 0 0 0-.714 1.7.75.75 0 1 1-1.072 1.05A2.495 2.495 0 0 1 2 11.5Zm10.5-1h-8a1 1 0 0 0-1 1v6.708A2.486 2.486 0 0 1 4.5 9h8Z"/>
          </svg>
          <h3 className="repo-name">{repo.name}</h3>
          {repo.privateRepository && (
            <span className="badge" style={{ background: 'var(--amber-dim)', color: 'var(--amber)' }}>
              Private
            </span>
          )}
        </div>
        <div className={`index-status-dot status-${repo.indexStatus.toLowerCase()}`} title={repo.indexStatus} />
      </div>

      {repo.description && (
        <p className="repo-description">{repo.description}</p>
      )}

      <div className="repo-card-meta">
        {repo.language && (
          <span className="repo-lang">
            <span className="lang-dot" style={{ background: langColor || 'var(--text-muted)' }} />
            {repo.language}
          </span>
        )}
        {repo.fork && (
          <span className="repo-meta-item">
            <svg width="12" height="12" viewBox="0 0 16 16" fill="currentColor">
              <path d="M5 5.372v.878c0 .414.336.75.75.75h4.5a.75.75 0 0 0 .75-.75v-.878a2.25 2.25 0 1 1 1.5 0v.878a2.25 2.25 0 0 1-2.25 2.25h-1.5v2.128a2.251 2.251 0 1 1-1.5 0V8.5h-1.5A2.25 2.25 0 0 1 3.5 6.25v-.878a2.25 2.25 0 1 1 1.5 0Z"/>
            </svg>
            Fork
          </span>
        )}
      </div>

      <div className="repo-card-actions">
        {repo.indexStatus === 'INDEXED' ? (
          <button className="btn btn-primary btn-sm" onClick={handleChat} id={`chat-btn-${repo.id}`}>
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
            </svg>
            Chat
          </button>
        ) : (
          <button
            className="btn btn-secondary btn-sm"
            onClick={handleIndex}
            disabled={indexing || repo.indexStatus === 'INDEXING'}
            id={`index-btn-${repo.id}`}
          >
            {repo.indexStatus === 'INDEXING' ? (
              <>
                <span className="btn-spinner" />
                Indexing…
              </>
            ) : (
              <>
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/>
                </svg>
                {repo.indexStatus === 'FAILED' ? 'Retry Index' : 'Index'}
              </>
            )}
          </button>
        )}

        <a
          href={repo.htmlUrl}
          target="_blank"
          rel="noopener noreferrer"
          className="btn btn-ghost btn-sm"
          onClick={e => e.stopPropagation()}
          id={`github-link-${repo.id}`}
        >
          <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
            <path d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0 0 24 12c0-6.63-5.37-12-12-12z"/>
          </svg>
          GitHub
        </a>
      </div>
    </article>
  )
}
