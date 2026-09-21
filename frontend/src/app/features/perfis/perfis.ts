import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { PermissaoDirective } from '../../core/auth/permissao.directive';
import { PerfilResponseDTO } from '../../core/auth/permissoes.model';
import { ConfirmExclusaoDialog } from '../../shared/confirm-exclusao-dialog/confirm-exclusao-dialog';
import { HistoricoDialog } from '../../shared/auditoria/historico-dialog/historico-dialog';
import { PerfilFormDialog } from './perfil-form-dialog/perfil-form-dialog';
import { PerfisService } from './perfis.service';

@Component({
  selector: 'app-perfis',
  imports: [MatButtonModule, PermissaoDirective],
  templateUrl: './perfis.html',
  styleUrl: './perfis.scss',
})
export class Perfis {
  private readonly service = inject(PerfisService);
  private readonly dialog = inject(MatDialog);

  protected readonly perfis = signal<PerfilResponseDTO[]>([]);
  protected readonly carregando = signal(true);

  constructor() {
    this.carregar();
  }

  private carregar(): void {
    this.carregando.set(true);
    this.service.listar().subscribe((perfis) => {
      this.perfis.set(perfis);
      this.carregando.set(false);
    });
  }

  protected resumoPermissoes(perfil: PerfilResponseDTO): string {
    const dominios = new Set(perfil.permissoes.map((p) => p.dominio)).size;
    const total = perfil.permissoes.length;
    if (total === 0) {
      return 'Nenhuma permissão';
    }
    return `${total} permissão(ões) em ${dominios} domínio(s)`;
  }

  protected novo(): void {
    this.abrirFormulario(null);
  }

  protected editar(perfil: PerfilResponseDTO): void {
    this.abrirFormulario(perfil);
  }

  protected verHistorico(perfil: PerfilResponseDTO): void {
    this.dialog.open(HistoricoDialog, {
      data: { entidade: 'Perfil', entidadeId: perfil.id, titulo: perfil.nome },
      autoFocus: false,
      width: '640px',
      maxWidth: '95vw',
    });
  }

  protected excluir(perfil: PerfilResponseDTO): void {
    this.dialog
      .open(ConfirmExclusaoDialog, {
        data: { titulo: `Excluir o perfil "${perfil.nome}"?` },
        autoFocus: false,
        width: '480px',
        maxWidth: '95vw',
      })
      .afterClosed()
      .subscribe((motivo?: string) => {
        if (!motivo) {
          return;
        }
        this.service.excluir(perfil.id, motivo).subscribe(() => this.carregar());
      });
  }

  private abrirFormulario(perfil: PerfilResponseDTO | null): void {
    this.dialog
      .open(PerfilFormDialog, { data: { perfil }, autoFocus: false, width: '640px', maxWidth: '95vw' })
      .afterClosed()
      .subscribe(() => this.carregar());
  }
}
