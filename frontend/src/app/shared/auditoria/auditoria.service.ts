import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { LogAuditoriaResponseDTO } from './auditoria.model';

@Injectable({ providedIn: 'root' })
export class AuditoriaService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/auditoria`;

  consultar(entidade: string, entidadeId: number): Observable<LogAuditoriaResponseDTO[]> {
    const params = new HttpParams().set('entidade', entidade).set('entidadeId', entidadeId);
    return this.http.get<LogAuditoriaResponseDTO[]>(this.baseUrl, { params });
  }
}
