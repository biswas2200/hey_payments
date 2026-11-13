/**
 * EnvelopeCodec — test suite
 * Per doc 03 §3.6: JSON → DEFLATE → Base45 → "HP1:<TYPE>:"
 * Multipart: "HP1:<TYPE>:<n>/<total>:<data>"
 * Negative tests named after their error codes.
 */
import { describe, expect, it } from 'vitest';
import {
  EnvelopeCodec,
  HP1_PREFIX,
  MAX_MULTIPART_PARTS,
  MAX_SINGLE_PAYLOAD_CHARS,
} from './envelope-codec';

const SAMPLE_ENV = {
  p: { v: 1, t: 'PR', prId: 'abc', amount: 12000 },
  s: 'BASE64URL_SIG',
};

describe('EnvelopeCodec.encode / decode round-trip', () => {
  it('encodes to a string starting with HP1:PR:', () => {
    const encoded = EnvelopeCodec.encode(SAMPLE_ENV, 'PR');
    expect(encoded[0]).toMatch(/^HP1:PR:/);
  });

  it('decode(encode(x)) deep-equals x', () => {
    const frames = EnvelopeCodec.encode(SAMPLE_ENV, 'PR');
    const result = EnvelopeCodec.decode(frames);
    expect(result).toEqual(SAMPLE_ENV);
  });

  it('encodes single frame for small payloads (≤ 1800 chars)', () => {
    const frames = EnvelopeCodec.encode(SAMPLE_ENV, 'PR');
    expect(frames.length).toBe(1);
  });

  it('encodes large payload into multiple frames', () => {
    // Build a payload large enough to exceed 1800 chars when encoded
    const large = { p: { v: 1, t: 'VC', data: 'X'.repeat(3000) }, s: 'SIG' };
    const frames = EnvelopeCodec.encode(large, 'VC');
    expect(frames.length).toBeGreaterThan(1);
  });

  it('multipart frames use format HP1:<TYPE>:<n>/<total>:<data>', () => {
    const large = { p: { v: 1, t: 'VC', data: 'X'.repeat(3000) }, s: 'SIG' };
    const frames = EnvelopeCodec.encode(large, 'VC');
    if (frames.length > 1) {
      expect(frames[0]).toMatch(/^HP1:VC:1\/\d+:/);
      expect(frames[1]).toMatch(/^HP1:VC:2\/\d+:/);
    }
  });

  it('decode assembles multipart frames regardless of arrival order', () => {
    const large = { p: { v: 1, t: 'VC', data: 'X'.repeat(3000) }, s: 'SIG' };
    const frames = EnvelopeCodec.encode(large, 'VC');
    if (frames.length > 1) {
      // Feed in reverse order
      const reversed = [...frames].reverse();
      const result = EnvelopeCodec.decode(reversed);
      expect(result).toEqual(large);
    }
  });
});

describe('EnvelopeCodec.decode — negative tests', () => {
  // UNKNOWN_PREFIX
  it('UNKNOWN_PREFIX rejects frames not starting with HP1:', () => {
    expect(() => EnvelopeCodec.decode(['HP2:PR:some_data'])).toThrow(/UNKNOWN_PREFIX/);
  });

  it('UNKNOWN_PREFIX rejects frames with no recognised prefix at all', () => {
    expect(() => EnvelopeCodec.decode(['QR:PR:some_data'])).toThrow(/UNKNOWN_PREFIX/);
  });

  // UNKNOWN_VERSION / wrong prefix format
  it('UNKNOWN_PREFIX rejects empty string', () => {
    expect(() => EnvelopeCodec.decode([''])).toThrow(/UNKNOWN_PREFIX/);
  });

  // MULTIPART_MAX_PARTS
  it('MULTIPART_MAX_PARTS rejects frames declaring more than 8 parts', () => {
    // Build a frame that claims to be part 1 of 9
    const encoded = EnvelopeCodec.encode(SAMPLE_ENV, 'PR')[0];
    const data = encoded.replace(/^HP1:PR:/, '');
    expect(() =>
      EnvelopeCodec.decode([`HP1:PR:1/9:${data}`, `HP1:PR:2/9:${data}`]),
    ).toThrow(/MULTIPART_MAX_PARTS/);
  });

  // MULTIPART_TYPE_MISMATCH
  it('MULTIPART_TYPE_MISMATCH rejects frames with different types', () => {
    const encoded = EnvelopeCodec.encode(SAMPLE_ENV, 'PR')[0];
    const data = encoded.replace(/^HP1:PR:/, '');
    expect(() =>
      EnvelopeCodec.decode([`HP1:PR:1/2:${data}`, `HP1:VC:2/2:${data}`]),
    ).toThrow(/MULTIPART_TYPE_MISMATCH/);
  });

  // MULTIPART_TOTAL_MISMATCH
  it('MULTIPART_TOTAL_MISMATCH rejects frames with different total counts', () => {
    const encoded = EnvelopeCodec.encode(SAMPLE_ENV, 'PR')[0];
    const data = encoded.replace(/^HP1:PR:/, '');
    expect(() =>
      EnvelopeCodec.decode([`HP1:PR:1/2:${data}`, `HP1:PR:2/3:${data}`]),
    ).toThrow(/MULTIPART_TOTAL_MISMATCH/);
  });

  // MULTIPART_DUPLICATE_CONFLICT
  it('MULTIPART_DUPLICATE_CONFLICT rejects two frames with the same index but different content', () => {
    const encoded = EnvelopeCodec.encode(SAMPLE_ENV, 'PR')[0];
    const data = encoded.replace(/^HP1:PR:/, '');
    const data2 = data + 'X';
    expect(() =>
      EnvelopeCodec.decode([`HP1:PR:1/2:${data}`, `HP1:PR:1/2:${data2}`]),
    ).toThrow(/MULTIPART_DUPLICATE_CONFLICT/);
  });

  // MULTIPART_BAD_INDEX
  it('MULTIPART_BAD_INDEX rejects a frame whose index is 0', () => {
    const encoded = EnvelopeCodec.encode(SAMPLE_ENV, 'PR')[0];
    const data = encoded.replace(/^HP1:PR:/, '');
    expect(() =>
      EnvelopeCodec.decode([`HP1:PR:0/2:${data}`, `HP1:PR:2/2:${data}`]),
    ).toThrow(/MULTIPART_BAD_INDEX/);
  });

  // MULTIPART_BAD_INDEX: index > total
  it('MULTIPART_BAD_INDEX rejects a frame whose index exceeds total', () => {
    const encoded = EnvelopeCodec.encode(SAMPLE_ENV, 'PR')[0];
    const data = encoded.replace(/^HP1:PR:/, '');
    expect(() =>
      EnvelopeCodec.decode([`HP1:PR:3/2:${data}`, `HP1:PR:2/2:${data}`]),
    ).toThrow(/MULTIPART_BAD_INDEX/);
  });

  // MULTIPART_MISSING_PARTS (incomplete set decoded as single)
  it('MULTIPART_MISSING_PARTS rejects decoding when some parts are absent', () => {
    // Provide only part 1 of 2
    const encoded = EnvelopeCodec.encode(SAMPLE_ENV, 'PR')[0];
    const data = encoded.replace(/^HP1:PR:/, '');
    expect(() =>
      EnvelopeCodec.decode([`HP1:PR:1/2:${data}`]),
    ).toThrow(/MULTIPART_MISSING_PARTS/);
  });

  // INFLATE_BOMB: Covered by deflate.spec.ts directly (EnvelopeCodec re-throws it)
  it('INFLATE_BOMB propagates from decoder when payload is too large', () => {
    // Build a bomb: 1 MiB of zeros → compress → Base45 → inject as a fake HP1 frame
    const { deflateSync } = require('fflate');
    const { base45Encode } = require('./base45');
    const bomb = new Uint8Array(1024 * 1024);
    const compressed = deflateSync(bomb, { level: 9 });
    const b45 = base45Encode(compressed);
    expect(() => EnvelopeCodec.decode([`HP1:PR:${b45}`])).toThrow(/INFLATE_BOMB/);
  });

  // MALFORMED_JSON
  it('MALFORMED_JSON rejects Base45-decodable but non-JSON content', () => {
    const { base45Encode } = require('./base45');
    const { deflateSync } = require('fflate');
    const garbage = deflateSync(new TextEncoder().encode('not json at all!!!'), { raw: true });
    const b45 = base45Encode(garbage);
    expect(() => EnvelopeCodec.decode([`HP1:PR:${b45}`])).toThrow(/MALFORMED_JSON/);
  });
});

describe('EnvelopeCodec constants', () => {
  it('HP1_PREFIX is "HP1"', () => {
    expect(HP1_PREFIX).toBe('HP1');
  });

  it('MAX_MULTIPART_PARTS is 8', () => {
    expect(MAX_MULTIPART_PARTS).toBe(8);
  });

  it('MAX_SINGLE_PAYLOAD_CHARS is 1800', () => {
    expect(MAX_SINGLE_PAYLOAD_CHARS).toBe(1800);
  });
});
