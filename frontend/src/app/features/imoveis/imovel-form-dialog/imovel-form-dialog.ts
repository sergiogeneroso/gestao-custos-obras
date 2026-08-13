import { Component, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import {
  ImovelFotoResponseDTO,
  ImovelRequestDTO,
  ImovelResponseDTO,
  STATUS_IMOVEL_LABEL,
  StatusImovel,
  TIPO_IMOVEL_LABEL,
  TipoImovel,
} from '../imovel.model';
import { ImoveisService } from '../imoveis.service';

export interface ImovelFormDialogData {
  imovel: ImovelResponseDTO | null;
}

@Component({
  selector: 'app-imovel-form-dialog',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
  ],
  templateUrl: './imovel-form-dialog.html',
  styleUrl: './imovel-form-dialog.scss',
})
export class ImovelFormDialog implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(ImoveisService);
  private readonly dialogRef = inject(MatDialogRef<ImovelFormDialog>);
  private readonly snackBar = inject(MatSnackBar);
  protected readonly data = inject<ImovelFormDialogData>(MAT_DIALOG_DATA);

  protected readonly imovel = this.data.imovel;

  protected readonly tipos: TipoImovel[] = ['LOTE', 'IMOVEL'];
  protected readonly status: StatusImovel[] = ['PLANEJAMENTO', 'CONSTRUCAO', 'FINALIZADO'];
  protected readonly tipoLabel = TIPO_IMOVEL_LABEL;
  protected readonly statusLabel = STATUS_IMOVEL_LABEL;

  protected readonly salvando = signal(false);
  protected readonly fotos = signal<ImovelFotoResponseDTO[]>([]);
  protected readonly urlsFotos = signal<Record<number, string>>({});
  protected readonly enviandoFoto = signal(false);
  protected readonly definindoPrincipal = signal(false);

  protected readonly form = this.fb.group({
    identificador: [this.imovel?.identificador ?? '', Validators.required],
    tipo: [this.imovel?.tipo ?? ('IMOVEL' as TipoImovel), Validators.required],
    endereco: [this.imovel?.endereco ?? ''],
    area: [this.imovel?.area ?? null, Validators.min(0)],
    valorAquisicaoInicial: [this.formatarMoeda(this.imovel?.valorAquisicaoInicial ?? null)],
    status: [this.imovel?.status ?? ('PLANEJAMENTO' as StatusImovel), Validators.required],
    descricao: [this.imovel?.descricao ?? ''],
  });

  ngOnInit(): void {
    if (this.imovel) {
      this.service.listarFotos(this.imovel.id).subscribe((fotos) => {
        this.fotos.set(fotos);
        fotos.forEach((foto) => this.carregarUrlFoto(foto));
      });
    }
  }

  ngOnDestroy(): void {
    Object.values(this.urlsFotos()).forEach((url) => URL.revokeObjectURL(url));
  }

  protected salvar(): void {
    if (this.form.invalid) {
      return;
    }

    this.salvando.set(true);

    const bruto = this.form.getRawValue();
    const dto = {
      ...bruto,
      valorAquisicaoInicial: this.parseMoeda(bruto.valorAquisicaoInicial),
    } as ImovelRequestDTO;
    const requisicao = this.imovel ? this.service.atualizar(this.imovel.id, dto) : this.service.criar(dto);

    requisicao.subscribe({
      next: () => {
        this.snackBar.open('Imóvel salvo com sucesso.', 'Fechar', { duration: 4000 });
        this.dialogRef.close(true);
      },
      error: (erro: HttpErrorResponse) => {
        this.salvando.set(false);
        const mensagem = erro.error?.mensagem ?? 'Não foi possível salvar o imóvel.';
        this.snackBar.open(mensagem, 'Fechar', { duration: 6000 });
      },
    });
  }

  protected fechar(): void {
    this.dialogRef.close();
  }

  protected formatarCampoValor(): void {
    const controle = this.form.controls.valorAquisicaoInicial;
    controle.setValue(this.formatarMoeda(this.parseMoeda(controle.value)), { emitEvent: false });
  }

  private formatarMoeda(valor: number | null): string {
    return valor != null ? valor.toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) : '';
  }

  private parseMoeda(texto: string | null): number | null {
    if (!texto) {
      return null;
    }
    const numero = Number(texto.replace(/\./g, '').replace(',', '.'));
    return Number.isNaN(numero) ? null : numero;
  }

  protected enviarFoto(event: Event): void {
    const input = event.target as HTMLInputElement;
    const arquivo = input.files?.[0];
    if (!arquivo || !this.imovel) {
      return;
    }

    this.enviandoFoto.set(true);
    this.service.adicionarFoto(this.imovel.id, arquivo).subscribe({
      next: (foto) => {
        this.fotos.update((atuais) => [...atuais, foto]);
        this.carregarUrlFoto(foto);
        this.enviandoFoto.set(false);
        input.value = '';
      },
      error: (erro: HttpErrorResponse) => {
        this.enviandoFoto.set(false);
        input.value = '';
        this.snackBar.open(erro.error?.mensagem ?? 'Não foi possível enviar a foto.', 'Fechar', { duration: 6000 });
      },
    });
  }

  protected definirComoPrincipal(foto: ImovelFotoResponseDTO): void {
    if (!this.imovel || foto.principal) {
      return;
    }

    this.definindoPrincipal.set(true);
    this.service.definirFotoPrincipal(this.imovel.id, foto.id).subscribe({
      next: (fotos) => {
        this.fotos.set(fotos);
        this.definindoPrincipal.set(false);
      },
      error: (erro: HttpErrorResponse) => {
        this.definindoPrincipal.set(false);
        this.snackBar.open(erro.error?.mensagem ?? 'Não foi possível definir a foto principal.', 'Fechar', {
          duration: 6000,
        });
      },
    });
  }

  protected removerFoto(foto: ImovelFotoResponseDTO): void {
    if (!this.imovel || !confirm(`Remover a foto "${foto.legenda ?? foto.id}"?`)) {
      return;
    }

    this.service.deletarFoto(this.imovel.id, foto.id).subscribe(() => {
      this.fotos.update((atuais) => atuais.filter((f) => f.id !== foto.id));
      this.urlsFotos.update(({ [foto.id]: urlRemovida, ...resto }) => {
        if (urlRemovida) {
          URL.revokeObjectURL(urlRemovida);
        }
        return resto;
      });
    });
  }

  private carregarUrlFoto(foto: ImovelFotoResponseDTO): void {
    this.service.baixarFoto(foto.url).subscribe((blob) => {
      const url = URL.createObjectURL(blob);
      this.urlsFotos.update((atuais) => ({ ...atuais, [foto.id]: url }));
    });
  }
}
