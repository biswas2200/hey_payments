import { TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { of, throwError } from 'rxjs';
import { AuthStore } from './auth.store';
import { AuthApiService } from '../api/auth-api.service';
import { KeyService } from '../crypto/key.service';
import type { DeviceResponse, TokenResponse, UserProfile } from '../api/models';

describe('AuthStore', () => {
  let store: AuthStore;
  let apiMock: Partial<AuthApiService>;
  let keyServiceMock: Partial<KeyService>;

  const mockTokenResponse: TokenResponse = {
    accessToken: 'mock-jwt-token-xyz',
    expiresIn: 900,
    user: {
      id: '01931234-0000-7000-8000-000000000001',
      role: 'PAYER',
      displayName: '+919876543210',
    },
  };

  const mockUserProfile: UserProfile = {
    id: '01931234-0000-7000-8000-000000000001',
    phone: '+919876543210',
    email: 'payer@example.com',
    role: 'PAYER',
    status: 'ACTIVE',
    createdAt: '2026-11-12T10:00:00Z',
  };

  const mockDeviceResponse: DeviceResponse = {
    deviceId: '01931234-0000-7000-8000-000000000009',
    status: 'ACTIVE',
    lastCounter: 0,
  };

  beforeEach(() => {
    apiMock = {
      login: vi.fn().mockReturnValue(of(mockTokenResponse)),
      register: vi.fn().mockReturnValue(of({ id: 'new-user-id' })),
      verifyOtp: vi.fn().mockReturnValue(of({ status: 'SUCCESS', message: 'Phone verified' })),
      refresh: vi.fn().mockReturnValue(of(mockTokenResponse)),
      logout: vi.fn().mockReturnValue(of(undefined)),
      getMe: vi.fn().mockReturnValue(of(mockUserProfile)),
      setPin: vi.fn().mockReturnValue(of(undefined)),
      registerDevice: vi.fn().mockReturnValue(of(mockDeviceResponse)),
      getMyDevice: vi.fn().mockReturnValue(of(mockDeviceResponse)),
    };

    keyServiceMock = {
      getOrCreateDeviceKey: vi.fn().mockResolvedValue({
        deviceId: 'dev-1',
        privateKey: {} as CryptoKey,
        publicKeyJwk: { kty: 'EC', crv: 'P-256', x: 'x1', y: 'y1' },
        createdAt: '2026-11-12T10:00:00Z',
      }),
      clearKeys: vi.fn().mockResolvedValue(undefined),
    };

    TestBed.configureTestingModule({
      providers: [
        AuthStore,
        { provide: AuthApiService, useValue: apiMock },
        { provide: KeyService, useValue: keyServiceMock },
      ],
    });

    store = TestBed.inject(AuthStore);
  });

  it('should initialize with empty unauthenticated state', () => {
    expect(store.currentUser()).toBeNull();
    expect(store.isAuthenticated()).toBe(false);
    expect(store.role()).toBeNull();
    expect(store.accessToken()).toBeNull();
    expect(store.activeDevice()).toBeNull();
    expect(store.loading()).toBe(false);
  });

  it('should login successfully, update tokens, user, and compute isAuthenticated true', async () => {
    await store.login({ phone: '+919876543210', password: 'Password123!' });

    expect(apiMock.login).toHaveBeenCalled();
    expect(store.accessToken()).toBe('mock-jwt-token-xyz');
    expect(store.currentUser()).toEqual(mockUserProfile);
    expect(store.isAuthenticated()).toBe(true);
    expect(store.isPayer()).toBe(true);
    expect(store.isMerchant()).toBe(false);
    expect(store.error()).toBeNull();
  });

  it('should handle login failure and set error signal', async () => {
    (apiMock.login as unknown as ReturnType<typeof vi.fn>).mockReturnValue(
      throwError(() => ({ error: { detail: 'Invalid credentials' } })),
    );

    await expect(store.login({ phone: '+919876543210', password: 'Wrong' })).rejects.toBeDefined();
    expect(store.isAuthenticated()).toBe(false);
    expect(store.error()).toBe('Invalid credentials');
  });

  it('should handle login error with generic fallback message', async () => {
    (apiMock.login as unknown as ReturnType<typeof vi.fn>).mockReturnValue(
      throwError(() => 'Network failure'),
    );

    await expect(store.login({ phone: '+919876543210', password: 'Wrong' })).rejects.toBeDefined();
    expect(store.error()).toBe('Login failed');
  });

  it('should register new user successfully', async () => {
    const res = await store.register({
      phone: '+919876543210',
      password: 'Password123!',
      role: 'PAYER',
    });

    expect(apiMock.register).toHaveBeenCalled();
    expect(res.id).toBe('new-user-id');
  });

  it('should handle registration failure and set error signal', async () => {
    (apiMock.register as unknown as ReturnType<typeof vi.fn>).mockReturnValue(
      throwError(() => ({ error: { detail: 'Phone already registered' } })),
    );

    await expect(
      store.register({
        phone: '+919876543210',
        password: 'Password123!',
        role: 'PAYER',
      }),
    ).rejects.toBeDefined();
    expect(store.error()).toBe('Phone already registered');
  });

  it('should verify OTP successfully', async () => {
    await store.verifyOtp({ phone: '+919876543210', otp: '123456' });
    expect(apiMock.verifyOtp).toHaveBeenCalled();
  });

  it('should handle verify OTP failure', async () => {
    (apiMock.verifyOtp as unknown as ReturnType<typeof vi.fn>).mockReturnValue(
      throwError(() => ({ error: { detail: 'Invalid OTP code' } })),
    );

    await expect(store.verifyOtp({ phone: '+919876543210', otp: '000000' })).rejects.toBeDefined();
    expect(store.error()).toBe('Invalid OTP code');
  });

  it('should register current device using non-extractable key from KeyService', async () => {
    await store.registerCurrentDevice('My Pixel Phone');

    expect(keyServiceMock.getOrCreateDeviceKey).toHaveBeenCalled();
    expect(apiMock.registerDevice).toHaveBeenCalledWith({
      publicKeyJwk: { kty: 'EC', crv: 'P-256', x: 'x1', y: 'y1' },
      label: 'My Pixel Phone',
      userAgent: expect.any(String),
    });
    expect(store.activeDevice()).toEqual(mockDeviceResponse);
  });

  it('should handle register device failure', async () => {
    (apiMock.registerDevice as unknown as ReturnType<typeof vi.fn>).mockReturnValue(
      throwError(() => ({ error: { detail: 'Device limit reached' } })),
    );

    await expect(store.registerCurrentDevice()).rejects.toBeDefined();
    expect(store.error()).toBe('Device limit reached');
  });

  it('should refresh session successfully', async () => {
    const res = await store.refreshSession();
    expect(res.accessToken).toBe('mock-jwt-token-xyz');
    expect(store.accessToken()).toBe('mock-jwt-token-xyz');
  });

  it('should logout if refresh session fails', async () => {
    (apiMock.refresh as unknown as ReturnType<typeof vi.fn>).mockReturnValue(
      throwError(() => new Error('Token revoked')),
    );

    await expect(store.refreshSession()).rejects.toBeDefined();
    expect(store.currentUser()).toBeNull();
    expect(store.accessToken()).toBeNull();
  });

  it('should set payment PIN', async () => {
    await store.setPin('123456');
    expect(apiMock.setPin).toHaveBeenCalledWith({ pin: '123456' });
  });

  it('should handle setPin error', async () => {
    (apiMock.setPin as unknown as ReturnType<typeof vi.fn>).mockReturnValue(
      throwError(() => ({ error: { detail: 'Weak PIN' } })),
    );

    await expect(store.setPin('000000')).rejects.toBeDefined();
    expect(store.error()).toBe('Weak PIN');
  });

  it('should clear tokens, state, and keys on logout', async () => {
    await store.login({ phone: '+919876543210', password: 'Password123!' });
    expect(store.isAuthenticated()).toBe(true);

    await store.logout();
    expect(apiMock.logout).toHaveBeenCalled();
    expect(keyServiceMock.clearKeys).toHaveBeenCalled();
    expect(store.currentUser()).toBeNull();
    expect(store.accessToken()).toBeNull();
    expect(store.isAuthenticated()).toBe(false);
  });
});
