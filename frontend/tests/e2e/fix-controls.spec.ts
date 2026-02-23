import { expect, test } from '@playwright/test'

test('Health and FIX controls flow works', async ({ page }) => {
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
  await expect(fixSection.getByText(/Last event:/)).toBeVisible()
  await expect(fixSection.getByText(/Last error:/)).toBeVisible()
  await expect(fixSection.getByText(/Last updated:/)).toBeVisible()

  const startResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/fix/start') &&
      response.request().method() === 'POST',
  )
  await startButton.click()
  const startResponse = await startResponsePromise
  expect(startResponse.ok()).toBeTruthy()

  const stopResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/fix/stop') && response.request().method() === 'POST',
  )
  await stopButton.click()
  const stopResponse = await stopResponsePromise
  expect(stopResponse.ok()).toBeTruthy()

  await expect(fixSection.getByText(/Status:/)).toBeVisible()
})
