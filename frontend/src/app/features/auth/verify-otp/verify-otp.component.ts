import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthStore } from '../../../core/auth/auth.store';

@Component({
  selector: 'app-verify-otp',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './verify-otp.component.html',
  styleUrl: './verify-otp.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class VerifyOtpComponent {
  readonly auth = inject(AuthStore);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly phone = signal('');
  readonly otp = signal('');
  readonly formError = signal<string | null>(null);

  constructor() {
    const phoneParam = this.route.snapshot.queryParamMap.get('phone');
    if (phoneParam) {
      this.phone.set(phoneParam);
    }
  }

  async onSubmit(): Promise<void> {
    const code = this.otp().trim();
    if (!code || code.length !== 6 || !/^\d{6}$/.test(code)) {
      this.formError.set('Please enter a valid 6-digit OTP code');
      return;
    }

    this.formError.set(null);
    try {
      await this.auth.verifyOtp({ phone: this.phone(), otp: code });
      await this.router.navigate(['/auth/login'], {
        queryParams: { verified: 'true' },
      });
    } catch {
      // Handled by AuthStore error signal
    }
  }
}
