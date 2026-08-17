import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { BuscaToolbar } from '../../shared/busca-toolbar/busca-toolbar';
import { PessoaFormDialog } from './pessoa-form-dialog/pessoa-form-dialog';
import { PessoaResponseDTO, TIPO_PESSOA_LABEL } from './pessoa.model';
import { PessoasService } from './pessoas.service';

@Component({
  selector: 'app-pessoas',
  imports: [MatButtonModule, BuscaToolbar],
  templateUrl: './pessoas.html',
  styleUrl: './pessoas.scss',
})
export class Pessoas implements OnInit {
  private readonly service = inject(PessoasService);
  private readonly dialog = inject(MatDialog);

  protected readonly pessoas = signal<PessoaResponseDTO[]>([]);
  protected readonly carregando = signal(true);
  protected readonly tipoLabel = TIPO_PESSOA_LABEL;

  protected readonly busca = signal('');

  protected readonly pessoasFiltradas = computed(() => {
    const termo = this.busca().trim().toLowerCase();
    if (!termo) {
      return this.pessoas();
    }
    return this.pessoas().filter((pessoa) =>
      [pessoa.nome, pessoa.documento].some((valor) => valor.toLowerCase().includes(termo)),
    );
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
