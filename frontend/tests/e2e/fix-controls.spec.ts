import { expect, test } from '@playwright/test'

const uiTimeoutMs = 20_000
const apiTimeoutMs = 20_000

test('Trading ticket + blotter + details flow works', async ({ page }) => {
  const mockedOrders: Array<Record<string, unknown>> = []

  await page.route('**/health', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ status: 'ok' }),
    })
  })

  await page.route('**/fix/status', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        status: 'RUNNING',
        details: 'Connected',
        config: {
          senderCompId: 'SENDER',
          targetCompId: 'TARGET',
          host: 'localhost',
          port: 9876,
        },
        diagnostics: {
          lastEvent: 'Connected',
          lastError: '',
          lastUpdatedAt: new Date().toISOString(),
        },
      }),
    })
  })

  await page.route('**/orders', async (route) => {
    const method = route.request().method()

    if (method === 'GET') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(mockedOrders),
      })
      return
    }

    if (method === 'POST') {
      const payload = route.request().postDataJSON() as Record<string, unknown>
      const id = `ord-${Date.now()}`
      const now = new Date().toISOString()
      const order = {
        id,
        status: 'NEW',
        symbol: payload.symbol,
        side: payload.side,
        qty: payload.qty,
        type: payload.type,
        price: payload.price ?? null,
        tif: payload.tif,
        updatedAt: now,
        message: 'Accepted',
      }

      mockedOrders.unshift(order)

      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ orderId: id, status: 'NEW', message: 'Accepted' }),
      })
      return
    }

    await route.fallback()
  })

  await page.route('**/orders/*', async (route) => {
    const method = route.request().method()
    if (method !== 'GET') {
      await route.fallback()
      return
    }

    const requestUrl = new URL(route.request().url())
    const id = requestUrl.pathname.split('/').pop()
    const order = mockedOrders.find((entry) => entry.id === id)

    if (!order) {
      await route.fulfill({
        status: 404,
        contentType: 'application/json',
        body: JSON.stringify({ message: 'Order not found' }),
      })
      return
    }

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(order),
    })
  })

  await page.route('**/exec-reports/simulate', async (route) => {
    const payload = route.request().postDataJSON() as Record<string, unknown>
    const orderId = payload.orderId
    const targetOrder = mockedOrders.find((entry) => entry.id === orderId)

    if (!targetOrder) {
      await route.fulfill({
        status: 404,
        contentType: 'application/json',
        body: JSON.stringify({ message: 'Order not found' }),
      })
      return
    }

    Object.assign(targetOrder, {
      status: 'FILLED',
      message: 'Simulated fill',
      updatedAt: new Date().toISOString(),
      lastExecType: '2',
      lastOrdStatus: '2',
      cumQty: targetOrder.qty,
      leavesQty: 0,
      avgPx: targetOrder.price,
      lastPx: targetOrder.price,
      text: 'Simulated fill',
    })

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ status: 'ok' }),
    })
  })

  await page.goto('/')

  await expect(page.getByRole('heading', { name: 'Trading UI' })).toBeVisible({
    timeout: uiTimeoutMs,
  })
  await expect(page.getByText('Backend: ok')).toBeVisible({ timeout: uiTimeoutMs })

  const ticket = page.getByRole('region', { name: 'New order ticket' })
  await expect(ticket).toBeVisible({ timeout: uiTimeoutMs })

  const symbol = `TSLA${Date.now().toString().slice(-4)}`
  await ticket.getByLabel('Symbol').fill(symbol)
  await ticket.getByLabel('Side').selectOption('BUY')
  await ticket.getByLabel('Qty').fill('10')
  await ticket.getByLabel('Type').selectOption('LIMIT')
  await ticket.getByLabel('Price').fill('250.50')
  await ticket.getByLabel('TIF').selectOption('DAY')

  const submitResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/orders') && response.request().method() === 'POST',
    { timeout: apiTimeoutMs },
  )
  await ticket.getByRole('button', { name: 'Submit Order' }).click()
  const submitResponse = await submitResponsePromise
  expect(submitResponse.ok()).toBeTruthy()

  let createdOrderId: string | null = null
  try {
    const payload = (await submitResponse.json()) as Record<string, unknown>
    createdOrderId =
      typeof payload.id === 'string'
        ? payload.id
        : typeof payload.orderId === 'string'
          ? payload.orderId
          : null
  } catch {
    createdOrderId = null
  }

  const blotter = page.getByRole('region', { name: 'Orders blotter' })
  await expect(blotter).toBeVisible({ timeout: uiTimeoutMs })

  const orderRow = blotter.locator('tbody tr', { hasText: symbol }).first()
  await expect(orderRow).toBeVisible({ timeout: uiTimeoutMs })

  const initialStatus = (await orderRow.locator('td').first().innerText()).trim()

  await orderRow.locator('button').first().click()
  const details = page.getByRole('complementary', { name: 'Order details' })
  await expect(details).toBeVisible({ timeout: uiTimeoutMs })
  await expect(details.getByText('Order Details')).toBeVisible({ timeout: uiTimeoutMs })

  if (createdOrderId) {
    const simulateSucceeded = await page.evaluate(async (orderId) => {
      const response = await fetch('/exec-reports/simulate', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ orderId }),
      })
      return response.ok
    }, createdOrderId)

    expect(simulateSucceeded).toBeTruthy()

    await expect
      .poll(
        async () => {
          const statusCell = await blotter
            .locator('tbody tr', { hasText: symbol })
            .first()
            .locator('td')
            .first()
            .innerText()
          return statusCell.trim()
        },
        { timeout: uiTimeoutMs },
      )
      .not.toBe(initialStatus)
  }
})
