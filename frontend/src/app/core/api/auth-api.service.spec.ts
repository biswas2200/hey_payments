import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { describe, beforeEach, afterEach, it, expect } from 'vitest';
import { AuthApiService } from './auth-api.service';
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

describe('AuthApiService', () => {
  let service: AuthApiService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [AuthApiService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthApiService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('should send POST request for register', () => {
    const req: RegisterRequest = {
      phone: '+919876543210',
      password: 'Password123!',
      role: 'PAYER',
    };

    service.register(req).subscribe((res) => {
      expect(res.id).toBe('u1');
    });

    const op = httpTesting.expectOne('/api/v1/auth/register');
    expect(op.request.method).toBe('POST');
    expect(op.request.body).toEqual(req);
    op.flush({ id: 'u1' });
  });

  it('should send POST request for verifyOtp', () => {
    const req: VerifyOtpRequest = {
      phone: '+919876543210',
      otp: '123456',
    };

    service.verifyOtp(req).subscribe((res) => {
      expect(res.status).toBe('SUCCESS');
    });

    const op = httpTesting.expectOne('/api/v1/auth/verify-otp');
    expect(op.request.method).toBe('POST');
    expect(op.request.body).toEqual(req);
    op.flush({ status: 'SUCCESS', message: 'Verified' });
  });

  it('should send POST request for login', () => {
    const req: LoginRequest = {
      phone: '+919876543210',
      password: 'Password123!',
    };
    const mockToken: TokenResponse = {
      accessToken: 'token123',
      expiresIn: 900,
      user: { id: 'u1', role: 'PAYER', displayName: 'User' },
    };

    service.login(req).subscribe((res) => {
      expect(res.accessToken).toBe('token123');
    });

    const op = httpTesting.expectOne('/api/v1/auth/login');
    expect(op.request.method).toBe('POST');
    expect(op.request.body).toEqual(req);
    op.flush(mockToken);
  });

  it('should send POST request for refresh', () => {
    const mockToken: TokenResponse = {
      accessToken: 'token456',
      expiresIn: 900,
      user: { id: 'u1', role: 'PAYER', displayName: 'User' },
    };

    service.refresh().subscribe((res) => {
      expect(res.accessToken).toBe('token456');
    });

    const op = httpTesting.expectOne('/api/v1/auth/refresh');
    expect(op.request.method).toBe('POST');
    expect(op.request.body).toEqual({});
    op.flush(mockToken);
  });

  it('should send POST request for logout', () => {
    service.logout().subscribe();

    const op = httpTesting.expectOne('/api/v1/auth/logout');
    expect(op.request.method).toBe('POST');
    op.flush(null);
  });

  it('should send GET request for getMe', () => {
    const mockProfile: UserProfile = {
      id: 'u1',
      phone: '+919876543210',
      role: 'PAYER',
      status: 'ACTIVE',
      createdAt: '2026-11-12T10:00:00Z',
    };

    service.getMe().subscribe((res) => {
      expect(res.id).toBe('u1');
      expect(res.role).toBe('PAYER');
    });

    const op = httpTesting.expectOne('/api/v1/users/me');
    expect(op.request.method).toBe('GET');
    op.flush(mockProfile);
  });

  it('should send PUT request to /api/v1/auth/pin with pin and password', () => {
    const req: SetPinRequest = { pin: '123456', password: 'Password123!' };

    service.setPin(req).subscribe();

    const op = httpTesting.expectOne('/api/v1/auth/pin');
    expect(op.request.method).toBe('PUT');
    expect(op.request.body).toEqual(req);
    op.flush(null);
  });

  it('should send POST request for registerDevice', () => {
    const req: RegisterDeviceRequest = {
      publicKeyJwk: { kty: 'EC', crv: 'P-256', x: 'x', y: 'y' },
      label: 'Pixel',
    };
    const mockDevice: DeviceResponse = {
      deviceId: 'd1',
      status: 'ACTIVE',
      lastCounter: 0,
    };

    service.registerDevice(req).subscribe((res) => {
      expect(res.deviceId).toBe('d1');
    });

    const op = httpTesting.expectOne('/api/v1/devices');
    expect(op.request.method).toBe('POST');
    expect(op.request.body).toEqual(req);
    op.flush(mockDevice);
  });

  it('should send GET request for getMyDevice', () => {
    const mockDevice: DeviceResponse = {
      deviceId: 'd1',
      status: 'ACTIVE',
      lastCounter: 0,
    };

    service.getMyDevice().subscribe((res) => {
      expect(res.deviceId).toBe('d1');
    });

    const op = httpTesting.expectOne('/api/v1/devices/me');
    expect(op.request.method).toBe('GET');
    op.flush(mockDevice);
  });
});
