import { CurrencyPipe, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, DestroyRef, effect, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { map, tap } from 'rxjs';
import { PermissoesService } from '../../core/auth/permissoes.service';
import { ConfirmDialog } from '../../shared/confirm-dialog/confirm-dialog';
import { mensagemErro } from '../../shared/erro/erro.util';
import { Autocomplete, OpcaoAutocomplete } from '../../shared/autocomplete/autocomplete';
import { MascaraDataDirective } from '../../shared/data/mascara-data.directive';
import { CategoriaDespesaResponseDTO } from '../categorias-despesa/categoria-despesa.model';
import { CategoriasDespesaService } from '../categorias-despesa/categorias-despesa.service';
import { FASE_IMOVEL_LABEL, FaseImovel, opcaoImovel } from '../imoveis/imovel.model';
import { ImoveisService } from '../imoveis/imoveis.service';
import { opcaoPessoa, PessoaResponseDTO } from '../pessoas/pessoa.model';
import { PessoasService } from '../pessoas/pessoas.service';
import { MatDialog } from '@angular/material/dialog';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { PermissaoDirective } from '../../core/auth/permissao.directive';
import { BuscaToolbar } from '../../shared/busca-toolbar/busca-toolbar';
import { ConfirmExclusaoDialog } from '../../shared/confirm-exclusao-dialog/confirm-exclusao-dialog';
import { ListagemPaginada } from '../../shared/pagina/listagem-paginada';
import { DespesaDetalheDialog } from './despesa-detalhe-dialog/despesa-detalhe-dialog';
import { DespesaFormDialog } from './despesa-form-dialog/despesa-form-dialog';
import {
  ajustarFiltro,
  contarMaisFiltros,
  DespesaResponseDTO,
  ETAPA_CONSTRUCAO_LABEL,
  ETAPAS_CONSTRUCAO,
  FILTRO_DESPESAS_VAZIO,
  FiltroDespesas,
} from './despesa.model';
import { DespesasService } from './despesas.service';

@Component({
  selector: 'app-despesas',
  imports: [
    CurrencyPipe,
    DatePipe,
    FormsModule,
    Autocomplete,
    MascaraDataDirective,
    MatButtonModule,
    MatCheckboxModule,
    MatDatepickerModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatPaginatorModule,
    BuscaToolbar,
    PermissaoDirective,
  ],
  templateUrl: './despesas.html',
  styleUrl: './despesas.scss',
})
export class Despesas implements OnInit {
  private readonly service = inject(DespesasService);
  private readonly dialog = inject(MatDialog);
  private readonly imoveisService = inject(ImoveisService);
  private readonly categoriasService = inject(CategoriasDespesaService);
  private readonly pessoasService = inject(PessoasService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly permissoes = inject(PermissoesService);

  // Sem "alterar" o ícone continua mostrando o estado, só não é clicável.
  protected readonly podeConferir = computed(() => this.permissoes.temPermissao('DESPESA', 'ALTERAR'));

  protected readonly fases: FaseImovel[] = ['LOTE', 'CONSTRUCAO', 'CASA'];
  protected readonly faseLabel = FASE_IMOVEL_LABEL;
  protected readonly etapas = ETAPAS_CONSTRUCAO;
  protected readonly etapaLabel = ETAPA_CONSTRUCAO_LABEL;

  protected readonly busca = signal('');
  protected readonly filtro = signal<FiltroDespesas>(FILTRO_DESPESAS_VAZIO);
  protected readonly maisFiltrosAberto = signal(false);
  protected readonly valorTotal = signal(0);

  protected readonly categorias = signal<CategoriaDespesaResponseDTO[]>([]);
  private readonly pessoas = signal<PessoaResponseDTO[]>([]);
  private readonly imoveis = signal<OpcaoAutocomplete[]>([]);

  protected readonly opcoesImovel = computed<OpcaoAutocomplete[]>(() => [
    { id: 'IMOVEL', rotulo: 'Qualquer imóvel' },
    { id: 'GERAL', rotulo: 'Gastos gerais (sem imóvel)' },
    ...this.imoveis(),
  ]);
  // No filtro, pessoa excluída continua escolhível — as despesas dela seguem ativas — e vem marcada.
  protected readonly opcoesPessoa = computed(() =>
    this.pessoas().map((p) => {
      const opcao = opcaoPessoa(p);
      return { ...opcao, detalhe: [opcao.detalhe, p.ativo ? null : 'excluída'].filter(Boolean).join(' · ') };
    }),
  );
  protected readonly quantidadeMaisFiltros = computed(() => contarMaisFiltros(this.filtro()));
  protected readonly temFiltro = computed(
    () => !!this.busca().trim() || JSON.stringify(this.filtro()) !== JSON.stringify(FILTRO_DESPESAS_VAZIO),
  );

  // Busca e filtros são resolvidos no backend junto com a paginação: filtrar só a página
  // carregada esconderia lançamentos que casam com o termo mas estão em outra página.
  protected readonly lista = new ListagemPaginada<DespesaResponseDTO>(
    inject(DestroyRef),
    (pagina, tamanho) =>
      this.service.listarPagina(this.busca().trim(), this.filtro(), pagina, tamanho).pipe(
        tap((resposta) => this.valorTotal.set(resposta.valorTotal)),
        map((resposta) => resposta.pagina),
      ),
  );

  constructor() {
    effect(() => {
      this.busca();
      this.filtro();
      this.lista.reiniciar();
    });
  }

  ngOnInit(): void {
    this.imoveisService.listar().subscribe((imoveis) => this.imoveis.set(imoveis.map(opcaoImovel)));
    this.categoriasService.listar().subscribe((categorias) => this.categorias.set(categorias));
    this.pessoasService.listar().subscribe((pessoas) => this.pessoas.set(pessoas));
  }

  protected alterar(parcial: Partial<FiltroDespesas>): void {
    this.filtro.set(ajustarFiltro({ ...this.filtro(), ...parcial }));
  }

  protected limparFiltros(): void {
    this.busca.set('');
    this.filtro.set(FILTRO_DESPESAS_VAZIO);
  }

  protected mudarPagina(evento: PageEvent): void {
    this.lista.mudarPagina(evento);
  }

  protected novo(): void {
    this.abrirFormulario(null);
  }

  protected editar(despesa: DespesaResponseDTO): void {
    this.abrirFormulario(despesa);
  }

  // Clique na linha abre a consulta; a edição sai de dentro dela ou do botão da coluna de ações.
  protected abrirDetalhe(despesa: DespesaResponseDTO): void {
    this.dialog
      .open(DespesaDetalheDialog, {
        data: {
          despesas: this.lista.itens(),
          indice: Math.max(0, this.lista.itens().indexOf(despesa)),
        },
        autoFocus: false,
        width: '900px',
        maxWidth: '95vw',
      })
      .afterClosed()
      .subscribe(() => this.lista.carregar());
  }

  /**
   * Coluna "Conferida" (ADR-048): marcar é um clique só, porque numa reunião são dezenas seguidas;
   * desfazer pede confirmação. A linha é trocada no lugar em vez de recarregar a página: com o
   * filtro "Pendentes" ela sumiria na hora, e um clique errado ficaria sem como ser desfeito ali.
   */
  protected alternarConferencia(despesa: DespesaResponseDTO): void {
    if (!despesa.conferidaEm) {
      this.gravarConferencia(despesa, true);
      return;
    }
    this.dialog
      .open(ConfirmDialog, {
        data: {
          titulo: 'Desfazer a conferência desta despesa?',
          mensagem: 'Ela volta a ficar pendente e poderá ser editada ou excluída.',
        },
        autoFocus: false,
        width: '420px',
        maxWidth: '95vw',
      })
      .afterClosed()
      .subscribe((confirmado?: boolean) => {
        if (confirmado) {
          this.gravarConferencia(despesa, false);
        }
      });
  }

  private gravarConferencia(despesa: DespesaResponseDTO, conferida: boolean): void {
    this.service.alterarConferencia(despesa.id, conferida).subscribe({
      next: (atualizada) =>
        // Só a conferência muda; anexos/comprovante não vêm calculados fora da busca paginada.
        this.lista.itens.update((itens) =>
          itens.map((item) => (item.id === despesa.id ? { ...item, conferidaEm: atualizada.conferidaEm } : item)),
        ),
      error: (erro: HttpErrorResponse) =>
        this.snackBar.open(mensagemErro(erro, 'Não foi possível alterar a conferência.'), 'Fechar', {
          duration: 6000,
        }),
    });
  }

  protected excluir(despesa: DespesaResponseDTO): void {
    this.dialog
      .open(ConfirmExclusaoDialog, {
        data: { titulo: `Excluir esta despesa de ${despesa.categoriaDespesaNome}?` },
        autoFocus: false,
        width: '480px',
        maxWidth: '95vw',
      })
      .afterClosed()
      .subscribe((motivo?: string) => {
        if (!motivo) {
          return;
        }
        this.service.excluir(despesa.id, motivo).subscribe(() => this.lista.carregar());
      });
  }

  private abrirFormulario(despesa: DespesaResponseDTO | null): void {
    this.dialog
      .open(DespesaFormDialog, {
        data: { despesa },
        autoFocus: false,
        width: '640px',
        maxWidth: '95vw',
      })
      .afterClosed()
      .subscribe(() => this.lista.carregar());
  }
}
