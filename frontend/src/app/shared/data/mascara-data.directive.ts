import { Directive, ElementRef, inject } from '@angular/core';
import { reformatarInput } from '../mascara/caret';
import { Mascara } from '../mascara/mascara.directive';

const MASCARA_DATA: Mascara = {
  limpar: (bruto) => bruto.replace(/\D/g, ''),
  formatar: (d) => {
    if (d.length <= 2) return d;
    if (d.length <= 4) return `${d.slice(0, 2)}/${d.slice(2)}`;
    return `${d.slice(0, 2)}/${d.slice(2, 4)}/${d.slice(4)}`;
  },
  maxLimpo: 8,
};

/**
 * `dd/mm/aaaa` enquanto se digita num `matDatepicker`. Uso:
 * `<input matInput [matDatepicker]="p" appMascaraData formControlName="data" />`.
 *
 * Não é `ControlValueAccessor`: o `MatDatepickerInput` já é, e quem converte o
 * texto em `Date` continua sendo ele, via `DataPtBrAdapter`. Esta diretiva só
 * pontua o texto — e, quando pontua, reemite o `input` para o datepicker ler o
 * texto já formatado, seja qual for a ordem em que os dois ouvem o evento.
 */
@Directive({
  selector: 'input[appMascaraData]',
  host: {
    inputmode: 'numeric',
    // O datepicker reescreve o texto sozinho (blur, calendário), então o texto
    // anterior à tecla só é confiável se lido imediatamente antes dela.
    '(beforeinput)': 'anterior = elemento.nativeElement.value',
    '(input)': 'aoDigitar($event)',
  },
})
export class MascaraDataDirective {
  protected readonly elemento = inject<ElementRef<HTMLInputElement>>(ElementRef);
  protected anterior = '';

  protected aoDigitar(evento: Event): void {
    const input = this.elemento.nativeElement;
    const digitado = input.value;
    const { exibido } = reformatarInput(input, evento, this.anterior, MASCARA_DATA);
    if (exibido !== digitado) {
      input.dispatchEvent(new Event('input', { bubbles: true }));
    }
  }
}
