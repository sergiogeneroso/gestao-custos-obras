import { Directive, ElementRef, forwardRef, inject } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

/**
 * Campo monetário em pt-BR: o controle guarda `number | null` (o que a API
 * espera) e o input exibe `100.000,00`. Substitui os pares
 * `formatarMoeda`/`parseMoeda` que estavam duplicados em cada diálogo.
 */
@Directive({
  selector: 'input[appMoeda]',
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => MoedaDirective), multi: true },
  ],
  host: {
    type: 'text',
    inputmode: 'decimal',
    '(input)': 'aoDigitar($any($event.target).value)',
    '(blur)': 'aoSair()',
    '(focus)': 'aoFocar()',
  },
})
export class MoedaDirective implements ControlValueAccessor {
  private readonly elemento = inject<ElementRef<HTMLInputElement>>(ElementRef);

  private aoMudar: (valor: number | null) => void = () => {};
  private aoTocar: () => void = () => {};
  private valor: number | null = null;

  writeValue(valor: number | null): void {
    this.valor = valor;
    this.elemento.nativeElement.value = formatarMoeda(valor);
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

  protected aoDigitar(texto: string): void {
    this.valor = parseMoeda(texto);
    this.aoMudar(this.valor);
  }

  // Enquanto edita, o separador de milhar atrapalha a digitação — só a máscara sai.
  protected aoFocar(): void {
    this.elemento.nativeElement.value = this.valor != null ? `${this.valor}`.replace('.', ',') : '';
  }

  protected aoSair(): void {
    this.elemento.nativeElement.value = formatarMoeda(this.valor);
    this.aoTocar();
  }
}

export function formatarMoeda(valor: number | null): string {
  return valor != null
    ? valor.toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
    : '';
}

export function parseMoeda(texto: string | null): number | null {
  if (!texto?.trim()) {
    return null;
  }
  const numero = Number(texto.replace(/\./g, '').replace(',', '.'));
  return Number.isNaN(numero) ? null : numero;
}
