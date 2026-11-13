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
        const device = await firstValueFrom(this.http.get<DeviceResponse>('/api/v1/devices/me'));
        this.activeDevice.set(device);
        if (device.merchantCertificate && device.deviceId) {
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
      await firstValueFrom(
        this.http.post<{ status: string; message: string }>('/api/v1/auth/verify-otp', req),
      );
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
      await firstValueFrom(this.http.put<void>('/api/v1/auth/pin', { pin, password }));
      const salt = crypto.getRandomValues(new Uint8Array(16));
      const hash = await derivePinHash(pin, salt);
      localStorage.setItem(
        'hp_pin_salt',
        Array.from(salt)
          .map((b) => b.toString(16).padStart(2, '0'))
          .join(''),
      );
      localStorage.setItem('hp_pin_hash', hash);
    } catch (err: unknown) {
      const detail = this.extractErrorMessage(err, 'Failed to set payment PIN');
      this.error.set(detail);
      throw err;
    } finally {
      this.loading.set(false);
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
      await firstValueFrom(this.http.post<void>('/api/v1/auth/logout', {}));
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
