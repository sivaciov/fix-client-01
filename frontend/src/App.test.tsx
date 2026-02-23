import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from './App'

afterEach(() => {
  cleanup()
  vi.restoreAllMocks()
})

describe('App', () => {
  it('calls /fix/status and renders returned FIX status', async () => {
    const fetchMock = vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
      if (input === '/health') {
        return Promise.resolve({
          ok: true,
          json: async () => ({ status: 'ok' }),
        } as Response)
      }

      if (input === '/fix/status' && !init?.method) {
        return Promise.resolve({
          ok: true,
          json: async () => ({ status: 'CONNECTED' }),
        } as Response)
      }

      return Promise.reject(new Error(`Unexpected request: ${String(input)}`))
    })

    vi.spyOn(globalThis, 'fetch').mockImplementation(fetchMock as typeof fetch)

    render(<App />)

    expect(await screen.findByText('Backend: ok')).toBeInTheDocument()
    expect(await screen.findByText('CONNECTED')).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledWith('/fix/status')
  })

  it('triggers /fix/start and /fix/stop POST requests from buttons', async () => {
    const fetchMock = vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
      if (input === '/health') {
        return Promise.resolve({
          ok: true,
          json: async () => ({ status: 'ok' }),
        } as Response)
      }

      if (input === '/fix/status') {
        return Promise.resolve({
          ok: true,
          json: async () => ({ status: 'DISCONNECTED' }),
        } as Response)
      }

      if (input === '/fix/start' && init?.method === 'POST') {
        return Promise.resolve({ ok: true } as Response)
      }

      if (input === '/fix/stop' && init?.method === 'POST') {
        return Promise.resolve({ ok: true } as Response)
      }

      return Promise.reject(new Error(`Unexpected request: ${String(input)}`))
    })

    vi.spyOn(globalThis, 'fetch').mockImplementation(fetchMock as typeof fetch)

    render(<App />)

    fireEvent.click(await screen.findByRole('button', { name: 'Start' }))
    fireEvent.click(screen.getByRole('button', { name: 'Stop' }))

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith('/fix/start', { method: 'POST' })
      expect(fetchMock).toHaveBeenCalledWith('/fix/stop', { method: 'POST' })
    })
  })

  it('polls /fix/status every 2 seconds', async () => {
    const fetchMock = vi.fn((input: RequestInfo | URL) => {
      if (input === '/health') {
        return Promise.resolve({
          ok: true,
          json: async () => ({ status: 'ok' }),
        } as Response)
      }

      if (input === '/fix/status') {
        return Promise.resolve({
          ok: true,
          json: async () => ({ status: 'RUNNING' }),
        } as Response)
      }

      return Promise.reject(new Error(`Unexpected request: ${String(input)}`))
    })

    let pollCallback: (() => void) | undefined
    const setIntervalSpy = vi
      .spyOn(window, 'setInterval')
      .mockImplementation(((handler: TimerHandler) => {
        if (typeof handler === 'function') {
          pollCallback = handler as () => void
        }
        return 1 as unknown as number
      }) as typeof window.setInterval)

    const clearIntervalSpy = vi
      .spyOn(window, 'clearInterval')
      .mockImplementation(() => {})

    vi.spyOn(globalThis, 'fetch').mockImplementation(fetchMock as typeof fetch)
    render(<App />)

    await screen.findByText('RUNNING')
    expect(setIntervalSpy).toHaveBeenCalledWith(expect.any(Function), 2000)
    expect(fetchMock).toHaveBeenCalledWith('/fix/status')

    expect(pollCallback).toBeTypeOf('function')
    expect(clearIntervalSpy).toHaveBeenCalledTimes(1)
  })
})
