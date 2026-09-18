import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Output,
  ViewChild,
  ElementRef,
} from '@angular/core';
import { forwardRef } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-date',
  standalone: true,
  imports: [MatIconModule],
  template: `
    <span class="date-wrap">
      <input
        class="date-mask"
        inputmode="numeric"
        maxlength="10"
        placeholder="dd/mm/aaaa"
        autocomplete="off"
        [value]="display"
        (input)="onMask($event)"
        (blur)="onBlur()"
      />
      <button
        type="button"
        class="date-btn"
        title="Elegir fecha"
        (click)="openNative()"
      >
        <mat-icon>calendar_today</mat-icon>
      </button>
      <input
        #native
        class="date-native"
        type="date"
        (change)="onNative($event)"
      />
    </span>
  `,
  styles: [
    `
      :host {
        display: inline-block;
        vertical-align: middle;
      }
      .date-wrap {
        position: relative;
        display: inline-block;
      }
      .date-mask {
        box-sizing: border-box;
        width: 100%;
        min-width: 150px;
        border: 1px solid var(--border);
        background: var(--card);
        color: var(--fg);
        border-radius: 6px;
        padding: 8px 34px 8px 12px;
        font: inherit;
        font-size: 14px;
        transition: border-color 0.15s ease, box-shadow 0.15s ease;
      }
      .date-mask:focus {
        outline: none;
        border-color: var(--primary);
        box-shadow: 0 0 0 2px color-mix(in srgb, var(--primary) 22%, transparent);
      }
      .date-mask::placeholder {
        color: var(--muted);
        opacity: 1;
      }
      .date-btn {
        position: absolute;
        top: 50%;
        right: 4px;
        transform: translateY(-50%);
        border: none;
        background: transparent;
        color: var(--muted);
        cursor: pointer;
        display: flex;
        align-items: center;
        justify-content: center;
        padding: 5px;
        border-radius: 6px;
        line-height: 0;
      }
      .date-btn:hover {
        color: var(--primary);
        background: color-mix(in srgb, var(--primary) 12%, transparent);
      }
      .date-btn mat-icon {
        font-size: 19px;
        width: 19px;
        height: 19px;
      }
      .date-native {
        position: absolute;
        width: 1px;
        height: 1px;
        opacity: 0;
        pointer-events: none;
        border: 0;
        padding: 0;
      }
      :host(.on-primary) .date-mask {
        background: rgba(255, 255, 255, 0.14);
        border-color: rgba(255, 255, 255, 0.55);
        color: #fff;
      }
      :host(.on-primary) .date-mask::placeholder {
        color: rgba(255, 255, 255, 0.75);
      }
      :host(.on-primary) .date-btn {
        color: rgba(255, 255, 255, 0.85);
      }
      :host(.on-primary) .date-btn:hover {
        color: #fff;
        background: rgba(255, 255, 255, 0.15);
      }
    `,
  ],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => AppDateComponent),
      multi: true,
    },
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AppDateComponent implements ControlValueAccessor {
  @Output() readonly change = new EventEmitter<string | null>();

  @ViewChild('native') private native!: ElementRef<HTMLInputElement>;

  private value: string | null = null;
  private committed: string | null = null;
  private onChange: (value: string | null) => void = () => {};
  private onTouched: () => void = () => {};

  get display(): string {
    return this.toDisplay(this.value);
  }

  writeValue(value: string | null): void {
    this.value = value ?? null;
    this.committed = this.value;
  }

  registerOnChange(fn: (value: string | null) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  onMask(event: Event): void {
    const input = event.target as HTMLInputElement;
    const masked = this.mask(input.value);
    if (masked !== input.value) {
      input.value = masked;
    }
    const iso = this.toIso(masked);
    this.value = iso;
    this.onChange(iso);
  }

  onBlur(): void {
    this.onTouched();
    if (this.value !== this.committed) {
      this.committed = this.value;
      this.change.emit(this.value);
    }
  }

  openNative(): void {
    const el = this.native.nativeElement;
    if (typeof el.showPicker === 'function') {
      try {
        el.showPicker();
        return;
      } catch {
        /* mostrar de forma estándar */
      }
    }
    el.click();
  }

  onNative(event: Event): void {
    const el = event.target as HTMLInputElement;
    const iso = el.value || null;
    this.value = iso;
    this.committed = iso;
    this.onChange(iso);
    this.onTouched();
    this.change.emit(iso);
  }

  private mask(raw: string): string {
    const digits = raw.replace(/\D/g, '').slice(0, 8);
    if (digits.length <= 2) return digits;
    if (digits.length <= 4) return `${digits.slice(0, 2)}/${digits.slice(2)}`;
    return `${digits.slice(0, 2)}/${digits.slice(2, 4)}/${digits.slice(4)}`;
  }

  private toIso(value: string | null): string | null {
    if (!value) return null;
    const match = value.match(/^(\d{2})\/(\d{2})\/(\d{4})$/);
    if (!match) return null;
    const [, dd, mm, yyyy] = match;
    const day = Number(dd);
    const month = Number(mm);
    const year = Number(yyyy);
    const date = new Date(year, month - 1, day);
    if (
      date.getFullYear() !== year ||
      date.getMonth() !== month - 1 ||
      date.getDate() !== day
    ) {
      return null;
    }
    return `${yyyy}-${mm}-${dd}`;
  }

  private toDisplay(value: string | null): string {
    if (!value) return '';
    const match = value.match(/^(\d{4})-(\d{2})-(\d{2})/);
    if (!match) return value;
    return `${match[3]}/${match[2]}/${match[1]}`;
  }
}