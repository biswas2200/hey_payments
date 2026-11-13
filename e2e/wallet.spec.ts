import { test, expect } from '@playwright/test';

test.describe('Hey Payments Payer Wallet & Funding E2E', () => {
  const mailpitUrl = process.env['MAILPIT_URL'] || 'http://127.0.0.1:8025';

  async function getLatestOtpFromMailpit(expectedEmailOrPhone: string): Promise<string> {
    let attempts = 0;
    while (attempts < 20) {
      const resp = await fetch(`${mailpitUrl}/api/v1/messages`);
      if (resp.ok) {
        const data = (await resp.json()) as {
          messages?: Array<{ ID: string; Snippet: string; To?: Array<{ Address: string }> }>;
        };
        if (data.messages && data.messages.length > 0) {
          for (const msg of data.messages) {
            const matchesRecipient =
              msg.To?.some((t) => t.Address.includes(expectedEmailOrPhone)) ||
              msg.Snippet.includes(expectedEmailOrPhone);
            if (matchesRecipient || !expectedEmailOrPhone) {
              const match = msg.Snippet.match(/\b\d{6}\b/);
              if (match) {
                return match[0];
              }
            }
          }
        }
      }
      await new Promise((r) => setTimeout(r, 500));
      attempts++;
    }
    throw new Error(`Failed to find OTP message in Mailpit for ${expectedEmailOrPhone}`);
  }

  async function registerAndLoginPayer(page: any): Promise<void> {
    const timestamp = Date.now().toString().slice(-6);
    const phoneDigits = `98765${timestamp}`.slice(0, 10);
    const email = `payer_wallet_${timestamp}@example.com`;
    const password = 'WalletPassword123!';
    const pin = '123456';

    // 1. Register
    await page.goto('/auth/register', { waitUntil: 'networkidle' });
    await page.fill('input#reg-phone', phoneDigits);
    await page.fill('input#reg-email', email);
    await page.fill('input#reg-password', password);
    await page.click('button[type="submit"]');

    // 2. Verify OTP
    await page.waitForURL((url: URL) => url.pathname.includes('/auth/verify-otp'));
    const otp = await getLatestOtpFromMailpit(email);
    await page.fill('input#otp', otp);
    await page.click('button[type="submit"]');

    // 3. Login
    await page.waitForURL((url: URL) => url.pathname.includes('/auth/login'));
    await page.fill('input#phone', phoneDigits);
    await page.fill('input#password', password);
    await page.click('button[type="submit"]');

    // 4. Device Register
    await page.waitForURL((url: URL) => url.pathname.includes('/auth/device-register'));
    await page.fill('input#device-label', 'Wallet Test Phone');
    await page.click('button[type="submit"]');

    // 5. Set PIN
    await page.waitForURL((url: URL) => url.pathname.includes('/auth/set-pin'));
    await page.fill('input#new-pin', pin);
    await page.fill('input#confirm-pin', pin);
    await page.fill('input#current-password', password);
    await page.click('button[type="submit"]');

    // 6. Arrive on /pay
    await page.waitForURL((url: URL) => url.pathname === '/pay');
  }

  test('Payer Home renders balances, navigates to Add Money, funds wallet, and updates balance', async ({
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

    await registerAndLoginPayer(page);

    // 1. Check initial home screen state
    await expect(page.locator('#offline-balance')).toBeVisible();
    await expect(page.locator('#offline-balance')).toContainText('₹0.00');
    await expect(page.locator('#main-balance')).toBeVisible();
    await expect(page.locator('#main-balance')).toContainText('₹0.00');
    await expect(page.locator('#add-money-btn')).toBeVisible();

    // 2. Click Add Money
    await page.click('#add-money-btn');
    await page.waitForURL((url) => url.pathname === '/wallet/fund');

    // 3. Select quick amount +₹500
    await page.click('#chip-500');
    await expect(page.locator('#fund-amount-input')).toHaveValue('500');

    // 4. Submit funding
    await page.click('#fund-submit-btn');

    // 5. Success screen verification
    await expect(page.locator('.success-card')).toBeVisible();
    await expect(page.locator('.success-title')).toContainText('Money Added Successfully');
    await expect(page.locator('.balance-val')).toContainText('₹500.00');

    // 6. Return to Home
    await page.click('#success-done-btn');
    await page.waitForURL((url) => url.pathname === '/pay');

    // 7. Verify balance updated on Home screen
    await expect(page.locator('#main-balance')).toContainText('₹500.00');

    // 8. Test daily limit enforcement via UI
    await page.click('#add-money-btn');
    await page.waitForURL((url) => url.pathname === '/wallet/fund');
    await page.fill('#fund-amount-input', '50000');
    // First funding of 500 + 50000 = 50500 > 50000 limit
    await page.click('#fund-submit-btn');

    await expect(page.locator('.error-alert')).toBeVisible();
    await expect(page.locator('.error-alert')).toContainText('Daily funding limit of ₹50,000 exceeded');

    const unexpectedErrors = consoleErrors.filter(
      (msg) => !msg.includes('status of 422'),
    );
    expect(unexpectedErrors).toEqual([]);
  });
});
