import { useEffect, useState } from 'react'
import './App.css'

type HealthResponse = {
  status?: string
}

function App() {
  const [message, setMessage] = useState('Checking backend health...')

  const checkHealth = async () => {
    try {
      const response = await fetch('http://localhost:8080/health')

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
      setMessage(
        'Backend: unreachable. Make sure the API is running on http://localhost:8080.',
      )
    }
  }

  useEffect(() => {
    void checkHealth()
  }, [])

  return (
    <main className="app">
      <h1>Health Check</h1>
      <p>{message}</p>
      <button type="button" onClick={() => void checkHealth()}>
        Retry
      </button>
    </main>
  )
}

export default App
