import { Component, EventEmitter, Output, inject } from "@angular/core";
import { NgxDropzoneModule } from "ngx-dropzone";

import { MatIconModule } from "@angular/material/icon";
import { FileService } from "../../services/file.service";

@Component({
    selector: "app-dropzone",
    templateUrl: "./dropzone.component.html",
    styleUrls: ["./dropzone.component.css"],
    imports: [NgxDropzoneModule, MatIconModule],
})
export class DropzoneComponent {
    private fileService = inject(FileService);
    @Output() uploaded = new EventEmitter<void>();
    files: { file: File }[] = [];

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
        this.fileService.uploadFile(file).subscribe({
            next: () => {
                this.onRemove({ file });
                this.uploaded.emit();
            },
            error: (error) => {
                console.error(`Upload failed: ${file.name}, Error:`, error);
            },
        });
    }
}
