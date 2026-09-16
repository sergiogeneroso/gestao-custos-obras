import { Directive, ElementRef, computed, forwardRef, inject, input } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { reformatarInput } from './caret';
import { MASCARAS, NomeMascara } from './catalogo';

/**
 * Campo com máscara: o controle guarda o valor **limpo** (o que a API espera e o
 * banco grava) e o input mostra o valor pontuado — inclusive enquanto se digita.
 *
 * Até setembro de 2026 a máscara só entrava no blur, para não ter de reposicionar
 * o cursor. O dono do produto pediu a máscara visível durante a digitação; o
 * cursor agora é tratado em `caret.ts`.
 *
 * Uso: `appMascara="cep"` para máscara fixa do catálogo; `[appMascara]="objeto"`
 * quando ela muda com o formulário (CPF/CNPJ conforme o tipo da pessoa).
 */
export interface Mascara {
  /** Tira a pontuação, deixando só o que é gravado. */
  limpar(bruto: string): string;
  /** Aplica a pontuação para exibição. */
  formatar(limpo: string): string;
  /** Tamanho máximo do valor limpo — o que passar disso é descartado na digitação. */
  maxLimpo: number;
  /** O valor cresce da direita (moeda em caixa registradora): o cursor se ancora no fim. */
  ancorarNoFim?: boolean;
}

@Directive({
  selector: 'input[appMascara]',
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => MascaraDirective), multi: true },
  ],
  host: {
    type: 'text',
    '(input)': 'aoDigitar($event)',
    '(blur)': 'aoTocar()',
  },
})
export class MascaraDirective implements ControlValueAccessor {
  readonly appMascara = input.required<Mascara | NomeMascara>();

  private readonly mascara = computed(() => {
    const m = this.appMascara();
    return typeof m === 'string' ? MASCARAS[m] : m;
  });
  private readonly elemento = inject<ElementRef<HTMLInputElement>>(ElementRef);

  private aoMudar: (valor: string | null) => void = () => {};
  protected aoTocar: () => void = () => {};
  /** Texto exibido antes da tecla — é como se descobre que o Backspace apagou só pontuação. */
  private anterior = '';

  writeValue(valor: string | null): void {
    const mascara = this.mascara();
    this.anterior = mascara.formatar(mascara.limpar(valor ?? ''));
    this.elemento.nativeElement.value = this.anterior;
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

  protected aoDigitar(evento: Event): void {
    const { limpo, exibido } = reformatarInput(this.elemento.nativeElement, evento, this.anterior, this.mascara());
    this.anterior = exibido;
    this.aoMudar(limpo || null);
  }
}
