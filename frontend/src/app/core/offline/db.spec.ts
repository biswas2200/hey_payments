import { beforeEach, describe, expect, it } from 'vitest';
import Dexie, { type Table } from 'dexie';
import { db } from './db';

describe('HeyPaymentsDb Dexie IndexedDB Store', () => {
  beforeEach(async () => {
    await db.keys.clear();
    await db.certs.clear();
  });

  it('should have database version 2 with meta table', () => {
    expect(db.verno).toBe(2);
  });

  it('should upgrade a v1 database to v2 without data loss and make meta store accessible', async () => {
    const upgradeDbName = 'test-upgrade-db';
    const v1Db = new Dexie(upgradeDbName);
    v1Db.version(1).stores({
      keys: 'deviceId, createdAt',
      certs: 'id, type, status, lastCounter',
    });
    await v1Db.open();
    await v1Db.table('keys').put({
      deviceId: 'dev-upgrade-1',
      createdAt: '2026-01-01T00:00:00.000Z',
    });
    v1Db.close();

    class TestV2Db extends Dexie {
      keys!: Table<{ deviceId: string; createdAt: string }, string>;
      certs!: Table<unknown, string>;
      meta!: Table<{ key: string; value: unknown }, string>;

      constructor() {
        super(upgradeDbName);
        this.version(1).stores({
          keys: 'deviceId, createdAt',
          certs: 'id, type, status, lastCounter',
        });
        this.version(2).stores({
          meta: 'key',
        });
      }
    }

    const v2Db = new TestV2Db();
    await v2Db.open();
    expect(v2Db.verno).toBe(2);

    const oldKey = await v2Db.keys.get('dev-upgrade-1');
    expect(oldKey).toBeDefined();
    expect(oldKey?.deviceId).toBe('dev-upgrade-1');

    await v2Db.meta.put({ key: 'pin_salt', value: 'somesalt' });
    const metaRecord = await v2Db.meta.get('pin_salt');
    expect(metaRecord?.value).toBe('somesalt');

    v2Db.close();
    await Dexie.delete(upgradeDbName);
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
