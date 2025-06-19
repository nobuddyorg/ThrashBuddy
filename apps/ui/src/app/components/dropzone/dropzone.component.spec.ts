import { TestBed } from "@angular/core/testing";
import { DropzoneComponent } from "./dropzone.component";
import { provideHttpClient } from "@angular/common/http";
import { provideHttpClientTesting, HttpTestingController } from "@angular/common/http/testing";
import { AppComponent } from "../../app.component";

describe("DropzoneComponent", () => {
    let component: DropzoneComponent;
    let httpMock: HttpTestingController;

    const mockAppComponent = {
        getStatus: jasmine.createSpy("getStatus"),
    };

    const baseUrl = `${window.location.protocol}//${window.location.hostname}:${window.location.port}/api`;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DropzoneComponent],
            providers: [
                provideHttpClient(),
                provideHttpClientTesting(),
                { provide: AppComponent, useValue: mockAppComponent },
            ],
        }).compileComponents();

        const fixture = TestBed.createComponent(DropzoneComponent);
        component = fixture.componentInstance;
        httpMock = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
        httpMock.verify();
    });

    it("should create the component", () => {
        expect(component).toBeTruthy();
    });

    describe("file management", () => {
        it("should add files on select", () => {
            const file = new File(["hello"], "test.txt", { type: "text/plain" });
            component.onSelect({ addedFiles: [file] });
            expect(component.files.length).toBe(1);
            expect(component.files[0].file).toBe(file);
        });

        it("should remove file on onRemove", () => {
            const file = new File(["bye"], "delete.txt", { type: "text/plain" });
            component.files = [{ file }];
            component.onRemove({ file });
            expect(component.files.length).toBe(0);
        });
    });

    describe("upload logic", () => {
        it("should upload a file and remove it on success", () => {
            const file = new File(["upload"], "upload.txt", { type: "text/plain" });
            component.files = [{ file }];

            component.uploadFile(file);

            const req = httpMock.expectOne(`${baseUrl}/upload`);
            expect(req.request.method).toBe("POST");
            expect(req.request.body.get("file")).toBe(file);

            req.flush({ status: "success", message: "ok" });

            expect(component.files.length).toBe(0);
            expect(mockAppComponent.getStatus).toHaveBeenCalled();
        });

        it("should log error and keep file on upload failure", () => {
            const file = new File(["fail"], "fail.txt", { type: "text/plain" });
            component.files = [{ file }];
            spyOn(console, "error");

            component.uploadFile(file);

            const req = httpMock.expectOne(`${baseUrl}/upload`);
            req.error(new ProgressEvent("error"));

            expect(console.error).toHaveBeenCalled();
            expect(component.files.length).toBe(1);
        });

        it("should call uploadFile for all files in uploadAll", () => {
            const file1 = new File(["f1"], "f1.txt", { type: "text/plain" });
            const file2 = new File(["f2"], "f2.txt", { type: "text/plain" });
            component.files = [{ file: file1 }, { file: file2 }];

            spyOn(component, "uploadFile");

            component.uploadAll();

            expect(component.uploadFile).toHaveBeenCalledWith(file1);
            expect(component.uploadFile).toHaveBeenCalledWith(file2);
        });
    });
});
