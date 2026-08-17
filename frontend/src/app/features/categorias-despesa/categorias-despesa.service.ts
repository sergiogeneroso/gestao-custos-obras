import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CategoriaDespesaResponseDTO } from './categoria-despesa.model';

@Injectable({ providedIn: 'root' })
export class CategoriasDespesaService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/categorias-despesa`;

  listar(): Observable<CategoriaDespesaResponseDTO[]> {
    return this.http.get<CategoriaDespesaResponseDTO[]>(this.baseUrl);
  }
}
