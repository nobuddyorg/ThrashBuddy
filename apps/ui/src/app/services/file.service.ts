import { Injectable, inject } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";
import { ApiBaseUrlService } from "./api-base-url.service";

interface ApiResponse {
    success: boolean;
    message?: string;
}

@Injectable({ providedIn: "root" })
export class FileService {
    private baseUrl = inject(ApiBaseUrlService).baseUrl;
    private http = inject(HttpClient);

    uploadFile(file: File): Observable<ApiResponse> {
        const formData = new FormData();
        formData.append("file", file);
        return this.http.post<ApiResponse>(`${this.baseUrl}/upload`, formData);
    }

    deleteFile(fileName: string): Observable<ApiResponse> {
        return this.http.delete<ApiResponse>(`${this.baseUrl}/delete?fileName=${encodeURIComponent(fileName)}`);
    }
}
