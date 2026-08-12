import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from '../../auth/auth.service';

@Component({
  selector: 'app-shell',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatToolbarModule,
    MatSidenavModule,
    MatListModule,
    MatButtonModule,
  ],
  templateUrl: './shell.html',
  styleUrl: './shell.scss',
})
export class Shell {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly usuario = this.authService.usuario;

  protected readonly links = [
    { path: 'imoveis', label: 'Imóveis' },
    { path: 'aportantes', label: 'Aportantes' },
    { path: 'etapas-projeto', label: 'Etapas do Projeto' },
    { path: 'despesas', label: 'Despesas' },
    { path: 'orcamento-etapa', label: 'Orçamento por Etapa' },
    { path: 'relatorios', label: 'Relatórios' },
  ];

  protected sair(): void {
    this.authService.logout();
    this.router.navigateByUrl('/login');
  }
}
