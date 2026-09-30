import { test, expect, Page } from '@playwright/test';

async function login(page: Page) {
  await page.goto('./');
  await page.waitForSelector('input[name="username"]', { timeout: 10000 });
  await page.fill('input[name="username"]', 'admin');
  await page.fill('input[name="password"]', 'admin123');
  await page.press('input[name="password"]', 'Enter');
  await page.waitForURL('**/dashboard', { timeout: 10000 });
}

test('probe file button', async ({ browser }) => {
  test.setTimeout(120000);
  for (const tema of ['light', 'dark'] as const) {
    const page: Page = await browser.newPage({ viewport: { width: 1440, height: 1000 } });
    await page.addInitScript(t => localStorage.setItem('ls-theme', t), tema);
    await login(page);
    await page.goto('redes-sociales');
    await expect(page.getByRole('heading', { name: 'Crear publicación' })).toBeVisible({ timeout: 15000 });
    const input = page.locator('input[type="file"]').first();
    const estilos = await input.evaluate(el => {
      const b = getComputedStyle(el, '::file-selector-button');
      return { bg: b.backgroundColor, color: b.color, border: b.borderColor, padding: b.padding, font: b.fontSize };
    });
    console.log('FILEBTN[' + tema + ']', JSON.stringify(estilos));
    const card = page.locator('section.card').nth(1);
    await card.scrollIntoViewIfNeeded();
    await page.waitForTimeout(300);
    await card.screenshot({ path: `/tmp/opencode/shots/publish-${tema}.png` });
    await page.close();
  }
});
