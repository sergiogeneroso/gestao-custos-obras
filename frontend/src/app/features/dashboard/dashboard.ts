import { CurrencyPipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { AuthService } from '../../core/auth/auth.service';
import { AuthImgDirective } from '../../shared/auth-img/auth-img.directive';
import { FASE_IMOVEL_LABEL, SITUACAO_IMOVEL_LABEL } from '../imoveis/imovel.model';
import { DashboardService, ResumoDashboard } from './dashboard.service';

@Component({
  selector: 'app-dashboard',
  imports: [CurrencyPipe, MatButtonToggleModule, AuthImgDirective],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly dashboardService = inject(DashboardService);

  protected readonly usuario = this.authService.usuario;
  protected readonly carregando = signal(true);
  protected readonly resumo = signal<ResumoDashboard | null>(null);
  protected readonly faseLabel = FASE_IMOVEL_LABEL;
  protected readonly situacaoLabel = SITUACAO_IMOVEL_LABEL;

  protected readonly layout = signal<'cards' | 'lista'>('cards');

  protected readonly saudacao = computed(() => {
    const hora = new Date().getHours();
    if (hora < 12) return 'Bom dia';
    if (hora < 18) return 'Boa tarde';
    return 'Boa noite';
  });

  protected readonly primeiroNome = computed(() => this.usuario()?.nome?.split(' ')[0] ?? '');

  ngOnInit(): void {
    this.dashboardService.carregarResumo().subscribe((resumo) => {
      this.resumo.set(resumo);
      this.carregando.set(false);
    });
  }
}
