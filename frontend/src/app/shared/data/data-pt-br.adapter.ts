import { Injectable } from '@angular/core';
import { MatDateFormats, NativeDateAdapter } from '@angular/material/core';

/**
 * O `NativeDateAdapter` interpreta o texto digitado com `Date.parse`, que lê
 * "19/08/2026" como mês 19 e devolve data inválida. Sem este override, só o
 * calendário funcionaria — digitar a data, não.
 */
@Injectable()
export class DataPtBrAdapter extends NativeDateAdapter {
  override parse(valor: unknown): Date | null {
    if (typeof valor !== 'string') {
      return super.parse(valor);
    }

    const partes = valor.trim().split(/[/\-.]/);
    if (partes.length !== 3) {
      return super.parse(valor);
    }

    const [dia, mes, ano] = partes.map(Number);
    if ([dia, mes, ano].some(Number.isNaN)) {
      return super.parse(valor);
    }

    const data = new Date(ano < 100 ? 2000 + ano : ano, mes - 1, dia);
    // Rejeita 31/02: o Date normaliza para 03/03 em vez de recusar.
    return data.getMonth() === mes - 1 && data.getDate() === dia ? data : null;
  }

  override getFirstDayOfWeek(): number {
    return 0;
  }
}

export const DATA_PT_BR_FORMATS: MatDateFormats = {
  parse: { dateInput: { day: 'numeric', month: 'numeric', year: 'numeric' } },
  display: {
    dateInput: { day: '2-digit', month: '2-digit', year: 'numeric' },
    monthYearLabel: { month: 'short', year: 'numeric' },
    dateA11yLabel: { day: 'numeric', month: 'long', year: 'numeric' },
    monthYearA11yLabel: { month: 'long', year: 'numeric' },
  },
};
