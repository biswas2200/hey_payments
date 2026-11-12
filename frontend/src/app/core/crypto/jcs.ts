/**
 * RFC 8785: JSON Canonicalization Scheme (JCS)
 * Pure TypeScript implementation with zero external dependencies.
 */

function compareKeys(a: string, b: string): number {
  const aLen = a.length;
  const bLen = b.length;
  let i = 0;
  let j = 0;
  while (i < aLen && j < bLen) {
    const cpA = a.codePointAt(i)!;
    const cpB = b.codePointAt(j)!;
    if (cpA !== cpB) {
      return cpA - cpB;
    }
    i += cpA > 0xffff ? 2 : 1;
    j += cpB > 0xffff ? 2 : 1;
  }
  return aLen - bLen;
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
    for (let i = 0; i < val.length; i++) {
      const item = val[i];
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
