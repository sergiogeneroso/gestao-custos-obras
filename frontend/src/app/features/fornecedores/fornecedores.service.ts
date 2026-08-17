import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { FornecedorRequestDTO, FornecedorResponseDTO } from './fornecedor.model';

@Injectable({ providedIn: 'root' })
export class FornecedoresService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/fornecedores`;

  listar(): Observable<FornecedorResponseDTO[]> {
    return this.http.get<FornecedorResponseDTO[]>(this.baseUrl);
  }

  criar(dto: FornecedorRequestDTO): Observable<FornecedorResponseDTO> {
    return this.http.post<FornecedorResponseDTO>(this.baseUrl, dto);
  }

  atualizar(id: number, dto: FornecedorRequestDTO): Observable<FornecedorResponseDTO> {
    return this.http.put<FornecedorResponseDTO>(`${this.baseUrl}/${id}`, dto);
  }

  inativar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
