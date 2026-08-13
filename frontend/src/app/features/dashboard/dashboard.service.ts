import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { forkJoin, map, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

type StatusImovel = 'PLANEJAMENTO' | 'CONSTRUCAO' | 'FINALIZADO';

interface ImovelResponseDTO {
  id: number;
  identificador: string;
  endereco: string;
  area: number;
  status: StatusImovel;
  ativo: boolean;
  fotoPrincipalUrl: string | null;
}

interface DespesaResponseDTO {
  id: number;
  imovelId: number;
  valor: number;
  dataPagamento: string;
  comprovanteUrl: string | null;
}

export interface ImovelResumo {
  id: number;
  identificador: string;
  endereco: string;
  area: number;
  status: StatusImovel;
  gastoRealizado: number;
  custoPorM2: number | null;
  fotoPrincipalUrl: string | null;
}

export interface ResumoDashboard {
  custoLancadoNoMes: number;
  custoMedioPorM2: number | null;
  despesasSemComprovante: number;
  imoveisRecentes: ImovelResumo[];
}

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);

  carregarResumo(): Observable<ResumoDashboard> {
    return forkJoin({
      imoveis: this.http.get<ImovelResponseDTO[]>(`${environment.apiUrl}/imoveis`),
      despesas: this.http.get<DespesaResponseDTO[]>(`${environment.apiUrl}/despesas`),
    }).pipe(map(({ imoveis, despesas }) => this.montarResumo(imoveis, despesas)));
  }

  private montarResumo(imoveis: ImovelResponseDTO[], despesas: DespesaResponseDTO[]): ResumoDashboard {
    const gastoPorImovel = new Map<number, number>();
    for (const despesa of despesas) {
      gastoPorImovel.set(despesa.imovelId, (gastoPorImovel.get(despesa.imovelId) ?? 0) + despesa.valor);
    }

    const agora = new Date();
    const custoLancadoNoMes = despesas
      .filter((d) => {
        const data = new Date(d.dataPagamento);
        return data.getMonth() === agora.getMonth() && data.getFullYear() === agora.getFullYear();
      })
      .reduce((soma, d) => soma + d.valor, 0);

    const despesasSemComprovante = despesas.filter((d) => !d.comprovanteUrl).length;

    const imoveisAtivos = imoveis.filter((i) => i.ativo);
    const areaTotal = imoveisAtivos.reduce((soma, i) => soma + (i.area ?? 0), 0);
    const gastoTotal = imoveisAtivos.reduce((soma, i) => soma + (gastoPorImovel.get(i.id) ?? 0), 0);
    const custoMedioPorM2 = areaTotal > 0 ? gastoTotal / areaTotal : null;

    const imoveisRecentes: ImovelResumo[] = [...imoveisAtivos]
      .sort((a, b) => b.id - a.id)
      .slice(0, 3)
      .map((imovel) => {
        const gastoRealizado = gastoPorImovel.get(imovel.id) ?? 0;
        return {
          id: imovel.id,
          identificador: imovel.identificador,
          endereco: imovel.endereco,
          area: imovel.area,
          status: imovel.status,
          gastoRealizado,
          custoPorM2: imovel.area > 0 ? gastoRealizado / imovel.area : null,
          fotoPrincipalUrl: imovel.fotoPrincipalUrl,
        };
      });

    return { custoLancadoNoMes, custoMedioPorM2, despesasSemComprovante, imoveisRecentes };
  }
}
