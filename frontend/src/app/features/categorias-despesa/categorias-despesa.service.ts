import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PaginaDTO } from '../../shared/pagina/pagina.model';
import { CategoriaDespesaRequestDTO, CategoriaDespesaResponseDTO } from './categoria-despesa.model';

@Injectable({ providedIn: 'root' })
export class CategoriasDespesaService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/categorias-despesa`;

  listar(): Observable<CategoriaDespesaResponseDTO[]> {
    return this.http.get<CategoriaDespesaResponseDTO[]>(this.baseUrl);
  }

  /** Página da tela de listagem: busca e filtros são resolvidos no backend. */
  listarPagina(busca: string, pagina: number, tamanho: number): Observable<PaginaDTO<CategoriaDespesaResponseDTO>> {
    let params = new HttpParams()
      .set('busca', busca)
      .set('pagina', pagina)
      .set('tamanho', tamanho);
    return this.http.get<PaginaDTO<CategoriaDespesaResponseDTO>>(`${this.baseUrl}/pagina`, { params });
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
