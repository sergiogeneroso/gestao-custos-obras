import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { DominioSistema } from './permissoes.model';
import { PermissoesService } from './permissoes.service';

// Bloqueia a navegação direta por URL a uma tela cujo domínio o usuário não acessa — o menu já
// esconde o item (shell.ts), mas a rota precisa da mesma trava pra quem digita/favorita a URL
// (ADR-046: "menu/rota inteira por acessar"). O backend já recusaria as chamadas de qualquer
// forma; isto só evita abrir uma tela vazia/quebrada por falta de dado.
export function permissaoGuard(dominio: DominioSistema): CanActivateFn {
  return () => {
    const permissoesService = inject(PermissoesService);
    const router = inject(Router);

    return permissoesService.temAcesso(dominio) ? true : router.createUrlTree(['/painel/dashboard']);
  };
}
