/**
 * Raw DEFLATE wrapper (no zlib header/trailer).
 * Uses fflate@0.8.3 for compression (deflateSync) and streaming Inflate
 * for decompression so we can enforce a 16 KiB output cap safely.
 * No Angular imports — pure TypeScript.
 *
 * Note: fflate's public typings omit the `raw` option, but it is a supported
 * runtime option per the fflate source. We use a type assertion to pass it.
 */
import { Inflate, deflateSync } from 'fflate';

/** Maximum allowed inflated output in bytes (16 KiB). */
export const INFLATE_MAX_BYTES = 16 * 1024; // 16 384

/**
 * Compresses bytes using raw DEFLATE (no zlib wrapper).
 */
export function deflateRaw(input: Uint8Array): Uint8Array {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  return deflateSync(input, { raw: true } as any);
}

/**
 * Decompresses raw DEFLATE bytes.
 * Throws `Error('INFLATE_BOMB: …')` when the output exceeds 16 KiB.
 * Throws `Error('CORRUPT_DEFLATE: …')` on corrupt input.
 */
export function inflateRaw(input: Uint8Array): Uint8Array {
  const chunks: Uint8Array[] = [];
  let totalBytes = 0;
  let bombError: Error | null = null;

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const inflater = new Inflate({ raw: true } as any, (chunk: Uint8Array) => {
    totalBytes += chunk.length;
    if (totalBytes > INFLATE_MAX_BYTES) {
      bombError = new Error(
        `INFLATE_BOMB: inflated size exceeded ${INFLATE_MAX_BYTES} bytes limit`,
      );
      return;
    }
    chunks.push(chunk);
  });

  try {
    inflater.push(input, true);
  } catch (e) {
    if (bombError) throw bombError;
    throw new Error(`CORRUPT_DEFLATE: ${(e as Error).message}`);
  }

  if (bombError) {
    throw bombError;
  }

  // Concatenate chunks into a fresh Uint8Array (not a Buffer subclass)
  const out = new Uint8Array(totalBytes);
  let offset = 0;
  for (const chunk of chunks) {
    // Copy each chunk byte-by-byte to ensure we have a plain Uint8Array
    const plain = new Uint8Array(chunk.buffer, chunk.byteOffset, chunk.byteLength);
    out.set(plain, offset);
    offset += plain.length;
  }
  return out;
}
