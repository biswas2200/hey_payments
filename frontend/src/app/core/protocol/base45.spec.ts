/**
 * RFC 9285 Base45 — test suite
 * Tests sourced from RFC 9285 §4 examples plus invalid-input cases.
 */
import { describe, expect, it } from 'vitest';
import { base45Decode, base45Encode } from './base45';

describe('base45Encode', () => {
  it('encodes "AB" → "BB8" (RFC 9285 §4 example)', () => {
    const bytes = new TextEncoder().encode('AB');
    expect(base45Encode(bytes)).toBe('BB8');
  });

  it('encodes "Hello!!" → "%69 VD92EX0" (RFC 9285 §4 example)', () => {
    const bytes = new TextEncoder().encode('Hello!!');
    expect(base45Encode(bytes)).toBe('%69 VD92EX0');
  });

  it('encodes "base-45" → "UJCLQE7W581" (RFC 9285 §4 example)', () => {
    const bytes = new TextEncoder().encode('base-45');
    expect(base45Encode(bytes)).toBe('UJCLQE7W581');
  });

  it('encodes "ietf!" → "QED8WEX0" (RFC 9285 §4 example)', () => {
    const bytes = new TextEncoder().encode('ietf!');
    expect(base45Encode(bytes)).toBe('QED8WEX0');
  });

  it('encodes empty input to empty string', () => {
    expect(base45Encode(new Uint8Array(0))).toBe('');
  });

  it('round-trips arbitrary binary data', () => {
    const bytes = new Uint8Array([0, 1, 127, 128, 254, 255]);
    expect(base45Decode(base45Encode(bytes))).toEqual(bytes);
  });
});

describe('base45Decode', () => {
  it('decodes "BB8" → "AB"', () => {
    const decoded = base45Decode('BB8');
    expect(new TextDecoder().decode(decoded)).toBe('AB');
  });

  it('decodes "%69 VD92EX0" → "Hello!!"', () => {
    const decoded = base45Decode('%69 VD92EX0');
    expect(new TextDecoder().decode(decoded)).toBe('Hello!!');
  });

  it('decodes "UJCLQE7W581" → "base-45"', () => {
    const decoded = base45Decode('UJCLQE7W581');
    expect(new TextDecoder().decode(decoded)).toBe('base-45');
  });

  it('decodes "QED8WEX0" → "ietf!"', () => {
    const decoded = base45Decode('QED8WEX0');
    expect(new TextDecoder().decode(decoded)).toBe('ietf!');
  });

  it('decodes empty string to empty Uint8Array', () => {
    expect(base45Decode('')).toEqual(new Uint8Array(0));
  });

  // ---- Negative tests ----

  it('INVALID_CHAR rejects a character outside the alphabet (lowercase "a")', () => {
    expect(() => base45Decode('Ba8')).toThrow(/INVALID_CHAR/);
  });

  it('INVALID_CHAR rejects "*" which is outside the alphabet', () => {
    expect(() => base45Decode('B*8')).toThrow(/INVALID_CHAR/);
  });

  it('TRIPLET_OVERFLOW rejects a triplet whose value > 65535', () => {
    // 'ZZZ' in base45 = 44 + 44*45 + 44*45*45 = 44 + 1980 + 89100 = 91124 > 65535
    expect(() => base45Decode('ZZZ')).toThrow(/TRIPLET_OVERFLOW/);
  });

  it('DANGLING_SINGLE rejects a string whose length mod 3 equals 1', () => {
    // Length 1 is not a valid base45 input (valid lengths are multiples of 3 or 3n+2)
    expect(() => base45Decode('B')).toThrow(/DANGLING_SINGLE/);
  });
});
