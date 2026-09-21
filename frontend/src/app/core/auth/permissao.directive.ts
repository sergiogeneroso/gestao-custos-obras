import { Directive, Input, TemplateRef, ViewContainerRef, effect, inject } from '@angular/core';
import { AcaoPermissao, DominioSistema } from './permissoes.model';
import { PermissoesService } from './permissoes.service';

/**
 * Esconde o elemento quando o usuário logado não tem a permissão indicada — botão "Novo" sem
 * incluir, "Editar" sem alterar, "Excluir" sem deletar (ADR-046). Uso:
 * `*appPermissao="'DESPESA:INCLUIR'"` (ação padrão, sem ":", é ACESSAR).
 */
@Directive({ selector: '[appPermissao]' })
export class PermissaoDirective {
  private readonly permissoesService = inject(PermissoesService);
  private readonly templateRef = inject(TemplateRef<unknown>);
  private readonly viewContainer = inject(ViewContainerRef);

  private dominio: DominioSistema | null = null;
  private acao: AcaoPermissao = 'ACESSAR';

  @Input()
  set appPermissao(valor: string) {
    const [dominio, acao] = valor.split(':');
    this.dominio = (dominio as DominioSistema) ?? null;
    this.acao = (acao as AcaoPermissao) ?? 'ACESSAR';
    this.atualizar();
  }

  constructor() {
    effect(() => {
      this.permissoesService.permissoes();
      this.atualizar();
    });
  }

  private atualizar(): void {
    this.viewContainer.clear();
    if (this.dominio && this.permissoesService.temPermissao(this.dominio, this.acao)) {
      this.viewContainer.createEmbeddedView(this.templateRef);
    }
  }
}
