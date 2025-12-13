import { TestBed, fakeAsync, tick } from "@angular/core/testing";
import { AppComponent } from "./app.component";
import { provideHttpClient } from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { TestService } from "./services/test.service";
import { FileService } from "./services/file.service";
import { MatSnackBarModule } from "@angular/material/snack-bar";
import { of, throwError } from "rxjs";

describe("AppComponent", () => {
    let testServiceSpy: jasmine.SpyObj<TestService>;
    let fileServiceSpy: jasmine.SpyObj<FileService>;

    beforeEach(async () => {
        testServiceSpy = jasmine.createSpyObj("TestService", ["startTest", "stopTest", "getStatus"]);
        fileServiceSpy = jasmine.createSpyObj("FileService", ["deleteFile"]);

        testServiceSpy.getStatus.and.returnValue(of({ status: "IDLE", data: [] }));

        await TestBed.configureTestingModule({
            imports: [AppComponent, MatSnackBarModule],
            providers: [
                provideHttpClient(),
                provideHttpClientTesting(),
                { provide: TestService, useValue: testServiceSpy },
                { provide: FileService, useValue: fileServiceSpy },
            ],
        }).compileComponents();
    });

    it("should create the app", () => {
        const { componentInstance } = TestBed.createComponent(AppComponent);
        expect(componentInstance).toBeTruthy();
    });

    it("should have the correct title value", () => {
        const { componentInstance } = TestBed.createComponent(AppComponent);
        expect(componentInstance.title).toBe("ThrashBuddy");
    });

    it("should render title in the DOM", () => {
        const fixture = TestBed.createComponent(AppComponent);
        fixture.detectChanges();
        const compiled = fixture.nativeElement as HTMLElement;
        expect(compiled.querySelector("h1")?.textContent).toContain("ThrashBuddy");
    });

    describe("computed signals", () => {
        it("should detect isIdle status", () => {
            const { componentInstance } = TestBed.createComponent(AppComponent);
            componentInstance.testStatus.set({ status: "IDLE" });
            expect(componentInstance.isIdle()).toBeTrue();
            expect(componentInstance.isRunning()).toBeFalse();
            expect(componentInstance.isStopping()).toBeFalse();
        });
    });

    describe("configuration logic", () => {
        it("should populate memory options on CPU change", () => {
            const { componentInstance } = TestBed.createComponent(AppComponent);
            componentInstance.selectedCpu = "1024m";
            componentInstance.onCpuChange();
            expect(componentInstance.memoryOptions.length).toBeGreaterThan(0);
            expect(componentInstance.selectedMemory).toBeDefined();
        });

        it("should fallback to empty memory options if CPU is unknown", () => {
            const { componentInstance } = TestBed.createComponent(AppComponent);
            componentInstance.selectedCpu = "non-existent-cpu";
            componentInstance.onCpuChange();
            expect(componentInstance.memoryOptions).toEqual([]);
        });

        it("should parse environment variables correctly", () => {
            const { componentInstance } = TestBed.createComponent(AppComponent);
            const input = "VAR1=value1\nVAR2=value2";
            const parsed = componentInstance["parseEnvVars"](input);
            expect(parsed).toEqual([
                { name: "VAR1", value: "value1" },
                { name: "VAR2", value: "value2" },
            ]);
        });
    });

    describe("status updates", () => {
        it("should call getStatus periodically", fakeAsync(() => {
            const fixture = TestBed.createComponent(AppComponent);
            const app = fixture.componentInstance;
            const getStatusSpy = spyOn(app, "getStatus").and.callThrough();

            fixture.detectChanges();
            tick(10000);

            expect(getStatusSpy).toHaveBeenCalledTimes(2);
        }));

        it("should retry on getStatus failure", fakeAsync(() => {
            spyOn(console, "error");
            const fixture = TestBed.createComponent(AppComponent);
            const app = fixture.componentInstance;

            let callCount = 0;
            testServiceSpy.getStatus.and.callFake(() => {
                callCount++;
                return callCount === 1 ? throwError(() => new Error("Fail")) : of({ status: "IDLE", data: [] });
            });

            app.getStatus();
            tick(1000);

            expect(testServiceSpy.getStatus).toHaveBeenCalledTimes(2);
        }));

        it("should fallback to empty files array when response.data is undefined", () => {
            const { componentInstance } = TestBed.createComponent(AppComponent);
            testServiceSpy.getStatus.and.returnValue(of({ status: "IDLE" }));

            componentInstance.getStatus();

            expect(componentInstance.files()).toEqual([]);
        });
    });

    describe("run and stop test", () => {
        it("should call startTest", () => {
            const { componentInstance } = TestBed.createComponent(AppComponent);
            testServiceSpy.startTest.and.returnValue(of({ success: true }));

            componentInstance.runTest();

            expect(testServiceSpy.startTest).toHaveBeenCalled();
            expect(testServiceSpy.getStatus).toHaveBeenCalled();
        });

        it("should handle error in runTest", () => {
            const { componentInstance } = TestBed.createComponent(AppComponent);
            testServiceSpy.startTest.and.returnValue(throwError(() => new Error("fail")));
            const errorSpy = spyOn(componentInstance as any, "showError");

            componentInstance.runTest();

            expect(errorSpy).toHaveBeenCalledWith("Start", jasmine.any(Error));
            expect(componentInstance.testStatus()).toEqual({ status: "ERROR" });
        });

        it("should call stopTest", () => {
            const { componentInstance } = TestBed.createComponent(AppComponent);
            testServiceSpy.stopTest.and.returnValue(of({ success: true }));

            componentInstance.stopTest();

            expect(testServiceSpy.stopTest).toHaveBeenCalled();
            expect(testServiceSpy.getStatus).toHaveBeenCalled();
        });

        it("should handle error in stopTest", () => {
            const { componentInstance } = TestBed.createComponent(AppComponent);
            testServiceSpy.stopTest.and.returnValue(throwError(() => new Error("fail")));
            const errorSpy = spyOn(componentInstance as any, "showError");

            componentInstance.stopTest();

            expect(errorSpy).toHaveBeenCalledWith("Stop", jasmine.any(Error));
            expect(componentInstance.testStatus()).toEqual({ status: "ERROR" });
        });
    });

    describe("file actions", () => {
        it("should call deleteFile and refresh status", () => {
            const { componentInstance } = TestBed.createComponent(AppComponent);
            fileServiceSpy.deleteFile.and.returnValue(of({ success: true }));

            componentInstance.deleteFile("test.txt");

            expect(fileServiceSpy.deleteFile).toHaveBeenCalledWith("test.txt");
            expect(testServiceSpy.getStatus).toHaveBeenCalled();
        });

        it("should handle error in deleteFile", () => {
            const { componentInstance } = TestBed.createComponent(AppComponent);
            spyOn(componentInstance as any, "showError");
            fileServiceSpy.deleteFile.and.returnValue(throwError(() => new Error("fail")));

            componentInstance.deleteFile("test.txt");

            expect(componentInstance["showError"]).toHaveBeenCalledWith("Delete: test.txt", jasmine.any(Error));
        });
    });

    describe("external links", () => {
        it("should open correct URL for openFiles", () => {
            const { componentInstance } = TestBed.createComponent(AppComponent);
            spyOn(window, "open");
            componentInstance.openFiles();
            expect(window.open).toHaveBeenCalledWith(jasmine.stringMatching(/minio/), "_blank");
        });

        it("should open correct URL for openMonitoring", () => {
            const { componentInstance } = TestBed.createComponent(AppComponent);
            spyOn(window, "open");
            componentInstance.openMonitoring();
            expect(window.open).toHaveBeenCalledWith(jasmine.stringMatching(/grafana/), "_blank");
        });
    });

    describe("lifecycle", () => {
        it("should cleanup subscriptions on destroy", () => {
            const { componentInstance } = TestBed.createComponent(AppComponent);
            const spy = spyOn(componentInstance["destroy$"], "next");
            componentInstance.ngOnDestroy();
            expect(spy).toHaveBeenCalled();
        });
    });

    describe("error fallback", () => {
        it("should fallback to 'Unknown error' in showError", () => {
            const { componentInstance } = TestBed.createComponent(AppComponent);
            spyOn(console, "error");
            const snackSpy = spyOn(componentInstance["snackBar"], "open");

            const weirdError = {};
            componentInstance["showError"]("Something broke", weirdError);

            expect(snackSpy).toHaveBeenCalledWith("Something broke: Unknown error", "Close", jasmine.any(Object));
        });
    });
});
