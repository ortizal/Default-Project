import { test, Page, expect } from '@playwright/test';
import { mkdirSync } from 'node:fs';

const OUT = '/tmp/opencode/shots';
const RUTAS = ['dashboard', 'pacientes', 'usuarios', 'redes-sociales', 'horarios'];

test('capturas', async ({ browser }) => {
  test.setTimeout(300000);
  mkdirSync(OUT, { recursive: true });
  for (const tema of ['light', 'dark'] as const) {
    const page: Page = await browser.newPage({ viewport: { width: 1440, height: 900 } });
    await page.addInitScript(t => localStorage.setItem('ls-theme', t), tema);
    await page.goto('./');
    await page.waitForSelector('input[name="username"]', { timeout: 15000 });
    await page.screenshot({ path: `${OUT}/${tema}-login.png`, fullPage: true });
    await page.fill('input[name="username"]', 'admin');
    await page.fill('input[name="password"]', 'admin123');
    await page.press('input[name="password"]', 'Enter');
    await page.waitForURL('**/dashboard', { timeout: 15000 });
    for (const ruta of RUTAS) {
      await page.goto(ruta);
      await page.waitForLoadState('networkidle').catch(() => undefined);
      await page.waitForTimeout(700);
      await page.screenshot({ path: `${OUT}/${tema}-${ruta.replace(/\//g, '_')}.png`, fullPage: true });
    }
    await page.close();
  }
  expect(true).toBe(true);
});
