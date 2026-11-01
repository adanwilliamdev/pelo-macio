import { Component, input } from '@angular/core';

/** Marca do Pelo Macio: monograma de pata em verde-água + nome. */
@Component({
  selector: 'app-logo',
  standalone: true,
  template: `
    <svg [attr.width]="tamanho()" [attr.height]="tamanho()" viewBox="0 0 40 40" aria-hidden="true">
      <rect width="40" height="40" rx="10" fill="#2dd4bf"/>
      <g fill="none" stroke="#0f172a" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" transform="translate(8 8)">
        <circle cx="11" cy="4" r="2"/><circle cx="18" cy="8" r="2"/><circle cx="20" cy="16" r="2"/>
        <path d="M9 10a5 5 0 0 1 5 5v3.5a3.5 3.5 0 0 1-6.84 1.045Q6.52 17.48 4.46 16.84A3.5 3.5 0 0 1 5.5 10Z"/>
      </g>
    </svg>
    @if (texto()) { <span class="nome" [style.font-size.px]="tamanho() * .45">Pelo Macio</span> }
  `,
  styles: [`
    :host { display: inline-flex; align-items: center; gap: .65rem; color: inherit; }
    .nome { font-weight: 700; line-height: 1; letter-spacing: -.02em; white-space: nowrap; }
  `],
})
export class LogoComponent {
  tamanho = input(38);
  texto = input(true);
}
