/**
 * Ponte entre o `Date` que o MatDatepicker usa nos controles e a string ISO
 * (`aaaa-MM-dd`) que a API troca com o backend (`LocalDate`).
 */

export function paraData(iso: string | null | undefined): Date | null {
  if (!iso) {
    return null;
  }
  // Sem o horário, `new Date('2026-08-19')` é lido como UTC e volta um dia atrás
  // em fuso negativo — por isso a data é montada campo a campo, em hora local.
  const [ano, mes, dia] = iso.slice(0, 10).split('-').map(Number);
  return new Date(ano, mes - 1, dia);
}

export function paraIso(data: Date | string | null | undefined): string | null {
  if (!data) {
    return null;
  }
  if (typeof data === 'string') {
    return data.slice(0, 10) || null;
  }
  const mes = `${data.getMonth() + 1}`.padStart(2, '0');
  const dia = `${data.getDate()}`.padStart(2, '0');
  return `${data.getFullYear()}-${mes}-${dia}`;
}
