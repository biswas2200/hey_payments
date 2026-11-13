/**
 * TypeScript API models re-exported from OpenAPI generated client
 * with extensions for Phase 1 simulation flows per proposals/002-openapi-phase1.md.
 */

export * from './generated/models';
import type { SignedEnvelope as ProtocolEnvelope } from '../crypto/crypto';
import type { Role } from './generated/models/role';

export interface UserSummary {
  id: string;
  role: Role;
  displayName?: string;
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

export interface VerifyOtpRequest {
  phone: string;
  otp: string;
}

export interface SetPinRequest {
  pin: string;
  password: string;
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

export type { ProtocolEnvelope };
