import { useEffect, useMemo, useState } from 'react'
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

type OrderType = 'MARKET' | 'LIMIT'
type OrderSide = 'BUY' | 'SELL'
type TimeInForce = 'DAY' | 'IOC' | 'FOK' | 'GTC'

type OrderSummary = {
  id: string
  status?: string
  symbol?: string
  side?: string
  qty?: number | null
  type?: string
  price?: number | null
  tif?: string
  updatedAt?: string
  message?: string
}

type OrderDetails = {
  id: string
  status?: string
  symbol?: string
  side?: string
  qty?: number | null
  type?: string
  price?: number | null
  tif?: string
  updatedAt?: string
  message?: string
  lastExecType?: string
  lastOrdStatus?: string
  cumQty?: number | null
  leavesQty?: number | null
  avgPx?: number | null
  lastPx?: number | null
  text?: string
  transactTime?: string
}

type SubmitResult = {
  kind: 'success' | 'error'
  message: string
}

const defaultFormState = {
  symbol: 'AAPL',
  side: 'BUY' as OrderSide,
  qty: '100',
  type: 'MARKET' as OrderType,
  price: '',
  tif: 'DAY' as TimeInForce,
}

const toNumberOrNull = (value: unknown): number | null => {
  if (typeof value === 'number' && Number.isFinite(value)) {
    return value
  }

  if (typeof value === 'string' && value.trim()) {
    const parsed = Number(value)
    return Number.isFinite(parsed) ? parsed : null
  }

  return null
}

const toStringOrUndefined = (value: unknown): string | undefined => {
  if (typeof value === 'string' && value.trim()) {
    return value
  }

  return undefined
}

const parseOrderSummary = (value: unknown): OrderSummary => {
  const record = typeof value === 'object' && value !== null ? value : {}
  const raw = record as Record<string, unknown>

  return {
    id: String(raw.id ?? raw.orderId ?? raw.clOrdId ?? '--'),
    status: toStringOrUndefined(raw.status ?? raw.ordStatus),
    symbol: toStringOrUndefined(raw.symbol),
    side: toStringOrUndefined(raw.side),
    qty: toNumberOrNull(raw.qty ?? raw.orderQty),
    type: toStringOrUndefined(raw.type ?? raw.ordType),
    price: toNumberOrNull(raw.price),
    tif: toStringOrUndefined(raw.tif ?? raw.timeInForce),
    updatedAt: toStringOrUndefined(raw.updatedAt ?? raw.lastUpdatedAt),
    message: toStringOrUndefined(raw.message ?? raw.text),
  }
}

const parseOrderDetails = (orderId: string, value: unknown): OrderDetails => {
  const summary = parseOrderSummary(value)
  const record = typeof value === 'object' && value !== null ? value : {}
  const raw = record as Record<string, unknown>

  return {
    ...summary,
    id: summary.id || orderId,
    lastExecType: toStringOrUndefined(raw.lastExecType),
    lastOrdStatus: toStringOrUndefined(raw.lastOrdStatus),
    cumQty: toNumberOrNull(raw.cumQty),
    leavesQty: toNumberOrNull(raw.leavesQty),
    avgPx: toNumberOrNull(raw.avgPx),
    lastPx: toNumberOrNull(raw.lastPx),
    text: toStringOrUndefined(raw.text),
    transactTime: toStringOrUndefined(raw.transactTime),
  }
}

const formatValue = (value: string | number | null | undefined): string => {
  if (value === null || value === undefined || value === '') {
    return '--'
  }

  return String(value)
}

const ordersPortHint =
  'Check dev proxy target and restart frontend with VITE_BACKEND_PORT set to the backend that serves /orders (for example: VITE_BACKEND_PORT=8081 npm run dev).'

function App() {
  const [message, setMessage] = useState('Checking backend health...')
  const [fixStatus, setFixStatus] = useState<FixStatusResponse | null>(null)
  const [fixError, setFixError] = useState<string | null>(null)

  const [orders, setOrders] = useState<OrderSummary[]>([])
  const [ordersError, setOrdersError] = useState<string | null>(null)
  const [ordersReady, setOrdersReady] = useState(false)

  const [submitResult, setSubmitResult] = useState<SubmitResult | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [formValues, setFormValues] = useState(defaultFormState)
  const [formError, setFormError] = useState<string | null>(null)

  const [selectedOrderId, setSelectedOrderId] = useState<string | null>(null)
  const [orderDetails, setOrderDetails] = useState<OrderDetails | null>(null)
  const [detailsError, setDetailsError] = useState<string | null>(null)
  const [detailsLoading, setDetailsLoading] = useState(false)

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
        const baseMessage = `Unable to load orders (request failed with status ${response.status}).`
        setOrdersError(response.status === 404 ? `${baseMessage} ${ordersPortHint}` : baseMessage)
        setOrdersReady(true)
        return
      }

      const payload = (await response.json()) as unknown
      const list = Array.isArray(payload)
        ? payload.map((entry) => parseOrderSummary(entry))
        : []

      setOrders(list)
      setOrdersError(null)
      setOrdersReady(true)
    } catch {
      setOrdersError('Unable to load orders right now. Please try again.')
      setOrdersReady(true)
    }
  }

  const fetchOrderDetails = async (orderId: string) => {
    setDetailsLoading(true)
    setDetailsError(null)

    try {
      const response = await fetch(`/orders/${orderId}`)

      if (!response.ok) {
        setDetailsError(
          `Unable to load order details (request failed with status ${response.status}).`,
        )
        setOrderDetails(null)
        return
      }

      const payload = (await response.json()) as unknown
      setOrderDetails(parseOrderDetails(orderId, payload))
    } catch {
      setDetailsError('Unable to load order details right now. Please try again.')
      setOrderDetails(null)
    } finally {
      setDetailsLoading(false)
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

  const onSubmitOrder = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setSubmitResult(null)
    setFormError(null)

    const qty = Number(formValues.qty)
    const isLimitOrder = formValues.type === 'LIMIT'
    const price = formValues.price ? Number(formValues.price) : NaN

    if (!formValues.symbol.trim()) {
      setFormError('Symbol is required.')
      return
    }

    if (!Number.isFinite(qty) || qty <= 0) {
      setFormError('Quantity must be a positive number.')
      return
    }

    if (isLimitOrder && (!Number.isFinite(price) || price <= 0)) {
      setFormError('LIMIT orders require a valid price.')
      return
    }

    const payload: Record<string, unknown> = {
      symbol: formValues.symbol.trim().toUpperCase(),
      side: formValues.side,
      qty,
      type: formValues.type,
      tif: formValues.tif,
    }

    if (isLimitOrder) {
      payload.price = price
    }

    setSubmitting(true)

    try {
      const response = await fetch('/orders', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(payload),
      })

      let responseMessage = ''
      try {
        const body = (await response.json()) as Record<string, unknown>
        responseMessage =
          toStringOrUndefined(body.message) ??
          toStringOrUndefined(body.status) ??
          toStringOrUndefined(body.orderId) ??
          ''
      } catch {
        responseMessage = ''
      }

      if (!response.ok) {
        const submitError =
          response.status === 404
            ? `Order submit failed with status ${response.status}. ${ordersPortHint}`
            : `Order submit failed with status ${response.status}.`
        setSubmitResult({
          kind: 'error',
          message: responseMessage || submitError,
        })
        return
      }

      setSubmitResult({
        kind: 'success',
        message: responseMessage || 'Order submitted successfully.',
      })

      setFormValues((current) => ({
        ...current,
        price: current.type === 'LIMIT' ? current.price : '',
      }))
      await fetchOrders()
    } catch {
      setSubmitResult({
        kind: 'error',
        message: 'Unable to submit order right now. Please try again.',
      })
    } finally {
      setSubmitting(false)
    }
  }

  useEffect(() => {
    void checkHealth()
    void fetchFixStatus()
    void fetchOrders()

    const pollId = window.setInterval(() => {
      void fetchFixStatus()
      void fetchOrders()
    }, 2000)

    return () => {
      window.clearInterval(pollId)
    }
  }, [])

  useEffect(() => {
    if (!selectedOrderId) {
      setOrderDetails(null)
      setDetailsError(null)
      return
    }

    void fetchOrderDetails(selectedOrderId)
  }, [selectedOrderId])

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

  const statusClassName = useMemo(
    () => `status-badge status-${currentStatus.toLowerCase()}`,
    [currentStatus],
  )

  const hasExecutionFields = useMemo(() => {
    if (!orderDetails) {
      return false
    }

    return [
      orderDetails.lastExecType,
      orderDetails.lastOrdStatus,
      orderDetails.cumQty,
      orderDetails.leavesQty,
      orderDetails.avgPx,
      orderDetails.lastPx,
      orderDetails.transactTime,
      orderDetails.text,
    ].some((value) => value !== undefined && value !== null && value !== '')
  }, [orderDetails])

  return (
    <main className="app">
      <header className="app-header">
        <h1>Trading UI</h1>
        <p>{message}</p>
        <button type="button" onClick={() => void checkHealth()}>
          Retry Health
        </button>
      </header>

      <section className="fix-controls" aria-label="FIX controls">
        <h2>FIX Session</h2>
        <p className="status-row">
          Status: <strong className={statusClassName}>{currentStatus}</strong>
        </p>
        <p>
          Session: {sender} -&gt; {target}
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

      <section className="ticket" aria-label="New order ticket">
        <h2>New Order</h2>
        <form className="ticket-form" onSubmit={onSubmitOrder}>
          <label>
            Symbol
            <input
              name="symbol"
              value={formValues.symbol}
              onChange={(event) =>
                setFormValues((current) => ({
                  ...current,
                  symbol: event.target.value,
                }))
              }
            />
          </label>

          <label>
            Side
            <select
              name="side"
              value={formValues.side}
              onChange={(event) =>
                setFormValues((current) => ({
                  ...current,
                  side: event.target.value as OrderSide,
                }))
              }
            >
              <option value="BUY">BUY</option>
              <option value="SELL">SELL</option>
            </select>
          </label>

          <label>
            Qty
            <input
              name="qty"
              type="number"
              min="1"
              value={formValues.qty}
              onChange={(event) =>
                setFormValues((current) => ({
                  ...current,
                  qty: event.target.value,
                }))
              }
            />
          </label>

          <label>
            Type
            <select
              name="type"
              value={formValues.type}
              onChange={(event) =>
                setFormValues((current) => ({
                  ...current,
                  type: event.target.value as OrderType,
                }))
              }
            >
              <option value="MARKET">MARKET</option>
              <option value="LIMIT">LIMIT</option>
            </select>
          </label>

          <label>
            Price
            <input
              name="price"
              type="number"
              min="0"
              step="0.01"
              placeholder={formValues.type === 'LIMIT' ? 'Required for LIMIT' : 'Optional'}
              value={formValues.price}
              onChange={(event) =>
                setFormValues((current) => ({
                  ...current,
                  price: event.target.value,
                }))
              }
            />
          </label>

          <label>
            TIF
            <select
              name="tif"
              value={formValues.tif}
              onChange={(event) =>
                setFormValues((current) => ({
                  ...current,
                  tif: event.target.value as TimeInForce,
                }))
              }
            >
              <option value="DAY">DAY</option>
              <option value="IOC">IOC</option>
              <option value="FOK">FOK</option>
              <option value="GTC">GTC</option>
            </select>
          </label>

          <button type="submit" disabled={submitting}>
            {submitting ? 'Submitting...' : 'Submit Order'}
          </button>
        </form>

        {formError ? <p className="error-message">{formError}</p> : null}
        {submitResult ? (
          <p
            className={submitResult.kind === 'error' ? 'error-message' : 'success-message'}
            role="status"
          >
            {submitResult.message}
          </p>
        ) : null}
      </section>

      <section className="blotter" aria-label="Orders blotter">
        <h2>Orders Blotter</h2>
        {ordersError ? <p className="error-message">{ordersError}</p> : null}
        <table>
          <thead>
            <tr>
              <th>Status</th>
              <th>Symbol</th>
              <th>Side</th>
              <th>Qty</th>
              <th>Type</th>
              <th>Price</th>
              <th>TIF</th>
              <th>Updated</th>
              <th>Message</th>
            </tr>
          </thead>
          <tbody>
            {orders.length === 0 ? (
              <tr>
                <td colSpan={9}>{ordersReady ? 'No orders yet.' : 'Loading orders...'}</td>
              </tr>
            ) : (
              orders.map((order) => (
                <tr key={order.id}>
                  <td>
                    <button
                      type="button"
                      className="row-button"
                      onClick={() => setSelectedOrderId(order.id)}
                    >
                      <span
                        className={`status-badge status-${(order.status || 'unknown').toLowerCase()}`}
                      >
                        {formatValue(order.status)}
                      </span>
                    </button>
                  </td>
                  <td>{formatValue(order.symbol)}</td>
                  <td>{formatValue(order.side)}</td>
                  <td>{formatValue(order.qty)}</td>
                  <td>{formatValue(order.type)}</td>
                  <td>{formatValue(order.price)}</td>
                  <td>{formatValue(order.tif)}</td>
                  <td>{formatValue(order.updatedAt)}</td>
                  <td>{formatValue(order.message)}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </section>

      <aside className={`details ${selectedOrderId ? 'open' : ''}`} aria-label="Order details">
        <div className="details-header">
          <h2>Order Details</h2>
          <button type="button" onClick={() => setSelectedOrderId(null)}>
            Close
          </button>
        </div>

        {!selectedOrderId ? <p>Select an order from the blotter.</p> : null}
        {detailsLoading ? <p>Loading details...</p> : null}
        {detailsError ? <p className="error-message">{detailsError}</p> : null}

        {selectedOrderId && !detailsLoading && orderDetails ? (
          <dl className="details-grid">
            <dt>ID</dt>
            <dd>{formatValue(orderDetails.id)}</dd>
            <dt>Status</dt>
            <dd>{formatValue(orderDetails.status)}</dd>
            <dt>Symbol</dt>
            <dd>{formatValue(orderDetails.symbol)}</dd>
            <dt>Side</dt>
            <dd>{formatValue(orderDetails.side)}</dd>
            <dt>Qty</dt>
            <dd>{formatValue(orderDetails.qty)}</dd>
            <dt>Type</dt>
            <dd>{formatValue(orderDetails.type)}</dd>
            <dt>Price</dt>
            <dd>{formatValue(orderDetails.price)}</dd>
            <dt>TIF</dt>
            <dd>{formatValue(orderDetails.tif)}</dd>
            <dt>Updated</dt>
            <dd>{formatValue(orderDetails.updatedAt)}</dd>
            <dt>Message</dt>
            <dd>{formatValue(orderDetails.message)}</dd>

            {hasExecutionFields ? (
              <>
                <dt>Last Exec Type</dt>
                <dd>{formatValue(orderDetails.lastExecType)}</dd>
                <dt>Last Ord Status</dt>
                <dd>{formatValue(orderDetails.lastOrdStatus)}</dd>
                <dt>Cum Qty</dt>
                <dd>{formatValue(orderDetails.cumQty)}</dd>
                <dt>Leaves Qty</dt>
                <dd>{formatValue(orderDetails.leavesQty)}</dd>
                <dt>Avg Px</dt>
                <dd>{formatValue(orderDetails.avgPx)}</dd>
                <dt>Last Px</dt>
                <dd>{formatValue(orderDetails.lastPx)}</dd>
                <dt>Transact Time</dt>
                <dd>{formatValue(orderDetails.transactTime)}</dd>
                <dt>Execution Text</dt>
                <dd>{formatValue(orderDetails.text)}</dd>
              </>
            ) : null}
          </dl>
        ) : null}
      </aside>
    </main>
  )
}

export default App
