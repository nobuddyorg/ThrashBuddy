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

  cpuOptions: string[] = ['512m', '1024m', '2048m', '4096m', '8192m', '16384m'];
  memoryOptions: string[] = [];
  selectedCpu: string = '512m';
  selectedMemory: string = '1024Mi';
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
            this.snackBar.open(
              `${response.message}: ${response.message || 'Unknown error'}`,
              'Close',
              {
                duration: 10000,
                panelClass: ['error-snackbar'],
              }
            );
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
      case '512m':
        this.memoryOptions = ['1024Mi', '2048Mi', '3072Mi', '4096Mi'];
        break;
      case '1024m':
        this.memoryOptions = [
          '2048Mi',
          '3072Mi',
          '4096Mi',
          '5120Mi',
          '6144Mi',
          '7168Mi',
          '8192Mi',
        ];
        break;
      case '2048m':
        for (let i = 4; i <= 16; i++) {
          this.memoryOptions.push((i * 1024).toString() + 'Mi');
        }
        break;
      case '4096m':
        for (let i = 8; i <= 30; i++) {
          this.memoryOptions.push((i * 1024).toString() + 'Mi');
        }
        break;
      case '8192m':
        for (let i = 16; i <= 60; i += 4) {
          this.memoryOptions.push((i * 1024).toString() + 'Mi');
        }
        break;
      case '16384m':
        for (let i = 32; i <= 120; i += 8) {
          this.memoryOptions.push((i * 1024).toString() + 'Mi');
        }
        break;
    }

    this.selectedMemory = this.memoryOptions[0];
  }

  private showError(message: string, error: any) {
    console.error(message, error);
    this.snackBar.open(
      `${message}: ${error.message || 'Unknown error'}`,
      'Close',
      {
        duration: 10000,
        panelClass: ['error-snackbar'],
      }
    );
  }

  async runTest() {
    this.testStatus.set({ status: 'RUNNING' });
    const payload = {
      cpu: this.selectedCpu,
      memory: this.selectedMemory,
      loadAgents: this.loadAgents,
      envVars: this.envVars
        ? this.envVars
            .split('\n')
            .map((env) => env.trim())
            .filter((env) => env.includes('='))
            .map((env) => {
              const [key, value] = env.split('=').map((part) => part.trim());
              return { name: key, value: value };
            })
        : [],
    };

    this.testService.startTest(payload).subscribe({
      next: (response) => this.testStatus.set(response),
      error: (err) => {
        this.showError('Error starting the test', err);
        this.testStatus.set({ status: 'ERROR' }); // Revert if request fails
      },
    });
  }

  async stopTest() {
    this.testStatus.set({ status: 'STOPPING' });

    this.testService.stopTest().subscribe({
      next: (response) => this.testStatus.set(response),
      error: (err) => {
        this.showError('Error stopping the test', err);
        this.testStatus.set({ status: 'ERROR' }); // Revert if request fails
      },
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
