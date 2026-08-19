import { Component, OnInit, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { paraIso } from '../../../shared/data/data.util';
import { MoedaDirective } from '../../../shared/moeda/moeda.directive';
import { PessoaResponseDTO } from '../../pessoas/pessoa.model';
import { PessoasService } from '../../pessoas/pessoas.service';
import { FASE_IMOVEL_LABEL, ImovelResponseDTO, PROXIMA_FASE } from '../imovel.model';
import { ImoveisService } from '../imoveis.service';

export interface ImovelFaseDialogData {
  imovel: ImovelResponseDTO;
}

@Component({
  selector: 'app-imovel-fase-dialog',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDatepickerModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MoedaDirective,
  ],
  templateUrl: './imovel-fase-dialog.html',
  styleUrl: './imovel-fase-dialog.scss',
})
export class ImovelFaseDialog implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(ImoveisService);
  private readonly pessoasService = inject(PessoasService);
  private readonly dialogRef = inject(MatDialogRef<ImovelFaseDialog>);
  private readonly snackBar = inject(MatSnackBar);
  protected readonly data = inject<ImovelFaseDialogData>(MAT_DIALOG_DATA);

  protected readonly imovel = this.data.imovel;
  protected readonly proximaFase = PROXIMA_FASE[this.imovel.fase]!;
  protected readonly faseLabel = FASE_IMOVEL_LABEL;

  protected readonly pessoas = signal<PessoaResponseDTO[]>([]);
  protected readonly salvando = signal(false);

  // Os dados da fase de destino são preenchidos na própria transição (ADR-033), todos opcionais:
  // alvará e habite-se costumam sair depois do fato que a transição registra.
  protected readonly form = this.fb.group({
    data: [new Date() as Date | null, Validators.required],

    areaConstruida: [null as number | null, Validators.min(0)],
    custoEstimado: [null as number | null, Validators.min(0)],
    previsaoConclusao: [null as Date | null],
    alvaraNumero: [''],
    alvaraEmissao: [null as Date | null],
    alvaraValidade: [null as Date | null],
    artNumero: [''],
    responsavelTecnicoId: [null as number | null],
    cno: [''],

    habiteSeNumero: [''],
    habiteSeData: [null as Date | null],
    dataAverbacao: [null as Date | null],
    quartos: [null as number | null, Validators.min(0)],
    suites: [null as number | null, Validators.min(0)],
    banheiros: [null as number | null, Validators.min(0)],
    vagasGaragem: [null as number | null, Validators.min(0)],
  });

  ngOnInit(): void {
    if (this.proximaFase === 'CONSTRUCAO') {
      this.pessoasService.listar().subscribe((pessoas) => this.pessoas.set(pessoas.filter((p) => p.ativo)));
    }
  }

  protected salvar(): void {
    if (this.form.invalid) {
      return;
    }

    this.salvando.set(true);
    const bruto = this.form.getRawValue();

    this.service
      .avancarFase(this.imovel.id, {
        novaFase: this.proximaFase,
        data: paraIso(bruto.data)!,
        construcao:
          this.proximaFase === 'CONSTRUCAO'
            ? {
                area: bruto.areaConstruida,
                custoEstimado: bruto.custoEstimado,
                previsaoConclusao: paraIso(bruto.previsaoConclusao),
                alvaraNumero: bruto.alvaraNumero || null,
                alvaraEmissao: paraIso(bruto.alvaraEmissao),
                alvaraValidade: paraIso(bruto.alvaraValidade),
                artNumero: bruto.artNumero || null,
                responsavelTecnicoId: bruto.responsavelTecnicoId,
                cno: bruto.cno || null,
              }
            : null,
        casa:
          this.proximaFase === 'CASA'
            ? {
                habiteSeNumero: bruto.habiteSeNumero || null,
                habiteSeData: paraIso(bruto.habiteSeData),
                dataAverbacao: paraIso(bruto.dataAverbacao),
                quartos: bruto.quartos,
                suites: bruto.suites,
                banheiros: bruto.banheiros,
                vagasGaragem: bruto.vagasGaragem,
              }
            : null,
      })
      .subscribe({
        next: (atualizado) => {
          this.snackBar.open('Fase atualizada com sucesso.', 'Fechar', { duration: 4000 });
          this.dialogRef.close(atualizado);
        },
        error: (erro: HttpErrorResponse) => {
          this.salvando.set(false);
          this.snackBar.open(erro.error?.mensagem ?? 'Não foi possível avançar a fase.', 'Fechar', { duration: 6000 });
        },
      });
  }

  protected fechar(): void {
    this.dialogRef.close();
  }
}
