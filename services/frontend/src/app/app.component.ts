import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ConfigurableButtonComponent } from './components/configurable-button/configurable-button.component';
import { MatSliderModule } from '@angular/material/slider';
import { MatSelectModule } from '@angular/material/select';
import { CommonModule } from '@angular/common';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-root',
  imports: [
    RouterOutlet,
    ConfigurableButtonComponent,
    MatSliderModule,
    MatSelectModule,
    CommonModule,
    MatFormFieldModule,
    MatInputModule,
    FormsModule,
  ],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css',
})
export class AppComponent {
  title = 'CloudThrash';

  cpuOptions: string[] = ['512', '1024', '2048', '4096', '8192', '16384'];
  memoryOptions: string[] = [];
  selectedCpu: string = '512';
  selectedMemory: string = '1024';
  loadAgents: number = 5;
  envVars: string = '';

  constructor() {
    this.onCpuChange();
  }

  onCpuChange(): void {
    this.memoryOptions = [];
    switch (this.selectedCpu) {
      case '512':
        this.memoryOptions = ['1024', '2048', '3072', '4096'];
        break;
      case '1024':
        this.memoryOptions = [
          '2048',
          '3072',
          '4096',
          '5120',
          '6144',
          '7168',
          '8192',
        ];
        break;
      case '2048':
        for (let i = 4; i <= 16; i++) {
          this.memoryOptions.push((i * 1024).toString());
        }
        break;
      case '4096':
        for (let i = 8; i <= 30; i++) {
          this.memoryOptions.push((i * 1024).toString());
        }
        break;
      case '8192':
        for (let i = 16; i <= 60; i += 4) {
          this.memoryOptions.push((i * 1024).toString());
        }
        break;
      case '16384':
        for (let i = 32; i <= 120; i += 8) {
          this.memoryOptions.push((i * 1024).toString());
        }
        break;
    }

    this.selectedMemory = this.memoryOptions[0];
  }

  runTest() {
    console.log('Running test from parent!' + this.envVars);
  }

  stopTest() {
    console.log('Stopping test from parent!' + this.selectedCpu);
  }

  openFiles() {
    const newPort = 32001;
    const newURL = `${window.location.protocol}//${window.location.hostname}:${newPort}/`;
    window.open(newURL, '_blank');
  }

  openMonitoring() {
    const newPort = 32002;
    const newURL = `${window.location.protocol}//${window.location.hostname}:${newPort}/`;
    window.open(newURL, '_blank');
  }
}
