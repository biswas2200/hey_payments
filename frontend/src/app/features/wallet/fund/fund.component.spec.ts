import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { FundComponent } from './fund.component';
import { WalletStore } from '../wallet.store';
import { signal } from '@angular/core';

describe('FundComponent', () => {
  let component: FundComponent;
  let fixture: ComponentFixture<FundComponent>;
  let walletStoreMock: any;
  let routerMock: { navigate: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    walletStoreMock = {
      wallet: signal({ mainBalance: 50000, offlineEscrow: 0 }),
      mainBalance: signal(50000),
      mainBalanceFormatted: signal('₹500.00'),
      loading: signal(false),
      error: signal<string | null>(null),
      fundWallet: vi.fn().mockResolvedValue({ mainBalance: 150000, offlineEscrow: 0 }),
      loadWallet: vi.fn().mockResolvedValue(undefined),
    };

    routerMock = {
      navigate: vi.fn().mockResolvedValue(true),
    };

    await TestBed.configureTestingModule({
      imports: [FundComponent],
      providers: [
        { provide: Router, useValue: routerMock },
        { provide: WalletStore, useValue: walletStoreMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(FundComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should render quick amount chips and update amount on click', () => {
    const el: HTMLElement = fixture.nativeElement;
    const chip500 = el.querySelector('#chip-500') as HTMLButtonElement;
    expect(chip500).toBeTruthy();
    chip500.click();
    fixture.detectChanges();

    const input = el.querySelector('#fund-amount-input') as HTMLInputElement;
    expect(input.value).toBe('500');
  });

  it('should call store.fundWallet with converted paise on submit', async () => {
    component.amount.set(1000);
    fixture.detectChanges();

    const submitBtn = fixture.nativeElement.querySelector('#fund-submit-btn') as HTMLButtonElement;
    submitBtn.click();
    await fixture.whenStable();

    expect(walletStoreMock.fundWallet).toHaveBeenCalledWith(100000, expect.any(String));
  });

  it('should display error message when funding fails', async () => {
    walletStoreMock.fundWallet.mockRejectedValue(new Error('Daily limit exceeded'));
    walletStoreMock.error.set('Daily limit exceeded');
    component.amount.set(60000);
    fixture.detectChanges();

    const submitBtn = fixture.nativeElement.querySelector('#fund-submit-btn') as HTMLButtonElement;
    submitBtn.click();
    await fixture.whenStable();
    fixture.detectChanges();

    const errorAlert = fixture.nativeElement.querySelector('.error-alert');
    expect(errorAlert?.textContent).toContain('Daily limit exceeded');
  });
});
