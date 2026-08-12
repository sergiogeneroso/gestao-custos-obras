import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface Usuario {
  nome: string;
  email: string;
  role: string;
}

interface LoginResponse extends Usuario {
  token: string;
}

const CHAVE_TOKEN = 'gestao-custos-obras.token';
const CHAVE_USUARIO = 'gestao-custos-obras.usuario';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

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
    );
  }

  logout(): void {
    localStorage.removeItem(CHAVE_TOKEN);
    localStorage.removeItem(CHAVE_USUARIO);
    this.usuarioSignal.set(null);
  }

  obterToken(): string | null {
    return localStorage.getItem(CHAVE_TOKEN);
  }

  private lerUsuarioArmazenado(): Usuario | null {
    const bruto = localStorage.getItem(CHAVE_USUARIO);
    return bruto ? (JSON.parse(bruto) as Usuario) : null;
  }
}
