import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatDialog } from '@angular/material/dialog';
import { BuscaToolbar } from '../../shared/busca-toolbar/busca-toolbar';
import { PessoaFormDialog } from './pessoa-form-dialog/pessoa-form-dialog';
import { PessoaResponseDTO, TIPO_PESSOA_LABEL } from './pessoa.model';
import { formatarDocumento } from '../../shared/mascara/documento';
import { formatarTelefone } from '../../shared/mascara/telefone';
import { PessoasService } from './pessoas.service';

@Component({
  selector: 'app-pessoas',
  imports: [MatButtonModule, MatButtonToggleModule, BuscaToolbar],
  templateUrl: './pessoas.html',
  styleUrl: './pessoas.scss',
})
export class Pessoas implements OnInit {
  private readonly service = inject(PessoasService);
  private readonly dialog = inject(MatDialog);

  protected readonly pessoas = signal<PessoaResponseDTO[]>([]);
  protected readonly carregando = signal(true);
  protected readonly tipoLabel = TIPO_PESSOA_LABEL;
  // Documento e telefone são gravados sem pontuação (ver shared/mascara); a máscara é da tela.
  protected readonly formatarDocumento = formatarDocumento;
  protected readonly formatarTelefone = formatarTelefone;

  protected readonly busca = signal('');
  protected readonly filtro = signal<'todas' | 'fornecedores'>('todas');

  protected readonly pessoasFiltradas = computed(() => {
    const termo = this.busca().trim().toLowerCase();
    const filtro = this.filtro();

    return this.pessoas().filter((pessoa) => {
      if (filtro === 'fornecedores' && !pessoa.fornecedor) return false;
      if (!termo) return true;
      // O documento é gravado sem pontuação, então a busca precisa das duas formas para
      // achar tanto quem digita '52998' quanto quem digita '529.98'.
      return [pessoa.nome, pessoa.documento, formatarDocumento(pessoa.documento, pessoa.tipoPessoa), pessoa.areaAtuacao]
        .filter((valor): valor is string => !!valor)
        .some((valor) => valor.toLowerCase().includes(termo));
    });
  });

  ngOnInit(): void {
    this.carregar();
  }

  protected novo(): void {
    this.abrirFormulario(null);
  }

  protected editar(pessoa: PessoaResponseDTO): void {
    this.abrirFormulario(pessoa);
  }

  protected inativar(pessoa: PessoaResponseDTO): void {
    if (!confirm(`Inativar a pessoa "${pessoa.nome}"?`)) {
      return;
    }
    this.service.inativar(pessoa.id).subscribe(() => this.carregar());
  }

  private abrirFormulario(pessoa: PessoaResponseDTO | null): void {
    this.dialog
      .open(PessoaFormDialog, { data: { pessoa }, autoFocus: false, width: '520px', maxWidth: '95vw' })
      .afterClosed()
      .subscribe(() => this.carregar());
  }

  private carregar(): void {
    this.carregando.set(true);
    this.service.listar().subscribe((pessoas) => {
      this.pessoas.set(pessoas);
      this.carregando.set(false);
    });
  }
}
