import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { permissaoGuard } from './core/auth/permissao.guard';
import { Shell } from './core/layout/shell/shell';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./core/layout/landing/landing').then((m) => m.Landing),
  },
  {
    path: 'login',
    loadComponent: () => import('./core/auth/login/login').then((m) => m.Login),
  },
  {
    path: 'painel',
    component: Shell,
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        path: 'dashboard',
        loadComponent: () => import('./features/dashboard/dashboard').then((m) => m.Dashboard),
      },
      {
        path: 'imoveis',
        canActivate: [permissaoGuard('IMOVEL')],
        loadComponent: () => import('./features/imoveis/imoveis').then((m) => m.Imoveis),
      },
      {
        path: 'pessoas',
        canActivate: [permissaoGuard('PESSOA')],
        loadComponent: () => import('./features/pessoas/pessoas').then((m) => m.Pessoas),
      },
      {
        path: 'categorias-despesa',
        canActivate: [permissaoGuard('CATEGORIA_DESPESA')],
        loadComponent: () =>
          import('./features/categorias-despesa/categorias-despesa').then((m) => m.CategoriasDespesa),
      },
      {
        path: 'despesas',
        canActivate: [permissaoGuard('DESPESA')],
        loadComponent: () => import('./features/despesas/despesas').then((m) => m.Despesas),
      },
      {
        path: 'contratos',
        canActivate: [permissaoGuard('CONTRATO_FINANCEIRO')],
        loadComponent: () => import('./features/contratos/contratos').then((m) => m.Contratos),
      },
      {
        path: 'orcamento-categoria',
        loadComponent: () =>
          import('./features/orcamento-categoria/orcamento-categoria').then((m) => m.OrcamentoCategoria),
      },
      {
        path: 'relatorios',
        canActivate: [permissaoGuard('RELATORIO')],
        loadComponent: () =>
          import('./features/relatorios/relatorios').then((m) => m.Relatorios),
      },
      {
        path: 'perfis',
        canActivate: [permissaoGuard('PERFIL')],
        loadComponent: () => import('./features/perfis/perfis').then((m) => m.Perfis),
      },
    ],
  },
];
