import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { HomeComponent } from './home.component';
import { WalletStore } from '../wallet/wallet.store';
import { AuthStore } from '../../core/auth/auth.store';
import { signal } from '@angular/core';

describe('HomeComponent', () => {
  let fixture: ComponentFixture<HomeComponent>;
  let walletStoreMock: {
    wallet: ReturnType<typeof signal>;
    mainBalance: ReturnType<typeof signal>;
    offlineEscrow: ReturnType<typeof signal>;
    mainBalanceFormatted: ReturnType<typeof signal>;
    offlineEscrowFormatted: ReturnType<typeof signal>;
    loading: ReturnType<typeof signal>;
    loadWallet: ReturnType<typeof vi.fn>;
  };
  let authStoreMock: {
    currentUser: ReturnType<typeof signal>;
    logout: ReturnType<typeof vi.fn>;
  };
  let routerMock: { navigate: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    walletStoreMock = {
      wallet: signal({ mainBalance: 425000, offlineEscrow: 164000 }),
      mainBalance: signal(425000),
      offlineEscrow: signal(164000),
      mainBalanceFormatted: signal('₹4,250.00'),
      offlineEscrowFormatted: signal('₹1,640.00'),
      loading: signal(false),
      loadWallet: vi.fn().mockResolvedValue(undefined),
    };

    authStoreMock = {
      currentUser: signal({ phone: '+919876543210', displayName: 'Priya', role: 'PAYER' }),
      logout: vi.fn().mockResolvedValue(undefined),
    };

    routerMock = {
      navigate: vi.fn().mockResolvedValue(true),
    };

    await TestBed.configureTestingModule({
      imports: [HomeComponent],
      providers: [
        { provide: Router, useValue: routerMock },
        { provide: WalletStore, useValue: walletStoreMock },
        { provide: AuthStore, useValue: authStoreMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(HomeComponent);
    fixture.detectChanges();
  });

  it('should render user greeting, offline balance, and main balance', () => {
    const el: HTMLElement = fixture.nativeElement;
    expect(el.textContent).toContain('Priya');
    expect(el.textContent).toContain('₹1,640.00');
    expect(el.textContent).toContain('₹4,250.00');
  });

  it('should navigate to /wallet/fund when clicking Add Money button', () => {
    const el: HTMLElement = fixture.nativeElement;
    const addBtn = el.querySelector('#add-money-btn') as HTMLButtonElement;
    expect(addBtn).toBeTruthy();
    addBtn.click();
    expect(routerMock.navigate).toHaveBeenCalledWith(['/wallet/fund']);
  });

  it('should logout and redirect to login when clicking logout button', async () => {
    const el: HTMLElement = fixture.nativeElement;
    const logoutBtn = el.querySelector('#logout-btn') as HTMLButtonElement;
    expect(logoutBtn).toBeTruthy();
    logoutBtn.click();
    await fixture.whenStable();
    expect(authStoreMock.logout).toHaveBeenCalled();
    expect(routerMock.navigate).toHaveBeenCalledWith(['/auth/login']);
  });
});
