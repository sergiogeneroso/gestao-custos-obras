import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { forkJoin, map, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { FaseImovel, ImovelResponseDTO, SituacaoImovel } from '../imoveis/imovel.model';

interface CarteiraDTO {
  totalInvestido: number;
  totalVendido: number;
  lucroRealizado: number;
  imoveisPorFase: Partial<Record<FaseImovel, number>>;
  imoveisPorSituacao: Partial<Record<SituacaoImovel, number>>;
  saldoDevedorTotal: number;
  saldoAReceberTotal: number;
  parcelasAVencer30Dias: number;
  parcelasAReceber30Dias: number;
  gastosGeraisPeriodo: number;
}

export interface ImovelResumo {
  id: number;
  identificador: string;
  endereco: string | null;
  fase: FaseImovel;
  situacao: SituacaoImovel;
  compraValor: number | null;
  fotoPrincipalUrl: string | null;
}

export interface ResumoDashboard extends CarteiraDTO {
  imoveisRecentes: ImovelResumo[];
}

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);

  carregarResumo(): Observable<ResumoDashboard> {
    return forkJoin({
      carteira: this.http.get<CarteiraDTO>(`${environment.apiUrl}/relatorios/carteira`),
      imoveis: this.http.get<ImovelResponseDTO[]>(`${environment.apiUrl}/imoveis`),
    }).pipe(
      map(({ carteira, imoveis }) => ({
        ...carteira,
        imoveisRecentes: [...imoveis]
          .filter((imovel) => imovel.ativo)
          .sort((a, b) => b.id - a.id)
          .slice(0, 3)
          .map(
            (imovel): ImovelResumo => ({
              id: imovel.id,
              identificador: imovel.identificador,
              endereco: imovel.endereco,
              fase: imovel.fase,
              situacao: imovel.situacao,
              compraValor: imovel.compraValor,
              fotoPrincipalUrl: imovel.fotoPrincipalUrl,
            }),
          ),
      })),
    );
  }
}
