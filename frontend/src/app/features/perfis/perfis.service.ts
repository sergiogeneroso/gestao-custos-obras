import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PerfilRequestDTO, PerfilResponseDTO } from '../../core/auth/permissoes.model';

@Injectable({ providedIn: 'root' })
export class PerfisService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/perfis`;

  listar(): Observable<PerfilResponseDTO[]> {
    return this.http.get<PerfilResponseDTO[]>(this.baseUrl);
  }

  criar(dto: PerfilRequestDTO): Observable<PerfilResponseDTO> {
    return this.http.post<PerfilResponseDTO>(this.baseUrl, dto);
  }

  atualizar(id: number, dto: PerfilRequestDTO): Observable<PerfilResponseDTO> {
    return this.http.put<PerfilResponseDTO>(`${this.baseUrl}/${id}`, dto);
  }

  excluir(id: number, motivo: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`, { body: { motivo } });
  }
}
