/**
 * TypeScript API models matching docs/openapi.yaml (HP-OFFLINE v1).
 */

import type { SignedEnvelope } from '../crypto/crypto';

export type Role = 'PAYER' | 'MERCHANT' | 'ADMIN';

export interface UserSummary {
  id: string;
  role: Role;
  displayName: string;
}

export interface UserProfile {
  id: string;
  phone: string;
  email?: string;
  role: Role;
  status: string;
  displayName?: string;
  createdAt: string;
}

export interface RegisterRequest {
  phone: string;
  email?: string;
  password: string;
  role: 'PAYER' | 'MERCHANT';
  displayName?: string;
}

export interface VerifyOtpRequest {
  phone: string;
  otp: string;
}

export interface LoginRequest {
  phone: string;
  password: string;
}

export interface TokenResponse {
  accessToken: string;
  expiresIn: number;
  user: UserSummary;
}

export interface SetPinRequest {
  pin: string;
}

export interface JwkDto {
  kid?: string;
  kty: string;
  crv: string;
  x: string;
  y: string;
  status?: string;
}

export interface RegisterDeviceRequest {
  publicKeyJwk: JwkDto;
  label?: string;
  userAgent?: string;
}

export interface DeviceResponse {
  deviceId: string;
  status: 'ACTIVE' | 'FROZEN' | 'REVOKED' | 'CLOSED';
  lastCounter: number;
  merchantCertificate?: SignedEnvelope;
}

export interface ProblemDetail {
  type: string;
  title: string;
  status: number;
  code: string;
  detail?: string;
  instance?: string;
  requestId?: string;
  errors?: { field?: string; message?: string }[];
}
