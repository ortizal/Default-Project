import { test, expect, Page } from '@playwright/test';

test.describe('Network diagnostic', () => {
  let page: Page;

  test.beforeEach(async ({ browser }) => {
    page = await browser.newPage();
    page.on('request', req => console.log('REQ:', req.method(), req.url()));
    page.on('response', async res => {
      if (res.status() >= 400) {
        console.log('ERR:', res.status(), res.url(), await res.text().catch(() => ''));
      }
    });
  });

  test.afterEach(async () => {
    await page.close();
  });

  test('Trace all network requests on odontologos', async () => {
    await page.route('**/api/v1/odontologos**', async route => {
      console.log('INTERCEPT:', route.request().url());
      await route.continue();
    });

    await page.goto('/');
    await page.waitForSelector('input[name="username"]', { timeout: 10000 });
    await page.fill('input[name="username"]', 'admin');
    await page.fill('input[name="password"]', 'admin123');
    await page.press('input[name="password"]', 'Enter');
    await page.waitForURL('**/dashboard', { timeout: 10000 });
    
    await page.click('a[href="/odontologos"]');
    await page.waitForURL('**/odontologos', { timeout: 10000 });
    await page.waitForTimeout(5000);

    const spinnerVisible = await page.locator('.spinner').count();
    console.log('Spinner count:', spinnerVisible);
    
    const rows = await page.locator('tbody tr').count();
    console.log('Table rows:', rows);
    
    await page.screenshot({ path: 'test-results/diag2.png' });
  });
});
