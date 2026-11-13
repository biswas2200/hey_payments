/**
 * RFC 9285 Base45 encoder/decoder
 * No Angular imports — pure TypeScript.
 */

const CHARSET = '0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ $%*+-./:';
const CHARSET_SIZE = 45;

// Reverse lookup table
const DECODE_MAP = new Map<string, number>();
for (let i = 0; i < CHARSET.length; i++) {
  DECODE_MAP.set(CHARSET[i], i);
}

/**
 * Encodes a Uint8Array to a Base45 string per RFC 9285.
 */
export function base45Encode(input: Uint8Array): string {
  const out: string[] = [];
  let i = 0;
  while (i < input.length) {
    if (i + 1 < input.length) {
      // Two bytes → three characters
      const n = (input[i] << 8) | input[i + 1];
      const c = n % CHARSET_SIZE;
      const d = Math.floor(n / CHARSET_SIZE) % CHARSET_SIZE;
      const e = Math.floor(n / (CHARSET_SIZE * CHARSET_SIZE));
      out.push(CHARSET[c], CHARSET[d], CHARSET[e]);
      i += 2;
    } else {
      // One byte → two characters
      const n = input[i];
      const c = n % CHARSET_SIZE;
      const d = Math.floor(n / CHARSET_SIZE);
      out.push(CHARSET[c], CHARSET[d]);
      i += 1;
    }
  }
  return out.join('');
}

/**
 * Decodes a Base45 string to a Uint8Array per RFC 9285.
 * Throws typed errors for invalid input.
 */
export function base45Decode(input: string): Uint8Array {
  if (input.length === 0) {
    return new Uint8Array(0);
  }
  if (input.length % 3 === 1) {
    throw new Error('DANGLING_SINGLE: Base45 input length mod 3 must not be 1');
  }

  const digits: number[] = [];
  for (const ch of input) {
    const v = DECODE_MAP.get(ch);
    if (v === undefined) {
      throw new Error(`INVALID_CHAR: "${ch}" is not in the Base45 alphabet`);
    }
    digits.push(v);
  }

  const out: number[] = [];
  let i = 0;
  while (i < digits.length) {
    if (i + 2 < digits.length) {
      // Three chars → two bytes
      const n = digits[i] + digits[i + 1] * CHARSET_SIZE + digits[i + 2] * CHARSET_SIZE * CHARSET_SIZE;
      if (n > 65535) {
        throw new Error(`TRIPLET_OVERFLOW: triplet value ${n} > 65535`);
      }
      out.push(n >> 8, n & 0xff);
      i += 3;
    } else {
      // Two chars → one byte
      const n = digits[i] + digits[i + 1] * CHARSET_SIZE;
      if (n > 255) {
        throw new Error(`TRIPLET_OVERFLOW: pair value ${n} > 255`);
      }
      out.push(n);
      i += 2;
    }
  }
  return new Uint8Array(out);
}
