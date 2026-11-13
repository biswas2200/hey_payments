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
import { LucideArrowLeft, LucideCheck } from '@lucide/angular';
import { WalletStore } from '../wallet.store';
import type { WalletResponse } from '../../../core/api/models';

@Component({
  selector: 'app-fund',
  standalone: true,
  imports: [CommonModule, LucideArrowLeft, LucideCheck],
  templateUrl: './fund.component.html',
  styleUrls: ['./fund.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FundComponent implements OnInit {
  readonly walletStore = inject(WalletStore);
  private readonly router = inject(Router);

  readonly amount = signal<number>(500);
  readonly source = signal<'SIM_BANK'>('SIM_BANK');
  readonly isSubmitting = signal<boolean>(false);
  readonly successResponse = signal<WalletResponse | null>(null);

  pendingIdempotencyKey: string | null = null;

  readonly isValidAmount = computed(() => {
    const val = this.amount();
    return val > 0 && val <= 50000;
  });

  ngOnInit(): void {
    void this.walletStore.loadWallet();
  }

  selectAmount(amt: number): void {
    if (this.amount() !== amt) {
      this.pendingIdempotencyKey = null;
    }
    this.amount.set(amt);
  }

  onAmountInput(event: Event): void {
    const target = event.target as HTMLInputElement;
    const val = parseInt(target.value, 10);
    this.pendingIdempotencyKey = null;
    this.amount.set(isNaN(val) ? 0 : val);
  }

  onSourceChange(event: Event): void {
    const target = event.target as HTMLSelectElement;
    this.source.set(target.value as 'SIM_BANK');
  }

  async onSubmit(): Promise<void> {
    if (!this.isValidAmount() || this.isSubmitting()) {
      return;
    }

    if (!this.pendingIdempotencyKey) {
      this.pendingIdempotencyKey = crypto.randomUUID();
    }

    this.isSubmitting.set(true);
    try {
      const amountPaise = this.amount() * 100;
      const resp = await this.walletStore.fundWallet(
        amountPaise,
        this.source(),
        this.pendingIdempotencyKey,
      );
      this.successResponse.set(resp);
      this.pendingIdempotencyKey = null;
    } catch {
      // Keep pendingIdempotencyKey so retrying reuses the same key (MEDIUM-4)
    } finally {
      this.isSubmitting.set(false);
    }
  }

  onBack(): void {
    void this.router.navigate(['/pay']);
  }
}
