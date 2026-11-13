/**
 * Reactive AuthStore (Angular 22 Signals)
 * Centralizes user profile, access token, active device, and role states.
 * Uses generated OpenAPI client per docs/openapi.yaml.
 */

import { computed, inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../api/generated/services/auth.service';
import { DevicesService } from '../api/generated/services/devices.service';
import { KeyService } from '../crypto/key.service';
import { derivePinHash } from '../crypto/crypto';
import { db } from '../offline/db';
import { TokenStore } from './token.store';
import { TokenRefreshService } from './token-refresh.service';
import type {
  DeviceResponse,
  LoginRequest,
  ProblemDetail,
  ProtocolEnvelope,
  RegisterRequest,
  TokenResponse,
  UserProfile,
  VerifyOtpRequest,
} from '../api/models';

@Injectable({
  providedIn: 'root',
})
export class AuthStore {
  private readonly tokenStore = inject(TokenStore);
  private readonly refreshService = inject(TokenRefreshService);
  private readonly authApi = inject(AuthService);
  private readonly devicesApi = inject(DevicesService);
  private readonly http = inject(HttpClient);
  private readonly keys = inject(KeyService);

  readonly currentUser = signal<UserProfile | null>(null);
  readonly accessToken = this.tokenStore.accessToken;
  readonly activeDevice = signal<DeviceResponse | null>(null);
  readonly loading = signal<boolean>(false);
  readonly error = signal<string | null>(null);

  readonly isAuthenticated = computed(() => !!this.currentUser());
  readonly role = computed(() => this.currentUser()?.role ?? null);
  readonly isPayer = computed(() => this.role() === 'PAYER');
  readonly isMerchant = computed(() => this.role() === 'MERCHANT');

  async login(req: LoginRequest): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const tokenResp = await firstValueFrom(this.authApi.authLoginPost({ body: req }));
      this.tokenStore.setToken(tokenResp.accessToken ?? null, tokenResp.expiresIn);

      const profile = await firstValueFrom(this.http.get<UserProfile>('/api/v1/users/me'));
      this.currentUser.set(profile);

      try {
        const device = await firstValueFrom(this.devicesApi.devicesMeGet());
        this.activeDevice.set(device ?? null);
        if (device && device.merchantCertificate && device.deviceId) {
          await db.certs.put({
            id: device.deviceId,
            type: 'MC',
            envelope: device.merchantCertificate as unknown as ProtocolEnvelope,
            status: 'ACTIVE',
            spent: 0,
            lastCounter: 0,
            lastHash: '',
            createdAt: new Date().toISOString(),
          });
        }
      } catch {
        this.activeDevice.set(null);
      }
    } catch (err: unknown) {
      const detail = this.extractErrorMessage(err, 'Login failed');
      this.error.set(detail);
      throw err;
    } finally {
      this.loading.set(false);
    }
  }

  async register(req: RegisterRequest): Promise<{ id: string }> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const resp = await firstValueFrom(this.authApi.authRegisterPost({ body: req }));
      return { id: resp.id ?? '' };
    } catch (err: unknown) {
      const detail = this.extractErrorMessage(err, 'Registration failed');
      this.error.set(detail);
      throw err;
    } finally {
      this.loading.set(false);
    }
  }

  async verifyOtp(req: VerifyOtpRequest): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      await firstValueFrom(this.authApi.authVerifyOtpPost({ body: req }));
    } catch (err: unknown) {
      const detail = this.extractErrorMessage(err, 'OTP verification failed');
      this.error.set(detail);
      throw err;
    } finally {
      this.loading.set(false);
    }
  }

  async setPin(pin: string, password: string): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      await firstValueFrom(this.authApi.authPinPut({ body: { pin, password } }));
      const salt = crypto.getRandomValues(new Uint8Array(16));
      const hash = await derivePinHash(pin, salt);
      const saltHex = Array.from(salt)
        .map((b) => b.toString(16).padStart(2, '0'))
        .join('');

      await db.meta.put({ key: 'pin_salt', value: saltHex });
      await db.meta.put({ key: 'pin_hash', value: hash });
      await db.meta.put({ key: 'pin_failed_attempts', value: 0 });
      await db.meta.put({ key: 'pin_lockout_until', value: null });

      localStorage.removeItem('hp_pin_salt');
      localStorage.removeItem('hp_pin_hash');
    } catch (err: unknown) {
      const detail = this.extractErrorMessage(err, 'Failed to set payment PIN');
      this.error.set(detail);
      throw err;
    } finally {
      this.loading.set(false);
    }
  }

  async verifyPin(pin: string): Promise<boolean> {
    const lockoutUntilRecord = await db.meta.get('pin_lockout_until');
    const lockoutUntil = lockoutUntilRecord?.value as number | null;
    const now = Date.now();

    if (lockoutUntil && now < lockoutUntil) {
      const remainingMinutes = Math.max(1, Math.ceil((lockoutUntil - now) / 60_000));
      throw new Error(
        `PIN verification locked due to excessive failed attempts. Try again in ${remainingMinutes} minute(s).`,
      );
    }

    const saltRecord = await db.meta.get('pin_salt');
    const hashRecord = await db.meta.get('pin_hash');
    if (!saltRecord?.value || !hashRecord?.value) {
      throw new Error('PIN has not been configured on this device.');
    }

    const saltHex = saltRecord.value as string;
    const storedHash = hashRecord.value as string;
    const saltBytes = new Uint8Array(
      saltHex.match(/.{1,2}/g)?.map((byte) => parseInt(byte, 16)) ?? [],
    );

    const computedHash = await derivePinHash(pin, saltBytes);

    if (computedHash === storedHash) {
      await db.meta.put({ key: 'pin_failed_attempts', value: 0 });
      await db.meta.put({ key: 'pin_lockout_until', value: null });
      return true;
    }

    const attemptsRecord = await db.meta.get('pin_failed_attempts');
    const currentAttempts = ((attemptsRecord?.value as number) || 0) + 1;

    if (currentAttempts >= 5) {
      const lockDurationMs = 30 * 60 * 1000; // 30 minutes
      const newLockoutUntil = now + lockDurationMs;
      await db.meta.put({ key: 'pin_failed_attempts', value: currentAttempts });
      await db.meta.put({ key: 'pin_lockout_until', value: newLockoutUntil });
      throw new Error('Too many incorrect PIN attempts. Account locked for 30 minutes.');
    } else {
      await db.meta.put({ key: 'pin_failed_attempts', value: currentAttempts });
      const remaining = 5 - currentAttempts;
      throw new Error(
        `Incorrect PIN. ${remaining} attempt(s) remaining before a 30-minute lockout.`,
      );
    }
  }

  async registerCurrentDevice(label?: string): Promise<DeviceResponse> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const keyRecord = await this.keys.getOrCreateDeviceKey();
      const jwk = keyRecord.publicKeyJwk;

      const deviceResp = await firstValueFrom(
        this.devicesApi.devicesPost({
          'Idempotency-Key': crypto.randomUUID(),
          body: {
            publicKeyJwk: {
              kty: 'EC',
              crv: 'P-256',
              x: jwk.x ?? '',
              y: jwk.y ?? '',
            },
            label: label || 'Mobile Browser',
            userAgent: navigator.userAgent || 'HeyPayments/1.0',
          },
        }),
      );

      const deviceId = deviceResp.deviceId ?? '';
      // Synchronize local key record deviceId with server-assigned deviceId
      if (deviceId && keyRecord.deviceId !== deviceId) {
        await this.keys.updateDeviceId(keyRecord.deviceId, deviceId);
      }

      // Persist merchant certificate to Dexie db.certs if issued
      if (deviceResp.merchantCertificate && deviceId) {
        await db.certs.put({
          id: deviceId,
          type: 'MC',
          envelope: deviceResp.merchantCertificate as unknown as ProtocolEnvelope,
          status: 'ACTIVE',
          spent: 0,
          lastCounter: 0,
          lastHash: '',
          createdAt: new Date().toISOString(),
        });
      }

      this.activeDevice.set(deviceResp);
      return deviceResp;
    } catch (err: unknown) {
      const detail = this.extractErrorMessage(err, 'Device registration failed');
      this.error.set(detail);
      throw err;
    } finally {
      this.loading.set(false);
    }
  }

  async refreshSession(): Promise<TokenResponse> {
    try {
      const tokenResp = await firstValueFrom(this.refreshService.refreshSession());
      this.tokenStore.setToken(tokenResp.accessToken ?? null, tokenResp.expiresIn);
      return tokenResp;
    } catch (err) {
      await this.logout();
      throw err;
    }
  }

  async logout(): Promise<void> {
    try {
      await firstValueFrom(this.authApi.authLogoutPost());
    } catch {
      // Ignore network errors on logout
    } finally {
      this.tokenStore.clear();
      this.currentUser.set(null);
      this.activeDevice.set(null);
      this.error.set(null);
    }
  }

  private extractErrorMessage(err: unknown, defaultMessage: string): string {
    if (err && typeof err === 'object' && 'error' in err) {
      const problem = (err as { error: ProblemDetail }).error;
      if (problem && typeof problem === 'object' && problem.detail) {
        return problem.detail;
      }
    }
    return defaultMessage;
  }
}
