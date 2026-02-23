import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from './App'

afterEach(() => {
  cleanup()
  vi.restoreAllMocks()
})

describe('App', () => {
  it('validates that price is required for LIMIT orders', async () => {
    const fetchMock = vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
      if (input === '/health') {
        return Promise.resolve({ ok: true, json: async () => ({ status: 'ok' }) } as Response)
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
      if (input === '/orders' && !init?.method) {
        return Promise.resolve({ ok: true, json: async () => [] } as Response)
      }

      return Promise.reject(new Error(`Unexpected request: ${String(input)}`))
    })

    vi.spyOn(globalThis, 'fetch').mockImplementation(fetchMock as typeof fetch)
    render(<App />)

    fireEvent.change(await screen.findByLabelText('Type'), { target: { value: 'LIMIT' } })
    fireEvent.change(screen.getByLabelText('Symbol'), { target: { value: 'AAPL' } })
    fireEvent.click(screen.getByRole('button', { name: 'Send Order' }))

    expect(await screen.findByText('Price is required for LIMIT orders.')).toBeInTheDocument()
    expect(fetchMock).not.toHaveBeenCalledWith('/orders', expect.objectContaining({ method: 'POST' }))
  })

  it('submits successfully and refreshes orders table with new row', async () => {
    let ordersCall = 0
    const fetchMock = vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
      if (input === '/health') {
        return Promise.resolve({ ok: true, json: async () => ({ status: 'ok' }) } as Response)
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
      if (input === '/orders' && !init?.method) {
        ordersCall += 1
        if (ordersCall === 1) {
          return Promise.resolve({ ok: true, json: async () => [] } as Response)
        }

        return Promise.resolve({
          ok: true,
          json: async () => [
            {
              orderId: 'order-1',
              createdAt: '2026-02-23T00:00:00Z',
              symbol: 'AAPL',
              side: 'BUY',
              qty: 100,
              type: 'MARKET',
              price: null,
              tif: 'DAY',
              status: 'ACCEPTED',
              message: 'Order accepted',
            },
          ],
        } as Response)
      }
      if (input === '/orders' && init?.method === 'POST') {
        return Promise.resolve({
          ok: true,
          json: async () => ({
            orderId: 'order-1',
            status: 'ACCEPTED',
            message: 'Order accepted',
          }),
        } as Response)
      }

      return Promise.reject(new Error(`Unexpected request: ${String(input)}`))
    })

    vi.spyOn(globalThis, 'fetch').mockImplementation(fetchMock as typeof fetch)
    render(<App />)

    fireEvent.change(await screen.findByLabelText('Symbol'), { target: { value: 'AAPL' } })
    fireEvent.click(screen.getByRole('button', { name: 'Send Order' }))

    expect(await screen.findByText('ACCEPTED: Order accepted')).toBeInTheDocument()
    await waitFor(() => {
      expect(screen.getByText('order-1')).toBeInTheDocument()
      expect(screen.getByText('AAPL')).toBeInTheDocument()
      expect(screen.getByText('ACCEPTED')).toBeInTheDocument()
    })
  })
})
