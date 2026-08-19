import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ContratoDocumentoResponseDTO,
  ContratoFinanceiroRequestDTO,
  ContratoFinanceiroResponseDTO,
  ContratoQuitacaoRequestDTO,
  ParcelaPagamentoRequestDTO,
  TipoDocumentoContrato,
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

  atualizar(id: number, dto: ContratoFinanceiroRequestDTO): Observable<ContratoFinanceiroResponseDTO> {
    return this.http.put<ContratoFinanceiroResponseDTO>(`${this.baseUrl}/${id}`, dto);
  }

  quitar(id: number, dto: ContratoQuitacaoRequestDTO): Observable<ContratoFinanceiroResponseDTO> {
    return this.http.patch<ContratoFinanceiroResponseDTO>(`${this.baseUrl}/${id}/quitar`, dto);
  }

  pagarParcela(id: number, parcelaId: number, dto: ParcelaPagamentoRequestDTO): Observable<ContratoFinanceiroResponseDTO> {
    return this.http.patch<ContratoFinanceiroResponseDTO>(`${this.baseUrl}/${id}/parcelas/${parcelaId}/pagamento`, dto);
  }
  listarDocumentos(id: number): Observable<ContratoDocumentoResponseDTO[]> {
    return this.http.get<ContratoDocumentoResponseDTO[]>(`${this.baseUrl}/${id}/documentos`);
  }

  adicionarDocumento(
    id: number,
    arquivo: File,
    tipoDocumento: TipoDocumentoContrato,
    descricao: string | null,
  ): Observable<ContratoDocumentoResponseDTO> {
    const corpo = new FormData();
    corpo.append('arquivo', arquivo);
    corpo.append('tipoDocumento', tipoDocumento);
    if (descricao) {
      corpo.append('descricao', descricao);
    }
    return this.http.post<ContratoDocumentoResponseDTO>(`${this.baseUrl}/${id}/documentos`, corpo);
  }

  deletarDocumento(id: number, documentoId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}/documentos/${documentoId}`);
  }

  // O download passa pelo HttpClient para o interceptor anexar o token — <img src>/<a href> direto
  // não carregam o Authorization.
  baixarDocumento(url: string): Observable<Blob> {
    return this.http.get(url, { responseType: 'blob' });
  }

}
