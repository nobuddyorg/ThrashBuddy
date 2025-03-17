import {
  Component,
  computed,
  inject,
  signal,
  OnInit,
  OnDestroy,
} from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ConfigurableButtonComponent } from './components/configurable-button/configurable-button.component';
import { MatSliderModule } from '@angular/material/slider';
import { MatSelectModule } from '@angular/material/select';
import { CommonModule } from '@angular/common';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { FormsModule } from '@angular/forms';
import { TestService } from './test.service';
import { interval, Subject } from 'rxjs';
import { switchMap, takeUntil } from 'rxjs/operators';
import { MatSnackBar } from '@angular/material/snack-bar';

@Component({
  selector: 'app-root',
  standalone: true,
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
export class AppComponent implements OnInit, OnDestroy {
  title = 'CloudThrash';
  private testService = inject(TestService);
  private snackBar = inject(MatSnackBar);
  private destroy$ = new Subject<void>();

  testStatus = signal<{ status: string } | null>(null);

  isIdle = computed(() => this.testStatus()?.status === 'IDLE');
  isRunning = computed(() => this.testStatus()?.status === 'RUNNING');
  isStopping = computed(() => this.testStatus()?.status === 'STOPPING');

  cpuOptions: string[] = ['512', '1024', '2048', '4096', '8192', '16384'];
  memoryOptions: string[] = [];
  selectedCpu: string = '512';
  selectedMemory: string = '1024';
  loadAgents: number = 5;
  envVars: string = '';

  constructor() {
    this.onCpuChange();
  }

  ngOnInit() {
    this.getStatus();

    interval(10000)
      .pipe(
        switchMap(() => this.testService.getStatus()),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: (response) => {
          this.testStatus.set(response);
          console.log('Updated Status:', response);
          if (response.status === 'ERROR') {
            this.snackBar.open(`${response.message}: ${response.message || 'Unknown error'}`, 'Close', {
              duration: 10000,
              panelClass: ['error-snackbar'],
            });
          }
        },
        error: (err) => {
          console.error('Error getting test status:', err);
        },
      });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
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

  private showError(message: string, error: any) {
    console.error(message, error);
    this.snackBar.open(`${message}: ${error.message || 'Unknown error'}`, 'Close', {
      duration: 10000,
      panelClass: ['error-snackbar'],
    });
  }

  async runTest() {
    this.testService.startTest().subscribe({
      next: (response) => this.testStatus.set(response),
      error: (err) => this.showError('Error starting the test', err),
    });
  }

  async stopTest() {
    this.testService.stopTest().subscribe({
      next: (response) => this.testStatus.set(response),
      error: (err) => this.showError('Error stopping the test', err),
    });
  }

  async getStatus() {
    this.testService.getStatus().subscribe({
      next: (response) => this.testStatus.set(response),
      error: (err) => this.showError('Error getting test status', err),
    });
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
