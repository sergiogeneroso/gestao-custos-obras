import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PaginaDTO } from '../../shared/pagina/pagina.model';
import { DespesaAnexoResponseDTO, DespesaRequestDTO, DespesaResponseDTO, TipoAnexoDespesa } from './despesa.model';

@Injectable({ providedIn: 'root' })
export class DespesasService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/despesas`;

  listar(imovelId?: number | null): Observable<DespesaResponseDTO[]> {
    let params = new HttpParams();
    if (imovelId != null) {
      params = params.set('imovelId', imovelId);
    }
    return this.http.get<DespesaResponseDTO[]>(this.baseUrl, { params });
  }

  /** Página da tela de listagem. O escopo espelha o filtro com/sem imóvel da tela. */
  listarPagina(
    busca: string,
    escopo: 'TODAS' | 'IMOVEL' | 'GERAL',
    pagina: number,
    tamanho: number,
  ): Observable<PaginaDTO<DespesaResponseDTO>> {
    const params = new HttpParams()
      .set('busca', busca)
      .set('escopo', escopo)
      .set('pagina', pagina)
      .set('tamanho', tamanho);
    return this.http.get<PaginaDTO<DespesaResponseDTO>>(`${this.baseUrl}/pagina`, { params });
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
