import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class FileService {
  private baseUrl = `${window.location.protocol}//${window.location.hostname}:${window.location.port}/api`;
  private http = inject(HttpClient);

  deleteFile(fileName: string): Observable<any> {
    return this.http.delete(`${this.baseUrl}/delete?fileName=${fileName}`);
  }
}
