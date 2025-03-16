import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class TestService {
  private baseUrl = 'http://localhost:8080/test';

  constructor(private http: HttpClient) {}

  startTest(): Observable<any> {
    return this.http.post(`${this.baseUrl}/start`, {});
  }

  stopTest(): Observable<any> {
    return this.http.post(`${this.baseUrl}/stop`, {});
  }

  getStatus(): Observable<any> {
    return this.http.get(`${this.baseUrl}/status`);
  }
}
