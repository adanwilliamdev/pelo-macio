import { CurrencyPipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/auth.service';
import { mensagemErro } from '../../core/erro';
import { Produto, Servico } from '../../core/models';
import { API } from '../../core/api';
import { IconeComponent } from '../../shared/icone.component';

interface FormServico {
  id?: number; nome: string; categoria: string; duracaoMinutos: number | null; preco: number | null; ativo: boolean;
  duracaoMedio: number | null; precoMedio: number | null; duracaoGrande: number | null; precoGrande: number | null;
  insumos: { produtoId: number | null; quantidade: number | null }[];
}

@Component({
  selector: 'app-servicos',
  standalone: true,
  imports: [FormsModule, CurrencyPipe, IconeComponent],
  template: `
    <header class="topo">
      <div>
        <h1>Serviços</h1>
        <p class="sub">{{ itens().length }} {{ itens().length === 1 ? 'serviço cadastrado' : 'serviços cadastrados' }}</p>
      </div>
      <div class="acoes">
        @if (auth.isAdmin()) { <button class="primario" (click)="novo()"><app-icone nome="mais" /> Novo serviço</button> }
      </div>
    </header>

    @if (erro()) { <div class="erro" role="alert">{{ erro() }}</div> }

    @if (form(); as f) {
      <form class="card" (ngSubmit)="salvar()">
        <h2>{{ f.id ? 'Editar' : 'Novo' }} serviço</h2>
        <div class="form-grid">
          <div class="campo"><label for="sv-nome">Nome *</label><input id="sv-nome" name="nome" [(ngModel)]="f.nome" required></div>
          <div class="campo"><label for="sv-cat">Categoria</label><input id="sv-cat" name="categoria" [(ngModel)]="f.categoria" placeholder="Banho, tosa, spa…"></div>
        </div>
        <h2>Preço e duração por porte</h2>
        <p class="suave dica">Pequeno é obrigatório e serve de base. Em médio e grande, deixe vazio para usar o mesmo valor do pequeno.</p>
        <div class="portes">
          <fieldset><legend>Pequeno *</legend>
            <label for="sv-dur">Duração (min)</label><input id="sv-dur" type="number" name="dur" min="5" step="5" [(ngModel)]="f.duracaoMinutos" required>
            <label for="sv-preco">Preço (R$)</label><input id="sv-preco" type="number" name="preco" min="0" step="0.01" [(ngModel)]="f.preco" required></fieldset>
          <fieldset><legend>Médio</legend>
            <label for="sv-durm">Duração (min)</label><input id="sv-durm" type="number" name="durm" min="5" step="5" [(ngModel)]="f.duracaoMedio">
            <label for="sv-precom">Preço (R$)</label><input id="sv-precom" type="number" name="precom" min="0" step="0.01" [(ngModel)]="f.precoMedio"></fieldset>
          <fieldset><legend>Grande</legend>
            <label for="sv-durg">Duração (min)</label><input id="sv-durg" type="number" name="durg" min="5" step="5" [(ngModel)]="f.duracaoGrande">
            <label for="sv-precog">Preço (R$)</label><input id="sv-precog" type="number" name="precog" min="0" step="0.01" [(ngModel)]="f.precoGrande"></fieldset>
        </div>
        <div class="campo"><label for="sv-ativo"><input id="sv-ativo" type="checkbox" name="ativo" [(ngModel)]="f.ativo"> Serviço ativo</label></div>

        <h2>Insumos usados</h2>
        <p class="suave dica">O estoque baixa sozinho quando o atendimento é concluído.</p>
        @for (i of f.insumos; track $index) {
          <div class="insumo">
            <select [name]="'prod' + $index" aria-label="Produto" [(ngModel)]="i.produtoId">
              <option [ngValue]="null">Produto…</option>
              @for (p of produtos(); track p.id) { <option [ngValue]="p.id">{{ p.nome }} ({{ p.unidade }})</option> }
            </select>
            <input type="number" [name]="'qtd' + $index" aria-label="Quantidade" min="0" step="0.001" placeholder="Qtd" [(ngModel)]="i.quantidade">
            <button type="button" class="mini perigo" (click)="removerInsumo($index)">Remover</button>
          </div>
        }
        <button type="button" class="mini" (click)="addInsumo()"><app-icone nome="mais" /> Adicionar insumo</button>

        <div class="form-acoes" style="margin-top:1.25rem">
          <button class="primario" [disabled]="salvando()">{{ salvando() ? 'Salvando…' : 'Salvar' }}</button>
          <button type="button" (click)="form.set(null); erro.set('')">Cancelar</button>
        </div>
      </form>
    }

    <div class="card tabela-wrap">
      <table>
        <thead><tr><th>Serviço</th><th>Categoria</th><th>Pequeno</th><th>Médio</th><th>Grande</th><th>Insumos</th><th></th></tr></thead>
        <tbody>
          @for (s of itens(); track s.id; let n = $index) {
            <tr>
              <td><div class="ident"><span class="inicial" [attr.data-tom]="n % 4">{{ s.nome.charAt(0).toUpperCase() }}</span><strong>{{ s.nome }}</strong></div></td>
              <td>{{ s.categoria }}</td>
              <td>{{ s.preco | currency:'BRL' }} <small class="suave">{{ s.duracaoMinutos }} min</small></td>
              <td>{{ (s.precoMedio ?? s.preco) | currency:'BRL' }} <small class="suave">{{ s.duracaoMedio ?? s.duracaoMinutos }} min</small></td>
              <td>{{ (s.precoGrande ?? s.preco) | currency:'BRL' }} <small class="suave">{{ s.duracaoGrande ?? s.duracaoMinutos }} min</small></td>
              <td>{{ s.insumos.length }}</td>
              <td class="acoes-celula">
                @if (auth.isAdmin()) {
                  <button class="mini" (click)="editar(s)">Editar</button>
                  <button class="mini perigo" (click)="excluir(s)">Excluir</button>
                }
              </td>
            </tr>
          } @empty { <tr><td colspan="7">Nenhum serviço cadastrado ainda.@if (auth.isAdmin()) { Use “Novo serviço” para começar. }</td></tr> }
        </tbody>
      </table>
    </div>
  `,
  styles: [`
    .dica { margin: -.6rem 0 1rem; }
    .portes { display: grid; grid-template-columns: repeat(auto-fit, minmax(10rem, 1fr)); gap: 1rem; margin-bottom: 1.25rem; }
    fieldset { margin: 0; padding: .9rem 1rem 1rem; border: 1px solid var(--borda); border-radius: var(--raio-m); }
    legend { padding: 0 .4rem; font-weight: 600; font-size: .85rem; }
    fieldset input { margin-bottom: .7rem; }
    .insumo { display: grid; grid-template-columns: 2fr 1fr auto; gap: .6rem; align-items: center; margin-bottom: .6rem; }
    @media (max-width: 640px) { .insumo { grid-template-columns: 1fr 1fr; } .insumo select { grid-column: 1 / -1; } }
  `],
})
export class ServicosComponent implements OnInit {
  private http = inject(HttpClient);
  readonly auth = inject(AuthService);

  itens = signal<Servico[]>([]);
  produtos = signal<Produto[]>([]);
  form = signal<FormServico | null>(null);
  erro = signal('');
  salvando = signal(false);

  ngOnInit() {
    this.carregar();
    this.http.get<Produto[]>(`${API}/produtos`).subscribe(l => this.produtos.set(l));
  }

  carregar() {
    this.http.get<Servico[]>(`${API}/servicos`).subscribe({
      next: l => this.itens.set(l),
      error: e => this.erro.set(mensagemErro(e)),
    });
  }

  novo() {
    this.form.set({
      nome: '', categoria: '', duracaoMinutos: 60, preco: null, ativo: true, insumos: [],
      duracaoMedio: null, precoMedio: null, duracaoGrande: null, precoGrande: null,
    });
  }

  editar(s: Servico) {
    this.form.set({
      id: s.id, nome: s.nome, categoria: s.categoria ?? '', duracaoMinutos: s.duracaoMinutos, preco: s.preco,
      ativo: s.ativo !== false,
      duracaoMedio: s.duracaoMedio ?? null, precoMedio: s.precoMedio ?? null,
      duracaoGrande: s.duracaoGrande ?? null, precoGrande: s.precoGrande ?? null,
      insumos: s.insumos.map(i => ({ produtoId: i.produto.id, quantidade: i.quantidade })),
    });
  }

  addInsumo() { this.form.update(f => f && { ...f, insumos: [...f.insumos, { produtoId: null, quantidade: null }] }); }
  removerInsumo(i: number) { this.form.update(f => f && { ...f, insumos: f.insumos.filter((_, idx) => idx !== i) }); }

  salvar() {
    const f = this.form();
    if (!f) return;
    const corpo = {
      nome: f.nome, categoria: f.categoria, duracaoMinutos: f.duracaoMinutos, preco: f.preco, ativo: f.ativo,
      duracaoMedio: f.duracaoMedio, precoMedio: f.precoMedio, duracaoGrande: f.duracaoGrande, precoGrande: f.precoGrande,
      insumos: f.insumos
        .filter(i => i.produtoId && i.quantidade)
        .map(i => ({ produto: { id: i.produtoId }, quantidade: i.quantidade })),
    };
    this.salvando.set(true);
    this.erro.set('');
    const req = f.id ? this.http.put(`${API}/servicos/${f.id}`, corpo) : this.http.post(`${API}/servicos`, corpo);
    req.subscribe({
      next: () => { this.salvando.set(false); this.form.set(null); this.carregar(); },
      error: e => { this.salvando.set(false); this.erro.set(mensagemErro(e)); },
    });
  }

  excluir(s: Servico) {
    if (!window.confirm(`Excluir o serviço "${s.nome}"?`)) return;
    this.http.delete(`${API}/servicos/${s.id}`).subscribe({
      next: () => this.carregar(),
      error: e => this.erro.set(mensagemErro(e)),
    });
  }
}
