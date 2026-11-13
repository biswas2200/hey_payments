/**
 * Hey Payments Core Crypto Engine (ECDSA P-256, SHA-256, IEEE P1363, JCS)
 * Pure TypeScript relying on the standard Web Crypto API.
 */

import { canonicalize } from './jcs';

export interface SignedEnvelope<T extends Record<string, unknown> = Record<string, unknown>> {
  p: T;
  s: string;
}

export function base64UrlEncode(bytes: Uint8Array): string {
  let binary = '';
  for (const byte of bytes) {
    binary += String.fromCharCode(byte);
  }
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

export function base64UrlDecode(str: string): Uint8Array {
  let base64 = str.replace(/-/g, '+').replace(/_/g, '/');
  while (base64.length % 4 !== 0) {
    base64 += '=';
  }
  const binary = atob(base64);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i);
  }
  return bytes;
}

/**
 * Generates an ECDSA P-256 keypair for device registration.
 * Invariant: Private key MUST have extractable: false.
 */
export async function generateDeviceKeyPair(): Promise<CryptoKeyPair> {
  return crypto.subtle.generateKey(
    {
      name: 'ECDSA',
      namedCurve: 'P-256',
    },
    false, // extractable: false per AGENTS.md §6
    ['sign', 'verify'],
  );
}

export async function exportPublicKeyJwk(publicKey: CryptoKey): Promise<JsonWebKey> {
  return crypto.subtle.exportKey('jwk', publicKey);
}

export async function exportPublicKeySec1(publicKey: CryptoKey): Promise<Uint8Array> {
  const raw = await crypto.subtle.exportKey('raw', publicKey);
  return new Uint8Array(raw);
}

export async function importPublicKeyJwk(jwk: {
  kty: string;
  crv: string;
  x: string;
  y: string;
}): Promise<CryptoKey> {
  return crypto.subtle.importKey(
    'jwk',
    {
      kty: jwk.kty,
      crv: jwk.crv,
      x: jwk.x,
      y: jwk.y,
    },
    {
      name: 'ECDSA',
      namedCurve: 'P-256',
    },
    true,
    ['verify'],
  );
}

export async function importPublicKeySec1(sec1: Uint8Array | string): Promise<CryptoKey> {
  const bytes = typeof sec1 === 'string' ? base64UrlDecode(sec1) : sec1;
  return crypto.subtle.importKey(
    'raw',
    bytes as unknown as BufferSource,
    {
      name: 'ECDSA',
      namedCurve: 'P-256',
    },
    true,
    ['verify'],
  );
}

export function validatePayloadNumbers(val: unknown): void {
  if (typeof val === 'number') {
    if (!Number.isInteger(val)) {
      throw new Error(`Protocol payload rejected: non-integer number ${val}`);
    }
    if (val > Number.MAX_SAFE_INTEGER || val < Number.MIN_SAFE_INTEGER) {
      throw new Error(`Protocol payload rejected: integer exceeds 2^53 - 1: ${val}`);
    }
  } else if (Array.isArray(val)) {
    for (const item of val) {
      validatePayloadNumbers(item);
    }
  } else if (val !== null && typeof val === 'object') {
    for (const prop of Object.values(val as Record<string, unknown>)) {
      validatePayloadNumbers(prop);
    }
  }
}

export async function signEnvelope<T extends Record<string, unknown>>(
  payload: T,
  privateKey: CryptoKey,
): Promise<SignedEnvelope<T>> {
  validatePayloadNumbers(payload);
  const canonical = canonicalize(payload);
  const data = new TextEncoder().encode(canonical);

  const signatureBuffer = await crypto.subtle.sign(
    {
      name: 'ECDSA',
      hash: { name: 'SHA-256' },
    },
    privateKey,
    data,
  );

  const signatureBytes = new Uint8Array(signatureBuffer);
  const signatureBase64Url = base64UrlEncode(signatureBytes);

  return {
    p: payload,
    s: signatureBase64Url,
  };
}

export async function verifyEnvelope(
  envelope: SignedEnvelope,
  publicKey: CryptoKey,
): Promise<boolean> {
  try {
    if (!envelope || !envelope.p || !envelope.s) {
      return false;
    }
    validatePayloadNumbers(envelope.p);
    const signatureBytes = base64UrlDecode(envelope.s);
    if (signatureBytes.length !== 64) {
      return false;
    }
    const canonical = canonicalize(envelope.p);
    const data = new TextEncoder().encode(canonical);

    return await crypto.subtle.verify(
      {
        name: 'ECDSA',
        hash: { name: 'SHA-256' },
      },
      publicKey,
      signatureBytes as unknown as BufferSource,
      data as unknown as BufferSource,
    );
  } catch {
    return false;
  }
}

export async function sha256Hex(data: Uint8Array | string): Promise<string> {
  const bytes = typeof data === 'string' ? new TextEncoder().encode(data) : data;
  const hashBuffer = await crypto.subtle.digest('SHA-256', bytes as unknown as BufferSource);
  const hashArray = Array.from(new Uint8Array(hashBuffer));
  return hashArray.map((b) => b.toString(16).padStart(2, '0')).join('');
}

export const DEFAULT_PBKDF2_ITERATIONS = 600_000;

/**
 * Derives a 256-bit PBKDF2 hash using HMAC-SHA-256 and configurable iterations
 * (defaulting to 600,000 for production per docs/08-security-threat-model.md §4).
 */
export async function derivePinHash(
  pin: string,
  salt: Uint8Array,
  iterations: number = DEFAULT_PBKDF2_ITERATIONS,
): Promise<string> {
  const enc = new TextEncoder();
  const keyMaterial = await crypto.subtle.importKey(
    'raw',
    enc.encode(pin) as unknown as BufferSource,
    'PBKDF2',
    false,
    ['deriveBits'],
  );
  const derivedBits = await crypto.subtle.deriveBits(
    {
      name: 'PBKDF2',
      salt: salt as unknown as BufferSource,
      iterations,
      hash: 'SHA-256',
    },
    keyMaterial,
    256,
  );
  return Array.from(new Uint8Array(derivedBits))
    .map((b) => b.toString(16).padStart(2, '0'))
    .join('');
}
