/**
 * EnvelopeCodec — HP-OFFLINE v1 transport encoding
 *
 * Wire format per doc 03 §3.6:
 *   Single:    HP1:<TYPE>:<Base45(DEFLATE(JSON))>
 *   Multipart: HP1:<TYPE>:<n>/<total>:<Base45(DEFLATE(JSON) chunk)>
 *
 * Rules:
 *  - Prefix must be "HP1" (UNKNOWN_PREFIX otherwise)
 *  - Max 8 parts per message (MULTIPART_MAX_PARTS)
 *  - All parts must share the same type (MULTIPART_TYPE_MISMATCH)
 *  - All parts must share the same declared total (MULTIPART_TOTAL_MISMATCH)
 *  - Duplicate index with different content → MULTIPART_DUPLICATE_CONFLICT
 *  - Part index must be 1..total (MULTIPART_BAD_INDEX)
 *  - Assembled set must have all parts (MULTIPART_MISSING_PARTS)
 *  - Inflated size ≤ 16 KiB (INFLATE_BOMB — re-thrown from deflate.ts)
 *  - Decompressed bytes must be valid JSON (MALFORMED_JSON)
 *
 * No Angular imports — pure TypeScript.
 */
import { base45Decode, base45Encode } from './base45';
import { deflateRaw, inflateRaw } from './deflate';

export const HP1_PREFIX = 'HP1';
export const MAX_MULTIPART_PARTS = 8;
export const MAX_SINGLE_PAYLOAD_CHARS = 1800;

export class EnvelopeCodec {
  /**
   * Encodes a JSON-serialisable envelope into one or more HP1 wire frames.
   * Large payloads are automatically split into multipart frames.
   */
  static encode(envelope: unknown, type: string): string[] {
    const json = JSON.stringify(envelope);
    const compressed = deflateRaw(new TextEncoder().encode(json));
    const b45 = base45Encode(compressed);

    if (b45.length <= MAX_SINGLE_PAYLOAD_CHARS) {
      return [`${HP1_PREFIX}:${type}:${b45}`];
    }

    // Split into chunks of MAX_SINGLE_PAYLOAD_CHARS
    const chunks: string[] = [];
    for (let i = 0; i < b45.length; i += MAX_SINGLE_PAYLOAD_CHARS) {
      chunks.push(b45.slice(i, i + MAX_SINGLE_PAYLOAD_CHARS));
    }

    if (chunks.length > MAX_MULTIPART_PARTS) {
      // Further divide into exactly MAX_MULTIPART_PARTS parts
      // by redistributing chunk boundaries
      const chunkSize = Math.ceil(b45.length / MAX_MULTIPART_PARTS);
      const parts: string[] = [];
      for (let i = 0; i < b45.length; i += chunkSize) {
        parts.push(b45.slice(i, i + chunkSize));
      }
      const total = parts.length;
      return parts.map((part, idx) => `${HP1_PREFIX}:${type}:${idx + 1}/${total}:${part}`);
    }

    const total = chunks.length;
    return chunks.map((chunk, idx) => `${HP1_PREFIX}:${type}:${idx + 1}/${total}:${chunk}`);
  }

  /**
   * Decodes one or more HP1 wire frames back into the original envelope.
   * Frames may be provided in any order.
   */
  static decode(frames: string[]): unknown {
    if (!frames.length) {
      throw new Error('UNKNOWN_PREFIX: empty frame list');
    }

    // Validate prefix on first frame
    if (!frames[0].startsWith(`${HP1_PREFIX}:`)) {
      throw new Error(`UNKNOWN_PREFIX: frame does not start with "HP1:" — got: "${frames[0].slice(0, 10)}"`);
    }

    // Check all frames have the same prefix
    for (const frame of frames) {
      if (!frame.startsWith(`${HP1_PREFIX}:`)) {
        throw new Error(`UNKNOWN_PREFIX: mixed prefixes in frame set`);
      }
    }

    // Parse the first frame to determine if single or multipart
    const parsed = EnvelopeCodec._parseFrame(frames[0]);

    if (!parsed.isMultipart) {
      if (frames.length === 1) {
        return EnvelopeCodec._decompress(parsed.data);
      }
      // Multiple frames but first is single-format — treat all as multipart check
    }

    // Multipart assembly
    const parts = new Map<number, string>(); // index (1-based) → data
    let declaredTotal: number | null = null;
    let declaredType: string | null = null;

    for (const frame of frames) {
      const p = EnvelopeCodec._parseFrame(frame);

      // Validate type consistency
      if (declaredType === null) {
        declaredType = p.type;
      } else if (p.type !== declaredType) {
        throw new Error(`MULTIPART_TYPE_MISMATCH: got type "${p.type}", expected "${declaredType}"`);
      }

      if (!p.isMultipart) {
        // Single frame in a multipart context
        if (frames.length > 1) {
          throw new Error('MULTIPART_TYPE_MISMATCH: mixed single and multipart frames');
        }
        return EnvelopeCodec._decompress(p.data);
      }

      const { partIndex, partTotal, data } = p;

      // Validate total consistency
      if (declaredTotal === null) {
        declaredTotal = partTotal ?? null;
      } else if (partTotal !== declaredTotal) {
        throw new Error(
          `MULTIPART_TOTAL_MISMATCH: got total ${partTotal}, expected ${declaredTotal}`,
        );
      }

      // Validate total ≤ MAX_MULTIPART_PARTS
      if (partTotal! > MAX_MULTIPART_PARTS) {
        throw new Error(
          `MULTIPART_MAX_PARTS: total ${partTotal} exceeds maximum ${MAX_MULTIPART_PARTS}`,
        );
      }

      // Validate index range 1..total
      if (partIndex! < 1 || partIndex! > partTotal!) {
        throw new Error(
          `MULTIPART_BAD_INDEX: index ${partIndex} is out of range 1..${partTotal}`,
        );
      }

      // Duplicate index check
      if (parts.has(partIndex!)) {
        if (parts.get(partIndex!) !== data) {
          throw new Error(
            `MULTIPART_DUPLICATE_CONFLICT: part ${partIndex} received twice with different content`,
          );
        }
        // Identical duplicate — idempotent, ignore
      } else {
        parts.set(partIndex!, data);
      }
    }

    // Verify all parts are present
    const total = declaredTotal!;
    for (let i = 1; i <= total; i++) {
      if (!parts.has(i)) {
        const missing = Array.from({ length: total }, (_, k) => k + 1).filter((k) => !parts.has(k));
        throw new Error(`MULTIPART_MISSING_PARTS: missing parts ${missing.join(', ')} of ${total}`);
      }
    }

    // Assemble in order
    const assembled = Array.from({ length: total }, (_, i) => parts.get(i + 1)!).join('');
    return EnvelopeCodec._decompress(assembled);
  }

  private static _parseFrame(frame: string): {
    type: string;
    isMultipart: boolean;
    partIndex?: number;
    partTotal?: number;
    data: string;
  } {
    if (!frame.startsWith(`${HP1_PREFIX}:`)) {
      throw new Error(`UNKNOWN_PREFIX: "${frame.slice(0, 10)}"`);
    }
    // Strip "HP1:"
    const rest = frame.slice(4); // "PR:data" or "PR:1/3:data"
    const firstColon = rest.indexOf(':');
    if (firstColon === -1) {
      throw new Error(`UNKNOWN_PREFIX: malformed HP1 frame — no type separator`);
    }
    const type = rest.slice(0, firstColon);
    const afterType = rest.slice(firstColon + 1); // "data" or "1/3:data"

    // Check if multipart: starts with "<n>/<m>:"
    const multipartMatch = afterType.match(/^(\d+)\/(\d+):([\s\S]*)$/);
    if (multipartMatch) {
      const partIndex = parseInt(multipartMatch[1], 10);
      const partTotal = parseInt(multipartMatch[2], 10);
      const data = multipartMatch[3];

      if (partIndex < 1 || partIndex > partTotal) {
        throw new Error(
          `MULTIPART_BAD_INDEX: index ${partIndex} is out of range 1..${partTotal}`,
        );
      }
      if (partTotal > MAX_MULTIPART_PARTS) {
        throw new Error(
          `MULTIPART_MAX_PARTS: total ${partTotal} exceeds maximum ${MAX_MULTIPART_PARTS}`,
        );
      }
      return { type, isMultipart: true, partIndex, partTotal, data };
    }

    return { type, isMultipart: false, data: afterType };
  }

  private static _decompress(b45data: string): unknown {
    const compressed = base45Decode(b45data);
    // inflateRaw throws INFLATE_BOMB if output > 16 KiB
    const jsonBytes = inflateRaw(compressed);
    const json = new TextDecoder().decode(jsonBytes);
    try {
      return JSON.parse(json);
    } catch {
      throw new Error(`MALFORMED_JSON: could not parse decompressed bytes as JSON`);
    }
  }
}
