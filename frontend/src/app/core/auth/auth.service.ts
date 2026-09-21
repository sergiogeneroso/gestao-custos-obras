import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, map, switchMap, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PermissoesService } from './permissoes.service';

export interface Usuario {
  nome: string;
  email: string;
  // Nome do perfil (ADR-046) — substitui a antiga role. Só informativo aqui: quem decide o que
  // o usuário pode fazer é a matriz de permissões, consultada por PermissoesService.
  perfil: string | null;
}

interface LoginResponse extends Usuario {
  token: string;
}

const CHAVE_TOKEN = 'gestao-custos-obras.token';
const CHAVE_USUARIO = 'gestao-custos-obras.usuario';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly permissoesService = inject(PermissoesService);

  private readonly usuarioSignal = signal<Usuario | null>(this.lerUsuarioArmazenado());
  readonly usuario = this.usuarioSignal.asReadonly();
  readonly autenticado = computed(() => this.usuarioSignal() !== null);

  login(email: string, senha: string): Observable<Usuario> {
    return this.http.post<LoginResponse>(`${environment.apiUrl}/auth/login`, { email, senha }).pipe(
      tap(({ token, ...usuario }) => {
        localStorage.setItem(CHAVE_TOKEN, token);
        localStorage.setItem(CHAVE_USUARIO, JSON.stringify(usuario));
        this.usuarioSignal.set(usuario);
      }),
      switchMap((resposta) => this.permissoesService.carregar().pipe(map(() => resposta))),
      map(({ token: _token, ...usuario }) => usuario),
    );
  }

  logout(): void {
    localStorage.removeItem(CHAVE_TOKEN);
    localStorage.removeItem(CHAVE_USUARIO);
    this.usuarioSignal.set(null);
    this.permissoesService.limpar();
  }

  obterToken(): string | null {
    return localStorage.getItem(CHAVE_TOKEN);
  }

  private lerUsuarioArmazenado(): Usuario | null {
    const bruto = localStorage.getItem(CHAVE_USUARIO);
    return bruto ? (JSON.parse(bruto) as Usuario) : null;
  }
}
