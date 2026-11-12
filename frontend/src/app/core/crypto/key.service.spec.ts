import { beforeEach, describe, expect, it } from 'vitest';
import { KeyService } from './key.service';
import { db } from '../offline/db';
import { verifyEnvelope } from './crypto';

describe('KeyService', () => {
  let keyService: KeyService;

  beforeEach(async () => {
    await db.keys.clear();
    keyService = new KeyService();
  });

  it('should generate and persist non-extractable device key if none exists', async () => {
    const keyRecord = await keyService.getOrCreateDeviceKey('dev-test-1');
    expect(keyRecord.deviceId).toBe('dev-test-1');
    expect(keyRecord.privateKey.extractable).toBe(false);
    expect(keyRecord.publicKeyJwk.kty).toBe('EC');
    expect(keyRecord.publicKeyJwk.crv).toBe('P-256');

    // Subsequent call returns the existing key record
    const existing = await keyService.getOrCreateDeviceKey('dev-test-1');
    expect(existing.publicKeyJwk.x).toBe(keyRecord.publicKeyJwk.x);
    expect(existing.publicKeyJwk.y).toBe(keyRecord.publicKeyJwk.y);
  });

  it('should sign payload using the stored non-extractable device key', async () => {
    const keyRecord = await keyService.getOrCreateDeviceKey('dev-test-sign');
    const payload = { test: 'voucher-data', amount: 5000 };

    const envelope = await keyService.signWithDeviceKey(payload, 'dev-test-sign');
    expect(envelope.p).toEqual(payload);
    expect(envelope.s).toBeTruthy();

    const pubKey = await crypto.subtle.importKey(
      'jwk',
      keyRecord.publicKeyJwk,
      { name: 'ECDSA', namedCurve: 'P-256' },
      true,
      ['verify'],
    );

    const valid = await verifyEnvelope(envelope, pubKey);
    expect(valid).toBe(true);
  });

  it('should clear stored device keys on clearKeys()', async () => {
    await keyService.getOrCreateDeviceKey('dev-to-clear');
    expect(await keyService.getActiveKey()).toBeDefined();

    await keyService.clearKeys();
    expect(await keyService.getActiveKey()).toBeNull();
  });

  it('should update deviceId when server assigns a permanent UUID', async () => {
    await keyService.getOrCreateDeviceKey('temp-local-id');
    expect(await db.keys.get('temp-local-id')).toBeDefined();

    await keyService.updateDeviceId('temp-local-id', 'server-assigned-uuid');
    expect(await db.keys.get('temp-local-id')).toBeUndefined();
    const updated = await db.keys.get('server-assigned-uuid');
    expect(updated).toBeDefined();
    expect(updated?.deviceId).toBe('server-assigned-uuid');
    expect(updated?.privateKey.extractable).toBe(false);
  });
});
