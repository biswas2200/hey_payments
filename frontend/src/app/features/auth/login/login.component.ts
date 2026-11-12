import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthStore } from '../../../core/auth/auth.store';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoginComponent {
  readonly auth = inject(AuthStore);
  private readonly router = inject(Router);

  readonly phone = signal('');
  readonly password = signal('');
  readonly formError = signal<string | null>(null);

  async onSubmit(): Promise<void> {
    const rawPhone = this.phone().trim();
    const pwd = this.password().trim();

    if (!rawPhone || !pwd) {
      this.formError.set('Please enter a valid phone number and password');
      return;
    }

    this.formError.set(null);
    const formattedPhone = rawPhone.startsWith('+91') ? rawPhone : `+91${rawPhone}`;

    try {
      await this.auth.login({ phone: formattedPhone, password: pwd });

      if (!this.auth.activeDevice()) {
        await this.router.navigate(['/auth/device-register']);
      } else if (this.auth.isMerchant()) {
        await this.router.navigate(['/merchant']);
      } else {
        await this.router.navigate(['/pay']);
      }
    } catch {
      // Error handled by AuthStore signal
    }
  }
}
