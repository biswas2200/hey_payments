import { test, expect } from '@playwright/test';
import * as path from 'node:path';

test.describe('Hey Payments PWA Smoke Test', () => {
  test('should render styled PWA with zero console errors, Calm Ledger theme, status banner, and active service worker', async ({
    page,
  }) => {
    const consoleErrors: string[] = [];
    page.on('console', (msg) => {
      if (msg.type() === 'error') {
        consoleErrors.push(msg.text());
      }
    });

    page.on('pageerror', (err) => {
      consoleErrors.push(err.message);
    });

    const response = await page.goto('/', { waitUntil: 'networkidle' });
    expect(response?.status()).toBe(200);

    // 1. Assert zero console errors (specifically no CSP or Angular runtime errors)
    expect(consoleErrors).toEqual([]);

    // 2. Assert body background is Calm Ledger paper color: rgb(247, 245, 240)
    const bodyBg = await page.evaluate(() => {
      return window.getComputedStyle(document.body).backgroundColor;
    });
    expect(bodyBg).toBe('rgb(247, 245, 240)');

    // 3. Assert a visible status banner
    const banner = page.locator('.connectivity-banner');
    await expect(banner).toBeVisible();
    await expect(banner).toContainText('Online · synced');

    // 4. Assert brand logo is rendered
    const logo = page.locator('img.app-logo');
    await expect(logo).toBeVisible();

    // 5. Assert service worker is registered
    const hasServiceWorker = await page.evaluate(async () => {
      if (!('serviceWorker' in navigator)) return false;
      const registration = await navigator.serviceWorker.getRegistration();
      return !!registration;
    });
    expect(hasServiceWorker).toBe(true);

    // Capture light mode screenshot for review evidence
    const screenshotPath = path.resolve(__dirname, '../docs/design/screenshots/phase-0-shell-light.png');
    await page.screenshot({ path: screenshotPath, fullPage: true });
  });

  test('should support dark color scheme with dark background', async ({ page }) => {
    await page.emulateMedia({ colorScheme: 'dark' });
    await page.goto('/', { waitUntil: 'networkidle' });

    const bodyBg = await page.evaluate(() => {
      return window.getComputedStyle(document.body).backgroundColor;
    });
    // Dark mode background is #0B1420 -> rgb(11, 20, 32)
    expect(bodyBg).toBe('rgb(11, 20, 32)');

    // Capture dark mode screenshot for review evidence
    const screenshotPath = path.resolve(__dirname, '../docs/design/screenshots/phase-0-shell-dark.png');
    await page.screenshot({ path: screenshotPath, fullPage: true });
  });
});
