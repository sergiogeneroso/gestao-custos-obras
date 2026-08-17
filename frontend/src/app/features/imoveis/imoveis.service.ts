import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ImovelFaseRequestDTO,
  ImovelFotoResponseDTO,
  ImovelRequestDTO,
  ImovelResponseDTO,
  ImovelSituacaoRequestDTO,
} from './imovel.model';

@Injectable({ providedIn: 'root' })
export class ImoveisService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/imoveis`;

  listar(): Observable<ImovelResponseDTO[]> {
    return this.http.get<ImovelResponseDTO[]>(this.baseUrl);
  }

  criar(dto: ImovelRequestDTO): Observable<ImovelResponseDTO> {
    return this.http.post<ImovelResponseDTO>(this.baseUrl, dto);
  }

  atualizar(id: number, dto: ImovelRequestDTO): Observable<ImovelResponseDTO> {
    return this.http.put<ImovelResponseDTO>(`${this.baseUrl}/${id}`, dto);
  }

  inativar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
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
}
