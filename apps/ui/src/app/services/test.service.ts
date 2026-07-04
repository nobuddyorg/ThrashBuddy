import { Injectable, inject } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";
import { ApiBaseUrlService } from "./api-base-url.service";

interface EnvVar {
    name: string;
    value: string;
}

export interface StartTestPayload {
    cpu: string;
    memory: string;
    loadAgents: number;
    envVars: EnvVar[];
}

interface ApiResponse {
    success: boolean;
    message?: string;
}

export interface StatusResponse {
    status: string;
    data?: { filename: string; lastModified: string }[];
}

@Injectable({ providedIn: "root" })
export class TestService {
    private baseUrl = inject(ApiBaseUrlService).baseUrl;

    constructor(private http: HttpClient) {}

    startTest(payload: StartTestPayload): Observable<ApiResponse> {
        return this.http.post<ApiResponse>(`${this.baseUrl}/start`, payload);
    }

    stopTest(): Observable<ApiResponse> {
        return this.http.post<ApiResponse>(`${this.baseUrl}/stop`, {});
    }

    getStatus(): Observable<StatusResponse> {
        return this.http.get<StatusResponse>(`${this.baseUrl}/status`);
    }
}
