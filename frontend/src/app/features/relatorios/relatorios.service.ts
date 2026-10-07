import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { DespesaPorCategoriaDTO, ResultadoImovelDTO } from './relatorio.model';

@Injectable({ providedIn: 'root' })
export class RelatoriosService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/relatorios`;

  resultado(imovelId: number): Observable<ResultadoImovelDTO> {
    const params = new HttpParams().set('imovelId', imovelId);
    return this.http.get<ResultadoImovelDTO>(`${this.baseUrl}/resultado-imovel`, { params });
  }

  despesasPorCategoria(
    imovelId: number,
    dataInicio: string | null,
    dataFim: string | null,
  ): Observable<DespesaPorCategoriaDTO[]> {
    let params = new HttpParams().set('imovelId', imovelId);
    if (dataInicio) {
      params = params.set('dataInicio', dataInicio);
    }
    if (dataFim) {
      params = params.set('dataFim', dataFim);
    }
    return this.http.get<DespesaPorCategoriaDTO[]>(`${this.baseUrl}/despesas-por-categoria`, {
      params,
    });
  }

  exportarCsv(imovelId: number): Observable<Blob> {
    const params = new HttpParams().set('imovelId', imovelId).set('format', 'csv');
    return this.http.get(`${this.baseUrl}/resultado-imovel`, { params, responseType: 'blob' });
  }
}
