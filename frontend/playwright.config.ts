import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 1,
  workers: 1,
  reporter: [['html', { open: 'never' }], ['json', { outputFile: 'test-results/results.json' }]],
  use: {
    // La app se sirve bajo /dental_crm/ (baseHref en angular.json).
    // Las rutas relativas de los specs se resuelven contra esta base.
    baseURL: 'http://localhost:4200/dental_crm/',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'on-first-retry',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  // Si el dev server ya está levantado se reutiliza; si no, se arranca solo.
  webServer: {
    command: 'npm run start -- --host 127.0.0.1 --port 4200 --proxy-config proxy.conf.json',
    url: 'http://localhost:4200/dental_crm/',
    reuseExistingServer: true,
    timeout: 120_000,
  },
});
