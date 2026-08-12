import { Routes } from '@angular/router';
import { Shell } from './core/layout/shell/shell';

export const routes: Routes = [
  {
    path: '',
    component: Shell,
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
