import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { AuthService } from '../../core/auth/auth.service';
import { DashboardService, ImovelResumo } from './dashboard.service';

const STATUS_LABEL: Record<ImovelResumo['status'], string> = {
  PLANEJAMENTO: 'Planejamento',
  CONSTRUCAO: 'Construção',
  FINALIZADO: 'Finalizado',
};

@Component({
  selector: 'app-dashboard',
  imports: [CurrencyPipe, DecimalPipe, MatButtonToggleModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly dashboardService = inject(DashboardService);

  protected readonly usuario = this.authService.usuario;
  protected readonly carregando = signal(true);
  protected readonly resumo = signal<{
    custoLancadoNoMes: number;
    custoMedioPorM2: number | null;
    despesasSemComprovante: number;
    imoveisRecentes: ImovelResumo[];
  } | null>(null);

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

  protected statusLabel(status: ImovelResumo['status']): string {
    return STATUS_LABEL[status];
  }
}
