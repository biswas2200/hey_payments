import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { HomeComponent } from './home.component';
import { WalletStore } from '../wallet/wallet.store';
import { AuthStore } from '../../core/auth/auth.store';
import { signal } from '@angular/core';

describe('HomeComponent', () => {
  let component: HomeComponent;
  let fixture: ComponentFixture<HomeComponent>;
  let walletStoreMock: any;
  let authStoreMock: any;
  let router: Router;

  beforeEach(async () => {
    walletStoreMock = {
      wallet: signal({ mainBalance: 425000, offlineEscrow: 164000 }),
      mainBalance: signal(425000),
      offlineEscrow: signal(164000),
      mainBalanceFormatted: signal('₹4,250.00'),
      offlineEscrowFormatted: signal('₹1,640.00'),
      loading: signal(false),
      loadWallet: jasmine.createSpy('loadWallet').and.returnValue(Promise.resolve()),
    };

    authStoreMock = {
      currentUser: signal({ phone: '+919876543210', displayName: 'Priya', role: 'PAYER' }),
      logout: jasmine.createSpy('logout').and.returnValue(Promise.resolve()),
    };

    await TestBed.configureTestingModule({
      imports: [HomeComponent],
      providers: [
        provideRouter([]),
        { provide: WalletStore, useValue: walletStoreMock },
        { provide: AuthStore, useValue: authStoreMock },
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
    spyOn(router, 'navigate');

    fixture = TestBed.createComponent(HomeComponent);
    component = fixture.componentInstance;
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
    expect(router.navigate).toHaveBeenCalledWith(['/wallet/fund']);
  });

  it('should logout and redirect to login when clicking logout button', async () => {
    const el: HTMLElement = fixture.nativeElement;
    const logoutBtn = el.querySelector('#logout-btn') as HTMLButtonElement;
    expect(logoutBtn).toBeTruthy();
    logoutBtn.click();
    await fixture.whenStable();
    expect(authStoreMock.logout).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/auth/login']);
  });
});
