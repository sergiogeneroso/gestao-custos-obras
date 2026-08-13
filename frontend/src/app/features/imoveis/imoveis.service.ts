import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ImovelFotoResponseDTO, ImovelRequestDTO, ImovelResponseDTO } from './imovel.model';

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

  baixarFoto(url: string): Observable<Blob> {
    return this.http.get(url, { responseType: 'blob' });
  }
}
