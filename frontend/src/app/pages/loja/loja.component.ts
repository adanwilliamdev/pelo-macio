import { Component, inject, OnInit, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { API } from '../../core/api';
import { mensagemErro } from '../../core/erro';
import { Loja } from '../../core/models';
import { IconeComponent } from '../../shared/icone.component';

const DIAS = [
  { n: 1, rotulo: 'Seg' }, { n: 2, rotulo: 'Ter' }, { n: 3, rotulo: 'Qua' }, { n: 4, rotulo: 'Qui' },
  { n: 5, rotulo: 'Sex' }, { n: 6, rotulo: 'Sáb' }, { n: 7, rotulo: 'Dom' },
];

/** Configuração da loja e o link público de agendamento. */
@Component({
  selector: 'app-loja',
  standalone: true,
  imports: [FormsModule, IconeComponent],
  template: `
    <header class="topo">
      <div>
        <h1>Minha loja</h1>
        <p class="sub">Horário de funcionamento e agendamento online.</p>
      </div>
    </header>

    @if (erro()) { <div class="erro" role="alert">{{ erro() }}</div> }
    @if (ok()) { <div class="sucesso" role="status">{{ ok() }}</div> }

    @if (loja(); as l) {
      <section class="card link-card">
        <h2>Link de agendamento online</h2>
        <p class="suave">Divulgue no WhatsApp, no Instagram ou em um QR Code. O tutor escolhe o serviço e o horário sem precisar de login.</p>
        <div class="link">
          <app-icone nome="link" />
          <code>{{ linkPublico(l) }}</code>
          <button type="button" class="mini" (click)="copiar(l)"><app-icone [nome]="copiado() ? 'check' : 'copiar'" /> {{ copiado() ? 'Copiado' : 'Copiar' }}</button>
          <a class="abrir" [href]="linkPublico(l)" target="_blank" rel="noopener">Abrir</a>
        </div>
        @if (!l.agendamentoOnline) { <p class="aviso-off">O agendamento online está desligado: o link mostra “loja não encontrada”.</p> }
      </section>

      <form class="card" (ngSubmit)="salvar()">
        <h2>Dados e horários</h2>
        <div class="form-grid">
          <div class="campo"><label for="l-nome">Nome da loja *</label><input id="l-nome" name="nome" [(ngModel)]="l.nome" required maxlength="120"></div>
          <div class="campo"><label for="l-tel">Telefone</label><input id="l-tel" name="telefone" type="tel" [(ngModel)]="l.telefone" maxlength="30"></div>
          <div class="campo"><label for="l-ab">Abre às *</label><input id="l-ab" name="abertura" type="time" [(ngModel)]="l.abertura" required></div>
          <div class="campo"><label for="l-fe">Fecha às *</label><input id="l-fe" name="fechamento" type="time" [(ngModel)]="l.fechamento" required></div>
          <div class="campo"><label for="l-int">Intervalo entre horários (min) *</label>
            <input id="l-int" name="intervalo" type="number" min="5" max="240" step="5" [(ngModel)]="l.intervaloMinutos" required></div>
        </div>

        <div class="campo">
          <label>Dias de atendimento</label>
          <div class="dias">
            @for (d of dias; track d.n) {
              <label class="dia" [class.marcado]="aberto(l, d.n)">
                <input type="checkbox" [checked]="aberto(l, d.n)" (change)="alternarDia(l, d.n)"> {{ d.rotulo }}
              </label>
            }
          </div>
        </div>

        <div class="campo"><label for="l-on"><input id="l-on" type="checkbox" name="online" [(ngModel)]="l.agendamentoOnline"> Aceitar agendamentos pela página pública</label></div>

        <div class="form-acoes"><button class="primario" [disabled]="salvando()">{{ salvando() ? 'Salvando…' : 'Salvar' }}</button></div>
      </form>
    }
  `,
  styles: [`
    .link-card { margin-bottom: 1.25rem; }
    .link { display: flex; align-items: center; gap: .6rem; flex-wrap: wrap; margin-top: 1rem; padding: .6rem .8rem; border: 1px solid var(--borda); border-radius: var(--raio-m); background: var(--fundo); }
    .link code { flex: 1; min-width: 0; overflow-wrap: anywhere; font-size: .9rem; }
    .abrir { font-weight: 600; font-size: .85rem; }
    .aviso-off { margin-top: .75rem; color: var(--aviso-texto); font-size: .9rem; }
    .dias { display: flex; flex-wrap: wrap; gap: .5rem; }
    .dia { display: inline-flex; align-items: center; gap: .4rem; margin: 0; padding: .4rem .8rem; border: 1px solid var(--borda-forte); border-radius: 999px; cursor: pointer; font-weight: 500; }
    .dia.marcado { background: var(--acento-claro); border-color: var(--acento-forte); color: var(--acento-texto); }
    .dia input { position: absolute; opacity: 0; width: 0; height: 0; }
    .dia:has(input:focus-visible) { outline: 2px solid var(--acento-forte); outline-offset: 2px; }
  `],
})
export class LojaComponent implements OnInit {
  private http = inject(HttpClient);
  readonly dias = DIAS;
  loja = signal<Loja | null>(null);
  erro = signal('');
  ok = signal('');
  salvando = signal(false);
  copiado = signal(false);

  ngOnInit() {
    this.http.get<Loja>(`${API}/loja`).subscribe({
      next: l => this.loja.set({ ...l, abertura: l.abertura.slice(0, 5), fechamento: l.fechamento.slice(0, 5) }),
      error: e => this.erro.set(mensagemErro(e)),
    });
  }

  linkPublico(l: Loja): string {
    return `${window.location.origin}/agendar/${l.slug}`;
  }

  copiar(l: Loja) {
    navigator.clipboard?.writeText(this.linkPublico(l)).then(() => {
      this.copiado.set(true);
      setTimeout(() => this.copiado.set(false), 2000);
    });
  }

  aberto(l: Loja, dia: number): boolean {
    return l.diasFuncionamento.includes(dia);
  }

  alternarDia(l: Loja, dia: number) {
    l.diasFuncionamento = this.aberto(l, dia) ? l.diasFuncionamento.filter(d => d !== dia) : [...l.diasFuncionamento, dia];
  }

  salvar() {
    const l = this.loja();
    if (!l) return;
    this.erro.set('');
    this.ok.set('');
    if (l.diasFuncionamento.length === 0) { this.erro.set('Marque ao menos um dia de atendimento.'); return; }
    this.salvando.set(true);
    this.http.put<Loja>(`${API}/loja`, l).subscribe({
      next: salva => {
        this.loja.set({ ...salva, abertura: salva.abertura.slice(0, 5), fechamento: salva.fechamento.slice(0, 5) });
        this.salvando.set(false);
        this.ok.set('Configurações salvas.');
      },
      error: e => { this.salvando.set(false); this.erro.set(mensagemErro(e)); },
    });
  }
}
