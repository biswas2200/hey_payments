import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthStore } from '../../../core/auth/auth.store';

@Component({
  selector: 'app-set-pin',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './set-pin.component.html',
  styleUrl: './set-pin.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SetPinComponent {
  readonly auth = inject(AuthStore);
  private readonly router = inject(Router);

  readonly pin = signal('');
  readonly confirmPin = signal('');
  readonly password = signal('');
  readonly formError = signal<string | null>(null);

  async onSubmit(): Promise<void> {
    const p1 = this.pin().trim();
    const p2 = this.confirmPin().trim();
    const pwd = this.password().trim();

    if (!p1 || p1.length !== 6 || !/^\d{6}$/.test(p1)) {
      this.formError.set('PIN must be exactly 6 digits');
      return;
    }

    if (p1 !== p2) {
      this.formError.set('PINs do not match');
      return;
    }

    if (!pwd) {
      this.formError.set('Current password is required to verify PIN setup');
      return;
    }

    this.formError.set(null);
    try {
      await this.auth.setPin(p1, pwd);

      if (this.auth.isMerchant()) {
        await this.router.navigate(['/merchant']);
      } else {
        await this.router.navigate(['/pay']);
      }
    } catch {
      // Handled by AuthStore error signal
    }
  }
}
