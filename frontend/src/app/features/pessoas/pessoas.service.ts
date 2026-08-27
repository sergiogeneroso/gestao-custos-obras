import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PaginaDTO } from '../../shared/pagina/pagina.model';
import { PessoaRequestDTO, PessoaResponseDTO } from './pessoa.model';

@Injectable({ providedIn: 'root' })
export class PessoasService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/pessoas`;

  listar(): Observable<PessoaResponseDTO[]> {
    return this.http.get<PessoaResponseDTO[]>(this.baseUrl);
  }

  /** Página da tela de listagem: busca e filtros são resolvidos no backend. */
  listarPagina(busca: string, somenteFornecedores: boolean, pagina: number, tamanho: number): Observable<PaginaDTO<PessoaResponseDTO>> {
    let params = new HttpParams()
      .set('busca', busca)
      .set('pagina', pagina)
      .set('tamanho', tamanho);
    params = params.set('somenteFornecedores', somenteFornecedores);
    return this.http.get<PaginaDTO<PessoaResponseDTO>>(`${this.baseUrl}/pagina`, { params });
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
