import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { DespesaAnexoResponseDTO, DespesaRequestDTO, DespesaResponseDTO, TipoAnexoDespesa } from './despesa.model';

@Injectable({ providedIn: 'root' })
export class DespesasService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/despesas`;

  listar(): Observable<DespesaResponseDTO[]> {
    return this.http.get<DespesaResponseDTO[]>(this.baseUrl);
  }

  criar(dto: DespesaRequestDTO): Observable<DespesaResponseDTO> {
    return this.http.post<DespesaResponseDTO>(this.baseUrl, dto);
  }

  atualizar(id: number, dto: DespesaRequestDTO): Observable<DespesaResponseDTO> {
    return this.http.put<DespesaResponseDTO>(`${this.baseUrl}/${id}`, dto);
  }

  inativar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  listarAnexos(despesaId: number): Observable<DespesaAnexoResponseDTO[]> {
    return this.http.get<DespesaAnexoResponseDTO[]>(`${this.baseUrl}/${despesaId}/anexos`);
  }

  adicionarAnexo(despesaId: number, arquivo: File, tipoAnexo: TipoAnexoDespesa): Observable<DespesaAnexoResponseDTO> {
    const formData = new FormData();
    formData.append('arquivo', arquivo);
    const params = new HttpParams().set('tipoAnexo', tipoAnexo);
    return this.http.post<DespesaAnexoResponseDTO>(`${this.baseUrl}/${despesaId}/anexos`, formData, { params });
  }

  deletarAnexo(despesaId: number, anexoId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${despesaId}/anexos/${anexoId}`);
  }

  baixarAnexo(url: string): Observable<Blob> {
    return this.http.get(url, { responseType: 'blob' });
  }
}
