import { beforeEach, describe, expect, it, vi } from 'vitest';
import { SetPinComponent } from './set-pin.component';
import { AuthStore } from '../../../core/auth/auth.store';
import { Router } from '@angular/router';

describe('SetPinComponent', () => {
  let component: SetPinComponent;
  let authStoreMock: Partial<AuthStore>;
  let routerMock: Partial<Router>;

  beforeEach(() => {
    authStoreMock = {
      setPin: vi.fn().mockResolvedValue(undefined),
      loading: vi.fn().mockReturnValue(false) as unknown as AuthStore['loading'],
      error: vi.fn().mockReturnValue(null) as unknown as AuthStore['error'],
      isMerchant: vi.fn().mockReturnValue(false) as unknown as AuthStore['isMerchant'],
    };

    routerMock = {
      navigate: vi.fn().mockResolvedValue(true),
    };

    component = new SetPinComponent(authStoreMock as AuthStore, routerMock as Router);
  });

  it('should validate matching 6-digit PIN and confirmation', async () => {
    component.pin.set('123456');
    component.confirmPin.set('654321');

    await component.onSubmit();

    expect(authStoreMock.setPin).not.toHaveBeenCalled();
    expect(component.formError()).toBe('PINs do not match');
  });

  it('should save PIN and navigate to home', async () => {
    component.pin.set('123456');
    component.confirmPin.set('123456');

    await component.onSubmit();

    expect(authStoreMock.setPin).toHaveBeenCalledWith('123456');
    expect(routerMock.navigate).toHaveBeenCalledWith(['/pay']);
  });
});
