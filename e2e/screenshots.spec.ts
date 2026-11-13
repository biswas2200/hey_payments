import { test } from '@playwright/test';
import * as path from 'node:path';

test.describe('Auth Screens Visual Capture', () => {
  const mailpitUrl = process.env['MAILPIT_URL'] || 'http://127.0.0.1:8025';

  async function getLatestOtpFromMailpit(email: string): Promise<string> {
    let attempts = 0;
    while (attempts < 20) {
      const resp = await fetch(`${mailpitUrl}/api/v1/messages`);
      if (resp.ok) {
        const data = (await resp.json()) as { messages?: Array<{ Snippet: string; To?: Array<{ Address: string }> }> };
        if (data.messages && data.messages.length > 0) {
          for (const msg of data.messages) {
            if (msg.To?.some(t => t.Address.includes(email))) {
              const match = msg.Snippet.match(/\b\d{6}\b/);
              if (match) return match[0];
            }
          }
        }
      }
      await new Promise((r) => setTimeout(r, 500));
      attempts++;
    }
    return '123456';
  }

  for (const theme of ['light', 'dark'] as const) {
    test(`capture all 5 auth screens in ${theme} mode`, async ({ page }) => {
      const screenshotsDir = path.resolve(__dirname, '../docs/design/screenshots');

      if (theme === 'dark') {
        await page.emulateMedia({ colorScheme: 'dark' });
      } else {
        await page.emulateMedia({ colorScheme: 'light' });
      }

      const applyTheme = async () => {
        if (theme === 'dark') {
          await page.evaluate(() => document.documentElement.setAttribute('data-theme', 'dark'));
        }
      };

      // 1. Login Screen
      await page.goto('/auth/login', { waitUntil: 'networkidle' });
      await applyTheme();
      await page.screenshot({
        path: path.join(screenshotsDir, `phase-1-login-${theme}.png`),
        fullPage: true,
      });

      // 2. Register Screen
      await page.goto('/auth/register', { waitUntil: 'networkidle' });
      await applyTheme();
      await page.screenshot({
        path: path.join(screenshotsDir, `phase-1-register-${theme}.png`),
        fullPage: true,
      });

      // 3. Verify OTP Screen (prefilled phone query param)
      await page.goto('/auth/verify-otp?phone=%2B919876543210', { waitUntil: 'networkidle' });
      await applyTheme();
      await page.screenshot({
        path: path.join(screenshotsDir, `phase-1-verify-otp-${theme}.png`),
        fullPage: true,
      });

      // Register and login to access Device Register & Set PIN
      const ts = Date.now().toString().slice(-6);
      const phoneDigits = `98761${ts}`.slice(0, 10);
      const email = `payer_shot_${theme}_${ts}@example.com`;
      const password = 'Password123!';

      await page.goto('/auth/register', { waitUntil: 'networkidle' });
      await applyTheme();
      await page.fill('input#reg-phone', phoneDigits);
      await page.fill('input#reg-email', email);
      await page.fill('input#reg-password', password);
      await page.click('button[type="submit"]');

      await page.waitForURL((url) => url.pathname.includes('/auth/verify-otp'));
      await applyTheme();
      const otp = await getLatestOtpFromMailpit(email);
      await page.fill('input#otp', otp);
      await page.click('button[type="submit"]');

      await page.waitForURL((url) => url.pathname.includes('/auth/login'));
      await applyTheme();
      await page.fill('input#phone', phoneDigits);
      await page.fill('input#password', password);
      await page.click('button[type="submit"]');

      // 4. Device Register Screen
      await page.waitForURL((url) => url.pathname.includes('/auth/device-register'));
      await applyTheme();
      await page.screenshot({
        path: path.join(screenshotsDir, `phase-1-device-register-${theme}.png`),
        fullPage: true,
      });

      await page.fill('input#device-label', 'Pixel 7 Pro');
      await page.click('button[type="submit"]');

      // 5. Set PIN Screen
      await page.waitForURL((url) => url.pathname.includes('/auth/set-pin'));
      await applyTheme();
      await page.screenshot({
        path: path.join(screenshotsDir, `phase-1-set-pin-${theme}.png`),
        fullPage: true,
      });
    });
  }
});
