import {
  ChangeDetectionStrategy,
  Component,
  computed,
  forwardRef,
  input,
  output,
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

let seq = 0;

/** Campo de texto estándar (label + control + hint + error). */
@Component({
  selector: 'ui-input',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => UiInputComponent), multi: true },
  ],
  template: `
    @if (label()) {
      <label class="label" [class.required]="required()" [attr.for]="id">
        {{ label() }}
      </label>
    }
    <input
      class="input"
      [attr.id]="id"
      [attr.type]="type()"
      [attr.placeholder]="placeholder() || null"
      [attr.aria-invalid]="invalid() || null"
      [attr.autocomplete]="autocomplete() || null"
      [value]="value"
      [disabled]="isDisabled()"
      [readOnly]="readOnly()"
      (input)="onInput($event)"
      (blur)="onBlur()"
    />
    @if (error()) {
      <div class="field-msg error" role="alert">{{ error() }}</div>
    } @else if (hint()) {
      <div class="field-msg">{{ hint() }}</div>
    }
  `,
})
export class UiInputComponent implements ControlValueAccessor {
  readonly label = input('');
  readonly type = input('text');
  readonly placeholder = input('');
  readonly hint = input('');
  readonly error = input('');
  readonly autocomplete = input('');
  readonly required = input(false);
  readonly readOnly = input(false);
  readonly invalid = input(false);

  readonly blurred = output<void>();

  readonly id = `ui-in-${++seq}`;

  value = '';
  readonly disabled = input(false);
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
    this.value = (e.target as HTMLInputElement).value;
    this.onChange(this.value);
  }

  onBlur(): void {
    this.onTouched();
    this.blurred.emit();
  }
}
