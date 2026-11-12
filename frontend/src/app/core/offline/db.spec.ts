import { beforeEach, describe, expect, it } from 'vitest';
import { db } from './db';

describe('HeyPaymentsDb Dexie IndexedDB Store', () => {
  beforeEach(async () => {
    await db.keys.clear();
    await db.certs.clear();
  });

  it('should store and retrieve device key records', async () => {
    const keyPair = await crypto.subtle.generateKey({ name: 'ECDSA', namedCurve: 'P-256' }, false, [
      'sign',
      'verify',
    ]);
    const jwk = await crypto.subtle.exportKey('jwk', keyPair.publicKey);

    await db.keys.put({
      deviceId: 'dev-001',
      privateKey: keyPair.privateKey,
      publicKeyJwk: jwk,
      createdAt: new Date().toISOString(),
    });

    const retrieved = await db.keys.get('dev-001');
    expect(retrieved).toBeDefined();
    expect(retrieved?.deviceId).toBe('dev-001');
    expect(retrieved?.privateKey.extractable).toBe(false);
    expect(retrieved?.publicKeyJwk.x).toBe(jwk.x);
  });

  it('should store and query certificates', async () => {
    await db.certs.put({
      id: 'bc-100',
      type: 'BC',
      envelope: { p: { id: 'bc-100' }, s: 'sig' },
      status: 'ACTIVE',
      spent: 0,
      lastCounter: 0,
      lastHash: 'hash0',
      createdAt: new Date().toISOString(),
    });

    const activeCerts = await db.certs.where('status').equals('ACTIVE').toArray();
    expect(activeCerts.length).toBe(1);
    expect(activeCerts[0].id).toBe('bc-100');
  });
});
