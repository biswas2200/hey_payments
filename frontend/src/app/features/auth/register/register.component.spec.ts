import { TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ActivatedRoute, Router } from '@angular/router';
import { signal } from '@angular/core';
import { RegisterComponent } from './register.component';
import { AuthStore } from '../../../core/auth/auth.store';

describe('RegisterComponent', () => {
  let component: RegisterComponent;
  let authStoreMock: {
    register: ReturnType<typeof vi.fn>;
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
      register: vi.fn().mockResolvedValue({ id: 'u1' }),
      loading: signal(false),
      error: signal<string | null>(null),
    };

    routerMock = {
      navigate: vi.fn().mockResolvedValue(true),
      createUrlTree: vi.fn().mockReturnValue({}),
      serializeUrl: vi.fn().mockReturnValue(''),
    };

    await TestBed.configureTestingModule({
      imports: [RegisterComponent],
      providers: [
        { provide: Router, useValue: routerMock },
        { provide: ActivatedRoute, useValue: {} },
        { provide: AuthStore, useValue: authStoreMock },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(RegisterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should initialize with default PAYER role and empty inputs', () => {
    expect(component.role()).toBe('PAYER');
    expect(component.phone()).toBe('');
    expect(component.password()).toBe('');
  });

  it('should validate invalid phone number', async () => {
    component.phone.set('123');
    component.password.set('ValidPassword123');

    await component.onSubmit();
    expect(authStoreMock.register).not.toHaveBeenCalled();
    expect(component.formError()).toBe('Please enter a valid 10-digit mobile number');
  });

  it('should validate short password', async () => {
    component.phone.set('9876543210');
    component.password.set('short');

    await component.onSubmit();
    expect(authStoreMock.register).not.toHaveBeenCalled();
    expect(component.formError()).toBe('Password must be at least 10 characters long');
  });

  it('should validate required store name when role is MERCHANT', async () => {
    component.role.set('MERCHANT');
    component.phone.set('9876543210');
    component.password.set('ValidPassword123');
    component.displayName.set('');

    await component.onSubmit();
    expect(authStoreMock.register).not.toHaveBeenCalled();
    expect(component.formError()).toBe('Merchant display name is required');
  });

  it('should submit valid payer registration and navigate to verify-otp', async () => {
    component.phone.set('9876543210');
    component.password.set('ValidPassword123!');
    component.email.set('payer@example.com');

    await component.onSubmit();

    expect(authStoreMock.register).toHaveBeenCalledWith({
      phone: '+919876543210',
      password: 'ValidPassword123!',
      role: 'PAYER',
      email: 'payer@example.com',
      displayName: undefined,
    });

    expect(routerMock.navigate).toHaveBeenCalledWith(['/auth/verify-otp'], {
      queryParams: { phone: '+919876543210' },
    });
  });

  it('should submit valid merchant registration with display name', async () => {
    component.role.set('MERCHANT');
    component.phone.set('+919876543210');
    component.password.set('ValidPassword123!');
    component.displayName.set('Ravi Grocery');

    await component.onSubmit();

    expect(authStoreMock.register).toHaveBeenCalledWith({
      phone: '+919876543210',
      password: 'ValidPassword123!',
      role: 'MERCHANT',
      email: undefined,
      displayName: 'Ravi Grocery',
    });

    expect(routerMock.navigate).toHaveBeenCalledWith(['/auth/verify-otp'], {
      queryParams: { phone: '+919876543210' },
    });
  });

  it('should handle registration rejection gracefully', async () => {
    authStoreMock.register.mockRejectedValue(new Error('Already exists'));
    component.phone.set('9876543210');
    component.password.set('ValidPassword123!');

    await component.onSubmit();
    expect(routerMock.navigate).not.toHaveBeenCalled();
  });
});
