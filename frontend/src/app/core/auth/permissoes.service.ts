import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, of, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AcaoPermissao, DominioSistema, MinhasPermissoesResponseDTO } from './permissoes.model';

// Consultado logo após o login (ADR-046) — não lido de claim no JWT, porque a matriz é editável
// em runtime e um claim ficaria defasado até o próximo login.
@Injectable({ providedIn: 'root' })
export class PermissoesService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/perfis`;

  private readonly permissoesSignal = signal<MinhasPermissoesResponseDTO | null>(null);
  readonly permissoes = this.permissoesSignal.asReadonly();

  carregar(): Observable<MinhasPermissoesResponseDTO> {
    return this.http
      .get<MinhasPermissoesResponseDTO>(`${this.baseUrl}/minhas-permissoes`)
      .pipe(tap((resultado) => this.permissoesSignal.set(resultado)));
  }

  /** Evita refazer a chamada a cada navegação — só busca se esta sessão ainda não carregou. */
  garantirCarregado(): Observable<MinhasPermissoesResponseDTO> {
    const atual = this.permissoesSignal();
    return atual ? of(atual) : this.carregar();
  }

  limpar(): void {
    this.permissoesSignal.set(null);
  }

  temAcesso(dominio: DominioSistema): boolean {
    return this.temPermissao(dominio, 'ACESSAR');
  }

  temPermissao(dominio: DominioSistema, acao: AcaoPermissao): boolean {
    return !!this.permissoesSignal()?.permissoes[dominio]?.includes(acao);
  }
}
