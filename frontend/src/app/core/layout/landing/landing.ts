import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-landing',
  imports: [RouterLink],
  templateUrl: './landing.html',
  styleUrl: './landing.scss',
})
export class Landing {
  protected readonly capacidades = [
    {
      titulo: 'Despesa por etapa',
      texto:
        'Cada lançamento entra amarrado a um imóvel e a uma etapa do catálogo — fundação, alvenaria, acabamento — com comprovante anexado. O custo da obra deixa de ser um total e passa a ser uma sequência auditável.',
    },
    {
      titulo: 'Rateio entre aportantes',
      texto:
        'Uma despesa pode ser dividida entre vários aportantes, e a soma dos pagamentos nunca ultrapassa o valor lançado. Cada aportante tem extrato próprio, exportável em CSV.',
    },
    {
      titulo: 'Orçado contra realizado',
      texto:
        'Cada etapa é orçada uma vez por imóvel; o realizado sobe sozinho conforme as despesas entram. O custo por m² acompanha, e o desvio aparece antes do fim da obra, não depois.',
    },
  ];

  protected readonly vitrine = [
    {
      identificador: 'IMV-014',
      status: 'Construção',
      nome: 'Residencial Aldeia',
      endereco: 'Rua das Palmeiras, 320 — Lote 7 · 412 m²',
    },
    {
      identificador: 'IMV-013',
      status: 'Planejamento',
      nome: 'Edifício Marês',
      endereco: 'Av. Beira-Mar, 1180 — Torre única · 1.960 m²',
    },
    {
      identificador: 'LOT-009',
      status: 'Finalizado',
      nome: 'Lote Serra Azul',
      endereco: 'Estrada da Serra, km 4 — Quadra C · 780 m²',
    },
  ];
}
