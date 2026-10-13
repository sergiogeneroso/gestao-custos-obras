import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { DateAdapter, MAT_DATE_FORMATS, MAT_DATE_LOCALE } from '@angular/material/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';
import { PermissoesService } from '../../core/auth/permissoes.service';
import { DATA_PT_BR_FORMATS, DataPtBrAdapter } from '../../shared/data/data-pt-br.adapter';
import { CategoriasDespesaService } from '../categorias-despesa/categorias-despesa.service';
import { ImoveisService } from '../imoveis/imoveis.service';
import { PessoasService } from '../pessoas/pessoas.service';
import { DespesaResponseDTO } from './despesa.model';
import { Despesas } from './despesas';
import { DespesasService } from './despesas.service';

// Cobre a coluna de conferência (ADR-048): marcar é um clique só (dezenas seguidas numa reunião),
// desfazer pede confirmação, e despesa conferida não deixa editar nem excluir.

async function montar(despesas: DespesaResponseDTO[], confirmarDialogo = true) {
  const service = {
    listarPagina: vi.fn(() =>
      of({
        pagina: { conteudo: despesas, pagina: 0, tamanho: 20, totalElementos: despesas.length, totalPaginas: 1 },
        valorTotal: 0,
      }),
    ),
    alterarConferencia: vi.fn((id: number, conferida: boolean) =>
      of({ ...despesas.find((d) => d.id === id)!, conferidaEm: conferida ? '2026-10-07T20:00:00' : null }),
    ),
  };
  const dialog = { open: vi.fn(() => ({ afterClosed: () => of(confirmarDialogo || undefined) })) };
  TestBed.configureTestingModule({
    providers: [
      { provide: MAT_DATE_LOCALE, useValue: 'pt-BR' },
      { provide: DateAdapter, useClass: DataPtBrAdapter, deps: [MAT_DATE_LOCALE] },
      { provide: MAT_DATE_FORMATS, useValue: DATA_PT_BR_FORMATS },
      { provide: DespesasService, useValue: service },
      { provide: MatDialog, useValue: dialog },
      { provide: MatSnackBar, useValue: { open: vi.fn() } },
      { provide: PermissoesService, useValue: { permissoes: signal({}), temPermissao: () => true } },
      { provide: ImoveisService, useValue: { listar: () => of([]) } },
      { provide: CategoriasDespesaService, useValue: { listar: () => of([]) } },
      { provide: PessoasService, useValue: { listar: () => of([]) } },
    ],
  });
  const fixture = TestBed.createComponent(Despesas);
  fixture.detectChanges();
  // A listagem espera a digitação parar (250 ms) antes de consultar.
  await new Promise((resolve) => setTimeout(resolve, 300));
  fixture.detectChanges();

  const botao = (rotulo: string) =>
    [...fixture.nativeElement.querySelectorAll('button')].filter(
      (b: HTMLButtonElement) => b.getAttribute('aria-label') === rotulo || b.textContent?.trim() === rotulo,
    ) as HTMLButtonElement[];
  return { fixture, service, dialog, botao };
}

describe('Despesas — coluna de conferência', () => {
  it('marca a pendente com um clique, sem confirmação', async () => {
    const { fixture, service, dialog, botao } = await montar([despesa(1, null)]);

    botao('Marcar como conferida')[0].click();
    fixture.detectChanges();

    expect(service.alterarConferencia).toHaveBeenCalledWith(1, true);
    expect(dialog.open).not.toHaveBeenCalled();
    expect(botao('Desfazer conferência')).toHaveLength(1);
  });

  it('desfazer pede confirmação e só grava se confirmado', async () => {
    const cancelado = await montar([despesa(2, '2026-09-12T20:00:00')], false);
    cancelado.botao('Desfazer conferência')[0].click();
    expect(cancelado.dialog.open).toHaveBeenCalled();
    expect(cancelado.service.alterarConferencia).not.toHaveBeenCalled();
    TestBed.resetTestingModule();

    const confirmado = await montar([despesa(2, '2026-09-12T20:00:00')]);
    confirmado.botao('Desfazer conferência')[0].click();
    expect(confirmado.service.alterarConferencia).toHaveBeenCalledWith(2, false);
  });

  it('despesa conferida não deixa editar nem excluir', async () => {
    const { botao } = await montar([despesa(1, null), despesa(2, '2026-09-12T20:00:00')]);

    expect(botao('Editar').map((b) => b.disabled)).toEqual([false, true]);
    expect(botao('Excluir despesa').map((b) => b.disabled)).toEqual([false, true]);
  });
});

function despesa(id: number, conferidaEm: string | null): DespesaResponseDTO {
  return {
    id,
    imovelId: 1,
    imovelIdentificador: 'LOTE-01',
    categoriaDespesaId: 1,
    categoriaDespesaNome: 'Mão de obra',
    pagadorId: 1,
    pagadorNome: 'Pagador',
    beneficiarioId: null,
    beneficiarioNome: null,
    contratoFinanceiroId: null,
    faseImovel: 'CONSTRUCAO',
    etapaConstrucao: null,
    valor: 1500,
    dataPagamento: '2026-09-01',
    descricao: null,
    observacao: null,
    ativo: true,
    quantidadeAnexos: 1,
    temComprovante: true,
    conferidaEm,
  };
}
