import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class TestService {
  private baseUrl = `${window.location.protocol}//${window.location.hostname}:${window.location.port}/api`;

  constructor(private http: HttpClient) {}

  startTest(payload: { cpu: string; memory: string; loadAgents: number; envVars: { name: string; value: string; }[] }): Observable<any> {
    return this.http.post(`${this.baseUrl}/start`, payload);
  }  

  stopTest(): Observable<any> {
    return this.http.post(`${this.baseUrl}/stop`, {});
  }

  getStatus(): Observable<any> {
    return this.http.get(`${this.baseUrl}/status`);
  }
}
