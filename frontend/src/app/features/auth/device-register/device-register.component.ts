import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthStore } from '../../../core/auth/auth.store';

@Component({
  selector: 'app-device-register',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './device-register.component.html',
  styleUrl: './device-register.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DeviceRegisterComponent {
  readonly auth = inject(AuthStore);
  private readonly router = inject(Router);

  readonly label = signal('Mobile Phone');
  readonly formError = signal<string | null>(null);

  async onRegister(): Promise<void> {
    const lbl = this.label().trim();
    if (!lbl) {
      this.formError.set('Please provide a name or label for this device');
      return;
    }

    this.formError.set(null);
    try {
      await this.auth.registerCurrentDevice(lbl);
      await this.router.navigate(['/auth/set-pin']);
    } catch {
      // Handled by AuthStore error signal
    }
  }
}
