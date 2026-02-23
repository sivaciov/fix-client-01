import { useEffect, useState } from 'react'
import './App.css'

type HealthResponse = {
  status?: string
}

type FixStatusConfig = {
  senderCompId?: string
  targetCompId?: string
  host?: string
  port?: number | null
}

type FixStatusDiagnostics = {
  lastEvent?: string
  lastError?: string
  lastUpdatedAt?: string
}

type FixStatusResponse = {
  status?: string
  details?: string
  sessions?: string[]
  config?: FixStatusConfig
  diagnostics?: FixStatusDiagnostics
}

function App() {
  const [message, setMessage] = useState('Checking backend health...')
  const [fixStatus, setFixStatus] = useState<FixStatusResponse | null>(null)
  const [fixError, setFixError] = useState<string | null>(null)

  const checkHealth = async () => {
    try {
      const response = await fetch('/health')

      if (!response.ok) {
        setMessage(
          `Backend: unavailable (request failed with status ${response.status})`,
        )
        return
      }

      const data = (await response.json()) as HealthResponse

      if (data.status === 'ok') {
        setMessage('Backend: ok')
        return
      }

      setMessage(
        `Backend: unexpected response${
          data.status ? ` (status=${data.status})` : ''
        }`,
      )
    } catch {
      setMessage('Backend: unreachable. Make sure the API is running.')
    }
  }

  const fetchFixStatus = async () => {
    try {
      const response = await fetch('/fix/status')

      if (!response.ok) {
        setFixError(
          `Unable to load FIX status (request failed with status ${response.status}).`,
        )
        return
      }

      const data = (await response.json()) as FixStatusResponse
      setFixStatus(data)
      setFixError(null)
    } catch {
      setFixError('Unable to load FIX status right now. Please try again.')
    }
  }

  const triggerFixAction = async (url: '/fix/start' | '/fix/stop') => {
    try {
      const response = await fetch(url, { method: 'POST' })

      if (!response.ok) {
        setFixError(
          `FIX request failed with status ${response.status}. Please try again.`,
        )
        return
      }

      setFixError(null)
      await fetchFixStatus()
    } catch {
      setFixError('Unable to reach FIX controls right now. Please try again.')
    }
  }

  useEffect(() => {
    void checkHealth()
    void fetchFixStatus()

    const pollId = window.setInterval(() => {
      void fetchFixStatus()
    }, 2000)

    return () => {
      window.clearInterval(pollId)
    }
  }, [])

  const config = fixStatus?.config
  const diagnostics = fixStatus?.diagnostics
  const sender = config?.senderCompId || '--'
  const target = config?.targetCompId || '--'
  const host = config?.host || '--'
  const port = config?.port ?? '--'
  const details = fixStatus?.details || '--'
  const currentStatus = fixStatus?.status || 'Loading...'
  const lastEvent = diagnostics?.lastEvent || '--'
  const lastError = diagnostics?.lastError || '--'
  const lastUpdatedAt = diagnostics?.lastUpdatedAt || '--'

  return (
    <main className="app">
      <h1>Health Check</h1>
      <p>{message}</p>
      <button type="button" onClick={() => void checkHealth()}>
        Retry
      </button>

      <section className="fix-controls" aria-label="FIX controls">
        <h2>FIX Session</h2>
        <p className="status-row">
          Status: <strong className={`status-badge status-${currentStatus.toLowerCase()}`}>{currentStatus}</strong>
        </p>
        <p>Session: {sender} → {target}</p>
        <p>Endpoint: {host}:{port}</p>
        <p>Details: {details}</p>
        <p>Last event: {lastEvent}</p>
        <p>Last error: {lastError}</p>
        <p>Last updated: {lastUpdatedAt}</p>
        {fixError ? <p className="error-message">{fixError}</p> : null}
        <div className="fix-buttons">
          <button type="button" onClick={() => void triggerFixAction('/fix/start')}>
            Start
          </button>
          <button type="button" onClick={() => void triggerFixAction('/fix/stop')}>
            Stop
          </button>
        </div>
      </section>
    </main>
  )
}

export default App
