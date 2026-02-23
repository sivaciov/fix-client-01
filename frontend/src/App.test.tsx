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
          json: async () => ({
            status: 'RUNNING',
            details: '',
            sessions: ['FIX.4.4:YOUR_SENDER_COMP_ID->YOUR_TARGET_COMP_ID'],
            config: {
              senderCompId: 'YOUR_SENDER_COMP_ID',
              targetCompId: 'YOUR_TARGET_COMP_ID',
              host: 'localhost',
              port: 9876,
            },
            diagnostics: {
              lastEvent: 'Initiator started',
              lastError: '',
              lastUpdatedAt: '2026-02-23T00:00:00Z',
            },
          }),
        } as Response)
      }

      return Promise.reject(new Error(`Unexpected request: ${String(input)}`))
    })

    vi.spyOn(globalThis, 'fetch').mockImplementation(fetchMock as typeof fetch)

    render(<App />)

    expect(await screen.findByText('Backend: ok')).toBeInTheDocument()
    expect(await screen.findByText('FIX Session')).toBeInTheDocument()
    expect(await screen.findByText('RUNNING')).toBeInTheDocument()
    expect(await screen.findByText('Session: YOUR_SENDER_COMP_ID → YOUR_TARGET_COMP_ID')).toBeInTheDocument()
    expect(await screen.findByText('Endpoint: localhost:9876')).toBeInTheDocument()
    expect(await screen.findByText('Last event: Initiator started')).toBeInTheDocument()
    expect(await screen.findByText('Last error: --')).toBeInTheDocument()
    expect(await screen.findByText('Last updated: 2026-02-23T00:00:00Z')).toBeInTheDocument()
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
          json: async () => ({
            status: 'STOPPED',
            details: '',
            sessions: [],
            config: {},
            diagnostics: {},
          }),
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
          json: async () => ({
            status: 'RUNNING',
            details: '',
            sessions: [],
            config: {},
            diagnostics: {},
          }),
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
