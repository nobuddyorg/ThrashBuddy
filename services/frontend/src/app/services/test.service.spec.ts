import { TestBed } from "@angular/core/testing";
import { provideHttpClient, HttpClient } from "@angular/common/http";
import { provideHttpClientTesting, HttpTestingController } from "@angular/common/http/testing";
import { TestService, StartTestPayload, StatusResponse } from "./test.service";

describe("TestService (standalone setup)", () => {
    let service: TestService;
    let httpMock: HttpTestingController;

    const baseUrl = `${window.location.protocol}//${window.location.hostname}:${window.location.port}/api`;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [provideHttpClient(), provideHttpClientTesting(), TestService],
        });

        service = TestBed.inject(TestService);
        httpMock = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
        httpMock.verify();
    });

    it("should be created", () => {
        expect(service).toBeTruthy();
    });

    it("should send POST to /start", () => {
        const payload: StartTestPayload = {
            cpu: "2",
            memory: "4Gi",
            loadAgents: 5,
            envVars: [{ name: "ENV_VAR", value: "123" }],
        };

        const mockResponse = { success: true };

        service.startTest(payload).subscribe((res) => {
            expect(res).toEqual(mockResponse);
        });

        const req = httpMock.expectOne(`${baseUrl}/start`);
        expect(req.request.method).toBe("POST");
        expect(req.request.body).toEqual(payload);
        req.flush(mockResponse);
    });

    it("should send POST to /stop", () => {
        const mockResponse = { success: true };

        service.stopTest().subscribe((res) => {
            expect(res).toEqual(mockResponse);
        });

        const req = httpMock.expectOne(`${baseUrl}/stop`);
        expect(req.request.method).toBe("POST");
        expect(req.request.body).toEqual({});
        req.flush(mockResponse);
    });

    it("should send GET to /status", () => {
        const mockResponse: StatusResponse = {
            status: "IDLE",
            data: [{ filename: "log.txt", lastModified: "2025-03-29T10:00:00Z" }],
        };

        service.getStatus().subscribe((res) => {
            expect(res).toEqual(mockResponse);
        });

        const req = httpMock.expectOne(`${baseUrl}/status`);
        expect(req.request.method).toBe("GET");
        req.flush(mockResponse);
    });
});
