/**
 * DEFLATE wrapper (raw deflate, no zlib header) — test suite
 * Uses fflate under the hood.
 */
import { describe, expect, it } from 'vitest';
import { deflateRaw, inflateRaw } from './deflate';

describe('deflateRaw / inflateRaw', () => {
  // Helper: compare Uint8Array content regardless of internal buffer type
  function expectBytesEqual(a: Uint8Array, b: Uint8Array): void {
    expect(a.length).toBe(b.length);
    expect(Array.from(a)).toEqual(Array.from(b));
  }

  it('round-trips a simple ASCII payload', () => {
    const input = new TextEncoder().encode('Hello, HP-OFFLINE!');
    const compressed = deflateRaw(input);
    const restored = inflateRaw(compressed);
    expectBytesEqual(restored, input);
  });

  it('round-trips binary data with all byte values 0–255', () => {
    const input = new Uint8Array(256);
    for (let i = 0; i < 256; i++) input[i] = i;
    expectBytesEqual(inflateRaw(deflateRaw(input)), input);
  });

  it('round-trips empty input', () => {
    const input = new Uint8Array(0);
    expectBytesEqual(inflateRaw(deflateRaw(input)), input);
  });

  it('compressed output is smaller than input for compressible data', () => {
    const input = new Uint8Array(1000).fill(65); // 1000 'A's
    expect(deflateRaw(input).length).toBeLessThan(input.length);
  });

  // ---- Security / size-cap negative tests ----

  it('INFLATE_BOMB: rejects compressed data whose inflated size exceeds 16 KiB', () => {
    // Compress 1 MiB of zeros → fflate produces a tiny blob; inflating it must be rejected fast
    const { deflateSync } = require('fflate');
    const bomb = new Uint8Array(1024 * 1024); // 1 MiB of zeros
    const compressed = deflateSync(bomb, { level: 9 });
    expect(() => inflateRaw(compressed)).toThrow(/INFLATE_BOMB/);
  });

  it('INFLATE_BOMB: rejects data that expands to exactly 16385 bytes (one over cap)', () => {
    // Create just-over-cap input (16385 identical bytes)
    const { deflateSync } = require('fflate');
    const over = new Uint8Array(16385).fill(66);
    const compressed = deflateSync(over, { level: 9 });
    expect(() => inflateRaw(compressed)).toThrow(/INFLATE_BOMB/);
  });

  it('allows inflation of data at exactly 16 KiB (boundary)', () => {
    // 16 KiB of zeros should be accepted
    const input = new Uint8Array(16384).fill(0);
    const compressed = deflateRaw(input);
    expect(() => inflateRaw(compressed)).not.toThrow();
    expect(inflateRaw(compressed)).toEqual(input);
  });

  it('CORRUPT_DEFLATE: rejects garbage bytes', () => {
    expect(() => inflateRaw(new Uint8Array([0xde, 0xad, 0xbe, 0xef]))).toThrow(
      /CORRUPT_DEFLATE/,
    );
  });
});
