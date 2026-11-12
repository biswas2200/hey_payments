/**
 * Reactive AuthStore (Angular 22 Signals)
 * Centralizes user profile, access token, active device, and role states.
 */

import { computed, inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AuthApiService } from '../api/auth-api.service';
import { KeyService } from '../crypto/key.service';
import { derivePinHash } from '../crypto/crypto';
import type {
  DeviceResponse,
  LoginRequest,
  ProblemDetail,
  RegisterRequest,
  TokenResponse,
  UserProfile,
  VerifyOtpRequest,
} from '../api/models';

@Injectable({
  providedIn: 'root',
})
export class AuthStore {
  readonly currentUser = signal<UserProfile | null>(null);
  readonly accessToken = signal<string | null>(null);
  readonly activeDevice = signal<DeviceResponse | null>(null);
  readonly loading = signal<boolean>(false);
  readonly error = signal<string | null>(null);

  readonly isAuthenticated = computed(() => !!this.currentUser());
  readonly role = computed(() => this.currentUser()?.role ?? null);
  readonly isPayer = computed(() => this.role() === 'PAYER');
  readonly isMerchant = computed(() => this.role() === 'MERCHANT');

  private readonly api = inject(AuthApiService);
  private readonly keys = inject(KeyService);

  async login(req: LoginRequest): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const tokenResp = await firstValueFrom(this.api.login(req));
      this.accessToken.set(tokenResp.accessToken);

      const profile = await firstValueFrom(this.api.getMe());
      this.currentUser.set(profile);
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
      return await firstValueFrom(this.api.register(req));
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
      await firstValueFrom(this.api.verifyOtp(req));
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
      await firstValueFrom(this.api.setPin({ pin, password }));
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
        this.api.registerDevice({
          publicKeyJwk: {
            kty: jwk.kty ?? 'EC',
            crv: jwk.crv ?? 'P-256',
            x: jwk.x ?? '',
            y: jwk.y ?? '',
          },
          label: label || 'Mobile Browser',
          userAgent: navigator.userAgent || 'HeyPayments/1.0',
        }),
      );

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
      const tokenResp = await firstValueFrom(this.api.refresh());
      this.accessToken.set(tokenResp.accessToken);
      return tokenResp;
    } catch (err) {
      await this.logout();
      throw err;
    }
  }

  async logout(): Promise<void> {
    try {
      await firstValueFrom(this.api.logout());
    } catch {
      // Ignore network errors on logout
    } finally {
      this.accessToken.set(null);
      this.currentUser.set(null);
      this.activeDevice.set(null);
      this.error.set(null);
      await this.keys.clearKeys();
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
