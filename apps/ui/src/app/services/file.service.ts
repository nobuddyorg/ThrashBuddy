import { Injectable, inject } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";

interface ApiResponse {
    success: boolean;
    message?: string;
}

@Injectable({ providedIn: "root" })
export class FileService {
    private baseUrl = `${window.location.protocol}//${window.location.hostname}${window.location.port === "4200" ? ":8080" : `:${window.location.port}`}/api`;
    private http = inject(HttpClient);

    deleteFile(fileName: string): Observable<ApiResponse> {
        return this.http.delete<ApiResponse>(`${this.baseUrl}/delete?fileName=${encodeURIComponent(fileName)}`);
    }
}
