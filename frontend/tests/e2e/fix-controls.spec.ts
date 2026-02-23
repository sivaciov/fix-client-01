import { expect, test } from '@playwright/test'

const uiTimeoutMs = 20_000
const apiTimeoutMs = 20_000

test('Trading ticket + blotter + details flow works against live API', async ({ page, request }) => {
  await page.goto('/')

  await expect(page.getByRole('heading', { name: 'Trading UI' })).toBeVisible({
    timeout: uiTimeoutMs,
  })
  await expect(page.getByText('Backend: ok')).toBeVisible({ timeout: uiTimeoutMs })

  const ticket = page.getByRole('region', { name: 'New order ticket' })
  await expect(ticket).toBeVisible({ timeout: uiTimeoutMs })

  const symbol = `TSLA${Date.now().toString().slice(-5)}`
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

  const submitPayload = (await submitResponse.json()) as Record<string, unknown>
  const createdOrderId =
    typeof submitPayload.orderId === 'string'
      ? submitPayload.orderId
      : typeof submitPayload.id === 'string'
        ? submitPayload.id
        : null

  expect(createdOrderId).toBeTruthy()

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
    const detailsApi = await request.get(`/orders/${createdOrderId}`, {
      failOnStatusCode: false,
    })
    expect(detailsApi.ok()).toBeTruthy()

    const simulateResponse = await request.post('/exec-reports/simulate', {
      data: {
        orderId: createdOrderId,
        execType: '2',
        ordStatus: '2',
        leavesQty: 0,
        cumQty: 10,
        avgPx: 250.5,
        lastPx: 250.5,
        text: 'Simulated fill from Playwright',
      },
      failOnStatusCode: false,
    })

    if (simulateResponse.status() !== 404) {
      expect(simulateResponse.ok()).toBeTruthy()

      await expect
        .poll(
          async () => {
            const statusCell = await blotter
              .locator('tbody tr', { hasText: symbol })
              .first()
              .locator('td')
              .first()
              .innerText()
            return statusCell.trim().toUpperCase()
          },
          { timeout: uiTimeoutMs },
        )
        .not.toBe(initialStatus.toUpperCase())
    }
  }
})
