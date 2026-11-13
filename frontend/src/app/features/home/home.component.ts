import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { LucideLogOut, LucideWifiOff } from '@lucide/angular';
import { WalletStore } from '../wallet/wallet.store';
import { AuthStore } from '../../core/auth/auth.store';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, LucideLogOut, LucideWifiOff],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HomeComponent implements OnInit {
  readonly walletStore = inject(WalletStore);
  private readonly authStore = inject(AuthStore);
  private readonly router = inject(Router);

  readonly isOnline = signal<boolean>(navigator.onLine ?? true);

  readonly userName = computed(() => {
    const user = this.authStore.currentUser();
    return user?.displayName || user?.phone || 'Payer';
  });

  readonly userInitial = computed(() => {
    const name = this.userName();
    return name.charAt(0).toUpperCase();
  });

  readonly greeting = computed(() => {
    const hour = new Date().getHours();
    if (hour < 12) return 'Good morning';
    if (hour < 17) return 'Good afternoon';
    return 'Good evening';
  });

  readonly escrowPercentage = computed(() => {
    const escrow = this.walletStore.offlineEscrow();
    const capacity = 200000; // ₹2,000 in paise
    return Math.min(100, Math.round((escrow / capacity) * 100));
  });

  ngOnInit(): void {
    void this.walletStore.loadWallet();
  }

  onAddMoney(): void {
    void this.router.navigate(['/wallet/fund']);
  }

  async onLogout(): Promise<void> {
    await this.authStore.logout();
    await this.router.navigate(['/auth/login']);
  }
}
