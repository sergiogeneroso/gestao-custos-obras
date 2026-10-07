import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatAutocompleteTrigger } from '@angular/material/autocomplete';
import { By } from '@angular/platform-browser';
import { describe, expect, it } from 'vitest';
import { Autocomplete, filtrarOpcoes, IdOpcao, OpcaoAutocomplete } from './autocomplete';

const opcoes: OpcaoAutocomplete[] = [
  { id: 1, rotulo: 'João Pereira', detalhe: 'Pedreiro', termos: '12345678900' },
  { id: 2, rotulo: 'Maria Souza', termos: '98765432100' },
  { id: 3, rotulo: 'José Antigo', inativo: true },
];

describe('filtrarOpcoes', () => {
  it('ignora acento e caixa no nome', () => {
    expect(filtrarOpcoes(opcoes, 'joao', false).map((o) => o.id)).toEqual([1]);
  });

  it('busca também no detalhe e nos termos escondidos', () => {
    expect(filtrarOpcoes(opcoes, 'PEDREIRO', false).map((o) => o.id)).toEqual([1]);
    expect(filtrarOpcoes(opcoes, '987654', false).map((o) => o.id)).toEqual([2]);
  });

  it('casa palavra por palavra, ignorando hífen e pontuação', () => {
    const imoveis: OpcaoAutocomplete[] = [
      { id: 10, rotulo: 'LOTE-10-BOM-SUCESSO-3' },
      { id: 21, rotulo: 'LOTE-21-BOM-SUCESSO-3' },
    ];
    expect(filtrarOpcoes(imoveis, 'lote 10', false).map((o) => o.id)).toEqual([10]);
    expect(filtrarOpcoes(imoveis, 'sucesso 21', false).map((o) => o.id)).toEqual([21]);
    expect(filtrarOpcoes(opcoes, '123.456.789', false).map((o) => o.id)).toEqual([1]);
  });

  it('só oferece inativo quando a tela pede', () => {
    expect(filtrarOpcoes(opcoes, 'jose', false)).toEqual([]);
    expect(filtrarOpcoes(opcoes, 'jose', true).map((o) => o.id)).toEqual([3]);
  });

  it('sem texto devolve todos os ativos', () => {
    expect(filtrarOpcoes(opcoes, '', false).map((o) => o.id)).toEqual([1, 2]);
  });
});

// O componente no DOM: o que filtrarOpcoes não cobre é a ligação com o FormControl — o rótulo
// exibido, o inativo já gravado e o que sobra no controle quando o painel fecha sem escolha.
@Component({
  imports: [ReactiveFormsModule, Autocomplete],
  template: `<app-autocomplete [formControl]="pessoa" rotulo="Pagador" [opcoes]="opcoes" />`,
})
class Host {
  opcoes = opcoes;
  pessoa = new FormControl<IdOpcao | null>(3);
}

function montar() {
  const fixture = TestBed.createComponent(Host);
  document.body.appendChild(fixture.nativeElement);
  fixture.detectChanges();
  const input = fixture.nativeElement.querySelector('input') as HTMLInputElement;
  const gatilho = fixture.debugElement.query(By.directive(MatAutocompleteTrigger)).injector.get(MatAutocompleteTrigger);
  const digitar = (texto: string) => {
    gatilho.openPanel();
    fixture.detectChanges();
    input.value = texto;
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  };
  const fecharPainel = () => {
    gatilho.closePanel();
    fixture.detectChanges();
  };
  return { host: fixture.componentInstance, input, digitar, fecharPainel };
}

describe('Autocomplete no DOM', () => {
  it('exibe o rótulo do valor gravado, mesmo de pessoa inativa', () => {
    const { input } = montar();

    expect(input.value).toBe('José Antigo');
  });

  it('texto que não virou escolha volta ao valor anterior ao fechar o painel', () => {
    const { host, input, digitar, fecharPainel } = montar();

    digitar('xyz');
    fecharPainel();

    expect(host.pessoa.value).toBe(3);
    expect(input.value).toBe('José Antigo');
  });

  it('campo esvaziado limpa o valor ao fechar o painel', () => {
    const { host, digitar, fecharPainel } = montar();

    digitar('');
    fecharPainel();

    expect(host.pessoa.value).toBeNull();
  });
});
