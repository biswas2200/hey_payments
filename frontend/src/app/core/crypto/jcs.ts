import canonicalizeNpm from 'canonicalize';

/**
 * RFC 8785: JSON Canonicalization Scheme (JCS)
 * Backed by the standard 'canonicalize' package per doc 02.
 */
export function canonicalize(val: unknown): string {
  if (val === undefined || typeof val === 'symbol' || typeof val === 'function') {
    throw new TypeError(`Unsupported type for JCS canonicalization: ${typeof val}`);
  }
  if (typeof val === 'number') {
    if (!Number.isFinite(val)) {
      throw new TypeError('JCS numbers must be finite numbers');
    }
  }
  const result = canonicalizeNpm(val);
  if (result === undefined) {
    throw new TypeError(`Unsupported type for JCS canonicalization: ${typeof val}`);
  }
  return result;
}
