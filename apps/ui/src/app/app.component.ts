import { Component, computed, inject, signal, OnInit, OnDestroy } from "@angular/core";
import { RouterOutlet } from "@angular/router";
import { MatSliderModule } from "@angular/material/slider";
import { MatSelectModule } from "@angular/material/select";
import { CommonModule } from "@angular/common";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatInputModule } from "@angular/material/input";
import { FormsModule } from "@angular/forms";
import { TestService, StartTestPayload } from "./services/test.service";
import { FileService } from "./services/file.service";
import { Subject, interval } from "rxjs";
import { takeUntil } from "rxjs/operators";
import { MatSnackBar } from "@angular/material/snack-bar";
import { DropzoneComponent } from "./components/dropzone/dropzone.component";
import { MatIconModule } from "@angular/material/icon";
import { MatTooltipModule } from "@angular/material/tooltip";

interface FileMeta {
    filename: string;
    lastModified: string;
}

interface StatusResponse {
    status: string;
    data?: FileMeta[];
}

@Component({
    selector: "app-root",
    standalone: true,
    imports: [
        RouterOutlet,
        MatSliderModule,
        MatSelectModule,
        CommonModule,
        MatFormFieldModule,
        MatInputModule,
        FormsModule,
        DropzoneComponent,
        MatIconModule,
        MatTooltipModule,
    ],
    templateUrl: "./app.component.html",
    styleUrl: "./app.component.css",
})
export class AppComponent implements OnInit, OnDestroy {
    title = "ThrashBuddy";
    private testService = inject(TestService);
    private fileService = inject(FileService);
    private snackBar = inject(MatSnackBar);
    private destroy$ = new Subject<void>();

    testStatus = signal<StatusResponse | null>(null);
    files = signal<FileMeta[]>([]);

    isIdle = computed(() => {
        const status = this.testStatus();
        return status !== null && status.status === "IDLE";
    });

    isRunning = computed(() => {
        const status = this.testStatus();
        return status !== null && status.status === "RUNNING";
    });

    isStopping = computed(() => {
        const status = this.testStatus();
        return status !== null && status.status === "STOPPING";
    });

    cpuOptions: string[] = ["512m", "1024m", "2048m", "4096m", "8192m", "16384m"];
    memoryOptions: string[] = [];
    selectedCpu = "512m";
    selectedMemory = "1024Mi";
    loadAgents = 5;
    envVars = "";
    showSettings = false;

    constructor() {
        this.onCpuChange();
    }

    ngOnInit() {
        this.getStatus();
        interval(10000)
            .pipe(takeUntil(this.destroy$))
            .subscribe(() => {
                this.getStatus();
            });
    }

    ngOnDestroy() {
        this.destroy$.next();
        this.destroy$.complete();
    }

    onCpuChange(): void {
        const memoryMap: Record<string, string[]> = {
            "512m": ["1024Mi", "2048Mi", "3072Mi", "4096Mi"],
            "1024m": ["2048Mi", "3072Mi", "4096Mi", "5120Mi", "6144Mi", "7168Mi", "8192Mi"],
            "2048m": this.generateMemoryOptions(4, 16),
            "4096m": this.generateMemoryOptions(8, 30),
            "8192m": this.generateMemoryOptions(16, 60, 4),
            "16384m": this.generateMemoryOptions(32, 120, 8),
        };

        this.memoryOptions = memoryMap[this.selectedCpu] ?? [];
        this.selectedMemory = this.memoryOptions[0];
    }

    generateMemoryOptions(from: number, to: number, step = 1): string[] {
        return Array.from(
            { length: Math.floor((to - from) / step) + 1 },
            (_, idx) => `${String((from + idx * step) * 1024)}Mi`,
        );
    }

    private showError(message: string, error: unknown) {
        console.error(message, error);
        const err = error as { error?: { message?: string }; message?: string };
        const errorMsg = err.error?.message ?? err.message ?? "Unknown error";

        this.snackBar.open(`${message}: ${String(errorMsg)}`, "Close", {
            duration: 30000,
            panelClass: ["error-snackbar"],
        });
    }

    runTest() {
        this.testStatus.set({ status: "RUNNING" });

        const payload: StartTestPayload = {
            cpu: this.selectedCpu,
            memory: this.selectedMemory,
            loadAgents: this.loadAgents,
            envVars: this.parseEnvVars(this.envVars),
        };

        this.testService.startTest(payload).subscribe({
            next: () => {
                this.getStatus();
            },
            error: (err) => {
                this.showError("Start", err);
                this.testStatus.set({ status: "ERROR" });
            },
        });
    }

    stopTest() {
        this.testStatus.set({ status: "STOPPING" });

        this.testService.stopTest().subscribe({
            next: () => {
                this.getStatus();
            },
            error: (err) => {
                this.showError("Stop", err);
                this.testStatus.set({ status: "ERROR" });
            },
        });
    }

    getStatus() {
        this.testService.getStatus().subscribe({
            next: (response) => {
                this.testStatus.set(response);
                this.files.set(response.data ?? []);
            },
            error: (err) => {
                this.showError("Status", err);
                setTimeout(() => {
                    this.getStatus();
                }, 1000);
            },
        });
    }

    openFiles() {
        this.openExternal("minio");
    }

    openMonitoring() {
        this.openExternal("grafana");
    }

    openData() {
        this.openExternal("influx");
    }

    private openExternal(sub: string) {
        const url = `${window.location.protocol}//${sub}.${window.location.hostname}:${window.location.port}/`;
        window.open(url, "_blank");
    }

    deleteFile(fileName: string) {
        this.fileService.deleteFile(fileName).subscribe({
            next: () => {
                this.getStatus();
            },
            error: (err) => {
                this.showError(`Delete: ${fileName}`, err);
            },
        });
    }

    private parseEnvVars(input: string): { name: string; value: string }[] {
        return input
            .split("\n")
            .map((env) => env.trim())
            .filter((env) => env.includes("="))
            .map((env) => {
                const [name, value] = env.split("=").map((part) => part.trim());
                return { name, value };
            });
    }

    toggleSettings() {
        this.showSettings = !this.showSettings;
    }
}
