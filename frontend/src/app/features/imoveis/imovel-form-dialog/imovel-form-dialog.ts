import { Component, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { paraData, paraIso } from '../../../shared/data/data.util';
import { MoedaDirective } from '../../../shared/moeda/moeda.directive';
import { PessoaResponseDTO } from '../../pessoas/pessoa.model';
import { PessoasService } from '../../pessoas/pessoas.service';
import { ImovelFotoResponseDTO, ImovelRequestDTO, ImovelResponseDTO } from '../imovel.model';
import { ImoveisService } from '../imoveis.service';

export interface ImovelFormDialogData {
  imovel: ImovelResponseDTO | null;
}

@Component({
  selector: 'app-imovel-form-dialog',
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
  templateUrl: './imovel-form-dialog.html',
  styleUrl: './imovel-form-dialog.scss',
})
export class ImovelFormDialog implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(ImoveisService);
  private readonly pessoasService = inject(PessoasService);
  private readonly dialogRef = inject(MatDialogRef<ImovelFormDialog>);
  private readonly snackBar = inject(MatSnackBar);
  protected readonly data = inject<ImovelFormDialogData>(MAT_DIALOG_DATA);

  protected readonly imovel = this.data.imovel;

  // Cada seção só aparece quando o imóvel já viveu aquela fase (ADR-033): no cadastro de um lote
  // novo, campos de obra e de casa não existem na tela.
  protected readonly mostrarConstrucao = this.imovel?.fase === 'CONSTRUCAO' || this.imovel?.fase === 'CASA';
  protected readonly mostrarCasa = this.imovel?.fase === 'CASA';
  protected readonly mostrarVenda = !!this.imovel && this.imovel.situacao !== 'ADQUIRIDO';

  protected readonly pessoas = signal<PessoaResponseDTO[]>([]);
  protected readonly salvando = signal(false);
  protected readonly fotos = signal<ImovelFotoResponseDTO[]>([]);
  protected readonly urlsFotos = signal<Record<number, string>>({});
  protected readonly enviandoFoto = signal(false);
  protected readonly definindoPrincipal = signal(false);

  // No cadastro ainda não há id para onde subir o arquivo: as fotos escolhidas ficam em memória e
  // sobem logo depois do POST. `previa` é a URL de objeto usada só para exibir a miniatura.
  protected readonly fotosPendentes = signal<{ arquivo: File; previa: string }[]>([]);

  protected readonly form = this.fb.group({
    identificador: [this.imovel?.identificador ?? '', Validators.required],
    endereco: [this.imovel?.endereco ?? ''],
    numero: [this.imovel?.numero ?? ''],
    bairro: [this.imovel?.bairro ?? ''],
    cidade: [this.imovel?.cidade ?? ''],
    uf: [this.imovel?.uf ?? ''],
    cep: [this.imovel?.cep ?? ''],
    observacaoEndereco: [this.imovel?.observacaoEndereco ?? ''],

    matricula: [this.imovel?.lote?.matricula ?? ''],
    cartorio: [this.imovel?.lote?.cartorio ?? ''],
    dataRegistro: [paraData(this.imovel?.lote?.dataRegistro)],
    inscricaoMunicipal: [this.imovel?.lote?.inscricaoMunicipal ?? ''],
    areaLote: [this.imovel?.lote?.area ?? null, Validators.min(0)],

    areaConstruida: [this.imovel?.construcao?.area ?? null, Validators.min(0)],
    dataInicioConstrucao: [paraData(this.imovel?.construcao?.dataInicio)],
    custoEstimadoObra: [this.imovel?.construcao?.custoEstimado ?? null],
    previsaoConclusao: [paraData(this.imovel?.construcao?.previsaoConclusao)],
    alvaraNumero: [this.imovel?.construcao?.alvaraNumero ?? ''],
    alvaraEmissao: [paraData(this.imovel?.construcao?.alvaraEmissao)],
    alvaraValidade: [paraData(this.imovel?.construcao?.alvaraValidade)],
    artNumero: [this.imovel?.construcao?.artNumero ?? ''],
    responsavelTecnicoId: [this.imovel?.construcao?.responsavelTecnicoId ?? null],
    cno: [this.imovel?.construcao?.cno ?? ''],

    dataConclusaoObra: [paraData(this.imovel?.casa?.dataConclusaoObra)],
    habiteSeNumero: [this.imovel?.casa?.habiteSeNumero ?? ''],
    habiteSeData: [paraData(this.imovel?.casa?.habiteSeData)],
    dataAverbacao: [paraData(this.imovel?.casa?.dataAverbacao)],
    quartos: [this.imovel?.casa?.quartos ?? null, Validators.min(0)],
    suites: [this.imovel?.casa?.suites ?? null, Validators.min(0)],
    banheiros: [this.imovel?.casa?.banheiros ?? null, Validators.min(0)],
    vagasGaragem: [this.imovel?.casa?.vagasGaragem ?? null, Validators.min(0)],

    compraValor: [this.imovel?.compraValor ?? null],
    compraData: [paraData(this.imovel?.compraData) ?? new Date(), Validators.required],
    compraVendedorId: [this.imovel?.compraVendedorId ?? null],
    vendaValorPretendido: [this.imovel?.vendaValorPretendido ?? null],
    descricao: [this.imovel?.descricao ?? ''],
  });

  ngOnInit(): void {
    this.pessoasService.listar().subscribe((pessoas) => this.pessoas.set(pessoas.filter((p) => p.ativo)));

    if (this.imovel) {
      this.service.listarFotos(this.imovel.id).subscribe((fotos) => {
        this.fotos.set(fotos);
        fotos.forEach((foto) => this.carregarUrlFoto(foto));
      });
    }
  }

  ngOnDestroy(): void {
    Object.values(this.urlsFotos()).forEach((url) => URL.revokeObjectURL(url));
    this.fotosPendentes().forEach((pendente) => URL.revokeObjectURL(pendente.previa));
  }

  protected salvar(): void {
    if (this.form.invalid) {
      return;
    }

    this.salvando.set(true);

    const bruto = this.form.getRawValue();
    // Os campos das fases vão agrupados (ADR-031); construcao e casa só são aplicados pelo backend
    // se o imóvel já alcançou aquela fase.
    const dto = {
      identificador: bruto.identificador,
      endereco: bruto.endereco || null,
      numero: bruto.numero || null,
      bairro: bruto.bairro || null,
      cidade: bruto.cidade || null,
      uf: bruto.uf || null,
      cep: bruto.cep || null,
      observacaoEndereco: bruto.observacaoEndereco || null,
      lote: {
        matricula: bruto.matricula || null,
        cartorio: bruto.cartorio || null,
        dataRegistro: paraIso(bruto.dataRegistro),
        inscricaoMunicipal: bruto.inscricaoMunicipal || null,
        area: bruto.areaLote,
      },
      construcao: this.mostrarConstrucao
        ? {
            area: bruto.areaConstruida,
            dataInicio: paraIso(bruto.dataInicioConstrucao),
            previsaoConclusao: paraIso(bruto.previsaoConclusao),
            custoEstimado: bruto.custoEstimadoObra,
            alvaraNumero: bruto.alvaraNumero || null,
            alvaraEmissao: paraIso(bruto.alvaraEmissao),
            alvaraValidade: paraIso(bruto.alvaraValidade),
            artNumero: bruto.artNumero || null,
            responsavelTecnicoId: bruto.responsavelTecnicoId,
            responsavelTecnicoNome: null,
            cno: bruto.cno || null,
          }
        : null,
      casa: this.mostrarCasa
        ? {
            dataConclusaoObra: paraIso(bruto.dataConclusaoObra),
            habiteSeNumero: bruto.habiteSeNumero || null,
            habiteSeData: paraIso(bruto.habiteSeData),
            dataAverbacao: paraIso(bruto.dataAverbacao),
            quartos: bruto.quartos,
            suites: bruto.suites,
            banheiros: bruto.banheiros,
            vagasGaragem: bruto.vagasGaragem,
          }
        : null,
      compraValor: bruto.compraValor,
      compraData: paraIso(bruto.compraData),
      compraVendedorId: bruto.compraVendedorId,
      vendaValorPretendido: bruto.vendaValorPretendido,
      descricao: bruto.descricao || null,
    } as ImovelRequestDTO;
    const requisicao = this.imovel ? this.service.atualizar(this.imovel.id, dto) : this.service.criar(dto);

    requisicao.subscribe({
      next: (salvo) => this.enviarFotosPendentes(salvo.id),
      error: (erro: HttpErrorResponse) => {
        this.salvando.set(false);
        const mensagem = erro.error?.mensagem ?? 'Não foi possível salvar o imóvel.';
        this.snackBar.open(mensagem, 'Fechar', { duration: 6000 });
      },
    });
  }

  // O imóvel já está salvo neste ponto: se um upload falhar, o certo é dizer qual arquivo ficou de
  // fora, não fingir que a operação inteira falhou.
  private enviarFotosPendentes(imovelId: number): void {
    const pendentes = this.fotosPendentes();
    if (pendentes.length === 0) {
      this.snackBar.open('Imóvel salvo com sucesso.', 'Fechar', { duration: 4000 });
      this.dialogRef.close(true);
      return;
    }

    const falhas: string[] = [];
    forkJoin(
      pendentes.map((pendente) =>
        this.service.adicionarFoto(imovelId, pendente.arquivo).pipe(
          catchError(() => {
            falhas.push(pendente.arquivo.name);
            return of(null);
          }),
        ),
      ),
    ).subscribe(() => {
      const mensagem = falhas.length
        ? `Imóvel salvo, mas estas fotos não subiram: ${falhas.join(', ')}.`
        : 'Imóvel salvo com sucesso.';
      this.snackBar.open(mensagem, 'Fechar', { duration: falhas.length ? 8000 : 4000 });
      this.dialogRef.close(true);
    });
  }

  protected fechar(): void {
    this.dialogRef.close();
  }

  protected enviarFoto(event: Event): void {
    const input = event.target as HTMLInputElement;
    const arquivo = input.files?.[0];
    if (!arquivo) {
      return;
    }

    // Sem imóvel salvo ainda, a foto entra na fila e sobe depois do POST.
    if (!this.imovel) {
      this.fotosPendentes.update((atuais) => [...atuais, { arquivo, previa: URL.createObjectURL(arquivo) }]);
      input.value = '';
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

  protected removerFotoPendente(indice: number): void {
    const pendente = this.fotosPendentes()[indice];
    URL.revokeObjectURL(pendente.previa);
    this.fotosPendentes.update((atuais) => atuais.filter((_, i) => i !== indice));
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
