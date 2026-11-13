import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  template: `
    <div class="auth-card">
      <div class="header">
        <h1 class="title">Hey Payments Dashboard</h1>
        <p class="subtitle">Offline Gateway Active</p>
      </div>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardComponent {}
