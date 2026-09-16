import { Directive, ElementRef, forwardRef, inject } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { reformatarInput } from '../mascara/caret';
import { Mascara } from '../mascara/mascara.directive';

/**
 * Campo monetário em pt-BR: o controle guarda `number | null` (o que a API
 * espera) e o input exibe `100.000,00` o tempo todo, inclusive ao digitar.
 *
 * Digitação em caixa registradora: só dígitos, os dois últimos são os centavos
 * (`12345` → `123,45`). Não há vírgula para digitar, então o valor nunca fica
 * ambíguo. Campo apagado é `null`, não zero — em juros, preço à vista e custo
 * estimado, vazio e zero dizem coisas diferentes ao relatório.
 */
@Directive({
  selector: 'input[appMoeda]',
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => MoedaDirective), multi: true },
  ],
  host: {
    type: 'text',
    inputmode: 'numeric',
    '(input)': 'aoDigitar($event)',
    '(blur)': 'aoTocar()',
  },
})
export class MoedaDirective implements ControlValueAccessor {
  private readonly elemento = inject<ElementRef<HTMLInputElement>>(ElementRef);

  private aoMudar: (valor: number | null) => void = () => {};
  protected aoTocar: () => void = () => {};
  private anterior = '';

  writeValue(valor: number | null): void {
    this.anterior = formatarMoeda(valor);
    this.elemento.nativeElement.value = this.anterior;
  }

  registerOnChange(fn: (valor: number | null) => void): void {
    this.aoMudar = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.aoTocar = fn;
  }

  setDisabledState(desabilitado: boolean): void {
    this.elemento.nativeElement.disabled = desabilitado;
  }

  protected aoDigitar(evento: Event): void {
    const { limpo, exibido } = reformatarInput(this.elemento.nativeElement, evento, this.anterior, MASCARA_MOEDA);
    this.anterior = exibido;
    this.aoMudar(limpo ? Number(limpo) / 100 : null);
  }
}

/** O valor limpo são os centavos em dígitos, sem zero à esquerda. */
export const MASCARA_MOEDA: Mascara = {
  limpar: (bruto) => bruto.replace(/\D/g, '').replace(/^0+/, ''),
  formatar: (centavos) => (centavos ? formatarMoeda(Number(centavos) / 100) : ''),
  // precision 14, scale 2 no banco: 12 dígitos inteiros + 2 de centavos.
  maxLimpo: 14,
  ancorarNoFim: true,
};

export function formatarMoeda(valor: number | null): string {
  return valor != null
    ? valor.toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
    : '';
}
