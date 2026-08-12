import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { Shell } from './core/layout/shell/shell';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./core/auth/login/login').then((m) => m.Login),
  },
  {
    path: '',
    component: Shell,
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'imoveis', pathMatch: 'full' },
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
