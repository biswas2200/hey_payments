import { beforeEach, describe, expect, it, vi } from 'vitest';
import { VerifyOtpComponent } from './verify-otp.component';
import { AuthStore } from '../../../core/auth/auth.store';
import { ActivatedRoute, Router } from '@angular/router';

describe('VerifyOtpComponent', () => {
  let component: VerifyOtpComponent;
  let authStoreMock: Partial<AuthStore>;
  let routerMock: Partial<Router>;
  let routeMock: Partial<ActivatedRoute>;

  beforeEach(() => {
    authStoreMock = {
      verifyOtp: vi.fn().mockResolvedValue(undefined),
      loading: vi.fn().mockReturnValue(false) as unknown as AuthStore['loading'],
      error: vi.fn().mockReturnValue(null) as unknown as AuthStore['error'],
    };

    routerMock = {
      navigate: vi.fn().mockResolvedValue(true),
    };

    routeMock = {
      snapshot: {
        queryParamMap: {
          get: vi.fn().mockReturnValue('+919876543210'),
        },
      } as unknown as ActivatedRoute['snapshot'],
    };

    component = new VerifyOtpComponent(
      authStoreMock as AuthStore,
      routerMock as Router,
      routeMock as ActivatedRoute,
    );
  });

  it('should read phone from query parameters', () => {
    expect(component.phone()).toBe('+919876543210');
  });

  it('should validate 6-digit OTP code before submission', async () => {
    component.otp.set('123'); // incomplete

    await component.onSubmit();

    expect(authStoreMock.verifyOtp).not.toHaveBeenCalled();
    expect(component.formError()).toBe('Please enter a valid 6-digit OTP code');
  });

  it('should submit valid OTP and navigate to login', async () => {
    component.otp.set('123456');

    await component.onSubmit();

    expect(authStoreMock.verifyOtp).toHaveBeenCalledWith({
      phone: '+919876543210',
      otp: '123456',
    });
    expect(routerMock.navigate).toHaveBeenCalledWith(['/auth/login'], {
      queryParams: { verified: 'true' },
    });
  });
});
