import { Component, computed, effect, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatSidenavModule } from '@angular/material/sidenav';
import { BreakpointObserver } from '@angular/cdk/layout';
import { toSignal } from '@angular/core/rxjs-interop';
import { map } from 'rxjs';
import { AuthService } from '../../auth/auth.service';

const CHAVE_MENU_RECOLHIDO = 'gestao-custos-obras.menuRecolhido';

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatSidenavModule],
  templateUrl: './shell.html',
  styleUrl: './shell.scss',
})
export class Shell {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly breakpointObserver = inject(BreakpointObserver);

  protected readonly usuario = this.authService.usuario;

  /**
   * No celular o menu vira gaveta sobreposta: ocupar coluna fixa numa tela de 375px não sobra
   * espaço para o conteúdo. O `mode`/`opened` do sidenav seguem daqui.
   */
  protected readonly telaEstreita = toSignal(
    this.breakpointObserver.observe('(max-width: 767px)').pipe(map((estado) => estado.matches)),
    { initialValue: false },
  );

  /** No desktop o recolhimento é escolha do usuário e persiste; no celular a gaveta manda. */
  protected readonly recolhido = signal(localStorage.getItem(CHAVE_MENU_RECOLHIDO) === 'true');

  protected readonly gavetaAberta = signal(false);

  constructor() {
    effect(() => localStorage.setItem(CHAVE_MENU_RECOLHIDO, String(this.recolhido())));
  }

  protected alternarMenu(): void {
    if (this.telaEstreita()) {
      this.gavetaAberta.update((aberta) => !aberta);
    } else {
      this.recolhido.update((valor) => !valor);
    }
  }

  protected fecharGaveta(): void {
    if (this.telaEstreita()) {
      this.gavetaAberta.set(false);
    }
  }

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
    { path: 'imoveis', label: 'Imóveis', icone: 'ph-buildings' },
    { path: 'despesas', label: 'Despesas', icone: 'ph-receipt' },
    { path: 'pessoas', label: 'Pessoas', icone: 'ph-users-three' },
    { path: 'contratos', label: 'Contratos financeiros', icone: 'ph-handshake' },
    { path: 'categorias-despesa', label: 'Categorias de despesa', icone: 'ph-list-checks' },
    { path: 'relatorios', label: 'Relatórios', icone: 'ph-chart-line-up' },
  ];

  protected sair(): void {
    this.authService.logout();
    this.router.navigateByUrl('/login');
  }
}
