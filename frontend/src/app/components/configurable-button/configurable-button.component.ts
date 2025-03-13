// configurable-button.component.ts
import { Component, Input } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { CommonModule } from '@angular/common';
import { MatTooltipModule } from '@angular/material/tooltip';

@Component({
  selector: 'app-configurable-button',
  templateUrl: './configurable-button.component.html',
  styleUrls: ['./configurable-button.component.css'],
  imports: [CommonModule, MatButtonModule, MatIconModule, MatTooltipModule],
})
export class ConfigurableButtonComponent {
  @Input() icon!: string;
  @Input() text!: string;
  @Input() tooltip!: string;
  @Input() type: string = 'mat-flat-button';
  @Input() action?: () => void;

  handleClick() {
    if (this.action) {
      this.action();
    }
  }
}
