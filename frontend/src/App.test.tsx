import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'

afterEach(() => {
  cleanup()
  vi.restoreAllMocks()
})

beforeEach(() => {
  vi.spyOn(window, 'setInterval').mockImplementation(
    () => 1 as unknown as ReturnType<typeof setInterval>,
  )
  vi.spyOn(window, 'clearInterval').mockImplementation(() => {})
})

const okJson = (data: unknown): Response =>
  ({
    ok: true,
    status: 200,
    json: async () => data,
  }) as Response

describe('App', () => {
  it('renders FIX status and orders blotter from API', async () => {
    const fetchMock = vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
      if (input === '/health') {
        return Promise.resolve(okJson({ status: 'ok' }))
      }

      if (input === '/fix/status' && !init?.method) {
        return Promise.resolve(
          okJson({
            status: 'RUNNING',
            config: {
              senderCompId: 'SENDER',
              targetCompId: 'TARGET',
              host: 'localhost',
              port: 9876,
            },
            diagnostics: {
              lastEvent: 'Initiator started',
            },
          }),
        )
      }

      if (input === '/orders' && !init?.method) {
        return Promise.resolve(
          okJson([
            {
              id: 'ord-1',
              status: 'NEW',
              symbol: 'AAPL',
              side: 'BUY',
              qty: 100,
              type: 'LIMIT',
              price: 190.25,
              tif: 'DAY',
              updatedAt: '2026-02-23T00:00:00Z',
              message: 'Accepted',
            },
          ]),
        )
      }

      return Promise.reject(new Error(`Unexpected request: ${String(input)}`))
    })

    vi.spyOn(globalThis, 'fetch').mockImplementation(fetchMock as typeof fetch)

    render(<App />)

    expect(await screen.findByText('Backend: ok')).toBeInTheDocument()
    expect(await screen.findByText('FIX Session')).toBeInTheDocument()
    expect(await screen.findByText('RUNNING')).toBeInTheDocument()
    expect(await screen.findByText('Orders Blotter')).toBeInTheDocument()
    expect(await screen.findByText('AAPL')).toBeInTheDocument()
    expect(await screen.findByText('Accepted')).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledWith('/orders')
  })

  it('requires price for LIMIT orders', async () => {
    const fetchMock = vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
      if (input === '/health') {
        return Promise.resolve(okJson({ status: 'ok' }))
      }

      if (input === '/fix/status' && !init?.method) {
        return Promise.resolve(okJson({ status: 'STOPPED' }))
      }

      if (input === '/orders' && !init?.method) {
        return Promise.resolve(okJson([]))
      }

      return Promise.reject(new Error(`Unexpected request: ${String(input)}`))
    })

    vi.spyOn(globalThis, 'fetch').mockImplementation(fetchMock as typeof fetch)

    render(<App />)

    fireEvent.change(await screen.findByLabelText('Type'), {
      target: { value: 'LIMIT' },
    })
    fireEvent.change(screen.getByLabelText('Price'), {
      target: { value: '' },
    })

    fireEvent.click(screen.getByRole('button', { name: 'Submit Order' }))

    expect(await screen.findByText('LIMIT orders require a valid price.')).toBeInTheDocument()
    expect(fetchMock).not.toHaveBeenCalledWith(
      '/orders',
      expect.objectContaining({ method: 'POST' }),
    )
  })

  it('submits order to /orders and shows success result', async () => {
    const fetchMock = vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
      if (input === '/health') {
        return Promise.resolve(okJson({ status: 'ok' }))
      }

      if (input === '/fix/status' && !init?.method) {
        return Promise.resolve(okJson({ status: 'STOPPED' }))
      }

      if (input === '/orders' && !init?.method) {
        return Promise.resolve(okJson([]))
      }

      if (input === '/orders' && init?.method === 'POST') {
        return Promise.resolve(okJson({ message: 'Order accepted' }))
      }

      return Promise.reject(new Error(`Unexpected request: ${String(input)}`))
    })

    vi.spyOn(globalThis, 'fetch').mockImplementation(fetchMock as typeof fetch)

    render(<App />)

    fireEvent.click(await screen.findByRole('button', { name: 'Submit Order' }))

    expect(await screen.findByText('Order accepted')).toBeInTheDocument()

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        '/orders',
        expect.objectContaining({
          method: 'POST',
        }),
      )
    })
  })

  it('loads order details when a blotter row is clicked', async () => {
    const fetchMock = vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
      if (input === '/health') {
        return Promise.resolve(okJson({ status: 'ok' }))
      }

      if (input === '/fix/status' && !init?.method) {
        return Promise.resolve(okJson({ status: 'RUNNING' }))
      }

      if (input === '/orders' && !init?.method) {
        return Promise.resolve(
          okJson([
            {
              id: 'ord-2',
              status: 'PENDING',
              symbol: 'MSFT',
              side: 'SELL',
              qty: 50,
              type: 'LIMIT',
              price: 410,
              tif: 'DAY',
              updatedAt: '2026-02-23T00:00:00Z',
              message: 'Working',
            },
          ]),
        )
      }

      if (input === '/orders/ord-2' && !init?.method) {
        return Promise.resolve(
          okJson({
            id: 'ord-2',
            status: 'FILLED',
            symbol: 'MSFT',
            side: 'SELL',
            qty: 50,
            type: 'LIMIT',
            price: 410,
            tif: 'DAY',
            updatedAt: '2026-02-23T00:01:00Z',
            message: 'Filled',
            lastExecType: '2',
            lastOrdStatus: '2',
            cumQty: 50,
            leavesQty: 0,
            avgPx: 410,
            lastPx: 410,
            transactTime: '2026-02-23T00:00:59Z',
            text: 'Sim fill',
          }),
        )
      }

      return Promise.reject(new Error(`Unexpected request: ${String(input)}`))
    })

    vi.spyOn(globalThis, 'fetch').mockImplementation(fetchMock as typeof fetch)

    render(<App />)

    fireEvent.click(await screen.findByRole('button', { name: 'PENDING' }))

    expect(await screen.findByText('Order Details')).toBeInTheDocument()
    expect(await screen.findByText('FILLED')).toBeInTheDocument()
    expect(await screen.findByText('Last Exec Type')).toBeInTheDocument()
    expect(await screen.findByText('Sim fill')).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledWith('/orders/ord-2')
  })
})
