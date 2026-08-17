import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ContratoFinanceiroRequestDTO,
  ContratoFinanceiroResponseDTO,
  ContratoQuitacaoRequestDTO,
  ParcelaPagamentoRequestDTO,
} from './contrato.model';

@Injectable({ providedIn: 'root' })
export class ContratosService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/contratos-financeiros`;

  listar(imovelId?: number | null): Observable<ContratoFinanceiroResponseDTO[]> {
    let params = new HttpParams();
    if (imovelId != null) {
      params = params.set('imovelId', imovelId);
    }
    return this.http.get<ContratoFinanceiroResponseDTO[]>(this.baseUrl, { params });
  }

  criar(dto: ContratoFinanceiroRequestDTO): Observable<ContratoFinanceiroResponseDTO> {
    return this.http.post<ContratoFinanceiroResponseDTO>(this.baseUrl, dto);
  }

  quitar(id: number, dto: ContratoQuitacaoRequestDTO): Observable<ContratoFinanceiroResponseDTO> {
    return this.http.patch<ContratoFinanceiroResponseDTO>(`${this.baseUrl}/${id}/quitar`, dto);
  }

  pagarParcela(id: number, parcelaId: number, dto: ParcelaPagamentoRequestDTO): Observable<ContratoFinanceiroResponseDTO> {
    return this.http.patch<ContratoFinanceiroResponseDTO>(`${this.baseUrl}/${id}/parcelas/${parcelaId}/pagamento`, dto);
  }
}
