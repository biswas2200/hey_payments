import { beforeEach, describe, expect, it, vi } from 'vitest';
import { LoginComponent } from './login.component';
import { AuthStore } from '../../../core/auth/auth.store';
import { Router } from '@angular/router';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let authStoreMock: Partial<AuthStore>;
  let routerMock: Partial<Router>;

  beforeEach(() => {
    authStoreMock = {
      login: vi.fn().mockResolvedValue(undefined),
      loading: vi.fn().mockReturnValue(false) as unknown as AuthStore['loading'],
      error: vi.fn().mockReturnValue(null) as unknown as AuthStore['error'],
      isMerchant: vi.fn().mockReturnValue(false) as unknown as AuthStore['isMerchant'],
      activeDevice: vi.fn().mockReturnValue(null) as unknown as AuthStore['activeDevice'],
    };

    routerMock = {
      navigate: vi.fn().mockResolvedValue(true),
    };

    component = new LoginComponent(authStoreMock as AuthStore, routerMock as Router);
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
});
