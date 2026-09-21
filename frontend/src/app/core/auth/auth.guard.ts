import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';
import { AuthService } from './auth.service';
import { PermissoesService } from './permissoes.service';

// Garante as permissões carregadas antes de liberar a navegação — inclusive num F5, quando o
// usuário já está autenticado (token no localStorage) mas a matriz ainda não foi buscada nesta
// sessão do navegador. Como este guard fica no Shell (rota pai), os guards de domínio das rotas
// filhas (permissaoGuard) já encontram PermissoesService carregado.
export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const permissoesService = inject(PermissoesService);
  const router = inject(Router);

  if (!authService.autenticado()) {
    return router.createUrlTree(['/login']);
  }

  return permissoesService.garantirCarregado().pipe(map(() => true));
};
