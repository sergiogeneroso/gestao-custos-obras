import { Component, DestroyRef, effect, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatDialog } from '@angular/material/dialog';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { BuscaToolbar } from '../../shared/busca-toolbar/busca-toolbar';
import { ConfirmExclusaoDialog } from '../../shared/confirm-exclusao-dialog/confirm-exclusao-dialog';
import { HistoricoDialog } from '../../shared/auditoria/historico-dialog/historico-dialog';
import { PessoaFormDialog } from './pessoa-form-dialog/pessoa-form-dialog';
import { PessoaResponseDTO, TIPO_PESSOA_LABEL } from './pessoa.model';
import { formatarDocumento } from '../../shared/mascara/documento';
import { formatarTelefone } from '../../shared/mascara/telefone';
import { ListagemPaginada } from '../../shared/pagina/listagem-paginada';
import { PessoasService } from './pessoas.service';

@Component({
  selector: 'app-pessoas',
  imports: [MatButtonModule, MatButtonToggleModule, MatPaginatorModule, BuscaToolbar],
  templateUrl: './pessoas.html',
  styleUrl: './pessoas.scss',
})
export class Pessoas {
  private readonly service = inject(PessoasService);
  private readonly dialog = inject(MatDialog);

  protected readonly tipoLabel = TIPO_PESSOA_LABEL;
  // Documento e telefone são gravados sem pontuação (ver shared/mascara); a máscara é da tela.
  protected readonly formatarDocumento = formatarDocumento;
  protected readonly formatarTelefone = formatarTelefone;

  protected readonly busca = signal('');
  protected readonly filtro = signal<'todas' | 'fornecedores'>('todas');

  // Busca e filtro são resolvidos no backend junto com a paginação: filtrar só a página carregada
  // esconderia registros que casam com o termo mas estão em outra página.
  protected readonly lista = new ListagemPaginada<PessoaResponseDTO>(
    inject(DestroyRef),
    (pagina, tamanho) =>
      this.service.listarPagina(this.busca().trim(), this.filtro() === 'fornecedores', pagina, tamanho),
  );

  constructor() {
    // Qualquer mudança de critério volta para a primeira página — senão a busca poderia cair numa
    // página que não existe mais no resultado novo.
    effect(() => {
      this.busca();
      this.filtro();
      this.lista.reiniciar();
    });
  }

  protected mudarPagina(evento: PageEvent): void {
    this.lista.mudarPagina(evento);
  }

  protected novo(): void {
    this.abrirFormulario(null);
  }

  protected editar(pessoa: PessoaResponseDTO): void {
    this.abrirFormulario(pessoa);
  }

  protected verHistorico(pessoa: PessoaResponseDTO): void {
    this.dialog.open(HistoricoDialog, {
      data: { entidade: 'Pessoa', entidadeId: pessoa.id, titulo: pessoa.nome },
      autoFocus: false,
      width: '640px',
      maxWidth: '95vw',
    });
  }

  protected excluir(pessoa: PessoaResponseDTO): void {
    this.dialog
      .open(ConfirmExclusaoDialog, {
        data: { titulo: `Excluir a pessoa "${pessoa.nome}"?` },
        autoFocus: false,
        width: '480px',
        maxWidth: '95vw',
      })
      .afterClosed()
      .subscribe((motivo?: string) => {
        if (!motivo) {
          return;
        }
        this.service.excluir(pessoa.id, motivo).subscribe(() => this.lista.carregar());
      });
  }

  private abrirFormulario(pessoa: PessoaResponseDTO | null): void {
    this.dialog
      .open(PessoaFormDialog, { data: { pessoa }, autoFocus: false, width: '520px', maxWidth: '95vw' })
      .afterClosed()
      .subscribe(() => this.lista.carregar());
  }
}
