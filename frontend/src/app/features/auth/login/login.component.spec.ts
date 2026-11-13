import { TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ActivatedRoute, Router } from '@angular/router';
import { signal } from '@angular/core';
import { LoginComponent } from './login.component';
import { AuthStore } from '../../../core/auth/auth.store';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let authStoreMock: {
    login: ReturnType<typeof vi.fn>;
    loading: ReturnType<typeof signal<boolean>>;
    error: ReturnType<typeof signal<string | null>>;
    isMerchant: ReturnType<typeof signal<boolean>>;
    activeDevice: ReturnType<typeof signal<unknown>>;
  };
  let routerMock: {
    navigate: ReturnType<typeof vi.fn>;
    createUrlTree: ReturnType<typeof vi.fn>;
    serializeUrl: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    authStoreMock = {
      login: vi.fn().mockResolvedValue(undefined),
      loading: signal(false),
      error: signal<string | null>(null),
      isMerchant: signal(false),
      activeDevice: signal<unknown>(null),
    };

    routerMock = {
      navigate: vi.fn().mockResolvedValue(true),
      createUrlTree: vi.fn().mockReturnValue({}),
      serializeUrl: vi.fn().mockReturnValue(''),
    };

    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        { provide: Router, useValue: routerMock },
        { provide: ActivatedRoute, useValue: {} },
        { provide: AuthStore, useValue: authStoreMock },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should initialize with empty form fields', () => {
    expect(component.phone()).toBe('');
    expect(component.password()).toBe('');
    expect(component.formError()).toBeNull();
  });

  it('should validate phone and password before submission', async () => {
    component.phone.set('');
    component.password.set('');

    await component.onSubmit();
    expect(authStoreMock.login).not.toHaveBeenCalled();
    expect(component.formError()).toBe('Please enter a valid phone number and password');
  });

  it('should submit login and navigate to device register if no active device', async () => {
    component.phone.set('9876543210');
    component.password.set('ValidPassword123!');

    await component.onSubmit();

    expect(authStoreMock.login).toHaveBeenCalledWith({
      phone: '+919876543210',
      password: 'ValidPassword123!',
    });
    expect(routerMock.navigate).toHaveBeenCalledWith(['/auth/device-register']);
  });

  it('should navigate to /merchant if active device exists and user is merchant', async () => {
    authStoreMock.activeDevice.set({ deviceId: 'dev-1' });
    authStoreMock.isMerchant.set(true);

    component.phone.set('+919876543210');
    component.password.set('ValidPassword123!');

    await component.onSubmit();
    expect(routerMock.navigate).toHaveBeenCalledWith(['/merchant']);
  });

  it('should navigate to /pay if active device exists and user is payer', async () => {
    authStoreMock.activeDevice.set({ deviceId: 'dev-1' });
    authStoreMock.isMerchant.set(false);

    component.phone.set('9876543210');
    component.password.set('ValidPassword123!');

    await component.onSubmit();
    expect(routerMock.navigate).toHaveBeenCalledWith(['/pay']);
  });

  it('should render error banner when auth error exists', () => {
    authStoreMock.error.set('Invalid credentials');
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();

    const banner = fixture.nativeElement.querySelector('.error-banner');
    expect(banner?.textContent).toContain('Invalid credentials');
  });

  it('should catch and handle login rejection gracefully', async () => {
    authStoreMock.login.mockRejectedValue(new Error('Auth failed'));
    component.phone.set('9876543210');
    component.password.set('ValidPassword123!');

    await component.onSubmit();
    expect(routerMock.navigate).not.toHaveBeenCalled();
  });
});
