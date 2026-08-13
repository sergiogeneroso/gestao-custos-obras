import { Component, computed, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatSidenavModule } from '@angular/material/sidenav';
import { AuthService } from '../../auth/auth.service';

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatSidenavModule],
  templateUrl: './shell.html',
  styleUrl: './shell.scss',
})
export class Shell {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly usuario = this.authService.usuario;

  protected readonly iniciais = computed(() => {
    const nome = this.usuario()?.nome ?? '';
    return nome
      .split(' ')
      .filter(Boolean)
      .slice(0, 2)
      .map((parte) => parte[0]?.toUpperCase())
      .join('');
  });

  protected readonly links = [
    { path: 'dashboard', label: 'Dashboard', icone: 'ph-squares-four' },
    { path: 'imoveis', label: 'Imóveis e lotes', icone: 'ph-buildings' },
    { path: 'despesas', label: 'Despesas', icone: 'ph-receipt' },
    { path: 'aportantes', label: 'Aportantes', icone: 'ph-users-three' },
    { path: 'etapas-projeto', label: 'Etapas do projeto', icone: 'ph-list-checks' },
    { path: 'orcamento-etapa', label: 'Orçamentos', icone: 'ph-scales' },
    { path: 'relatorios', label: 'Relatórios', icone: 'ph-chart-line-up' },
  ];

  protected sair(): void {
    this.authService.logout();
    this.router.navigateByUrl('/login');
  }
}
