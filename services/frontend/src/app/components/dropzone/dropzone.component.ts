import { Component, inject } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { NgxDropzoneModule } from "ngx-dropzone";
import { CommonModule } from "@angular/common";
import { MatIconModule } from "@angular/material/icon";
import { AppComponent } from "../../app.component";

@Component({
    selector: "app-dropzone",
    templateUrl: "./dropzone.component.html",
    styleUrls: ["./dropzone.component.css"],
    imports: [NgxDropzoneModule, CommonModule, MatIconModule],
})
export class DropzoneComponent {
    private baseUrl = `${window.location.protocol}//${window.location.hostname}:${window.location.port}/api`;
    private appComponent = inject(AppComponent);
    files: { file: File }[] = [];

    constructor(private http: HttpClient) {}

    onSelect(event: { addedFiles: File[] }) {
        for (const file of event.addedFiles) {
            this.files.push({ file });
        }
    }

    onRemove(fileObj: { file: File }) {
        this.files = this.files.filter((f) => f.file !== fileObj.file);
    }

    uploadAll() {
        this.files.forEach((fileObj) => {
            this.uploadFile(fileObj.file);
        });
    }

    uploadFile(file: File) {
        const formData = new FormData();
        formData.append("file", file);

        this.http.post<{ message: string; status: string }>(`${this.baseUrl}/upload`, formData).subscribe({
            next: (response) => {
                console.log(`Upload successful: ${file.name}, Response:`, response);
                this.onRemove({ file });
                this.appComponent.getStatus();
            },
            error: (error) => {
                console.error(`Upload failed: ${file.name}, Error:`, error);
            },
        });
    }
}
