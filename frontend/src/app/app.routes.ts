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
        path: 'aportantes',
        loadComponent: () =>
          import('./features/aportantes/aportantes').then((m) => m.Aportantes),
      },
      {
        path: 'etapas-projeto',
        loadComponent: () =>
          import('./features/etapas-projeto/etapas-projeto').then((m) => m.EtapasProjeto),
      },
      {
        path: 'despesas',
        loadComponent: () => import('./features/despesas/despesas').then((m) => m.Despesas),
      },
      {
        path: 'orcamento-etapa',
        loadComponent: () =>
          import('./features/orcamento-etapa/orcamento-etapa').then((m) => m.OrcamentoEtapa),
      },
      {
        path: 'relatorios',
        loadComponent: () =>
          import('./features/relatorios/relatorios').then((m) => m.Relatorios),
      },
    ],
  },
];
