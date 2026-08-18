import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ResultadoImovelDTO } from './relatorio.model';

@Injectable({ providedIn: 'root' })
export class RelatoriosService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/relatorios`;

  resultado(imovelId: number): Observable<ResultadoImovelDTO> {
    const params = new HttpParams().set('imovelId', imovelId);
    return this.http.get<ResultadoImovelDTO>(`${this.baseUrl}/resultado-imovel`, { params });
  }

  exportarCsv(imovelId: number): Observable<Blob> {
    const params = new HttpParams().set('imovelId', imovelId).set('format', 'csv');
    return this.http.get(`${this.baseUrl}/resultado-imovel`, { params, responseType: 'blob' });
  }
}
