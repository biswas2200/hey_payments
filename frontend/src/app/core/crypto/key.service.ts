/**
 * Pure TypeScript Key Management Service (HP-OFFLINE v1)
 * Manages device keys stored in IndexedDB (Dexie).
 * Guaranteed non-extractable private keys per AGENTS.md §6.
 */

import { db, type KeyRecord } from '../offline/db';
import { exportPublicKeyJwk, generateDeviceKeyPair, signEnvelope, type SignedEnvelope } from './crypto';

export class KeyService {
  /**
   * Retrieves the existing device key or generates and persists a new one with extractable: false.
   */
  async getOrCreateDeviceKey(preferredDeviceId?: string): Promise<KeyRecord> {
    if (preferredDeviceId) {
      const existing = await db.keys.get(preferredDeviceId);
      if (existing) {
        return existing;
      }
    }

    // Check if any key exists in the database
    const firstKey = await db.keys.toCollection().first();
    if (firstKey && !preferredDeviceId) {
      return firstKey;
    }

    const deviceId = preferredDeviceId || crypto.randomUUID();
    const keyPair = await generateDeviceKeyPair();
    const publicJwk = await exportPublicKeyJwk(keyPair.publicKey);

    const record: KeyRecord = {
      deviceId,
      privateKey: keyPair.privateKey,
      publicKeyJwk: publicJwk,
      createdAt: new Date().toISOString(),
    };

    await db.keys.put(record);
    return record;
  }

  /**
   * Returns the primary active key record if stored, or null.
   */
  async getActiveKey(): Promise<KeyRecord | null> {
    const first = await db.keys.toCollection().first();
    return first ?? null;
  }

  /**
   * Signs a payload using the specified device key (or default active key).
   */
  async signWithDeviceKey<T extends Record<string, unknown>>(
    payload: T,
    deviceId?: string,
  ): Promise<SignedEnvelope<T>> {
    const keyRecord = await this.getOrCreateDeviceKey(deviceId);
    return signEnvelope(payload, keyRecord.privateKey);
  }

  /**
   * Clears all stored device keys (e.g. on device close or logout).
   */
  async clearKeys(): Promise<void> {
    await db.keys.clear();
  }
}

export const keyService = new KeyService();
