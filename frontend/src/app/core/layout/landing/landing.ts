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
      titulo: 'Ciclo de vida do imóvel',
      texto:
        'Compra-se um lote, que pode virar construção e terminar como casa — sempre o mesmo imóvel, com o custo separado por fase. Construir é opcional: parte da carteira é comprada e revendida sem obra.',
    },
    {
      titulo: 'Pessoas, sem rateio automático',
      texto:
        'Cada despesa tem um pagador e, quando já se sabe, um beneficiário — o fornecedor, o banco, o vendedor do lote. Cada pessoa tem extrato próprio, exportável em CSV.',
    },
    {
      titulo: 'Resultado por imóvel',
      texto:
        'Valor de compra, despesas de todas as fases e juros efetivamente pagos compõem o custo; a venda fecha o resultado com lucro, margem e rentabilidade anualizada.',
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
