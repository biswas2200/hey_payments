import { describe, expect, it } from 'vitest';
import { TokenStore } from './token.store';

describe('TokenStore', () => {
  it('should initialize with null token and expiry', () => {
    const store = new TokenStore();
    expect(store.accessToken()).toBeNull();
    expect(store.expiresAt()).toBeNull();
  });

  it('should set token and expiry when provided', () => {
    const store = new TokenStore();
    const now = Date.now();
    store.setToken('test-access-token', 900);
    expect(store.accessToken()).toBe('test-access-token');
    expect(store.expiresAt()).toBeGreaterThanOrEqual(now + 899 * 1000);
  });

  it('should clear token and expiry on clear()', () => {
    const store = new TokenStore();
    store.setToken('test-access-token', 900);
    store.clear();
    expect(store.accessToken()).toBeNull();
    expect(store.expiresAt()).toBeNull();
  });
});
