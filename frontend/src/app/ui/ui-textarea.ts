import {
  ChangeDetectionStrategy,
  Component,
  computed,
  forwardRef,
  input,
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

let seq = 0;

/** Área de texto estándar (label + control + hint + error). */
@Component({
  selector: 'ui-textarea',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => UiTextareaComponent), multi: true },
  ],
  template: `
    @if (label()) {
      <label class="label" [class.required]="required()" [attr.for]="id">
        {{ label() }}
      </label>
    }
    <textarea
      class="input"
      [attr.id]="id"
      [attr.placeholder]="placeholder() || null"
      [attr.rows]="rows()"
      [attr.aria-invalid]="invalid() || null"
      [value]="value"
      [disabled]="isDisabled()"
      [readOnly]="readOnly()"
      (input)="onInput($event)"
      (blur)="onBlur()"
    ></textarea>
    @if (error()) {
      <div class="field-msg error" role="alert">{{ error() }}</div>
    } @else if (hint()) {
      <div class="field-msg">{{ hint() }}</div>
    }
  `,
})
export class UiTextareaComponent implements ControlValueAccessor {
  readonly label = input('');
  readonly placeholder = input('');
  readonly hint = input('');
  readonly error = input('');
  readonly rows = input(4);
  readonly required = input(false);
  readonly readOnly = input(false);
  readonly invalid = input(false);
  readonly disabled = input(false);

  readonly id = `ui-ta-${++seq}`;

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
    this.value = (e.target as HTMLTextAreaElement).value;
    this.onChange(this.value);
  }

  onBlur(): void {
    this.onTouched();
  }
}
