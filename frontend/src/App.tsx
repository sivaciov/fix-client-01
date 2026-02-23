import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
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

type Side = 'BUY' | 'SELL'
type OrderType = 'MARKET' | 'LIMIT'
type Tif = 'DAY' | 'IOC' | 'GTC'
type OrderStatus = 'ACCEPTED' | 'REJECTED'

type CreateOrderResponse = {
  orderId: string
  status: OrderStatus
  message: string
}

type OrderRow = {
  orderId: string
  createdAt: string
  symbol: string
  side: Side
  qty: number
  type: OrderType
  price?: number | null
  tif: Tif
  status: OrderStatus
  message: string
}

function App() {
  const [message, setMessage] = useState('Checking backend health...')
  const [fixStatus, setFixStatus] = useState<FixStatusResponse | null>(null)
  const [fixError, setFixError] = useState<string | null>(null)

  const [symbol, setSymbol] = useState('')
  const [side, setSide] = useState<Side>('BUY')
  const [qty, setQty] = useState<number>(100)
  const [type, setType] = useState<OrderType>('MARKET')
  const [price, setPrice] = useState('')
  const [tif, setTif] = useState<Tif>('DAY')

  const [orderError, setOrderError] = useState<string | null>(null)
  const [orderMessage, setOrderMessage] = useState<string | null>(null)
  const [orders, setOrders] = useState<OrderRow[]>([])

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

  const fetchOrders = async () => {
    try {
      const response = await fetch('/orders')
      if (!response.ok) {
        setOrderError(
          `Unable to load orders (request failed with status ${response.status}).`,
        )
        return
      }

      const data = (await response.json()) as OrderRow[]
      setOrders(data)
    } catch {
      setOrderError('Unable to load orders right now. Please try again.')
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

  const submitOrder = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()

    if (!symbol.trim()) {
      setOrderError('Symbol is required.')
      return
    }
    if (!qty || qty <= 0) {
      setOrderError('Quantity must be greater than 0.')
      return
    }
    if (type === 'LIMIT' && !price.trim()) {
      setOrderError('Price is required for LIMIT orders.')
      return
    }

    const payload: {
      symbol: string
      side: Side
      qty: number
      type: OrderType
      price?: number
      tif: Tif
    } = {
      symbol: symbol.trim(),
      side,
      qty,
      type,
      tif,
    }

    if (type === 'LIMIT') {
      payload.price = Number(price)
    }

    try {
      const response = await fetch('/orders', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(payload),
      })

      if (!response.ok) {
        setOrderMessage(null)
        if (response.status === 400) {
          const error = (await response.json()) as { message?: string }
          setOrderError(error.message || 'Order validation failed.')
          return
        }
        setOrderError(`Order request failed with status ${response.status}.`)
        return
      }

      const data = (await response.json()) as CreateOrderResponse
      setOrderMessage(`${data.status}: ${data.message}`)
      setOrderError(null)
      await fetchOrders()
    } catch {
      setOrderMessage(null)
      setOrderError('Unable to submit order right now. Please try again.')
    }
  }

  useEffect(() => {
    void checkHealth()
    void fetchFixStatus()
    void fetchOrders()

    const pollId = window.setInterval(() => {
      void fetchFixStatus()
    }, 2000)

    return () => {
      window.clearInterval(pollId)
    }
  }, [])

  useEffect(() => {
    if (type === 'MARKET') {
      setPrice('')
    }
  }, [type])

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
          Status:{' '}
          <strong className={`status-badge status-${currentStatus.toLowerCase()}`}>
            {currentStatus}
          </strong>
        </p>
        <p>
          Session: {sender} → {target}
        </p>
        <p>
          Endpoint: {host}:{port}
        </p>
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

      <section className="order-ticket" aria-label="New order form">
        <h2>New Order</h2>
        <form onSubmit={(event) => void submitOrder(event)}>
          <label>
            Symbol
            <input
              aria-label="Symbol"
              type="text"
              value={symbol}
              onChange={(event) => setSymbol(event.target.value)}
            />
          </label>

          <label>
            Side
            <select
              aria-label="Side"
              value={side}
              onChange={(event) => setSide(event.target.value as Side)}
            >
              <option value="BUY">BUY</option>
              <option value="SELL">SELL</option>
            </select>
          </label>

          <label>
            Qty
            <input
              aria-label="Qty"
              type="number"
              value={qty}
              onChange={(event) => setQty(Number(event.target.value))}
              min={1}
            />
          </label>

          <label>
            Type
            <select
              aria-label="Type"
              value={type}
              onChange={(event) => setType(event.target.value as OrderType)}
            >
              <option value="MARKET">MARKET</option>
              <option value="LIMIT">LIMIT</option>
            </select>
          </label>

          <label>
            Price
            <input
              aria-label="Price"
              type="number"
              step="0.01"
              value={price}
              onChange={(event) => setPrice(event.target.value)}
              disabled={type !== 'LIMIT'}
            />
          </label>

          <label>
            TIF
            <select
              aria-label="TIF"
              value={tif}
              onChange={(event) => setTif(event.target.value as Tif)}
            >
              <option value="DAY">DAY</option>
              <option value="IOC">IOC</option>
              <option value="GTC">GTC</option>
            </select>
          </label>

          <button type="submit">Send Order</button>
        </form>
        {orderError ? <p className="error-message">{orderError}</p> : null}
        {orderMessage ? <p className="order-message">{orderMessage}</p> : null}
      </section>

      <section className="order-log" aria-label="Orders table">
        <h2>Orders</h2>
        <table>
          <thead>
            <tr>
              <th>Order ID</th>
              <th>Time</th>
              <th>Symbol</th>
              <th>Side</th>
              <th>Qty</th>
              <th>Type</th>
              <th>Price</th>
              <th>TIF</th>
              <th>Status</th>
              <th>Message</th>
            </tr>
          </thead>
          <tbody>
            {orders.map((order) => (
              <tr key={order.orderId}>
                <td>{order.orderId}</td>
                <td>{order.createdAt}</td>
                <td>{order.symbol}</td>
                <td>{order.side}</td>
                <td>{order.qty}</td>
                <td>{order.type}</td>
                <td>{order.price ?? '--'}</td>
                <td>{order.tif}</td>
                <td>{order.status}</td>
                <td>{order.message || '--'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </main>
  )
}

export default App
