import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { mensagemErro } from '../../core/erro';
import { ComissaoResumo, Lancamento, ResumoFinanceiro } from '../../core/models';
import { API } from '../../core/api';
import { IconeComponent } from '../../shared/icone.component';
import { categoriaRotulo, tipoRotulo } from '../../core/rotulos';

const iso = (d: Date) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;

@Component({
  selector: 'app-financeiro',
  standalone: true,
  imports: [FormsModule, CurrencyPipe, DatePipe, IconeComponent],
  template: `
    <header class="topo">
      <div>
        <h1>Financeiro</h1>
        <p class="sub">Receitas, despesas e comissões do período.</p>
      </div>
      <div class="acoes">
        <input type="date" aria-label="De" [(ngModel)]="de" (ngModelChange)="carregar()">
        <span class="suave">até</span>
        <input type="date" aria-label="Até" [(ngModel)]="ate" (ngModelChange)="carregar()">
        <button class="primario" (click)="novo()"><app-icone nome="mais" /> Lançamento</button>
      </div>
    </header>

    @if (erro()) { <div class="erro" role="alert">{{ erro() }}</div> }

    @if (resumo(); as r) {
      <section class="card caixa" aria-label="Resumo do período">
        <div class="valor-caixa entrou"><span>Entrou</span><strong>{{ r.receitas | currency:'BRL' }}</strong></div>
        <div class="valor-caixa saiu"><span>Saiu</span><strong>{{ r.despesas | currency:'BRL' }}</strong></div>
        <div class="valor-caixa saldo"><span>Saldo do período</span><strong>{{ r.saldo | currency:'BRL' }}</strong></div>
      </section>
    }

    @if (form(); as f) {
      <form class="card" (ngSubmit)="salvar()">
        <h2>Novo lançamento</h2>
        <div class="form-grid">
          <div class="campo"><label for="fn-tipo">Tipo</label>
            <select id="fn-tipo" name="tipo" [(ngModel)]="f.tipo"><option value="RECEITA">Receita</option><option value="DESPESA">Despesa</option></select></div>
          <div class="campo"><label for="fn-cat">Categoria</label>
            <select id="fn-cat" name="cat" [(ngModel)]="f.categoria">
              <option value="OUTROS">Outros</option><option value="SERVICO">Serviço</option>
              <option value="COMPRA_ESTOQUE">Compra de estoque</option><option value="COMISSAO">Comissão</option>
            </select></div>
          <div class="campo"><label for="fn-desc">Descrição *</label><input id="fn-desc" name="desc" [(ngModel)]="f.descricao" required></div>
          <div class="campo"><label for="fn-valor">Valor (R$) *</label><input id="fn-valor" type="number" name="valor" min="0.01" step="0.01" [(ngModel)]="f.valor" required></div>
          <div class="campo"><label for="fn-data">Data *</label><input id="fn-data" type="date" name="data" [(ngModel)]="f.data" required></div>
        </div>
        <div class="form-acoes">
          <button class="primario">Salvar</button>
          <button type="button" (click)="form.set(null); erro.set('')">Cancelar</button>
        </div>
      </form>
    }

    <div class="grid cols-2">
      <section class="card">
        <h2>Comissões no período</h2>
        <ul class="comissoes">
          @for (c of comissoes(); track c.profissionalId; let n = $index) {
            <li>
              <span class="inicial" [attr.data-tom]="n % 4">{{ c.profissional.charAt(0).toUpperCase() }}</span>
              <strong>{{ c.profissional }}</strong>
              <span class="espaco"></span>
              <span class="soma">{{ c.total | currency:'BRL' }}</span>
            </li>
          } @empty { <li class="vazio">Sem comissões neste período.</li> }
        </ul>
      </section>
    </div>

    <div class="card tabela-wrap">
      <h2>Lançamentos</h2>
      <table>
        <thead><tr><th>Data</th><th>Tipo</th><th>Categoria</th><th>Descrição</th><th>Valor</th><th></th></tr></thead>
        <tbody>
          @for (l of lancamentos(); track l.id) {
            <tr>
              <td>{{ l.data | date:'dd/MM/yyyy' }}</td>
              <td><span [class]="'badge ' + l.tipo">{{ tipo(l.tipo) }}</span></td>
              <td>{{ categoria(l.categoria) }}</td><td>{{ l.descricao }}</td>
              <td [class]="'val ' + l.tipo">{{ l.valor | currency:'BRL' }}</td>
              <td class="acoes-celula"><button class="mini perigo" (click)="excluir(l)">Excluir</button></td>
            </tr>
          } @empty { <tr><td colspan="6" class="celula-vazia">
            <div class="estado-vazio">
              <span class="ico"><app-icone nome="financeiro" /></span>
              <strong>Nenhum lançamento neste período</strong>
              <p>Receitas dos atendimentos e despesas lançadas aparecem aqui.</p>
            </div>
          </td></tr> }
        </tbody>
      </table>
    </div>
  `,
  styles: [`
    /* o saldo é o destaque; entrou e saiu ficam em segundo plano */
    .caixa { display: grid; grid-template-columns: 1fr 1fr 1.4fr; gap: 0; padding: 0; overflow: hidden; margin-bottom: 1.25rem; }
    .valor-caixa { display: flex; flex-direction: column; gap: .2rem; padding: 1.5rem 1.75rem; }
    .valor-caixa + .valor-caixa { border-left: 1px solid var(--borda); }
    .valor-caixa span { color: var(--texto-suave); font-size: .875rem; font-weight: 500; }
    .valor-caixa strong { font-weight: 700; font-size: clamp(1.5rem, 2.4vw, 1.9rem); line-height: 1.1; letter-spacing: -.02em; font-variant-numeric: tabular-nums; }
    .entrou strong { color: var(--ok); }
    .saiu strong { color: var(--erro); }
    .saldo { background: var(--marinho); }
    .saldo span { color: var(--marinho-txt); }
    .saldo strong { color: #fff; font-size: clamp(1.75rem, 2.8vw, 2.25rem); }
    @media (max-width: 760px) { .caixa { grid-template-columns: 1fr; } .valor-caixa + .valor-caixa { border-left: 0; border-top: 1px solid var(--borda); } }

    .comissoes { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: .6rem; }
    .comissoes li { display: flex; align-items: center; gap: .85rem; padding: .6rem .8rem .6rem .6rem; border-radius: var(--raio-m); background: var(--superficie-2); }
    .comissoes .vazio { justify-content: center; }
    .soma { font-weight: 700; color: var(--texto); }
    .val { font-weight: 600; white-space: nowrap; }
    .val.RECEITA { color: var(--ok); }
    .val.DESPESA { color: var(--erro); }
  `],
})
export class FinanceiroComponent implements OnInit {
  readonly tipo = tipoRotulo;
  readonly categoria = categoriaRotulo;
  private http = inject(HttpClient);

  de = iso(new Date(new Date().getFullYear(), new Date().getMonth(), 1));
  ate = iso(new Date(new Date().getFullYear(), new Date().getMonth() + 1, 0));
  resumo = signal<ResumoFinanceiro | null>(null);
  comissoes = signal<ComissaoResumo[]>([]);
  lancamentos = signal<Lancamento[]>([]);
  form = signal<{ tipo: string; categoria: string; descricao: string; valor: number | null; data: string } | null>(null);
  erro = signal('');

  ngOnInit() { this.carregar(); }

  private params() { return new HttpParams().set('de', this.de).set('ate', this.ate); }

  carregar() {
    const params = this.params();
    const falha = (e: unknown) => this.erro.set(mensagemErro(e));
    this.http.get<ResumoFinanceiro>(`${API}/financeiro/resumo`, { params }).subscribe({ next: r => this.resumo.set(r), error: falha });
    this.http.get<ComissaoResumo[]>(`${API}/financeiro/comissoes`, { params }).subscribe({ next: r => this.comissoes.set(r), error: falha });
    this.http.get<Lancamento[]>(`${API}/financeiro/lancamentos`, { params }).subscribe({ next: r => this.lancamentos.set(r), error: falha });
  }

  novo() { this.form.set({ tipo: 'DESPESA', categoria: 'OUTROS', descricao: '', valor: null, data: iso(new Date()) }); }

  salvar() {
    const f = this.form();
    if (!f) return;
    this.erro.set('');
    this.http.post(`${API}/financeiro/lancamentos`, f).subscribe({
      next: () => { this.form.set(null); this.carregar(); },
      error: e => this.erro.set(mensagemErro(e)),
    });
  }

  excluir(l: Lancamento) {
    if (!window.confirm('Excluir este lançamento?')) return;
    this.http.delete(`${API}/financeiro/lancamentos/${l.id}`).subscribe({
      next: () => this.carregar(),
      error: e => this.erro.set(mensagemErro(e)),
    });
  }
}
