import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { Dashboard, Produto } from '../../core/models';
import { mensagemErro } from '../../core/erro';
import { API } from '../../core/api';
import { IconeComponent } from '../../shared/icone.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CurrencyPipe, DatePipe, RouterLink, IconeComponent],
  template: `
    <header class="topo">
      <div>
        <h1>{{ saudacao }}, {{ primeiroNome }}</h1>
        <p class="sub">{{ resumoDoDia() }}</p>
      </div>
      <div class="acoes"><a class="atalho" routerLink="/agenda"><app-icone nome="agenda" /> Abrir agenda</a></div>
    </header>

    @if (erro()) { <div class="erro" role="alert">{{ erro() }}</div> }

    @if (d(); as d) {
      <section class="metricas" aria-label="Resumo">
        <article class="metrica">
          <div class="cabeca">
            <div>
              <p class="rotulo">Atendimentos hoje</p>
              <h3 class="valor">{{ d.agendamentosHoje }}</h3>
            </div>
            <span class="selo"><app-icone nome="agenda" /></span>
          </div>
          <p class="rodape">{{ d.agendamentosHoje === 0 ? 'Agenda livre hoje' : 'Marcados para hoje' }}</p>
        </article>

        <article class="metrica">
          <div class="cabeca">
            <div>
              <p class="rotulo">Tutores ativos</p>
              <h3 class="valor">{{ d.clientesAtivos }}</h3>
            </div>
            <span class="selo"><app-icone nome="clientes" /></span>
          </div>
          <p class="rodape">{{ d.clientesAtivos === 1 ? 'tutor cadastrado' : 'tutores cadastrados' }}</p>
        </article>

        @if (d.receitaMes !== null) {
          <article class="metrica">
            <div class="cabeca">
              <div>
                <p class="rotulo">Recebido neste mês</p>
                <h3 class="valor">{{ d.receitaMes | currency:'BRL' }}</h3>
              </div>
              <span class="selo"><app-icone nome="financeiro" /></span>
            </div>
            <div class="rodape tendencia">
              @if (variacao(d.receitaSerie); as v) {
                <span class="variacao" [class.negativa]="v.pct < 0">
                  <app-icone [nome]="v.pct < 0 ? 'baixa' : 'alta'" />
                  {{ v.pct > 0 ? '+' : '' }}{{ v.pct }}%
                </span>
                <span>vs. mês anterior</span>
              } @else {
                <span>Sem mês anterior para comparar</span>
              }
              @if (pontos(d.receitaSerie); as p) {
                <svg class="spark" viewBox="0 0 100 32" preserveAspectRatio="none" role="img" aria-label="Receita dos últimos 6 meses">
                  <polyline [attr.points]="p" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" vector-effect="non-scaling-stroke"/>
                </svg>
              }
            </div>
          </article>
        }
      </section>

      <div class="grid cols-2">
        <section class="card">
          <h2>Próximos atendimentos</h2>
          <ol class="fila">
            @for (a of d.proximos; track a.id) {
              <li>
                <time [attr.datetime]="a.dataHoraInicio">
                  <strong>{{ a.dataHoraInicio | date:'HH:mm' }}</strong>
                  <small>{{ a.dataHoraInicio | date:'EEE dd/MM' }}</small>
                </time>
                <div>
                  <strong>{{ a.pet.nome }} <span class="suave">· {{ a.cliente.nome }}</span></strong>
                  <span class="suave">{{ a.servico.nome }} com {{ a.profissional.nome }}</span>
                </div>
              </li>
            } @empty {
              <li class="estado-vazio">
                <span class="ico"><app-icone nome="agenda" /></span>
                <strong>Nada marcado por enquanto</strong>
                <p>Quando houver atendimentos, eles aparecem aqui.</p>
                <a routerLink="/agenda">Agendar um atendimento</a>
              </li>
            }
          </ol>
        </section>

        <section class="card">
          <h2>Estoque pedindo reposição</h2>
          <ul class="niveis">
            @for (p of d.estoqueBaixo; track p.id) {
              <li>
                <div class="linha">
                  <strong>{{ p.nome }}</strong>
                  <span class="espaco"></span>
                  <span class="badge baixo">{{ p.quantidadeEstoque }} {{ p.unidade }}</span>
                </div>
                <div class="barra" role="img" [attr.aria-label]="'Nível em ' + nivel(p) + '% do mínimo'">
                  <i [style.width.%]="nivel(p)"></i>
                </div>
              </li>
            } @empty {
              <li class="estado-vazio">
                <span class="ico"><app-icone nome="estoque" /></span>
                <strong>Tudo em dia</strong>
                <p>Nenhum produto abaixo do estoque mínimo.</p>
              </li>
            }
          </ul>
        </section>
      </div>
    }
  `,
  styles: [`
    .atalho {
      display: inline-flex; align-items: center; gap: .5rem; padding: .55rem 1rem; border-radius: var(--raio-m);
      font-weight: 600; text-decoration: none; color: var(--marinho); background: var(--acento);
      border: 1px solid var(--acento); box-shadow: var(--sombra); transition: background .15s, color .15s;
    }
    .atalho:hover { background: var(--acento-forte); border-color: var(--acento-forte); color: #fff; }

    /* cards de resumo */
    .metricas { display: grid; grid-template-columns: repeat(auto-fit, minmax(min(100%, 250px), 1fr)); gap: 1.25rem; margin-bottom: 1.25rem; }
    .metrica { background: var(--superficie); border: 1px solid var(--borda); border-radius: var(--raio-g); box-shadow: var(--sombra); padding: 1.35rem 1.5rem; display: flex; flex-direction: column; gap: 1rem; }
    .cabeca { display: flex; align-items: flex-start; justify-content: space-between; gap: 1rem; }
    .rotulo { font-size: .875rem; font-weight: 500; color: var(--texto-suave); }
    .valor { margin-top: .25rem; font-size: clamp(1.75rem, 2.6vw, 2rem); font-weight: 700; line-height: 1.1; letter-spacing: -.02em; white-space: nowrap; font-variant-numeric: tabular-nums; }
    .selo { flex: none; display: grid; place-items: center; width: 2.75rem; height: 2.75rem; border-radius: var(--raio-m); background: var(--superficie-2); color: var(--marinho-3); font-size: 1.1rem; }
    .rodape { display: flex; align-items: center; gap: .5rem; font-size: .82rem; color: var(--texto-suave); }
    .tendencia { flex-wrap: wrap; }
    .variacao { display: inline-flex; align-items: center; gap: .25rem; padding: .1rem .5rem; border-radius: 999px; font-weight: 600; color: var(--ok); background: var(--ok-claro); }
    .variacao.negativa { color: var(--erro); background: var(--erro-claro); }
    .variacao app-icone { font-size: .85rem; }
    .spark { margin-left: auto; width: 5.5rem; height: 1.75rem; color: var(--acento-forte); }

    /* fila do dia: a hora é a informação principal */
    .fila { list-style: none; margin: 0; padding: 0; }
    .fila li { display: grid; grid-template-columns: 4.6rem 1fr; gap: 1rem; align-items: baseline; padding: .8rem 0; border-top: 1px solid #f1f5f9; }
    .fila li:first-child { border-top: 0; padding-top: 0; }
    .fila time { display: flex; flex-direction: column; line-height: 1.2; }
    .fila time strong { font-size: 1.2rem; font-weight: 700; font-variant-numeric: tabular-nums; }
    .fila time small { color: var(--texto-suave); font-size: .75rem; }
    .fila div { display: flex; flex-direction: column; }
    .fila li.estado-vazio { display: flex; align-items: center; border: 0; padding: 1.5rem 1rem; }

    .niveis { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 1.2rem; }
    .barra { height: .5rem; margin-top: .5rem; border-radius: 999px; background: var(--superficie-2); overflow: hidden; }
    .barra i { display: block; height: 100%; border-radius: 999px; background: var(--acento); }
    .niveis li.estado-vazio { padding: 1.5rem 1rem; }
  `],
})
export class DashboardComponent {
  private auth = inject(AuthService);
  d = signal<Dashboard | null>(null);
  erro = signal('');

  readonly primeiroNome = (this.auth.sessao()?.nome ?? '').trim().split(/\s+/)[0] || 'bem-vindo';
  readonly saudacao = (() => {
    const h = new Date().getHours();
    return h < 12 ? 'Bom dia' : h < 18 ? 'Boa tarde' : 'Boa noite';
  })();

  constructor() {
    inject(HttpClient).get<Dashboard>(`${API}/dashboard`).subscribe({
      next: r => this.d.set(r),
      error: e => this.erro.set(mensagemErro(e)),
    });
  }

  resumoDoDia(): string {
    const n = this.d()?.agendamentosHoje;
    if (n === undefined) return 'Preparando o seu dia…';
    if (n === 0) return 'Nenhum atendimento marcado para hoje.';
    return n === 1 ? 'Você tem 1 atendimento marcado para hoje.' : `Você tem ${n} atendimentos marcados para hoje.`;
  }

  /** Quanto resta do estoque mínimo, em %, limitado a 0-100. */
  nivel(p: Produto): number {
    if (!p.estoqueMinimo) return 0;
    return Math.max(4, Math.min(100, Math.round((p.quantidadeEstoque / p.estoqueMinimo) * 100)));
  }

  /** Variação % do último mês contra o anterior; nula quando não há base de comparação. */
  variacao(serie: number[] | null): { pct: number } | null {
    if (!serie || serie.length < 2) return null;
    const atual = serie[serie.length - 1];
    const anterior = serie[serie.length - 2];
    if (!anterior) return null;
    return { pct: Math.round(((atual - anterior) / anterior) * 100) };
  }

  /** Pontos do sparkline (viewBox 100×32); nulo se não há dados ou todos são zero. */
  pontos(serie: number[] | null): string | null {
    if (!serie || serie.length < 2) return null;
    const max = Math.max(...serie);
    const min = Math.min(...serie);
    if (max === 0) return null;
    const faixa = max - min || 1;
    return serie
      .map((v, i) => `${((i / (serie.length - 1)) * 100).toFixed(1)},${(30 - ((v - min) / faixa) * 28).toFixed(1)}`)
      .join(' ');
  }
}
