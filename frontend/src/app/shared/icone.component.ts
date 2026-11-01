import { Component, input } from '@angular/core';

export type NomeIcone =
  | 'painel' | 'agenda' | 'clientes' | 'fichas' | 'profissionais' | 'servicos' | 'estoque' | 'financeiro'
  | 'usuarios' | 'conta' | 'sincronizar' | 'sair' | 'mais' | 'esquerda' | 'direita' | 'busca'
  | 'email' | 'cadeado' | 'alta' | 'baixa' | 'caixa-vazia' | 'relogio' | 'seta'
  | 'pata' | 'loja' | 'link' | 'check' | 'copiar';

/** Ícones no estilo Lucide (traço de 24px), herdam a cor do texto (currentColor). */
@Component({
  selector: 'app-icone',
  standalone: true,
  template: `
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
      @switch (nome()) {
        @case ('painel') { <rect x="3" y="3" width="7" height="9" rx="1"/><rect x="14" y="3" width="7" height="5" rx="1"/><rect x="14" y="12" width="7" height="9" rx="1"/><rect x="3" y="16" width="7" height="5" rx="1"/> }
        @case ('agenda') { <rect x="3" y="4" width="18" height="18" rx="2"/><path d="M16 2v4M8 2v4M3 10h18"/> }
        @case ('clientes') { <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75"/> }
        @case ('fichas') { <rect x="8" y="2" width="8" height="4" rx="1"/><path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2M12 11h4M12 16h4M8 11h.01M8 16h.01"/> }
        @case ('profissionais') { <circle cx="12" cy="8" r="5"/><path d="M20 21a8 8 0 0 0-16 0"/> }
        @case ('servicos') { <circle cx="6" cy="6" r="3"/><circle cx="6" cy="18" r="3"/><path d="M20 4 8.12 15.88M14.47 14.48 20 20M8.12 8.12 12 12"/> }
        @case ('estoque') { <path d="M11 21.73a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73z"/><path d="M12 22V12M3.3 7l7.7 4.7a2 2 0 0 0 2 0L20.7 7M7.5 4.27l9 5.15"/> }
        @case ('financeiro') { <path d="M19 7V4a1 1 0 0 0-1-1H5a2 2 0 0 0 0 4h15a1 1 0 0 1 1 1v4h-3a2 2 0 0 0 0 4h3a1 1 0 0 0 1-1v-2a1 1 0 0 0-1-1"/><path d="M3 5v14a2 2 0 0 0 2 2h15a1 1 0 0 0 1-1v-4"/> }
        @case ('usuarios') { <path d="M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1c2 0 4.5-1.2 6.24-2.72a1.17 1.17 0 0 1 1.52 0C14.51 3.81 17 5 19 5a1 1 0 0 1 1 1z"/><path d="m9 12 2 2 4-4"/> }
        @case ('conta') { <circle cx="12" cy="12" r="10"/><circle cx="12" cy="10" r="3"/><path d="M7 20.66V19a2 2 0 0 1 2-2h6a2 2 0 0 1 2 2v1.66"/> }
        @case ('sincronizar') { <path d="M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8"/><path d="M21 3v5h-5"/><path d="M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16"/><path d="M8 16H3v5"/> }
        @case ('sair') { <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9"/> }
        @case ('mais') { <path d="M5 12h14M12 5v14"/> }
        @case ('esquerda') { <path d="m15 18-6-6 6-6"/> }
        @case ('direita') { <path d="m9 18 6-6-6-6"/> }
        @case ('busca') { <circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/> }
        @case ('email') { <rect x="2" y="4" width="20" height="16" rx="2"/><path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7"/> }
        @case ('cadeado') { <rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/> }
        @case ('alta') { <path d="M16 7h6v6M22 7l-8.5 8.5-5-5L2 17"/> }
        @case ('baixa') { <path d="M16 17h6v-6M22 17l-8.5-8.5-5 5L2 7"/> }
        @case ('caixa-vazia') { <path d="M22 12h-6l-2 3h-4l-2-3H2"/><path d="M5.45 5.11 2 12v6a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-6l-3.45-6.89A2 2 0 0 0 16.76 4H7.24a2 2 0 0 0-1.79 1.11z"/> }
        @case ('relogio') { <circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/> }
        @case ('pata') { <circle cx="11" cy="4" r="2"/><circle cx="18" cy="8" r="2"/><circle cx="20" cy="16" r="2"/><path d="M9 10a5 5 0 0 1 5 5v3.5a3.5 3.5 0 0 1-6.84 1.045Q6.52 17.48 4.46 16.84A3.5 3.5 0 0 1 5.5 10Z"/> }
        @case ('loja') { <path d="m2 7 4.41-4.41A2 2 0 0 1 7.83 2h8.34a2 2 0 0 1 1.42.59L22 7"/><path d="M4 12v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8"/><path d="M15 22v-4a2 2 0 0 0-2-2h-2a2 2 0 0 0-2 2v4"/><path d="M2 7h20"/><path d="M22 7v3a2 2 0 0 1-2 2 2.7 2.7 0 0 1-1.59-.63.7.7 0 0 0-.82 0A2.7 2.7 0 0 1 16 12a2.7 2.7 0 0 1-1.59-.63.7.7 0 0 0-.82 0A2.7 2.7 0 0 1 12 12a2.7 2.7 0 0 1-1.59-.63.7.7 0 0 0-.82 0A2.7 2.7 0 0 1 8 12a2.7 2.7 0 0 1-1.59-.63.7.7 0 0 0-.82 0A2.7 2.7 0 0 1 4 12a2 2 0 0 1-2-2V7"/> }
        @case ('link') { <path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/> }
        @case ('check') { <path d="M20 6 9 17l-5-5"/> }
        @case ('copiar') { <rect width="14" height="14" x="8" y="8" rx="2" ry="2"/><path d="M4 16c-1.1 0-2-.9-2-2V4c0-1.1.9-2 2-2h10c1.1 0 2 .9 2 2"/> }
        @case ('seta') { <path d="M5 12h14M12 5l7 7-7 7"/> }
      }
    </svg>
  `,
  styles: [`:host { display: inline-flex; width: 1.25em; height: 1.25em; flex: none; } svg { width: 100%; height: 100%; }`],
})
export class IconeComponent {
  nome = input.required<NomeIcone>();
}
