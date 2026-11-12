import { test, expect } from '@playwright/test';
import * as path from 'node:path';

test.describe('Hey Payments PWA Smoke Test', () => {
  test('should render styled PWA with zero console errors, Calm Ledger theme, status banner, light logo, and active service worker', async ({
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

    // 4. Assert brand logo is rendered with light wordmark visible and dark wordmark hidden
    const lightLogo = page.locator('img.app-logo--light');
    const darkLogo = page.locator('img.app-logo--dark');
    await expect(lightLogo).toBeVisible();
    await expect(darkLogo).toBeHidden();

    // 5. Assert service worker is registered
    const hasServiceWorker = await page.evaluate(async () => {
      if (!('serviceWorker' in navigator)) return false;
      const registration = await navigator.serviceWorker.getRegistration();
      return !!registration;
    });
    expect(hasServiceWorker).toBe(true);

    // Capture light mode screenshot to test-results (does not dirty docs/)
    const screenshotPath = path.resolve(__dirname, '../test-results/screenshots/phase-0-shell-light.png');
    await page.screenshot({ path: screenshotPath, fullPage: true });
  });

  test('should support dark color scheme with dark background and visible dark wordmark', async ({ page }) => {
    const consoleErrors: string[] = [];
    page.on('console', (msg) => {
      if (msg.type() === 'error') {
        consoleErrors.push(msg.text());
      }
    });

    page.on('pageerror', (err) => {
      consoleErrors.push(err.message);
    });

    await page.emulateMedia({ colorScheme: 'dark' });
    const response = await page.goto('/', { waitUntil: 'networkidle' });
    expect(response?.status()).toBe(200);

    // Assert zero console errors in dark mode
    expect(consoleErrors).toEqual([]);

    const bodyBg = await page.evaluate(() => {
      return window.getComputedStyle(document.body).backgroundColor;
    });
    // Dark mode background is #0B1420 -> rgb(11, 20, 32)
    expect(bodyBg).toBe('rgb(11, 20, 32)');

    // Banner is visible
    const banner = page.locator('.connectivity-banner');
    await expect(banner).toBeVisible();

    // Dark wordmark is visible, light wordmark is hidden
    const lightLogo = page.locator('img.app-logo--light');
    const darkLogo = page.locator('img.app-logo--dark');
    await expect(darkLogo).toBeVisible();
    await expect(lightLogo).toBeHidden();

    // Capture dark mode screenshot to test-results (does not dirty docs/)
    const screenshotPath = path.resolve(__dirname, '../test-results/screenshots/phase-0-shell-dark.png');
    await page.screenshot({ path: screenshotPath, fullPage: true });
  });

  test('should toggle dark wordmark when data-theme="dark" attribute is present', async ({ page }) => {
    await page.goto('/', { waitUntil: 'networkidle' });
    await page.evaluate(() => {
      document.documentElement.setAttribute('data-theme', 'dark');
    });

    const lightLogo = page.locator('img.app-logo--light');
    const darkLogo = page.locator('img.app-logo--dark');
    await expect(darkLogo).toBeVisible();
    await expect(lightLogo).toBeHidden();
  });
});
