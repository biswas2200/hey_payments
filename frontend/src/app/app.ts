import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  inject,
  isDevMode,
  signal,
} from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { LucideCircleCheck, LucideRefreshCw, LucideWifiOff } from '@lucide/angular';
import { ApiService, BackendStatus } from './services/api.service';
import { ConnectivityService } from './core/connectivity/connectivity.service';

@Component({
  imports: [RouterOutlet, LucideCircleCheck, LucideRefreshCw, LucideWifiOff],
  selector: 'app-root',
  styleUrl: './app.scss',
  templateUrl: './app.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class App implements OnInit {
  protected readonly title = signal('Hey Payments');
  protected readonly isDev = signal(isDevMode());
  protected readonly backendStatus = signal<BackendStatus | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly loading = signal(false);

  protected readonly connectivity = inject(ConnectivityService);
  private readonly apiService = inject(ApiService);

  ngOnInit(): void {
    if (this.isDev()) {
      this.checkBackendHealth();
    }
  }

  checkBackendHealth(): void {
    this.loading.set(true);
    this.error.set(null);
    this.apiService.getHealth().subscribe({
      next: (data) => {
        this.backendStatus.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Could not connect to backend service.');
        this.loading.set(false);
      },
    });
  }
}
