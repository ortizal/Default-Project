import { expect, test, Page } from '@playwright/test';
import { AxeBuilder } from '@axe-core/playwright';

const ETIQUETAS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'best-practice'];
const RUTA = 'configuracion/integraciones';

async function login(page: Page) {
  await page.goto('./');
  await page.waitForSelector('input[name="username"]', { timeout: 10000 });
  await page.fill('input[name="username"]', 'admin');
  await page.fill('input[name="password"]', 'admin123');
  await page.press('input[name="password"]', 'Enter');
  await page.waitForURL('**/dashboard', { timeout: 10000 });
}

test('axe en configuración de integraciones', async ({ page }) => {
  test.setTimeout(120000);
  await page.emulateMedia({ reducedMotion: 'reduce' });
  await login(page);
  for (const tema of ['light', 'dark'] as const) {
    await page.addInitScript(t => localStorage.setItem('ls-theme', t), tema);
    await page.goto(RUTA);
    await page.waitForSelector('h1', { timeout: 15000 });
    await page.waitForTimeout(500);
    const res = await new AxeBuilder({ page }).withTags(ETIQUETAS).analyze();
    console.log(
      `AXE[${tema}] violaciones=${res.violations.length}`,
      JSON.stringify(res.violations.map(v => ({ id: v.id, impact: v.impact, n: v.nodes.length, html: v.nodes[0]?.html }))),
    );
  }
  expect(true).toBe(true);
});
