import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { ApiService, BackendStatus } from './services/api.service';

@Component({
  imports: [CommonModule, RouterOutlet],
  selector: 'app-root',
  styleUrl: './app.scss',
  templateUrl: './app.html',
})
export class App implements OnInit {
  protected readonly title = signal('Hey Payments');
  protected readonly backendStatus = signal<BackendStatus | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly loading = signal(false);

  private readonly apiService = inject(ApiService);

  ngOnInit(): void {
    this.checkBackendHealth();
  }

  checkBackendHealth(): void {
    this.loading.set(true);
    this.error.set(null);
    this.apiService.getHealth().subscribe({
      next: (data) => {
        this.backendStatus.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set('Could not connect to Spring Boot backend (http://localhost:8080).');
        this.loading.set(false);
      }
    });
  }
}
