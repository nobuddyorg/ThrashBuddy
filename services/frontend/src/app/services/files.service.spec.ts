import { TestBed } from "@angular/core/testing";
import { FileService } from "./file.service";
import { provideHttpClient, HttpClient } from "@angular/common/http";
import { provideHttpClientTesting, HttpTestingController } from "@angular/common/http/testing";

describe("FileService", () => {
    let service: FileService;
    let httpMock: HttpTestingController;

    const baseUrl = `${window.location.protocol}//${window.location.hostname}:${window.location.port}/api`;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [provideHttpClient(), provideHttpClientTesting(), FileService],
        });

        service = TestBed.inject(FileService);
        httpMock = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
        httpMock.verify();
    });

    it("should be created", () => {
        expect(service).toBeTruthy();
    });

    it("should send DELETE request to delete file", () => {
        const fileName = "report.txt";
        const encoded = encodeURIComponent(fileName);
        const mockResponse = { success: true };

        service.deleteFile(fileName).subscribe((res) => {
            expect(res).toEqual(mockResponse);
        });

        const req = httpMock.expectOne(`${baseUrl}/delete?fileName=${encoded}`);
        expect(req.request.method).toBe("DELETE");
        req.flush(mockResponse);
    });
});
