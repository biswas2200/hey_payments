import { beforeEach, describe, expect, it, vi } from 'vitest';
import { DeviceRegisterComponent } from './device-register.component';
import { AuthStore } from '../../../core/auth/auth.store';
import { Router } from '@angular/router';

describe('DeviceRegisterComponent', () => {
  let component: DeviceRegisterComponent;
  let authStoreMock: Partial<AuthStore>;
  let routerMock: Partial<Router>;

  beforeEach(() => {
    authStoreMock = {
      registerCurrentDevice: vi.fn().mockResolvedValue({
        deviceId: 'dev-1',
        status: 'ACTIVE',
        lastCounter: 0,
      }),
      loading: vi.fn().mockReturnValue(false) as unknown as AuthStore['loading'],
      error: vi.fn().mockReturnValue(null) as unknown as AuthStore['error'],
      isMerchant: vi.fn().mockReturnValue(false) as unknown as AuthStore['isMerchant'],
      isPayer: vi.fn().mockReturnValue(true) as unknown as AuthStore['isPayer'],
    };

    routerMock = {
      navigate: vi.fn().mockResolvedValue(true),
    };

    component = new DeviceRegisterComponent(authStoreMock as AuthStore, routerMock as Router);
  });

  it('should register device and advance to set-pin', async () => {
    component.label.set('Personal Phone');

    await component.onRegister();

    expect(authStoreMock.registerCurrentDevice).toHaveBeenCalledWith('Personal Phone');
    expect(routerMock.navigate).toHaveBeenCalledWith(['/auth/set-pin']);
  });
});
