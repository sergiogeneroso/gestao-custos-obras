import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CategoriaDespesaRequestDTO, CategoriaDespesaResponseDTO } from './categoria-despesa.model';

@Injectable({ providedIn: 'root' })
export class CategoriasDespesaService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/categorias-despesa`;

  listar(): Observable<CategoriaDespesaResponseDTO[]> {
    return this.http.get<CategoriaDespesaResponseDTO[]>(this.baseUrl);
  }

  criar(dto: CategoriaDespesaRequestDTO): Observable<CategoriaDespesaResponseDTO> {
    return this.http.post<CategoriaDespesaResponseDTO>(this.baseUrl, dto);
  }

  atualizar(id: number, dto: CategoriaDespesaRequestDTO): Observable<CategoriaDespesaResponseDTO> {
    return this.http.put<CategoriaDespesaResponseDTO>(`${this.baseUrl}/${id}`, dto);
  }

  deletar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
