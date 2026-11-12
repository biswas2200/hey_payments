/**
 * Auth & Device API Client matching docs/openapi.yaml.
 */

import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import type {
  DeviceResponse,
  LoginRequest,
  RegisterDeviceRequest,
  RegisterRequest,
  SetPinRequest,
  TokenResponse,
  UserProfile,
  VerifyOtpRequest,
} from './models';

@Injectable({
  providedIn: 'root',
})
export class AuthApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1';

  register(req: RegisterRequest): Observable<{ id: string }> {
    return this.http.post<{ id: string }>(`${this.baseUrl}/auth/register`, req);
  }

  verifyOtp(req: VerifyOtpRequest): Observable<{ status: string; message: string }> {
    return this.http.post<{ status: string; message: string }>(
      `${this.baseUrl}/auth/verify-otp`,
      req,
    );
  }

  login(req: LoginRequest): Observable<TokenResponse> {
    return this.http.post<TokenResponse>(`${this.baseUrl}/auth/login`, req);
  }

  refresh(): Observable<TokenResponse> {
    return this.http.post<TokenResponse>(`${this.baseUrl}/auth/refresh`, {});
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/auth/logout`, {});
  }

  getMe(): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.baseUrl}/users/me`);
  }

  setPin(req: SetPinRequest): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/users/me/pin`, req);
  }

  registerDevice(req: RegisterDeviceRequest): Observable<DeviceResponse> {
    return this.http.post<DeviceResponse>(`${this.baseUrl}/devices`, req);
  }

  getMyDevice(): Observable<DeviceResponse> {
    return this.http.get<DeviceResponse>(`${this.baseUrl}/devices/me`);
  }
}
