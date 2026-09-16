import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { DateAdapter, MAT_DATE_FORMATS, MAT_DATE_LOCALE } from '@angular/material/core';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { describe, expect, it } from 'vitest';
import { DATA_PT_BR_FORMATS, DataPtBrAdapter } from '../data/data-pt-br.adapter';
import { MascaraDataDirective } from '../data/mascara-data.directive';
import { MoedaDirective } from '../moeda/moeda.directive';
import { MascaraDirective } from './mascara.directive';

// As diretivas no DOM de verdade: o que caret.spec não cobre é a ligação com o
// evento `input`, a seleção do elemento e o FormControl.
@Component({
  imports: [ReactiveFormsModule, MascaraDirective, MoedaDirective, MascaraDataDirective, MatDatepickerModule],
  template: `
    <input id="cep" appMascara="cep" [formControl]="cep" />
    <input id="valor" appMoeda [formControl]="valor" />
    <input id="data" [matDatepicker]="p" appMascaraData [formControl]="data" />
    <mat-datepicker #p />
  `,
})
class Host {
  cep = new FormControl<string | null>('30140071');
  valor = new FormControl<number | null>(1234.5);
  data = new FormControl<Date | null>(null);
}

function montar() {
  TestBed.configureTestingModule({
    providers: [
      { provide: MAT_DATE_LOCALE, useValue: 'pt-BR' },
      { provide: DateAdapter, useClass: DataPtBrAdapter, deps: [MAT_DATE_LOCALE] },
      { provide: MAT_DATE_FORMATS, useValue: DATA_PT_BR_FORMATS },
    ],
  });
  const fixture = TestBed.createComponent(Host);
  document.body.appendChild(fixture.nativeElement);
  fixture.detectChanges();
  const campo = (id: string) => fixture.nativeElement.querySelector(`#${id}`) as HTMLInputElement;
  return { host: fixture.componentInstance, campo };
}

/** Simula o navegador: aplica a tecla no texto e dispara o `input`. */
function teclar(input: HTMLInputElement, tecla: string): void {
  input.focus();
  const i = input.selectionStart ?? input.value.length;
  if (tecla === 'Backspace') {
    input.value = input.value.slice(0, i - 1) + input.value.slice(i);
    input.setSelectionRange(i - 1, i - 1);
    input.dispatchEvent(new InputEvent('input', { inputType: 'deleteContentBackward' }));
  } else {
    input.value = input.value.slice(0, i) + tecla + input.value.slice(i);
    input.setSelectionRange(i + 1, i + 1);
    input.dispatchEvent(new InputEvent('input', { inputType: 'insertText', data: tecla }));
  }
}

describe('diretivas de máscara no DOM', () => {
  it('appMascara formata o valor inicial e a cada tecla, gravando limpo', () => {
    const { host, campo } = montar();
    const cep = campo('cep');
    expect(cep.value).toBe('30140-071');

    cep.value = '';
    cep.dispatchEvent(new InputEvent('input', { inputType: 'deleteContentBackward' }));
    for (const d of '123456') teclar(cep, d);
    expect(cep.value).toBe('12345-6');
    expect(host.cep.value).toBe('123456');
  });

  it('Backspace logo depois do hífen apaga o dígito anterior', () => {
    const { host, campo } = montar();
    const cep = campo('cep');
    cep.focus();
    cep.setSelectionRange(6, 6); // "30140-|071"
    teclar(cep, 'Backspace');
    // Tirou o 0 antes do hífen; o cursor fica onde ele estava: "3014|0-71".
    expect(cep.value).toBe('30140-71');
    expect(cep.selectionStart).toBe(4);
    teclar(cep, 'Backspace');
    expect(cep.value).toBe('30107-1');
    expect(host.cep.value).toBe('301071');
  });

  it('appMoeda digita em caixa registradora e esvazia para null', () => {
    const { host, campo } = montar();
    const valor = campo('valor');
    expect(valor.value).toBe('1.234,50');

    teclar(valor, '7');
    expect(valor.value).toBe('12.345,07');
    expect(host.valor.value).toBe(12345.07);

    valor.value = '';
    valor.dispatchEvent(new InputEvent('input', { inputType: 'deleteContentBackward' }));
    expect(host.valor.value).toBeNull();
  });

  it('appMascaraData pontua e o datepicker lê a data completa', () => {
    const { host, campo } = montar();
    const data = campo('data');
    for (const d of '19082026') teclar(data, d);
    expect(data.value).toBe('19/08/2026');
    expect(host.data.value).toEqual(new Date(2026, 7, 19));
  });
});
