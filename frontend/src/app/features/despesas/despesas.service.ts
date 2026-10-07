import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { paraIso } from '../../shared/data/data.util';
import {
  BuscaDespesaDTO,
  DespesaAnexoResponseDTO,
  DespesaRequestDTO,
  DespesaResponseDTO,
  FiltroDespesas,
  TipoAnexoDespesa,
} from './despesa.model';

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

  /** Página da tela de listagem, com o total em R$ de tudo o que casa com os filtros. */
  listarPagina(
    busca: string,
    filtro: FiltroDespesas,
    pagina: number,
    tamanho: number,
  ): Observable<BuscaDespesaDTO> {
    let params = new HttpParams().set('busca', busca).set('pagina', pagina).set('tamanho', tamanho);
    const opcionais: Record<string, string | number | null> = {
      escopo: filtro.imovel === 'IMOVEL' || filtro.imovel === 'GERAL' ? filtro.imovel : null,
      imovelId: typeof filtro.imovel === 'number' ? filtro.imovel : null,
      categoriaDespesaId: filtro.categoriaDespesaId,
      pagadorId: filtro.pagadorId,
      beneficiarioId: filtro.beneficiarioId,
      faseImovel: filtro.faseImovel,
      etapaConstrucao: filtro.etapaConstrucao,
      dataInicio: paraIso(filtro.dataInicio),
      dataFim: paraIso(filtro.dataFim),
      semComprovante: filtro.semComprovante ? 'true' : null,
    };
    for (const [nome, valor] of Object.entries(opcionais)) {
      if (valor !== null) {
        params = params.set(nome, valor);
      }
    }
    return this.http.get<BuscaDespesaDTO>(`${this.baseUrl}/pagina`, { params });
  }

  criar(dto: DespesaRequestDTO): Observable<DespesaResponseDTO> {
    return this.http.post<DespesaResponseDTO>(this.baseUrl, dto);
  }

  atualizar(id: number, dto: DespesaRequestDTO): Observable<DespesaResponseDTO> {
    return this.http.put<DespesaResponseDTO>(`${this.baseUrl}/${id}`, dto);
  }

  excluir(id: number, motivo: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`, { body: { motivo } });
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
