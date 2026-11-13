import { TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ActivatedRoute, Router } from '@angular/router';
import { signal } from '@angular/core';
import { VerifyOtpComponent } from './verify-otp.component';
import { AuthStore } from '../../../core/auth/auth.store';

describe('VerifyOtpComponent', () => {
  let component: VerifyOtpComponent;
  let authStoreMock: {
    verifyOtp: ReturnType<typeof vi.fn>;
    loading: ReturnType<typeof signal<boolean>>;
    error: ReturnType<typeof signal<string | null>>;
  };
  let routerMock: {
    navigate: ReturnType<typeof vi.fn>;
    createUrlTree: ReturnType<typeof vi.fn>;
    serializeUrl: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    authStoreMock = {
      verifyOtp: vi.fn().mockResolvedValue({ status: 'SUCCESS' }),
      loading: signal(false),
      error: signal<string | null>(null),
    };

    routerMock = {
      navigate: vi.fn().mockResolvedValue(true),
      createUrlTree: vi.fn().mockReturnValue({}),
      serializeUrl: vi.fn().mockReturnValue(''),
    };

    await TestBed.configureTestingModule({
      imports: [VerifyOtpComponent],
      providers: [
        { provide: Router, useValue: routerMock },
        { provide: AuthStore, useValue: authStoreMock },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              queryParamMap: {
                get: vi.fn((key: string) => (key === 'phone' ? '+919876543210' : null)),
              },
            },
          },
        },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(VerifyOtpComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should initialize phone from route query params', () => {
    expect(component.phone()).toBe('+919876543210');
    expect(component.otp()).toBe('');
  });

  it('should validate 6-digit OTP code before submission', async () => {
    component.otp.set('123');

    await component.onSubmit();
    expect(authStoreMock.verifyOtp).not.toHaveBeenCalled();
    expect(component.formError()).toBe('Please enter a valid 6-digit OTP code');
  });

  it('should submit valid 6-digit OTP and navigate to login with verified flag', async () => {
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

  it('should handle verification failure gracefully', async () => {
    authStoreMock.verifyOtp.mockRejectedValue(new Error('Invalid OTP'));
    component.otp.set('123456');

    await component.onSubmit();
    expect(routerMock.navigate).not.toHaveBeenCalled();
  });
});
