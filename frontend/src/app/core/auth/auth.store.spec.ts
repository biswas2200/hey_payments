import { TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { of, throwError } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { AuthStore } from './auth.store';
import { AuthService } from '../api/generated/services/auth.service';
import { DevicesService } from '../api/generated/services/devices.service';
import { KeyService } from '../crypto/key.service';
import { TokenRefreshService } from './token-refresh.service';
import { TokenStore } from './token.store';
import { setPbkdf2IterationsForTesting } from '../crypto/crypto';
import { db } from '../offline/db';
import type { DeviceResponse, TokenResponse, UserProfile } from '../api/models';

describe('AuthStore', () => {
  let store: AuthStore;
  let authApiMock: {
    authLoginPost: ReturnType<typeof vi.fn>;
    authRegisterPost: ReturnType<typeof vi.fn>;
    authVerifyOtpPost: ReturnType<typeof vi.fn>;
    authLogoutPost: ReturnType<typeof vi.fn>;
    authPinPut: ReturnType<typeof vi.fn>;
  };
  let devicesApiMock: {
    devicesPost: ReturnType<typeof vi.fn>;
    devicesMeGet: ReturnType<typeof vi.fn>;
  };
  let httpMock: {
    get: ReturnType<typeof vi.fn>;
    post: ReturnType<typeof vi.fn>;
    put: ReturnType<typeof vi.fn>;
  };
  let keyServiceMock: Partial<KeyService>;
  let refreshServiceMock: { refreshSession: ReturnType<typeof vi.fn> };

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
    setPbkdf2IterationsForTesting(1000);
    authApiMock = {
      authLoginPost: vi.fn().mockReturnValue(of(mockTokenResponse)),
      authRegisterPost: vi.fn().mockReturnValue(of({ id: 'new-user-id' })),
      authVerifyOtpPost: vi.fn().mockReturnValue(of(undefined)),
      authLogoutPost: vi.fn().mockReturnValue(of(undefined)),
      authPinPut: vi.fn().mockReturnValue(of(undefined)),
    };

    devicesApiMock = {
      devicesPost: vi.fn().mockReturnValue(of(mockDeviceResponse)),
      devicesMeGet: vi.fn().mockReturnValue(of(mockDeviceResponse)),
    };

    httpMock = {
      get: vi.fn().mockImplementation((url: string) => {
        if (url.includes('/users/me')) return of(mockUserProfile);
        return of({});
      }),
      post: vi.fn().mockReturnValue(of({ status: 'SUCCESS', message: 'Phone verified' })),
      put: vi.fn().mockReturnValue(of(undefined)),
    };

    keyServiceMock = {
      getOrCreateDeviceKey: vi.fn().mockResolvedValue({
        deviceId: 'dev-1',
        privateKey: {} as CryptoKey,
        publicKeyJwk: { kty: 'EC', crv: 'P-256', x: 'x1', y: 'y1' },
        createdAt: '2026-11-12T10:00:00Z',
      }),
      updateDeviceId: vi.fn().mockResolvedValue(undefined),
      clearKeys: vi.fn().mockResolvedValue(undefined),
    };

    refreshServiceMock = {
      refreshSession: vi.fn().mockReturnValue(of(mockTokenResponse)),
    };

    TestBed.configureTestingModule({
      providers: [
        TokenStore,
        AuthStore,
        { provide: AuthService, useValue: authApiMock },
        { provide: DevicesService, useValue: devicesApiMock },
        { provide: HttpClient, useValue: httpMock },
        { provide: KeyService, useValue: keyServiceMock },
        { provide: TokenRefreshService, useValue: refreshServiceMock },
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

    expect(authApiMock.authLoginPost).toHaveBeenCalled();
    expect(httpMock.get).toHaveBeenCalledWith('/api/v1/users/me');
    expect(devicesApiMock.devicesMeGet).toHaveBeenCalled();
    expect(store.accessToken()).toBe('mock-jwt-token-xyz');
    expect(store.currentUser()).toEqual(mockUserProfile);
    expect(store.activeDevice()).toEqual(mockDeviceResponse);
    expect(store.isAuthenticated()).toBe(true);
    expect(store.isPayer()).toBe(true);
    expect(store.isMerchant()).toBe(false);
    expect(store.error()).toBeNull();
  });

  it('should login successfully and set activeDevice null when GET /devices/me returns null (204)', async () => {
    devicesApiMock.devicesMeGet.mockReturnValue(of(null as unknown as DeviceResponse));

    await store.login({ phone: '+919876543210', password: 'Password123!' });
    expect(store.isAuthenticated()).toBe(true);
    expect(store.activeDevice()).toBeNull();
  });

  it('should handle login failure and set error signal', async () => {
    authApiMock.authLoginPost.mockReturnValue(
      throwError(() => ({ error: { detail: 'Invalid credentials' } })),
    );

    await expect(store.login({ phone: '+919876543210', password: 'Wrong' })).rejects.toBeDefined();
    expect(store.isAuthenticated()).toBe(false);
    expect(store.error()).toBe('Invalid credentials');
  });

  it('should handle login error with generic fallback message', async () => {
    authApiMock.authLoginPost.mockReturnValue(throwError(() => 'Network failure'));

    await expect(store.login({ phone: '+919876543210', password: 'Wrong' })).rejects.toBeDefined();
    expect(store.error()).toBe('Login failed');
  });

  it('should register new user successfully', async () => {
    const res = await store.register({
      phone: '+919876543210',
      password: 'Password123!',
      role: 'PAYER',
    });

    expect(authApiMock.authRegisterPost).toHaveBeenCalled();
    expect(res.id).toBe('new-user-id');
  });

  it('should handle registration failure and set error signal', async () => {
    authApiMock.authRegisterPost.mockReturnValue(
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
    expect(authApiMock.authVerifyOtpPost).toHaveBeenCalledWith({
      body: {
        phone: '+919876543210',
        otp: '123456',
      },
    });
  });

  it('should handle verify OTP failure', async () => {
    authApiMock.authVerifyOtpPost.mockReturnValue(
      throwError(() => ({ error: { detail: 'Invalid OTP code' } })),
    );

    await expect(store.verifyOtp({ phone: '+919876543210', otp: '000000' })).rejects.toBeDefined();
    expect(store.error()).toBe('Invalid OTP code');
  });

  it('should register current device using non-extractable key from KeyService and sync deviceId', async () => {
    await store.registerCurrentDevice('My Pixel Phone');

    expect(keyServiceMock.getOrCreateDeviceKey).toHaveBeenCalled();
    expect(devicesApiMock.devicesPost).toHaveBeenCalledWith({
      'Idempotency-Key': expect.any(String),
      body: {
        publicKeyJwk: { kty: 'EC', crv: 'P-256', x: 'x1', y: 'y1' },
        label: 'My Pixel Phone',
        userAgent: expect.any(String),
      },
    });
    expect(keyServiceMock.updateDeviceId).toHaveBeenCalledWith(
      'dev-1',
      mockDeviceResponse.deviceId,
    );
    expect(store.activeDevice()).toEqual(mockDeviceResponse);
  });

  it('should handle register device failure', async () => {
    devicesApiMock.devicesPost.mockReturnValue(
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
    refreshServiceMock.refreshSession.mockReturnValue(throwError(() => new Error('Token revoked')));

    await expect(store.refreshSession()).rejects.toBeDefined();
    expect(store.currentUser()).toBeNull();
    expect(store.accessToken()).toBeNull();
  });

  it('should set payment PIN', async () => {
    await store.setPin('123456', 'Password123!');
    expect(authApiMock.authPinPut).toHaveBeenCalledWith({
      body: {
        pin: '123456',
        password: 'Password123!',
      },
    });
  });

  it('should handle setPin error', async () => {
    authApiMock.authPinPut.mockReturnValue(throwError(() => ({ error: { detail: 'Weak PIN' } })));

    await expect(store.setPin('000000', 'Password123!')).rejects.toBeDefined();
    expect(store.error()).toBe('Weak PIN');
  });

  it('should store PIN hash and salt in Dexie meta instead of localStorage', async () => {
    localStorage.clear();
    await db.meta.clear();

    await store.setPin('123456', 'Password123!');
    expect(localStorage.getItem('hp_pin_hash')).toBeNull();
    expect(localStorage.getItem('hp_pin_salt')).toBeNull();

    const storedHash = await db.meta.get('pin_hash');
    const storedSalt = await db.meta.get('pin_salt');
    expect(storedHash?.value).toBeDefined();
    expect(storedSalt?.value).toBeDefined();
  });

  it('should verify PIN correctly, increment failed attempts on failure, and lockout for 30 minutes after 5 strikes', async () => {
    await db.meta.clear();
    await store.setPin('123456', 'Password123!');

    // Correct PIN succeeds
    const ok = await store.verifyPin('123456');
    expect(ok).toBe(true);

    // Incorrect attempts 1 through 4
    for (let i = 1; i <= 4; i++) {
      await expect(store.verifyPin('999999')).rejects.toThrow(
        new RegExp(`${5 - i} attempt\\(s\\) remaining`),
      );
      const attempts = await db.meta.get('pin_failed_attempts');
      expect(attempts?.value).toBe(i);
    }

    // 5th strike triggers 30-minute lockout
    await expect(store.verifyPin('999999')).rejects.toThrow(/locked for 30 minutes/i);
    const lockout = await db.meta.get('pin_lockout_until');
    expect(typeof lockout?.value).toBe('number');
    expect((lockout?.value as number) > Date.now()).toBe(true);

    // Subsequent attempt while locked out is rejected even with correct PIN
    await expect(store.verifyPin('123456')).rejects.toThrow(
      /locked due to excessive failed attempts/i,
    );
  });

  it('should clear tokens and user session on logout while preserving device keys', async () => {
    await store.login({ phone: '+919876543210', password: 'Password123!' });
    expect(store.isAuthenticated()).toBe(true);

    await store.logout();
    expect(authApiMock.authLogoutPost).toHaveBeenCalled();
    expect(keyServiceMock.clearKeys).not.toHaveBeenCalled();
    expect(store.currentUser()).toBeNull();
    expect(store.accessToken()).toBeNull();
    expect(store.isAuthenticated()).toBe(false);
  });
});
