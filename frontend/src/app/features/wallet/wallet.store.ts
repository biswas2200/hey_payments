import { computed, inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { WalletService } from '../../core/api/generated/services/wallet.service';
import type { WalletResponse } from '../../core/api/models';

export function formatPaise(paise: number): string {
  const rupees = Math.floor(paise / 100);
  const remainder = paise % 100;
  return `₹${rupees.toLocaleString('en-IN')}.${remainder.toString().padStart(2, '0')}`;
}

@Injectable({
  providedIn: 'root',
})
export class WalletStore {
  private readonly walletApi = inject(WalletService);

  readonly wallet = signal<WalletResponse | null>(null);
  readonly loading = signal<boolean>(false);
  readonly error = signal<string | null>(null);

  readonly mainBalance = computed(() => this.wallet()?.mainBalance ?? 0);
  readonly offlineEscrow = computed(() => this.wallet()?.offlineEscrow ?? 0);
  readonly mainBalanceFormatted = computed(() => formatPaise(this.mainBalance()));
  readonly offlineEscrowFormatted = computed(() => formatPaise(this.offlineEscrow()));

  async loadWallet(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const resp = await firstValueFrom(this.walletApi.walletsMeGet());
      this.wallet.set(resp);
    } catch (err: unknown) {
      const apiErr = err as { error?: { detail?: string } };
      const fallbackErr = err as Error;
      const message = apiErr?.error?.detail ?? fallbackErr?.message ?? 'Failed to load wallet';
      this.error.set(message);
    } finally {
      this.loading.set(false);
    }
  }

  async fundWallet(amountPaise: number, source: 'SIM_BANK' = 'SIM_BANK'): Promise<WalletResponse> {
    this.loading.set(true);
    this.error.set(null);

    const prevWallet = this.wallet();
    // Optimistic update
    if (prevWallet) {
      this.wallet.set({
        ...prevWallet,
        mainBalance: (prevWallet.mainBalance ?? 0) + amountPaise,
      });
    }

    try {
      const idempotencyKey = crypto.randomUUID();
      const resp = await firstValueFrom(
        this.walletApi.walletsMeFundPost({
          'Idempotency-Key': idempotencyKey,
          body: { amount: amountPaise, source },
        }),
      );
      this.wallet.set(resp);
      return resp;
    } catch (err: unknown) {
      // Rollback optimistic update
      this.wallet.set(prevWallet);
      const apiErr = err as { error?: { detail?: string; title?: string } };
      const fallbackErr = err as Error;
      const message =
        apiErr?.error?.detail ?? apiErr?.error?.title ?? fallbackErr?.message ?? 'Funding failed';
      this.error.set(message);
      throw err;
    } finally {
      this.loading.set(false);
    }
  }
}
