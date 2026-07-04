import { Injectable } from "@angular/core";

@Injectable({ providedIn: "root" })
export class ApiBaseUrlService {
    readonly baseUrl = `${window.location.protocol}//${window.location.hostname}${window.location.port === "4200" ? ":8080" : `:${window.location.port}`}/api`;
}
