import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PessoaRequestDTO, PessoaResponseDTO } from './pessoa.model';

@Injectable({ providedIn: 'root' })
export class PessoasService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/pessoas`;

  listar(): Observable<PessoaResponseDTO[]> {
    return this.http.get<PessoaResponseDTO[]>(this.baseUrl);
  }

  criar(dto: PessoaRequestDTO): Observable<PessoaResponseDTO> {
    return this.http.post<PessoaResponseDTO>(this.baseUrl, dto);
  }

  atualizar(id: number, dto: PessoaRequestDTO): Observable<PessoaResponseDTO> {
    return this.http.put<PessoaResponseDTO>(`${this.baseUrl}/${id}`, dto);
  }

  inativar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
