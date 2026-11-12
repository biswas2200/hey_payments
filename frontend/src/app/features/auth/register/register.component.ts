import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthStore } from '../../../core/auth/auth.store';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RegisterComponent {
  readonly auth = inject(AuthStore);
  private readonly router = inject(Router);

  readonly phone = signal('');
  readonly email = signal('');
  readonly displayName = signal('');
  readonly password = signal('');
  readonly role = signal<'PAYER' | 'MERCHANT'>('PAYER');
  readonly formError = signal<string | null>(null);

  async onSubmit(): Promise<void> {
    const rawPhone = this.phone().trim();
    const pwd = this.password().trim();
    const currentRole = this.role();

    if (!rawPhone || rawPhone.length < 10) {
      this.formError.set('Please enter a valid 10-digit mobile number');
      return;
    }

    if (!pwd || pwd.length < 10) {
      this.formError.set('Password must be at least 10 characters long');
      return;
    }

    if (currentRole === 'MERCHANT' && !this.displayName().trim()) {
      this.formError.set('Merchant display name is required');
      return;
    }

    this.formError.set(null);
    const formattedPhone = rawPhone.startsWith('+91') ? rawPhone : `+91${rawPhone}`;

    try {
      await this.auth.register({
        phone: formattedPhone,
        email: this.email().trim() || undefined,
        password: pwd,
        role: currentRole,
        displayName: currentRole === 'MERCHANT' ? this.displayName().trim() : undefined,
      });

      await this.router.navigate(['/auth/verify-otp'], {
        queryParams: { phone: formattedPhone },
      });
    } catch {
      // Handled by AuthStore error signal
    }
  }
}
