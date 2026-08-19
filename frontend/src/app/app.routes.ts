import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
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
        loadComponent: () => import('./features/imoveis/imoveis').then((m) => m.Imoveis),
      },
      {
        path: 'pessoas',
        loadComponent: () => import('./features/pessoas/pessoas').then((m) => m.Pessoas),
      },
      {
        path: 'categorias-despesa',
        loadComponent: () =>
          import('./features/categorias-despesa/categorias-despesa').then((m) => m.CategoriasDespesa),
      },
      {
        path: 'despesas',
        loadComponent: () => import('./features/despesas/despesas').then((m) => m.Despesas),
      },
      {
        path: 'contratos',
        loadComponent: () => import('./features/contratos/contratos').then((m) => m.Contratos),
      },
      {
        path: 'orcamento-categoria',
        loadComponent: () =>
          import('./features/orcamento-categoria/orcamento-categoria').then((m) => m.OrcamentoCategoria),
      },
      {
        path: 'relatorios',
        loadComponent: () =>
          import('./features/relatorios/relatorios').then((m) => m.Relatorios),
      },
    ],
  },
];
