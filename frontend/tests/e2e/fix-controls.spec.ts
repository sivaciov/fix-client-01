import { expect, test } from '@playwright/test'

test('Health, FIX dashboard, and order ticket flow works', async ({ page }) => {
  await page.goto('/')

  await expect(page.getByRole('heading', { name: 'Health Check' })).toBeVisible()
  await expect(page.getByText('Backend: ok')).toBeVisible()

  const fixSection = page.getByRole('region', { name: 'FIX controls' })
  await expect(fixSection).toBeVisible()
  await expect(fixSection.getByRole('heading', { name: 'FIX Session' })).toBeVisible()

  const startButton = fixSection.getByRole('button', { name: 'Start' })
  const stopButton = fixSection.getByRole('button', { name: 'Stop' })
  await expect(startButton).toBeVisible()
  await expect(stopButton).toBeVisible()

  const orderForm = page.getByRole('region', { name: 'New order form' })
  await expect(orderForm).toBeVisible()
  await expect(orderForm.getByRole('heading', { name: 'New Order' })).toBeVisible()

  await orderForm.getByLabel('Symbol').fill('AAPL')
  await orderForm.getByLabel('Type').selectOption('MARKET')

  const createOrderResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/orders') && response.request().method() === 'POST',
  )

  await orderForm.getByRole('button', { name: 'Send Order' }).click()

  const createOrderResponse = await createOrderResponsePromise
  expect(createOrderResponse.ok()).toBeTruthy()
  await expect(page.getByText(/REJECTED:/)).toBeVisible()

  const ordersTable = page.getByRole('region', { name: 'Orders table' })
  await expect(ordersTable).toBeVisible()
  await expect(ordersTable.getByRole('heading', { name: 'Orders' })).toBeVisible()
  await expect(ordersTable.getByText('AAPL')).toBeVisible()
  await expect(ordersTable.getByRole('cell', { name: 'REJECTED', exact: true })).toBeVisible()
})
