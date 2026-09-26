import {
  ChangeDetectionStrategy,
  Component,
  computed,
  forwardRef,
  input,
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

export interface UiOption {
  value: string;
  label: string;
}

let seq = 0;

/** Select estándar (label + opciones + hint + error). */
@Component({
  selector: 'ui-select',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => UiSelectComponent), multi: true },
  ],
  template: `
    @if (label()) {
      <label class="label" [class.required]="required()" [attr.for]="id">
        {{ label() }}
      </label>
    }
    <select
      class="input"
      [attr.id]="id"
      [attr.aria-invalid]="invalid() || null"
      [disabled]="isDisabled()"
      (change)="onInput($event)"
      (blur)="onBlur()"
    >
      @if (placeholder()) {
        <option value="">{{ placeholder() }}</option>
      }
      @for (o of options(); track o.value) {
        <option [value]="o.value" [selected]="o.value === value">{{ o.label }}</option>
      }
      <ng-content />
    </select>
    @if (error()) {
      <div class="field-msg error" role="alert">{{ error() }}</div>
    } @else if (hint()) {
      <div class="field-msg">{{ hint() }}</div>
    }
  `,
})
export class UiSelectComponent implements ControlValueAccessor {
  readonly label = input('');
  readonly options = input<UiOption[]>([]);
  readonly placeholder = input('');
  readonly hint = input('');
  readonly error = input('');
  readonly required = input(false);
  readonly invalid = input(false);
  readonly disabled = input(false);

  readonly id = `ui-sel-${++seq}`;

  value = '';
  private cvaDisabled = false;

  readonly isDisabled = computed(() => this.cvaDisabled || this.disabled());

  private onChange: (v: string) => void = () => {};
  private onTouched: () => void = () => {};

  writeValue(v: unknown): void {
    this.value = v == null ? '' : String(v);
  }

  registerOnChange(fn: (v: string) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(state: boolean): void {
    this.cvaDisabled = state;
  }

  onInput(e: Event): void {
    this.value = (e.target as HTMLSelectElement).value;
    this.onChange(this.value);
  }

  onBlur(): void {
    this.onTouched();
  }
}
