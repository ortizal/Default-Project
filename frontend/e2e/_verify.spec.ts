import { test, expect, Page } from '@playwright/test';

async function login(page: Page) {
  await page.goto('./');
  await page.waitForSelector('input[name="username"]', { timeout: 10000 });
  await page.fill('input[name="username"]', 'admin');
  await page.fill('input[name="password"]', 'admin123');
  await page.press('input[name="password"]', 'Enter');
  await page.waitForURL('**/dashboard', { timeout: 10000 });
}

test('verifica mejoras visuales', async ({ browser }) => {
  test.setTimeout(180000);
  const page: Page = await browser.newPage({ viewport: { width: 1440, height: 1000 } });
  await login(page);

  await page.goto('dashboard');
  await expect(page.getByRole('heading', { name: 'Resumen del día' })).toBeVisible({ timeout: 15000 });
  const stats = await page.locator('.stat').evaluateAll(els =>
    els.map(el => ({
      lbl: (el.querySelector('.lbl') as HTMLElement)?.textContent?.trim(),
      num: (el.querySelector('.num') as HTMLElement)?.className,
      filete: getComputedStyle(el, '::before').backgroundColor,
    })),
  );
  console.log('STATS', JSON.stringify(stats, null, 1));

  await page.goto('usuarios');
  await expect(page.getByRole('heading', { name: 'Usuarios' }).first()).toBeVisible({ timeout: 15000 });
  const btn = page.locator('button.btn.outline.danger').first();
  const btnDatos = await btn.evaluate(el => {
    const s = getComputedStyle(el);
    return { text: el.textContent?.trim(), color: s.color, bg: s.backgroundColor, border: s.borderColor, h: s.minHeight };
  });
  console.log('BOTON_PELIGRO', JSON.stringify(btnDatos));

  const iconos = await page.locator('.sidebar-nav a').evaluateAll(els =>
    els.map(el => ({
      label: el.textContent?.trim(),
      icono: (el.querySelector('mat-icon') as HTMLElement)?.textContent?.trim(),
    })),
  );
  console.log('ICONOS', JSON.stringify(iconos));

  await page.close();
});
