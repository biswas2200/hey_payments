import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { LucideCircleCheck, LucideRefreshCw, LucideWifiOff } from '@lucide/angular';
import { ConnectivityService } from './core/connectivity/connectivity.service';

@Component({
  imports: [RouterOutlet, LucideCircleCheck, LucideRefreshCw, LucideWifiOff],
  selector: 'app-root',
  styleUrl: './app.scss',
  templateUrl: './app.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class App {
  protected readonly title = signal('Hey Payments');
  protected readonly connectivity = inject(ConnectivityService);
}
