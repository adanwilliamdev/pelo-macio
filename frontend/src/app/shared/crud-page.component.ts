import { Component, inject, OnInit, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../core/auth.service';
import { mensagemErro } from '../core/erro';
import { Campo, CrudConfig } from './crud-configs';
import { IconeComponent } from './icone.component';

type Opcao = { valor: string | number; rotulo: string };

/** Página de CRUD dirigida por configuração (lista + formulário). A config vem de route.data. */
@Component({
  selector: 'app-crud-page',
  standalone: true,
  imports: [FormsModule, IconeComponent],
  template: `
    <header class="topo">
      <div>
        <h1>{{ cfg.titulo }}</h1>
        <p class="sub">{{ itens().length }} {{ itens().length === 1 ? 'registro' : 'registros' }}</p>
      </div>
      <div class="acoes">
        @if (cfg.busca) {
          <label class="busca">
            <app-icone nome="busca" />
            <input placeholder="Buscar por nome" aria-label="Buscar" [(ngModel)]="busca" (ngModelChange)="carregar()">
          </label>
        }
        @if (podeEscrever()) {
          <button class="primario" (click)="novo()"><app-icone nome="mais" /> {{ cfg.feminino ? 'Nova' : 'Novo' }} {{ cfg.singular }}</button>
        }
      </div>
    </header>

    @if (erro()) { <div class="erro" role="alert">{{ erro() }}</div> }

    @if (form(); as f) {
      <form class="card" (ngSubmit)="salvar()">
        <h2>{{ editandoId() ? 'Editar' : (cfg.feminino ? 'Nova' : 'Novo') }} {{ cfg.singular }}</h2>
        <div class="form-grid">
          @for (c of cfg.campos; track c.chave) {
            <div class="campo" [style.grid-column]="c.tipo === 'textarea' ? '1 / -1' : null">
              @if (c.tipo === 'checkbox') {
                <label [for]="c.chave"><input type="checkbox" [id]="c.chave" [name]="c.chave" [(ngModel)]="f[c.chave]"> {{ c.rotulo }}</label>
              } @else {
                <label [for]="c.chave">{{ c.rotulo }}{{ c.obrigatorio ? ' *' : '' }}</label>
                @switch (c.tipo) {
                  @case ('textarea') {
                    <textarea [id]="c.chave" [name]="c.chave" rows="3" [(ngModel)]="f[c.chave]"></textarea>
                  }
                  @case ('select') {
                    <select [id]="c.chave" [name]="c.chave" [(ngModel)]="f[c.chave]" [required]="!!c.obrigatorio">
                      <option [ngValue]="null">Selecione…</option>
                      @for (o of opcoesDe(c); track o.valor) { <option [ngValue]="o.valor">{{ o.rotulo }}</option> }
                    </select>
                  }
                  @default {
                    <input [type]="c.tipo" [id]="c.chave" [name]="c.chave" [attr.step]="c.passo ?? null"
                           [(ngModel)]="f[c.chave]" [required]="!!c.obrigatorio">
                  }
                }
              }
            </div>
          }
        </div>
        <div class="form-acoes">
          <button class="primario" [disabled]="salvando()">{{ salvando() ? 'Salvando…' : 'Salvar' }}</button>
          <button type="button" (click)="cancelar()">Cancelar</button>
        </div>
      </form>
    }

    <div class="card tabela-wrap">
      <table>
        <thead>
          <tr>
            @for (col of cfg.colunas; track col.rotulo) { <th>{{ col.rotulo }}</th> }
            <th><span class="so-leitor">Ações</span></th>
          </tr>
        </thead>
        <tbody>
          @for (item of itens(); track item.id; let n = $index) {
            <tr>
              @for (col of cfg.colunas; track col.rotulo; let i = $index) {
                <td>
                  @if (i === 0) {
                    <div class="ident">
                      <span class="inicial" [attr.data-tom]="n % 4">{{ inicial(col.valor(item)) }}</span>
                      <strong>{{ col.valor(item) }}</strong>
                    </div>
                  } @else { {{ col.valor(item) }} }
                </td>
              }
              <td class="acoes-celula">
                @for (a of cfg.acoes ?? []; track a.rotulo) {
                  @if (podeEscrever()) { <button class="mini" (click)="acao(a, item)">{{ a.rotulo }}</button> }
                }
                @if (podeEscrever()) { <button class="mini" (click)="editar(item)">Editar</button> }
                @if (auth.isAdmin()) { <button class="mini perigo" (click)="excluir(item)">Excluir</button> }
              </td>
            </tr>
          } @empty {
            <tr><td [attr.colspan]="cfg.colunas.length + 1" class="celula-vazia">
              <div class="estado-vazio">
                <span class="ico"><app-icone nome="caixa-vazia" /></span>
                <strong>Nenhum registro por aqui ainda</strong>
                @if (podeEscrever()) { <p>Use “{{ cfg.feminino ? 'Nova' : 'Novo' }} {{ cfg.singular }}” para começar.</p> }
              </div>
            </td></tr>
          }
        </tbody>
      </table>
    </div>
  `,
  styles: [`
    .busca { position: relative; display: block; margin: 0; }
    .busca app-icone { position: absolute; left: .85rem; top: 50%; transform: translateY(-50%); color: var(--texto-suave); pointer-events: none; }
    .busca input { padding-left: 2.5rem; min-width: 14rem; }
    .so-leitor { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0 0 0 0); }
  `],
})
export class CrudPageComponent implements OnInit {
  private http = inject(HttpClient);
  readonly auth = inject(AuthService);
  readonly cfg: CrudConfig = inject(ActivatedRoute).snapshot.data['config'];

  itens = signal<any[]>([]);
  form = signal<Record<string, any> | null>(null);
  editandoId = signal<number | null>(null);
  erro = signal('');
  salvando = signal(false);
  busca = '';
  private opcoesRemotas = signal<Record<string, Opcao[]>>({});

  ngOnInit() {
    this.carregar();
    for (const c of this.cfg.campos.filter(c => c.origem)) {
      this.http.get<any[]>(c.origem!.url).subscribe(lista =>
        this.opcoesRemotas.update(o => ({
          ...o, [c.chave]: lista.map(x => ({ valor: x.id, rotulo: x[c.origem!.rotulo] })),
        })));
    }
  }

  inicial(v: unknown): string { return (String(v ?? '').trim().charAt(0) || '•').toUpperCase(); }

  podeEscrever() { return !this.cfg.escritaSomenteAdmin || this.auth.isAdmin(); }

  opcoesDe(c: Campo): Opcao[] {
    return c.opcoes ?? this.opcoesRemotas()[c.chave] ?? [];
  }

  carregar() {
    let params = new HttpParams();
    if (this.cfg.busca && this.busca.trim()) params = params.set('busca', this.busca.trim());
    this.http.get<any[]>(this.cfg.url, { params }).subscribe({
      next: l => this.itens.set(l),
      error: e => this.erro.set(mensagemErro(e)),
    });
  }

  novo() {
    const f: Record<string, any> = {};
    for (const c of this.cfg.campos) f[c.chave] = c.padrao ?? (c.tipo === 'select' ? null : '');
    this.editandoId.set(null);
    this.form.set(f);
  }

  editar(item: any) {
    const f: Record<string, any> = {};
    for (const c of this.cfg.campos) {
      f[c.chave] = c.origem ? (item[c.chave]?.id ?? null) : (item[c.chave] ?? (c.tipo === 'select' ? null : ''));
    }
    this.editandoId.set(item.id);
    this.form.set(f);
  }

  cancelar() { this.form.set(null); this.erro.set(''); }

  salvar() {
    const f = this.form();
    if (!f) return;
    const corpo: Record<string, any> = { ...f };
    for (const c of this.cfg.campos) {
      if (c.origem) corpo[c.chave] = f[c.chave] ? { id: f[c.chave] } : null;
      else if (c.tipo === 'number' && corpo[c.chave] === '') corpo[c.chave] = null;
      else if (c.tipo === 'select' && corpo[c.chave] === '') corpo[c.chave] = null;
    }
    const id = this.editandoId();
    this.salvando.set(true);
    this.erro.set('');
    const req = id ? this.http.put(`${this.cfg.url}/${id}`, corpo) : this.http.post(this.cfg.url, corpo);
    req.subscribe({
      next: () => { this.salvando.set(false); this.form.set(null); this.carregar(); },
      error: e => { this.salvando.set(false); this.erro.set(mensagemErro(e)); },
    });
  }

  excluir(item: any) {
    if (!window.confirm(`Excluir ${this.cfg.feminino ? 'esta' : 'este'} ${this.cfg.singular}?`)) return;
    this.http.delete(`${this.cfg.url}/${item.id}`).subscribe({
      next: () => this.carregar(),
      error: e => this.erro.set(mensagemErro(e)),
    });
  }

  acao(a: NonNullable<CrudConfig['acoes']>[number], item: any) {
    this.erro.set('');
    a.executar(item, { http: this.http, recarregar: () => this.carregar(), erro: e => this.erro.set(mensagemErro(e)) });
  }
}
