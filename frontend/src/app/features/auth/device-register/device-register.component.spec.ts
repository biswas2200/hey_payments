import { TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { Router } from '@angular/router';
import { signal } from '@angular/core';
import { DeviceRegisterComponent } from './device-register.component';
import { AuthStore } from '../../../core/auth/auth.store';

describe('DeviceRegisterComponent', () => {
  let component: DeviceRegisterComponent;
  let authStoreMock: {
    registerCurrentDevice: ReturnType<typeof vi.fn>;
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
      registerCurrentDevice: vi.fn().mockResolvedValue({ deviceId: 'dev-1' }),
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
      imports: [DeviceRegisterComponent],
      providers: [
        { provide: Router, useValue: routerMock },
        { provide: AuthStore, useValue: authStoreMock },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(DeviceRegisterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should initialize with default label and empty error', () => {
    expect(component.label()).toBe('Mobile Phone');
    expect(component.formError()).toBeNull();
  });

  it('should validate empty label', async () => {
    component.label.set('   ');

    await component.onRegister();
    expect(authStoreMock.registerCurrentDevice).not.toHaveBeenCalled();
    expect(component.formError()).toBe('Please provide a name or label for this device');
  });

  it('should submit valid label and navigate to /auth/set-pin', async () => {
    component.label.set('My Office Laptop');

    await component.onRegister();

    expect(authStoreMock.registerCurrentDevice).toHaveBeenCalledWith('My Office Laptop');
    expect(routerMock.navigate).toHaveBeenCalledWith(['/auth/set-pin']);
  });

  it('should render merchant certificate info when user is a merchant', () => {
    authStoreMock.isMerchant.set(true);
    const fixture = TestBed.createComponent(DeviceRegisterComponent);
    fixture.detectChanges();

    const info = fixture.nativeElement.querySelector('.info-text.highlight');
    expect(info?.textContent).toContain('Merchant Certificate (MC)');
  });

  it('should handle registration failure gracefully', async () => {
    authStoreMock.registerCurrentDevice.mockRejectedValue(new Error('Key generation failed'));
    component.label.set('My Phone');

    await component.onRegister();
    expect(routerMock.navigate).not.toHaveBeenCalled();
  });
});
