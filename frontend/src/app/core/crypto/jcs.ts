/**
 * RFC 8785: JSON Canonicalization Scheme (JCS)
 * Pure TypeScript implementation with zero external dependencies.
 */

function compareKeys(a: string, b: string): number {
  return a < b ? -1 : a > b ? 1 : 0;
}

export function canonicalize(val: unknown): string {
  if (val === null) {
    return 'null';
  }

  const type = typeof val;

  if (type === 'boolean') {
    return val ? 'true' : 'false';
  }

  if (type === 'number') {
    if (!Number.isFinite(val)) {
      throw new TypeError('JCS numbers must be finite numbers');
    }
    if (Object.is(val, -0)) {
      return '0';
    }
    return JSON.stringify(val);
  }

  if (type === 'string') {
    return JSON.stringify(val);
  }

  if (Array.isArray(val)) {
    const items: string[] = [];
    for (const item of val) {
      if (item === undefined || typeof item === 'symbol' || typeof item === 'function') {
        items.push('null');
      } else {
        items.push(canonicalize(item));
      }
    }
    return `[${items.join(',')}]`;
  }

  if (type === 'object') {
    // If it's a plain object or has a toJSON method
    const obj = val as Record<string, unknown>;
    const keys = Object.keys(obj)
      .filter((k) => {
        const v = obj[k];
        return v !== undefined && typeof v !== 'symbol' && typeof v !== 'function';
      })
      .sort(compareKeys);

    const parts: string[] = [];
    for (const key of keys) {
      const serializedKey = JSON.stringify(key);
      const serializedVal = canonicalize(obj[key]);
      parts.push(`${serializedKey}:${serializedVal}`);
    }
    return `{${parts.join(',')}}`;
  }

  throw new TypeError(`Unsupported type for JCS canonicalization: ${type}`);
}
