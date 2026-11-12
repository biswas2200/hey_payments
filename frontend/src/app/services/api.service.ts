import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface BackendStatus {
  status: string;
  service: string;
  timestamp: string;
}

@Injectable({
  providedIn: 'root',
})
export class ApiService {
  private readonly http = inject(HttpClient);

  getHealth(): Observable<BackendStatus> {
    return this.http.get<BackendStatus>('/api/health');
  }
}
