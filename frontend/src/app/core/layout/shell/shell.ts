import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';

@Component({
  selector: 'app-shell',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatToolbarModule,
    MatSidenavModule,
    MatListModule,
  ],
  templateUrl: './shell.html',
  styleUrl: './shell.scss',
})
export class Shell {
  protected readonly links = [
    { path: 'imoveis', label: 'Imóveis' },
    { path: 'aportantes', label: 'Aportantes' },
    { path: 'etapas-projeto', label: 'Etapas do Projeto' },
    { path: 'despesas', label: 'Despesas' },
    { path: 'orcamento-etapa', label: 'Orçamento por Etapa' },
    { path: 'relatorios', label: 'Relatórios' },
  ];
}
