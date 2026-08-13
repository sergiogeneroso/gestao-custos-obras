import { HttpClient } from '@angular/common/http';
import { Directive, ElementRef, OnChanges, OnDestroy, inject, input } from '@angular/core';

@Directive({
  selector: 'img[appAuthImg]',
})
export class AuthImgDirective implements OnChanges, OnDestroy {
  private readonly http = inject(HttpClient);
  private readonly el = inject<ElementRef<HTMLImageElement>>(ElementRef);

  readonly appAuthImg = input<string | null>(null);

  private urlAtual: string | null = null;

  ngOnChanges(): void {
    this.carregar(this.appAuthImg());
  }

  ngOnDestroy(): void {
    this.liberar();
  }

  private carregar(url: string | null): void {
    this.liberar();
    if (!url) {
      this.el.nativeElement.removeAttribute('src');
      return;
    }

    this.http.get(url, { responseType: 'blob' }).subscribe((blob) => {
      this.urlAtual = URL.createObjectURL(blob);
      this.el.nativeElement.src = this.urlAtual;
    });
  }

  private liberar(): void {
    if (this.urlAtual) {
      URL.revokeObjectURL(this.urlAtual);
      this.urlAtual = null;
    }
  }
}
