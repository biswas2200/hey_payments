import { TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { Router } from '@angular/router';
import { signal } from '@angular/core';
import { SetPinComponent } from './set-pin.component';
import { AuthStore } from '../../../core/auth/auth.store';

describe('SetPinComponent', () => {
  let component: SetPinComponent;
  let authStoreMock: {
    setPin: ReturnType<typeof vi.fn>;
    loading: ReturnType<typeof signal<boolean>>;
    error: ReturnType<typeof signal<string | null>>;
    isMerchant: ReturnType<typeof signal<boolean>>;
  };
  let routerMock: {
    navigate: ReturnType<typeof vi.fn>;
    createUrlTree: ReturnType<typeof vi.fn>;
    serializeUrl: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    authStoreMock = {
      setPin: vi.fn().mockResolvedValue(undefined),
      loading: signal(false),
      error: signal<string | null>(null),
      isMerchant: signal(false),
    };

    routerMock = {
      navigate: vi.fn().mockResolvedValue(true),
      createUrlTree: vi.fn().mockReturnValue({}),
      serializeUrl: vi.fn().mockReturnValue(''),
    };

    await TestBed.configureTestingModule({
      imports: [SetPinComponent],
      providers: [
        { provide: Router, useValue: routerMock },
        { provide: AuthStore, useValue: authStoreMock },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(SetPinComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should validate 6-digit numeric PIN format', async () => {
    component.pin.set('12');
    component.confirmPin.set('12');

    await component.onSubmit();
    expect(authStoreMock.setPin).not.toHaveBeenCalled();
    expect(component.formError()).toBe('PIN must be exactly 6 digits');
  });

  it('should validate PIN match', async () => {
    component.pin.set('123456');
    component.confirmPin.set('654321');

    await component.onSubmit();
    expect(authStoreMock.setPin).not.toHaveBeenCalled();
    expect(component.formError()).toBe('PINs do not match');
  });

  it('should validate password is required', async () => {
    component.pin.set('123456');
    component.confirmPin.set('123456');
    component.password.set('');

    await component.onSubmit();
    expect(authStoreMock.setPin).not.toHaveBeenCalled();
    expect(component.formError()).toBe('Current password is required to verify PIN setup');
  });

  it('should submit valid PIN and password and navigate to /pay for payer', async () => {
    component.pin.set('123456');
    component.confirmPin.set('123456');
    component.password.set('Password123!');

    await component.onSubmit();
    expect(authStoreMock.setPin).toHaveBeenCalledWith('123456', 'Password123!');
    expect(routerMock.navigate).toHaveBeenCalledWith(['/pay']);
  });

  it('should submit valid PIN and password and navigate to /merchant for merchant', async () => {
    authStoreMock.isMerchant.set(true);
    component.pin.set('654321');
    component.confirmPin.set('654321');
    component.password.set('Password123!');

    await component.onSubmit();
    expect(authStoreMock.setPin).toHaveBeenCalledWith('654321', 'Password123!');
    expect(routerMock.navigate).toHaveBeenCalledWith(['/merchant']);
  });

  it('should catch and handle setPin errors gracefully', async () => {
    authStoreMock.setPin.mockRejectedValue(new Error('Weak PIN'));
    component.pin.set('123456');
    component.confirmPin.set('123456');
    component.password.set('Password123!');

    await component.onSubmit();
    expect(routerMock.navigate).not.toHaveBeenCalled();
  });
});
