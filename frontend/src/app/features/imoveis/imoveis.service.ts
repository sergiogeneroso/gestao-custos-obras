import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PaginaDTO } from '../../shared/pagina/pagina.model';
import {
  EnvioDocumento,
  ImovelDocumentoResponseDTO,
  ImovelFaseRequestDTO,
  ImovelFotoResponseDTO,
  ImovelRequestDTO,
  ImovelResponseDTO,
  ImovelSituacaoRequestDTO,
  ImpactoExclusaoImovelResponseDTO,
  FaseImovel,
  SituacaoImovel,
} from './imovel.model';

@Injectable({ providedIn: 'root' })
export class ImoveisService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/imoveis`;

  listar(): Observable<ImovelResponseDTO[]> {
    return this.http.get<ImovelResponseDTO[]>(this.baseUrl);
  }

  /** Página da tela de listagem: busca, fase e situação são resolvidos no backend. */
  listarPagina(
    busca: string,
    fase: FaseImovel | '',
    situacao: SituacaoImovel | '',
    pagina: number,
    tamanho: number,
  ): Observable<PaginaDTO<ImovelResponseDTO>> {
    let params = new HttpParams()
      .set('busca', busca)
      .set('pagina', pagina)
      .set('tamanho', tamanho);
    if (fase) params = params.set('fase', fase);
    if (situacao) params = params.set('situacao', situacao);
    return this.http.get<PaginaDTO<ImovelResponseDTO>>(`${this.baseUrl}/pagina`, { params });
  }

  criar(dto: ImovelRequestDTO): Observable<ImovelResponseDTO> {
    return this.http.post<ImovelResponseDTO>(this.baseUrl, dto);
  }

  atualizar(id: number, dto: ImovelRequestDTO): Observable<ImovelResponseDTO> {
    return this.http.put<ImovelResponseDTO>(`${this.baseUrl}/${id}`, dto);
  }

  excluir(id: number, motivo: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`, { body: { motivo } });
  }

  impactoExclusao(id: number): Observable<ImpactoExclusaoImovelResponseDTO> {
    return this.http.get<ImpactoExclusaoImovelResponseDTO>(`${this.baseUrl}/${id}/impacto-exclusao`);
  }

  avancarFase(id: number, dto: ImovelFaseRequestDTO): Observable<ImovelResponseDTO> {
    return this.http.patch<ImovelResponseDTO>(`${this.baseUrl}/${id}/fase`, dto);
  }

  alterarSituacao(id: number, dto: ImovelSituacaoRequestDTO): Observable<ImovelResponseDTO> {
    return this.http.patch<ImovelResponseDTO>(`${this.baseUrl}/${id}/situacao`, dto);
  }

  listarFotos(imovelId: number): Observable<ImovelFotoResponseDTO[]> {
    return this.http.get<ImovelFotoResponseDTO[]>(`${this.baseUrl}/${imovelId}/fotos`);
  }

  adicionarFoto(imovelId: number, arquivo: File): Observable<ImovelFotoResponseDTO> {
    const formData = new FormData();
    formData.append('arquivo', arquivo);
    return this.http.post<ImovelFotoResponseDTO>(`${this.baseUrl}/${imovelId}/fotos`, formData);
  }

  deletarFoto(imovelId: number, fotoId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${imovelId}/fotos/${fotoId}`);
  }

  definirFotoPrincipal(imovelId: number, fotoId: number): Observable<ImovelFotoResponseDTO[]> {
    return this.http.patch<ImovelFotoResponseDTO[]>(`${this.baseUrl}/${imovelId}/fotos/${fotoId}/principal`, null);
  }

  baixarFoto(url: string): Observable<Blob> {
    return this.http.get(url, { responseType: 'blob' });
  }

  listarDocumentos(imovelId: number): Observable<ImovelDocumentoResponseDTO[]> {
    return this.http.get<ImovelDocumentoResponseDTO[]>(`${this.baseUrl}/${imovelId}/documentos`);
  }

  adicionarDocumento(imovelId: number, arquivo: File, dados: EnvioDocumento): Observable<ImovelDocumentoResponseDTO> {
    const formData = new FormData();
    formData.append('arquivo', arquivo);

    let params = new HttpParams().set('tipoDocumento', dados.tipoDocumento);
    if (dados.faseImovel) {
      params = params.set('faseImovel', dados.faseImovel);
    }
    if (dados.descricao) {
      params = params.set('descricao', dados.descricao);
    }
    if (dados.dataEmissao) {
      params = params.set('dataEmissao', dados.dataEmissao);
    }
    if (dados.dataValidade) {
      params = params.set('dataValidade', dados.dataValidade);
    }

    return this.http.post<ImovelDocumentoResponseDTO>(`${this.baseUrl}/${imovelId}/documentos`, formData, { params });
  }

  deletarDocumento(imovelId: number, documentoId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${imovelId}/documentos/${documentoId}`);
  }

  baixarDocumento(url: string): Observable<Blob> {
    return this.http.get(url, { responseType: 'blob' });
  }
}
