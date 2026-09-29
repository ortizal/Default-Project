import { test, expect, Page } from '@playwright/test';

const BG_CLARO = 'rgb(246, 248, 244)'; // --ls-background (light)
const BG_OSCURO = 'rgb(14, 23, 18)'; // --ls-background (dark)

async function login(page: Page) {
  await page.goto('./');
  await page.waitForSelector('input[name="username"]', { timeout: 10000 });
  await page.fill('input[name="username"]', 'admin');
  await page.fill('input[name="password"]', 'admin123');
  await page.press('input[name="password"]', 'Enter');
  await page.waitForURL('**/dashboard', { timeout: 10000 });
}

async function bg(page: Page): Promise<string> {
  return page.evaluate(() => getComputedStyle(document.body).backgroundColor);
}

async function clases(page: Page): Promise<string[]> {
  return page.evaluate(() => Array.from(document.documentElement.classList));
}

test.describe('Tema claro/oscuro — toggle manual', () => {
  let page: Page;

  test.beforeEach(async ({ browser }) => {
    page = await browser.newPage();
  });

  test.afterEach(async () => {
    await page.close();
  });

  test('sigue al sistema mientras no haya preferencia guardada', async () => {
    await page.emulateMedia({ colorScheme: 'light' });
    await login(page);
    expect(await bg(page)).toBe(BG_CLARO);
    expect(await clases(page)).toContain('theme-light');

    // El sistema manda mientras no haya elección guardada (clase siempre presente)
    await page.emulateMedia({ colorScheme: 'dark' });
    await expect.poll(() => bg(page)).toBe(BG_OSCURO);
    expect(await clases(page)).toContain('theme-dark');
    expect(await page.evaluate(() => localStorage.getItem('ls-theme'))).toBeNull();
  });

  test('el botón del topbar cambia el tema y lo recuerda tras recargar', async () => {
    await page.emulateMedia({ colorScheme: 'light' });
    await login(page);

    const toggle = page.locator('.topbar-tema');
    await expect(toggle).toBeVisible();
    await expect(toggle).toHaveAttribute('aria-label', 'Cambiar a modo oscuro');
    await expect(toggle).toHaveAttribute('title', 'Cambiar a modo oscuro');

    await toggle.click();
    expect(await bg(page)).toBe(BG_OSCURO);
    expect(await page.evaluate(() => document.documentElement.classList.contains('theme-dark'))).toBe(
      true,
    );
    await expect(toggle).toHaveAttribute('aria-label', 'Cambiar a modo claro');

    // Persistencia (sin parpadeo: la clase se aplica antes del primer pintado)
    await page.reload();
    await expect.poll(() => bg(page)).toBe(BG_OSCURO);
    expect(await page.evaluate(() => localStorage.getItem('ls-theme'))).toBe('dark');

    // Y se puede volver a claro
    await page.locator('.topbar-tema').click();
    await expect.poll(() => bg(page)).toBe(BG_CLARO);
    expect(await page.evaluate(() => localStorage.getItem('ls-theme'))).toBe('light');
  });

  test('la preferencia manual gana sobre la del sistema', async () => {
    await page.emulateMedia({ colorScheme: 'dark' });
    await page.addInitScript(() => localStorage.setItem('ls-theme', 'light'));

    await login(page);
    expect(await bg(page)).toBe(BG_CLARO);
    expect(await page.evaluate(() => document.documentElement.classList.contains('theme-light'))).toBe(
      true,
    );
  });

  test('sin errores de consola al alternar el tema', async () => {
    const errores: string[] = [];
    page.on('console', msg => {
      if (msg.type() === 'error') errores.push(msg.text());
    });
    page.on('pageerror', err => errores.push(err.message));

    await page.emulateMedia({ colorScheme: 'light' });
    await login(page);

    const toggle = page.locator('.topbar-tema');
    await toggle.click();
    await expect.poll(() => bg(page)).toBe(BG_OSCURO);
    await toggle.click();
    await expect.poll(() => bg(page)).toBe(BG_CLARO);

    expect(errores).toEqual([]);
  });
});
