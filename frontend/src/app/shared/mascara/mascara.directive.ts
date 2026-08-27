import { Directive, ElementRef, forwardRef, inject, input } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

/**
 * Campo com máscara de exibição: o controle guarda o valor **limpo** (o que a API
 * espera e o banco grava) e o input mostra o valor pontuado.
 *
 * Segue o mesmo desenho do `MoedaDirective`: enquanto o campo está focado o valor
 * aparece cru, e a máscara só entra no blur. É o que evita a briga de posição do
 * cursor a cada tecla, sem código de caret nenhum.
 */
export interface Mascara {
  /** Tira a pontuação, deixando só o que é gravado. */
  limpar(bruto: string): string;
  /** Aplica a pontuação para exibição. */
  formatar(limpo: string): string;
  /** Tamanho máximo do valor limpo — o que passar disso é descartado na digitação. */
  maxLimpo: number;
}

@Directive({
  selector: 'input[appMascara]',
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => MascaraDirective), multi: true },
  ],
  host: {
    type: 'text',
    '(input)': 'aoDigitar($any($event.target).value)',
    '(blur)': 'aoSair()',
    '(focus)': 'aoFocar()',
  },
})
export class MascaraDirective implements ControlValueAccessor {
  readonly appMascara = input.required<Mascara>();

  private readonly elemento = inject<ElementRef<HTMLInputElement>>(ElementRef);

  private aoMudar: (valor: string | null) => void = () => {};
  private aoTocar: () => void = () => {};
  private valor = '';

  writeValue(valor: string | null): void {
    this.valor = this.appMascara().limpar(valor ?? '');
    this.elemento.nativeElement.value = this.appMascara().formatar(this.valor);
  }

  registerOnChange(fn: (valor: string | null) => void): void {
    this.aoMudar = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.aoTocar = fn;
  }

  setDisabledState(desabilitado: boolean): void {
    this.elemento.nativeElement.disabled = desabilitado;
  }

  protected aoDigitar(texto: string): void {
    const mascara = this.appMascara();
    this.valor = mascara.limpar(texto).slice(0, mascara.maxLimpo);
    // Reescreve o input porque o `slice` pode ter descartado o excesso: sem isso o
    // caractere a mais fica visível na tela e ausente no controle.
    this.elemento.nativeElement.value = this.valor;
    this.aoMudar(this.valor || null);
  }

  protected aoFocar(): void {
    this.elemento.nativeElement.value = this.valor;
  }

  protected aoSair(): void {
    this.elemento.nativeElement.value = this.appMascara().formatar(this.valor);
    this.aoTocar();
  }
}
