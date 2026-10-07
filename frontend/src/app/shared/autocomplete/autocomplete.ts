import { Component, computed, inject, input, signal } from '@angular/core';
import { ControlValueAccessor, FormControl, FormGroupDirective, NgControl, NgForm, Validators } from '@angular/forms';
import { ErrorStateMatcher } from '@angular/material/core';
import { MatAutocompleteModule, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

export type IdOpcao = number | string;

/**
 * Uma escolha do autocompletar. `termos` entra na busca sem aparecer na tela (CPF/CNPJ, endereço);
 * `detalhe` aparece ao lado do rótulo e também é buscado (área de atuação, "excluída").
 */
export interface OpcaoAutocomplete {
  id: IdOpcao;
  rotulo: string;
  detalhe?: string | null;
  termos?: string | null;
  inativo?: boolean;
}

// Sem acento, sem caixa e pontuação como espaço: "joao" encontra "João", "lote 10" encontra "LOTE-10".
export function normalizar(texto: string): string {
  return texto
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, ' ')
    .trim();
}

/**
 * Inativo nunca é oferecido para escolha nova — só quando a tela pede (filtro de registros
 * antigos). Ele continua sendo exibido como valor atual, porque a busca do rótulo é feita sobre a
 * lista inteira, e não sobre as sugestões.
 */
export function filtrarOpcoes(opcoes: OpcaoAutocomplete[], texto: string, oferecerInativos: boolean): OpcaoAutocomplete[] {
  // Cada palavra digitada precisa aparecer em algum lugar, em qualquer ordem.
  const palavras = normalizar(texto).split(' ').filter(Boolean);
  return opcoes.filter((o) => {
    if (o.inativo && !oferecerInativos) {
      return false;
    }
    const alvo = normalizar(`${o.rotulo} ${o.detalhe ?? ''} ${o.termos ?? ''}`);
    // Sem espaços também, para "123.456" achar o documento gravado só com dígitos.
    const alvoJunto = alvo.replace(/ /g, '');
    return palavras.every((p) => alvo.includes(p) || alvoJunto.includes(p));
  });
}

/**
 * Campo de escolha com busca, no lugar do mat-select para listas longas (pessoas, imóveis). Entra
 * em formulário reativo como qualquer controle (`formControlName`) e guarda só o id escolhido.
 * Texto digitado que não virou escolha some ao fechar o painel e o valor anterior volta: o controle
 * nunca fica com um nome solto que não corresponde a registro nenhum.
 */
@Component({
  selector: 'app-autocomplete',
  imports: [MatAutocompleteModule, MatButtonModule, MatFormFieldModule, MatInputModule],
  template: `
    <mat-form-field appearance="outline" subscriptSizing="dynamic" class="autocomplete">
      <mat-label>{{ rotulo() }}</mat-label>
      <input
        matInput
        [matAutocomplete]="painel"
        [value]="textoExibido()"
        [placeholder]="placeholder()"
        [disabled]="desabilitado()"
        [required]="obrigatorio()"
        [errorStateMatcher]="erro"
        (input)="digitar($any($event.target).value)"
        (blur)="aoTocar()"
      />
      @if (valor() !== null && !obrigatorio() && !desabilitado()) {
        <button matSuffix mat-icon-button type="button" (click)="limpar()" [attr.aria-label]="'Limpar ' + rotulo()">
          <i class="ph ph-x"></i>
        </button>
      }
      <mat-autocomplete #painel [displayWith]="rotuloDe" (optionSelected)="escolher($event)" (closed)="fecharPainel()">
        @for (opcao of sugestoes(); track opcao.id) {
          <mat-option [value]="opcao.id">
            {{ opcao.rotulo }}
            @if (opcao.detalhe) {
              <span class="autocomplete__detalhe">· {{ opcao.detalhe }}</span>
            }
          </mat-option>
        } @empty {
          <mat-option disabled>Nada encontrado</mat-option>
        }
      </mat-autocomplete>
    </mat-form-field>
  `,
  styles: `
    :host { display: block; }
    .autocomplete { width: 100%; }
    .autocomplete__detalhe { opacity: 0.6; font-size: 0.9em; }
  `,
})
export class Autocomplete implements ControlValueAccessor {
  readonly rotulo = input.required<string>();
  readonly opcoes = input.required<OpcaoAutocomplete[]>();
  readonly oferecerInativos = input(false);
  readonly placeholder = input('');

  private readonly ngControl = inject(NgControl, { self: true, optional: true });

  protected readonly valor = signal<IdOpcao | null>(null);
  protected readonly desabilitado = signal(false);
  // null = não está digitando, mostra o rótulo do valor atual.
  private readonly textoDigitado = signal<string | null>(null);

  protected readonly rotuloDe = (id: IdOpcao | null): string =>
    this.opcoes().find((o) => o.id === id)?.rotulo ?? '';
  protected readonly textoExibido = computed(() => this.textoDigitado() ?? this.rotuloDe(this.valor()));
  protected readonly sugestoes = computed(() =>
    filtrarOpcoes(this.opcoes(), this.textoDigitado() ?? '', this.oferecerInativos()),
  );

  // O mat-form-field interno não enxerga o controle do formulário pai; sem isso o campo
  // obrigatório nunca ficaria vermelho.
  protected readonly erro: ErrorStateMatcher = {
    isErrorState: (_c: FormControl | null, form: FormGroupDirective | NgForm | null) =>
      !!this.ngControl?.invalid && !!(this.ngControl.touched || form?.submitted),
  };

  private aoMudar: (valor: IdOpcao | null) => void = () => {};
  protected aoTocar: () => void = () => {};

  constructor() {
    if (this.ngControl) {
      this.ngControl.valueAccessor = this;
    }
  }

  protected obrigatorio(): boolean {
    return !!this.ngControl?.control?.hasValidator(Validators.required);
  }

  // Digitar só busca; o valor muda ao escolher uma opção ou ao fechar o painel com o campo vazio.
  protected digitar(texto: string): void {
    this.textoDigitado.set(texto);
  }

  protected escolher(evento: MatAutocompleteSelectedEvent): void {
    this.textoDigitado.set(null);
    this.mudar(evento.option.value);
  }

  // No fechamento do painel, e não no blur: o blur dispara antes do clique na opção, e limpar o
  // texto ali refaz a lista de sugestões e tira a opção de baixo do cursor. Texto que não virou
  // escolha volta ao valor anterior (nunca apaga um vínculo por engano); campo esvaziado limpa.
  protected fecharPainel(): void {
    const texto = this.textoDigitado();
    this.textoDigitado.set(null);
    if (texto !== null && !texto.trim() && this.valor() !== null) {
      this.mudar(null);
    }
  }

  protected limpar(): void {
    this.textoDigitado.set(null);
    this.mudar(null);
  }

  private mudar(valor: IdOpcao | null): void {
    this.valor.set(valor);
    this.aoMudar(valor);
  }

  writeValue(valor: IdOpcao | null): void {
    this.valor.set(valor ?? null);
    this.textoDigitado.set(null);
  }

  registerOnChange(fn: (valor: IdOpcao | null) => void): void {
    this.aoMudar = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.aoTocar = fn;
  }

  setDisabledState(desabilitado: boolean): void {
    this.desabilitado.set(desabilitado);
  }
}
