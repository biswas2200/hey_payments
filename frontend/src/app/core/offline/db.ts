/**
 * Dexie-based IndexedDB Schema for Hey Payments (HP-OFFLINE v1)
 * Per docs/09-frontend-architecture.md §3.
 */

import Dexie, { type Table } from 'dexie';

export interface KeyRecord {
  deviceId: string;
  privateKey: CryptoKey;
  publicKeyJwk: JsonWebKey;
  createdAt: string;
}

export interface CertRecord {
  id: string; // BC id or MC deviceId
  type: 'BC' | 'MC';
  envelope: unknown;
  status: 'ACTIVE' | 'SUPERSEDED' | 'EXPIRED' | 'CLOSED';
  spent: number;
  lastCounter: number;
  lastHash: string;
  createdAt: string;
}

export interface MetaRecord {
  key: string;
  value: unknown;
}

export class HeyPaymentsDb extends Dexie {
  keys!: Table<KeyRecord, string>;
  certs!: Table<CertRecord, string>;
  meta!: Table<MetaRecord, string>;

  constructor() {
    super('heypayments');
    this.version(1).stores({
      keys: 'deviceId, createdAt',
      certs: 'id, type, status, lastCounter',
      meta: 'key',
    });
  }
}

export const db = new HeyPaymentsDb();
