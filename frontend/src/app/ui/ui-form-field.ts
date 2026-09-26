import { ChangeDetectionStrategy, Component, input } from '@angular/core';

let seq = 0;

/**
 * Envoltorio genérico de campo: label, hint y mensaje de error.
 * El control (input, select, date, etc.) se proyecta en <ng-content/>.
 */
@Component({
  selector: 'ui-form-field',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (label()) {
      <label class="label" [class.required]="required()" [attr.for]="fieldId()">
        {{ label() }}
      </label>
    }
    <ng-content />
    @if (error()) {
      <div class="field-msg error" role="alert">{{ error() }}</div>
    } @else if (hint()) {
      <div class="field-msg">{{ hint() }}</div>
    }
  `,
})
export class UiFormFieldComponent {
  readonly label = input('');
  readonly hint = input('');
  readonly error = input('');
  readonly required = input(false);

  readonly fieldId = input(`ui-f-${++seq}`);
}
