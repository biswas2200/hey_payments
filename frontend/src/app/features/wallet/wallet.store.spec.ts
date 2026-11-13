import { TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { of, throwError } from 'rxjs';
import { WalletStore } from './wallet.store';
import { WalletService } from '../../core/api/generated/services/wallet.service';
import type { WalletResponse } from '../../core/api/models';

describe('WalletStore', () => {
  let store: WalletStore;
  let walletApiMock: {
    walletsMeGet: ReturnType<typeof vi.fn>;
    walletsMeFundPost: ReturnType<typeof vi.fn>;
  };

  const mockWallet: WalletResponse = {
    mainBalance: 50000,
    offlineEscrow: 0,
    activeBc: undefined,
  };

  beforeEach(() => {
    walletApiMock = {
      walletsMeGet: vi.fn(),
      walletsMeFundPost: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [WalletStore, { provide: WalletService, useValue: walletApiMock }],
    });

    store = TestBed.inject(WalletStore);
  });

  it('should load wallet balances and format currency properly', async () => {
    walletApiMock.walletsMeGet.mockReturnValue(of(mockWallet));

    await store.loadWallet();

    expect(store.wallet()).toEqual(mockWallet);
    expect(store.mainBalance()).toBe(50000);
    expect(store.offlineEscrow()).toBe(0);
    expect(store.mainBalanceFormatted()).toBe('₹500.00');
  });

  it('should fund wallet and update balance', async () => {
    const updatedWallet: WalletResponse = {
      mainBalance: 150000,
      offlineEscrow: 0,
      activeBc: undefined,
    };
    walletApiMock.walletsMeFundPost.mockReturnValue(of(updatedWallet));

    await store.fundWallet(100000, 'SIM_BANK');

    expect(store.wallet()).toEqual(updatedWallet);
    expect(store.mainBalanceFormatted()).toBe('₹1,500.00');
    expect(store.error()).toBeNull();
  });

  it('should rollback balance on funding failure', async () => {
    store.wallet.set({ mainBalance: 50000, offlineEscrow: 0, activeBc: undefined });
    walletApiMock.walletsMeFundPost.mockReturnValue(
      throwError(() => ({
        error: { title: 'Limit Exceeded', detail: 'Daily limit exceeded' },
      })),
    );

    await expect(store.fundWallet(6000000, 'SIM_BANK')).rejects.toBeTruthy();
    expect(store.mainBalance()).toBe(50000);
    expect(store.error()).toContain('Daily limit exceeded');
  });
});
