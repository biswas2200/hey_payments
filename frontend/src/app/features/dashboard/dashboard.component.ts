import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { LucideLogOut } from '@lucide/angular';
import { AuthStore } from '../../core/auth/auth.store';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [LucideLogOut],
  template: `
    <div class="auth-card">
      <div class="header">
        <h1 class="title">Hey Payments Dashboard</h1>
        <p class="subtitle">Offline Gateway Active</p>
      </div>
      <div class="actions">
        <button type="button" class="btn btn-outline" id="logout-btn" (click)="onLogout()">
          <svg lucideLogOut class="btn-icon" [size]="18" aria-hidden="true"></svg>
          <span>Log Out</span>
        </button>
      </div>
    </div>
  `,
  styles: `
    .auth-card {
      background: var(--hp-surface);
      border-radius: var(--hp-r-card);
      padding: var(--hp-space-6);
      border: 1px solid var(--hp-line);
      box-shadow: var(--hp-shadow-card);
      margin: var(--hp-space-4) auto;
      max-width: 400px;
    }
    .header {
      margin-bottom: var(--hp-space-6);
      text-align: center;
    }
    .title {
      font-size: var(--hp-fs-h2);
      font-weight: 800;
      color: var(--hp-ink);
      margin: 0;
    }
    .subtitle {
      font-size: var(--hp-fs-small);
      color: var(--hp-muted);
      margin-top: var(--hp-space-1);
    }
    .actions {
      display: flex;
      justify-content: center;
      margin-top: var(--hp-space-4);
    }
    .btn {
      height: var(--hp-touch);
      border-radius: var(--hp-r-sm);
      font-weight: 800;
      font-size: var(--hp-fs-body);
      display: inline-flex;
      justify-content: center;
      align-items: center;
      gap: var(--hp-space-2);
      cursor: pointer;
      padding: 0 var(--hp-space-4);
      transition: opacity var(--hp-dur-fast) var(--hp-ease-standard);
    }
    .btn-outline {
      background: transparent;
      color: var(--hp-ink);
      border: 1.5px solid var(--hp-line);
    }
    .btn-outline:hover {
      background: var(--hp-surface-subtle);
    }
    .btn-icon {
      stroke: currentColor;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardComponent {
  private readonly auth = inject(AuthStore);
  private readonly router = inject(Router);

  async onLogout(): Promise<void> {
    await this.auth.logout();
    await this.router.navigate(['/auth/login']);
  }
}
