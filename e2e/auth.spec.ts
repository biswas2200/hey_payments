import { test, expect } from '@playwright/test';

test.describe('Hey Payments Auth & Device Workflows E2E', () => {
  const mailpitUrl = process.env['MAILPIT_URL'] || 'http://127.0.0.1:8025';

  async function getLatestOtpFromMailpit(expectedEmailOrPhone: string): Promise<string> {
    // Poll Mailpit API /api/v1/messages for the latest message
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

  test('should verify route access: unauthenticated routes redirect or render with zero console errors', async ({
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

    // 1. Public unauthenticated routes
    const publicRoutes = ['/auth/login', '/auth/register', '/auth/verify-otp'];
    for (const route of publicRoutes) {
      await page.goto(route, { waitUntil: 'networkidle' });
      await expect(page.locator('.auth-card')).toBeVisible();
      await expect(page.locator('form.form-body')).toBeVisible();
    }

    // 2. Protected routes when unauthenticated must redirect to /auth/login
    const protectedRoutes = ['/auth/device-register', '/auth/set-pin'];
    for (const route of protectedRoutes) {
      await page.goto(route, { waitUntil: 'networkidle' });
      await page.waitForURL((url) => url.pathname === '/auth/login');
      expect(page.url()).toContain('/auth/login');
      await expect(page.locator('.auth-card')).toBeVisible();
      await expect(page.locator('form.form-body')).toBeVisible();
    }

    expect(consoleErrors).toEqual([]);
  });

  test('full UI flow for Payer: register -> OTP from Mailpit -> verify -> login -> device register -> set PIN -> assert Dexie & key -> logout -> login again', async ({
    page,
  }) => {
    const timestamp = Date.now().toString().slice(-6);
    const phoneDigits = `98765${timestamp}`.slice(0, 10);
    const email = `payer_${timestamp}@example.com`;
    const password = 'PayerPassword123!';
    const pin = '123456';

    const consoleErrors: string[] = [];
    page.on('console', (msg) => {
      if (msg.type() === 'error') {
        consoleErrors.push(msg.text());
      }
    });
    page.on('pageerror', (err) => {
      consoleErrors.push(err.message);
    });

    // 1. Register
    await page.goto('/auth/register', { waitUntil: 'networkidle' });
    await page.fill('input#reg-phone', phoneDigits);
    await page.fill('input#reg-email', email);
    await page.fill('input#reg-password', password);
    await page.click('button[type="submit"]');

    // 2. Redirect to Verify OTP
    await page.waitForURL((url) => url.pathname.includes('/auth/verify-otp'));
    const otp = await getLatestOtpFromMailpit(email);
    await page.fill('input#otp', otp);
    await page.click('button[type="submit"]');

    // 3. Redirect to Login
    await page.waitForURL((url) => url.pathname.includes('/auth/login'));
    await page.fill('input#phone', phoneDigits);
    await page.fill('input#password', password);
    await page.click('button[type="submit"]');

    // 4. Redirect to Device Register (authenticated route check)
    await page.waitForURL((url) => url.pathname.includes('/auth/device-register'));
    await expect(page.locator('form.form-body')).toBeVisible();
    await page.fill('input#device-label', 'Payer Android Phone');
    await page.click('button[type="submit"]');

    // 5. Redirect to Set PIN (authenticated route check)
    await page.waitForURL((url) => url.pathname.includes('/auth/set-pin'));
    await expect(page.locator('form.form-body')).toBeVisible();
    await page.fill('input#new-pin', pin);
    await page.fill('input#confirm-pin', pin);
    await page.fill('input#current-password', password);
    await page.click('button[type="submit"]');

    // 6. Navigation strictly to /pay (never accept /auth/login)
    await page.waitForURL((url) => url.pathname === '/pay');
    expect(page.url()).toContain('/pay');
    await expect(page.locator('#logout-btn')).toBeVisible();

    // 7. Verify PIN salt & hash in Dexie meta, no hp_pin_* keys in localStorage, device key extractable === false
    const dexieChecks = await page.evaluate(async () => {
      const openReq = indexedDB.open('heypayments');
      const idb = await new Promise<IDBDatabase>((resolve, reject) => {
        openReq.onsuccess = () => resolve(openReq.result);
        openReq.onerror = () => reject(openReq.error);
      });

      // Assert meta store contains pin_salt and pin_hash
      if (!idb.objectStoreNames.contains('meta')) {
        throw new Error('Dexie objectStore "meta" does not exist');
      }
      const metaTx = idb.transaction('meta', 'readonly');
      const metaStore = metaTx.objectStore('meta');
      const saltRecord = await new Promise<any>((resolve, reject) => {
        const req = metaStore.get('pin_salt');
        req.onsuccess = () => resolve(req.result);
        req.onerror = () => reject(req.error);
      });
      const hashRecord = await new Promise<any>((resolve, reject) => {
        const req = metaStore.get('pin_hash');
        req.onsuccess = () => resolve(req.result);
        req.onerror = () => reject(req.error);
      });

      // Assert keys store contains non-extractable device key
      if (!idb.objectStoreNames.contains('keys')) {
        throw new Error('Dexie objectStore "keys" does not exist');
      }
      const keyTx = idb.transaction('keys', 'readonly');
      const keyStore = keyTx.objectStore('keys');
      const keyRecord = await new Promise<any>((resolve, reject) => {
        const req = keyStore.getAll();
        req.onsuccess = () => resolve(req.result[0]);
        req.onerror = () => reject(req.error);
      });

      // Check localStorage for forbidden keys
      const forbiddenLocalKeys = Object.keys(localStorage).filter((k) => k.startsWith('hp_pin_'));

      return {
        pinSalt: saltRecord?.value,
        pinHash: hashRecord?.value,
        extractable: keyRecord?.privateKey?.extractable,
        forbiddenLocalKeysCount: forbiddenLocalKeys.length,
      };
    });

    expect(typeof dexieChecks.pinSalt).toBe('string');
    expect(dexieChecks.pinSalt.length).toBeGreaterThan(10);
    expect(typeof dexieChecks.pinHash).toBe('string');
    expect(dexieChecks.pinHash.length).toBeGreaterThan(10);
    expect(dexieChecks.extractable).toBe(false);
    expect(dexieChecks.forbiddenLocalKeysCount).toBe(0);

    // 8. Logout control: click logout -> redirect to /auth/login -> refresh cookie cleared
    await page.click('#logout-btn');
    await page.waitForURL((url) => url.pathname === '/auth/login');
    expect(page.url()).toContain('/auth/login');

    const cookiesAfterLogout = await page.context().cookies();
    const rtCookie = cookiesAfterLogout.find((c) => c.name === 'hp_rt');
    const isCookieCleared =
      !rtCookie || rtCookie.value === '' || rtCookie.expires <= Date.now() / 1000;
    expect(isCookieCleared).toBe(true);

    // 9. Login again -> lands directly on /pay (landing)
    await page.fill('input#phone', phoneDigits);
    await page.fill('input#password', password);
    await page.click('button[type="submit"]');

    await page.waitForURL((url) => url.pathname === '/pay');
    expect(page.url()).toContain('/pay');

    expect(consoleErrors).toEqual([]);
  });

  test('full UI flow for Merchant: register -> OTP -> verify -> login -> device register -> verify MC in-browser via JWKS & assert mutated payload fails', async ({
    page,
  }) => {
    const timestamp = Date.now().toString().slice(-6);
    const phoneDigits = `98764${timestamp}`.slice(0, 10);
    const email = `merchant_${timestamp}@example.com`;
    const password = 'MerchantPassword123!';

    const consoleErrors: string[] = [];
    page.on('console', (msg) => {
      if (msg.type() === 'error') {
        consoleErrors.push(msg.text());
      }
    });
    page.on('pageerror', (err) => {
      consoleErrors.push(err.message);
    });

    // 1. Register as Merchant
    await page.goto('/auth/register', { waitUntil: 'networkidle' });
    await page.click('button[role="radio"]:has-text("Merchant")');
    await page.fill('input#reg-phone', phoneDigits);
    await page.fill('input#display-name', `Kirana Store ${timestamp}`);
    await page.fill('input#reg-email', email);
    await page.fill('input#reg-password', password);
    await page.click('button[type="submit"]');

    // 2. Verify OTP
    await page.waitForURL((url) => url.pathname.includes('/auth/verify-otp'));
    const otp = await getLatestOtpFromMailpit(email);
    await page.fill('input#otp', otp);
    await page.click('button[type="submit"]');

    // 3. Login
    await page.waitForURL((url) => url.pathname.includes('/auth/login'));
    await page.fill('input#phone', phoneDigits);
    await page.fill('input#password', password);
    await page.click('button[type="submit"]');

    // 4. Device Registration (authenticated form renders)
    await page.waitForURL((url) => url.pathname.includes('/auth/device-register'));
    await expect(page.locator('form.form-body')).toBeVisible();
    await page.fill('input#device-label', 'Merchant POS Terminal');
    await page.click('button[type="submit"]');

    // 5. Merchant lands strictly on /merchant (never accept /auth/login)
    await page.waitForURL((url) => url.pathname === '/merchant');
    expect(page.url()).toContain('/merchant');
    await expect(page.locator('#logout-btn')).toBeVisible();

    // 6. Test authenticated route access: navigating to /auth/device-register and /auth/set-pin renders form
    await page.goto('/auth/device-register', { waitUntil: 'networkidle' });
    await expect(page.locator('form.form-body')).toBeVisible();
    await page.goto('/auth/set-pin', { waitUntil: 'networkidle' });
    await expect(page.locator('form.form-body')).toBeVisible();

    // Return to /merchant
    await page.goto('/merchant', { waitUntil: 'networkidle' });

    // 7. Verify Merchant Certificate in browser IndexedDB, JCS-canonicalize, verify via JWKS, assert mutated payload fails
    const mcChecks = await page.evaluate(async () => {
      const openReq = indexedDB.open('heypayments');
      const dbInstance = await new Promise<IDBDatabase>((resolve, reject) => {
        openReq.onsuccess = () => resolve(openReq.result);
        openReq.onerror = () => reject(openReq.error);
      });

      // Verify device key is non-extractable
      const keyTx = dbInstance.transaction('keys', 'readonly');
      const keyStore = keyTx.objectStore('keys');
      const keyRecord = await new Promise<any>((resolve, reject) => {
        const req = keyStore.getAll();
        req.onsuccess = () => resolve(req.result[0]);
        req.onerror = () => reject(req.error);
      });
      const keyExtractable = keyRecord?.privateKey?.extractable;

      // Read certs
      const certTx = dbInstance.transaction('certs', 'readonly');
      const certStore = certTx.objectStore('certs');
      const allCerts = await new Promise<any[]>((resolve, reject) => {
        const req = certStore.getAll();
        req.onsuccess = () => resolve(req.result);
        req.onerror = () => reject(req.error);
      });

      const mcCert = allCerts.find((c) => c.type === 'MC');
      if (!mcCert || !mcCert.envelope) {
        throw new Error('MC cert not found in Dexie');
      }

      const envelope = mcCert.envelope;
      const payload = envelope.p;
      const kid = payload?.kid || envelope.kid;
      if (!kid) {
        throw new Error('MC envelope kid missing');
      }

      // Fetch JWKS
      const jwksResp = await fetch('/api/v1/keys/jwks');
      const jwks = (await jwksResp.json()) as { keys: Array<{ kid: string; [key: string]: any }> };
      const serverKey = jwks.keys.find((k) => k.kid === kid);
      if (!serverKey) {
        throw new Error(`JWKS key not found for kid: ${kid}`);
      }

      // Import server public key
      const cryptoKey = await crypto.subtle.importKey(
        'jwk',
        serverKey,
        { name: 'ECDSA', namedCurve: 'P-256' },
        false,
        ['verify'],
      );

      // JCS canonicalization
      function jcs(val: any): string {
        if (val === null || typeof val !== 'object') {
          return JSON.stringify(val);
        }
        if (Array.isArray(val)) {
          return '[' + val.map(jcs).join(',') + ']';
        }
        const keys = Object.keys(val).sort();
        return '{' + keys.map((k) => JSON.stringify(k) + ':' + jcs(val[k])).join(',') + '}';
      }

      // Base64Url decode to binary Uint8Array
      function b64urlToBytes(str: string): Uint8Array {
        const b64 = str.replace(/-/g, '+').replace(/_/g, '/');
        const pad = (4 - (b64.length % 4)) % 4;
        const padded = b64 + '='.repeat(pad);
        const binary = atob(padded);
        const bytes = new Uint8Array(binary.length);
        for (let i = 0; i < binary.length; i++) {
          bytes[i] = binary.charCodeAt(i);
        }
        return bytes;
      }

      const sigBytes = b64urlToBytes(envelope.s);
      const canonicalData = new TextEncoder().encode(jcs(payload));

      // 1. Verify legitimate signature
      const valid = await crypto.subtle.verify(
        { name: 'ECDSA', hash: { name: 'SHA-256' } },
        cryptoKey,
        sigBytes,
        canonicalData,
      );

      // 2. Mutate payload and verify it fails
      const mutatedPayload = { ...payload, displayName: 'Tampered Hacker Store' };
      const mutatedData = new TextEncoder().encode(jcs(mutatedPayload));
      const mutatedValid = await crypto.subtle.verify(
        { name: 'ECDSA', hash: { name: 'SHA-256' } },
        cryptoKey,
        sigBytes,
        mutatedData,
      );

      return {
        valid,
        mutatedValid,
        keyExtractable,
      };
    });

    expect(mcChecks.valid).toBe(true);
    expect(mcChecks.mutatedValid).toBe(false);
    expect(mcChecks.keyExtractable).toBe(false);

    expect(consoleErrors).toEqual([]);
  });
});
