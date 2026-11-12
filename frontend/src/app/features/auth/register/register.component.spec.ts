import { beforeEach, describe, expect, it, vi } from 'vitest';
import { RegisterComponent } from './register.component';
import { AuthStore } from '../../../core/auth/auth.store';
import { Router } from '@angular/router';

describe('RegisterComponent', () => {
  let component: RegisterComponent;
  let authStoreMock: Partial<AuthStore>;
  let routerMock: Partial<Router>;

  beforeEach(() => {
    authStoreMock = {
      register: vi.fn().mockResolvedValue({ id: 'user-123' }),
      loading: vi.fn().mockReturnValue(false) as unknown as AuthStore['loading'],
      error: vi.fn().mockReturnValue(null) as unknown as AuthStore['error'],
    };

    routerMock = {
      navigate: vi.fn().mockResolvedValue(true),
    };

    component = new RegisterComponent(authStoreMock as AuthStore, routerMock as Router);
  });

  it('should default to PAYER role', () => {
    expect(component.role()).toBe('PAYER');
  });

  it('should validate phone pattern and password length', async () => {
    component.phone.set('123'); // invalid
    component.password.set('short'); // < 10 chars

    await component.onSubmit();

    expect(authStoreMock.register).not.toHaveBeenCalled();
    expect(component.formError()).toBeTruthy();
  });

  it('should register payer and navigate to verify-otp with queryParams', async () => {
    component.phone.set('9876543210');
    component.password.set('SecurePass123!');
    component.role.set('PAYER');

    await component.onSubmit();

    expect(authStoreMock.register).toHaveBeenCalledWith({
      phone: '+919876543210',
      email: undefined,
      password: 'SecurePass123!',
      role: 'PAYER',
      displayName: undefined,
    });
    expect(routerMock.navigate).toHaveBeenCalledWith(['/auth/verify-otp'], {
      queryParams: { phone: '+919876543210' },
    });
  });
});
