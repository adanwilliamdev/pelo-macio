import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { API } from '../../core/api';
import { mensagemErro } from '../../core/erro';
import {
  Especie, HorarioLivre, LojaPublica, Porte, ProfissionalPublico, ReservaConfirmada, ReservaPublica, ServicoPublico,
} from '../../core/models';
import { IconeComponent } from '../../shared/icone.component';
import { LogoComponent } from '../../shared/logo.component';

type Passo = 1 | 2 | 3 | 4 | 5;

const PORTES: { valor: Porte; rotulo: string; dica: string }[] = [
  { valor: 'PEQUENO', rotulo: 'Pequeno', dica: 'até 10 kg' },
  { valor: 'MEDIO', rotulo: 'Médio', dica: '10 a 25 kg' },
  { valor: 'GRANDE', rotulo: 'Grande', dica: 'acima de 25 kg' },
];
const ESPECIES: { valor: Especie; rotulo: string }[] = [
  { valor: 'CACHORRO', rotulo: 'Cachorro' }, { valor: 'GATO', rotulo: 'Gato' }, { valor: 'OUTRO', rotulo: 'Outro' },
];

function iso(d: Date): string {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

/** Página pública: o tutor agenda sem login. A loja vem do endereço (/agendar/:slug). */
@Component({
  selector: 'app-agendar',
  standalone: true,
  imports: [FormsModule, CurrencyPipe, DatePipe, IconeComponent, LogoComponent],
  template: `
    <div class="pagina">
      <header class="cabecalho">
        <app-logo [tamanho]="34" />
        @if (loja(); as l) { <span class="nome-loja">{{ l.nome }}</span> }
      </header>

      <main class="conteudo">
        @if (naoEncontrada()) {
          <section class="card centro">
            <span class="ico"><app-icone nome="loja" /></span>
            <h1>Loja não encontrada</h1>
            <p class="suave">Confira o link ou fale com o pet shop. Pode ser que o agendamento online esteja desligado.</p>
          </section>
        } @else if (loja()) {
          @let l = loja()!;
          @if (passo() < 5) {
            <h1>Agende o banho e tosa do seu pet</h1>
            <ol class="passos" aria-label="Etapas">
              @for (p of etapas; track p.n) {
                <li [class.atual]="passo() === p.n" [class.feito]="passo() > p.n"><span>{{ passo() > p.n ? '✓' : p.n }}</span> {{ p.rotulo }}</li>
              }
            </ol>
          }

          @if (erro()) { <div class="erro" role="alert">{{ erro() }}</div> }

          @switch (passo()) {
            @case (1) {
              <form class="card" (ngSubmit)="irPara(2)">
                <h2>Sobre o seu pet</h2>
                <div class="campo"><label for="p-nome">Nome do pet *</label>
                  <input id="p-nome" name="petNome" [(ngModel)]="petNome" required maxlength="80" placeholder="Ex.: Thor"></div>
                <div class="form-grid">
                  <div class="campo"><label for="p-esp">Espécie *</label>
                    <select id="p-esp" name="especie" [(ngModel)]="especie">
                      @for (e of especies; track e.valor) { <option [ngValue]="e.valor">{{ e.rotulo }}</option> }
                    </select></div>
                  <div class="campo"><label for="p-raca">Raça</label>
                    <input id="p-raca" name="raca" [(ngModel)]="raca" maxlength="80" placeholder="Opcional"></div>
                </div>
                <div class="campo">
                  <label>Porte *</label>
                  <div class="portes" role="radiogroup" aria-label="Porte do pet">
                    @for (p of portes; track p.valor) {
                      <label class="porte" [class.marcado]="porte() === p.valor">
                        <input type="radio" name="porte" [value]="p.valor" [checked]="porte() === p.valor" (change)="porte.set(p.valor)">
                        <strong>{{ p.rotulo }}</strong><small>{{ p.dica }}</small>
                      </label>
                    }
                  </div>
                </div>
                <div class="form-acoes"><button class="primario" [disabled]="!petNome.trim()">Continuar</button></div>
              </form>
            }

            @case (2) {
              <section class="card">
                <h2>Qual serviço?</h2>
                <ul class="servicos">
                  @for (s of servicos(); track s.id) {
                    <li>
                      <button type="button" class="servico" [class.marcado]="servico()?.id === s.id" (click)="escolherServico(s)">
                        <span><strong>{{ s.nome }}</strong><small>{{ duracao(s) }} min</small></span>
                        <b>{{ preco(s) | currency:'BRL' }}</b>
                      </button>
                    </li>
                  } @empty { <li class="suave">Esta loja ainda não cadastrou serviços.</li> }
                </ul>
                <div class="form-acoes"><button type="button" (click)="irPara(1)">Voltar</button></div>
              </section>
            }

            @case (3) {
              <section class="card">
                <h2>Quando?</h2>
                <div class="form-grid">
                  <div class="campo"><label for="h-data">Data</label>
                    <input id="h-data" type="date" [min]="hojeIso" [max]="limiteIso" [ngModel]="data()" (ngModelChange)="mudarData($event)"></div>
                  <div class="campo"><label for="h-prof">Profissional</label>
                    <select id="h-prof" [ngModel]="profissionalId()" (ngModelChange)="mudarProfissional($event)">
                      <option [ngValue]="null">Qualquer um</option>
                      @for (p of profissionais(); track p.id) { <option [ngValue]="p.id">{{ p.nome }}</option> }
                    </select></div>
                </div>

                @if (carregandoHorarios()) {
                  <p class="suave">Buscando horários…</p>
                } @else if (horarios().length === 0) {
                  <div class="estado-vazio">
                    <span class="ico"><app-icone nome="agenda" /></span>
                    <strong>Sem horários nessa data</strong>
                    <p>Tente outro dia{{ fechadoNaData() ? ' (a loja não atende nesse dia da semana)' : '' }}.</p>
                  </div>
                } @else {
                  <div class="slots" role="listbox" aria-label="Horários disponíveis">
                    @for (h of horarios(); track h.inicio) {
                      <button type="button" class="slot" [class.marcado]="horario()?.inicio === h.inicio" (click)="horario.set(h)">{{ h.inicio | date:'HH:mm' }}</button>
                    }
                  </div>
                }
                <div class="form-acoes">
                  <button type="button" (click)="irPara(2)">Voltar</button>
                  <button type="button" class="primario" [disabled]="!horario()" (click)="irPara(4)">Continuar</button>
                </div>
              </section>
            }

            @case (4) {
              <form class="card" (ngSubmit)="confirmar()">
                <h2>Seus dados</h2>
                <div class="resumo">
                  <div><small>Pet</small><strong>{{ petNome }} ({{ porteRotulo() }})</strong></div>
                  <div><small>Serviço</small><strong>{{ servico()?.nome }}</strong></div>
                  <div><small>Horário</small><strong>{{ horario()?.inicio | date:"EEE, dd/MM 'às' HH:mm" }}</strong></div>
                  <div><small>Valor</small><strong>{{ servico() ? (preco(servico()!) | currency:'BRL') : '' }}</strong></div>
                </div>
                <div class="campo"><label for="t-nome">Seu nome *</label>
                  <input id="t-nome" name="nome" [(ngModel)]="nome" required maxlength="120" autocomplete="name"></div>
                <div class="form-grid">
                  <div class="campo"><label for="t-tel">WhatsApp *</label>
                    <input id="t-tel" name="telefone" type="tel" [(ngModel)]="telefone" required maxlength="30" autocomplete="tel" placeholder="(11) 98888-7777"></div>
                  <div class="campo"><label for="t-email">E-mail</label>
                    <input id="t-email" name="email" type="email" [(ngModel)]="email" maxlength="160" autocomplete="email" placeholder="Opcional"></div>
                </div>
                <div class="form-acoes">
                  <button type="button" (click)="irPara(3)">Voltar</button>
                  <button class="primario" [disabled]="enviando() || !nome.trim() || !telefone.trim()">{{ enviando() ? 'Confirmando…' : 'Confirmar agendamento' }}</button>
                </div>
              </form>
            }

            @case (5) {
              @if (confirmacao(); as c) {
                <section class="card centro">
                  <span class="ico ok"><app-icone nome="check" /></span>
                  <h1>Agendado!</h1>
                  <p>{{ c.pet }} está marcado para <strong>{{ c.servico }}</strong> em
                    <strong>{{ c.inicio | date:"EEEE, dd/MM 'às' HH:mm" }}</strong>, com {{ c.profissional }}.</p>
                  <p class="valor">{{ c.valor | currency:'BRL' }} <span class="suave">pagos na loja</span></p>
                  <p class="suave">Protocolo nº {{ c.protocolo }}.@if (l.telefone) { Para remarcar ou cancelar, fale com a loja: {{ l.telefone }}. }</p>
                  <div class="form-acoes centro-acoes"><button type="button" (click)="novoAgendamento()">Agendar outro pet</button></div>
                </section>
              }
            }
          }
        } @else {
          <p class="suave centro-texto">Carregando…</p>
        }
      </main>
    </div>
  `,
  styles: [`
    .pagina { min-height: 100vh; background: var(--fundo); }
    .cabecalho { display: flex; align-items: center; justify-content: space-between; gap: 1rem; padding: 1rem 1.5rem; background: var(--marinho); color: #fff; }
    .nome-loja { font-weight: 600; color: var(--marinho-txt); text-align: right; }
    .conteudo { max-width: 40rem; margin: 0 auto; padding: 2rem 1rem 3rem; }
    h1 { margin-bottom: 1.25rem; }
    .card + .card { margin-top: 1.25rem; }

    .passos { list-style: none; display: flex; gap: .35rem; margin: 0 0 1.5rem; padding: 0; font-size: .8rem; color: var(--texto-suave); }
    .passos li { flex: 1; display: flex; align-items: center; gap: .4rem; padding-bottom: .5rem; border-bottom: 3px solid var(--borda); }
    .passos li span { display: grid; place-items: center; width: 1.4rem; height: 1.4rem; border-radius: 50%; background: var(--superficie-3); font-weight: 600; font-size: .75rem; }
    .passos li.atual { color: var(--texto); font-weight: 600; border-color: var(--acento-forte); }
    .passos li.atual span, .passos li.feito span { background: var(--acento); color: var(--marinho); }
    .passos li.feito { border-color: var(--acento); }
    @media (max-width: 520px) { .passos li { font-size: 0; gap: 0; justify-content: center; } .passos li span { font-size: .75rem; } }

    .portes { display: grid; grid-template-columns: repeat(3, 1fr); gap: .6rem; }
    .porte { position: relative; display: flex; flex-direction: column; align-items: center; gap: .1rem; margin: 0; padding: .8rem .5rem; text-align: center; border: 1px solid var(--borda-forte); border-radius: var(--raio-m); cursor: pointer; }
    .porte input { position: absolute; opacity: 0; }
    .porte small { color: var(--texto-suave); font-weight: 400; }
    .porte.marcado { border-color: var(--acento-forte); background: var(--acento-bruma); box-shadow: 0 0 0 1px var(--acento-forte); }
    .porte:has(input:focus-visible) { outline: 2px solid var(--acento-forte); outline-offset: 2px; }

    .servicos { list-style: none; margin: 0 0 1rem; padding: 0; display: grid; gap: .6rem; }
    .servico { width: 100%; justify-content: space-between; text-align: left; padding: .9rem 1rem; box-shadow: none; }
    .servico span { display: flex; flex-direction: column; }
    .servico small { color: var(--texto-suave); font-weight: 400; }
    .servico.marcado { border-color: var(--acento-forte); background: var(--acento-bruma); }

    .slots { display: grid; grid-template-columns: repeat(auto-fill, minmax(5rem, 1fr)); gap: .5rem; margin: .5rem 0 1rem; }
    .slot { font-variant-numeric: tabular-nums; box-shadow: none; }
    .slot.marcado { background: var(--acento); border-color: var(--acento); font-weight: 700; }

    .resumo { display: grid; grid-template-columns: repeat(auto-fit, minmax(9rem, 1fr)); gap: .75rem; margin-bottom: 1.25rem; padding: 1rem; border-radius: var(--raio-m); background: var(--fundo); border: 1px solid var(--borda); }
    .resumo div { display: flex; flex-direction: column; }
    .resumo small { color: var(--texto-suave); }

    .centro { text-align: center; display: flex; flex-direction: column; align-items: center; gap: .6rem; padding: 2.5rem 1.5rem; }
    .centro .ico { display: grid; place-items: center; width: 3.5rem; height: 3.5rem; border-radius: 50%; background: var(--superficie-2); color: #94a3b8; font-size: 1.6rem; }
    .centro .ico.ok { background: var(--acento-claro); color: var(--acento-texto); }
    .valor { font-size: 1.4rem; font-weight: 700; }
    .centro-acoes { justify-content: center; }
    .centro-texto { text-align: center; }
  `],
})
export class AgendarComponent implements OnInit {
  private http = inject(HttpClient);
  private rota = inject(ActivatedRoute);

  readonly portes = PORTES;
  readonly especies = ESPECIES;
  readonly etapas = [{ n: 1, rotulo: 'Pet' }, { n: 2, rotulo: 'Serviço' }, { n: 3, rotulo: 'Horário' }, { n: 4, rotulo: 'Seus dados' }];
  readonly hojeIso = iso(new Date());
  readonly limiteIso = iso(new Date(Date.now() + 60 * 86_400_000));

  private slug = '';
  loja = signal<LojaPublica | null>(null);
  servicos = signal<ServicoPublico[]>([]);
  profissionais = signal<ProfissionalPublico[]>([]);
  naoEncontrada = signal(false);

  passo = signal<Passo>(1);
  petNome = '';
  especie: Especie = 'CACHORRO';
  raca = '';
  porte = signal<Porte>('PEQUENO');
  servico = signal<ServicoPublico | null>(null);
  data = signal(iso(new Date(Date.now() + 86_400_000)));
  profissionalId = signal<number | null>(null);
  horarios = signal<HorarioLivre[]>([]);
  horario = signal<HorarioLivre | null>(null);
  carregandoHorarios = signal(false);
  nome = '';
  telefone = '';
  email = '';
  enviando = signal(false);
  erro = signal('');
  confirmacao = signal<ReservaConfirmada | null>(null);

  readonly porteRotulo = computed(() => PORTES.find(p => p.valor === this.porte())?.rotulo ?? '');
  readonly fechadoNaData = computed(() => {
    const l = this.loja();
    if (!l) return false;
    const d = new Date(this.data() + 'T12:00:00').getDay();
    return !l.diasFuncionamento.includes(d === 0 ? 7 : d);
  });

  ngOnInit() {
    this.slug = this.rota.snapshot.paramMap.get('slug') ?? '';
    const base = `${API}/publico/lojas/${this.slug}`;
    this.http.get<LojaPublica>(base).subscribe({
      next: l => {
        this.loja.set(l);
        this.data.set(this.proximoDiaAberto(l));
        this.http.get<ServicoPublico[]>(`${base}/servicos`).subscribe(s => this.servicos.set(s));
        this.http.get<ProfissionalPublico[]>(`${base}/profissionais`).subscribe(p => this.profissionais.set(p));
      },
      error: () => this.naoEncontrada.set(true),
    });
  }

  /** Primeiro dia aberto a partir de amanhã (o dia de hoje costuma ter poucos horários). */
  private proximoDiaAberto(l: LojaPublica): string {
    const d = new Date(Date.now() + 86_400_000);
    for (let i = 0; i < 7; i++) {
      const dia = d.getDay() === 0 ? 7 : d.getDay();
      if (l.diasFuncionamento.includes(dia)) break;
      d.setDate(d.getDate() + 1);
    }
    return iso(d);
  }

  preco(s: ServicoPublico): number {
    return this.porte() === 'GRANDE' ? s.precoGrande : this.porte() === 'MEDIO' ? s.precoMedio : s.precoPequeno;
  }

  duracao(s: ServicoPublico): number {
    return this.porte() === 'GRANDE' ? s.duracaoGrande : this.porte() === 'MEDIO' ? s.duracaoMedio : s.duracaoPequeno;
  }

  irPara(p: Passo) {
    this.erro.set('');
    this.passo.set(p);
    if (p === 3) this.carregarHorarios();
  }

  escolherServico(s: ServicoPublico) {
    this.servico.set(s);
    this.horario.set(null);
    this.irPara(3);
  }

  mudarData(d: string) {
    this.data.set(d);
    this.carregarHorarios();
  }

  mudarProfissional(id: number | null) {
    this.profissionalId.set(id);
    this.carregarHorarios();
  }

  carregarHorarios() {
    const s = this.servico();
    if (!s || !this.data()) return;
    this.horario.set(null);
    this.carregandoHorarios.set(true);
    let params = new HttpParams().set('servicoId', s.id).set('porte', this.porte()).set('data', this.data());
    if (this.profissionalId()) params = params.set('profissionalId', this.profissionalId()!);
    this.http.get<HorarioLivre[]>(`${API}/publico/lojas/${this.slug}/horarios`, { params }).subscribe({
      next: h => { this.horarios.set(h); this.carregandoHorarios.set(false); },
      error: e => { this.horarios.set([]); this.carregandoHorarios.set(false); this.erro.set(mensagemErro(e)); },
    });
  }

  confirmar() {
    const s = this.servico();
    const h = this.horario();
    if (!s || !h) return;
    const corpo: ReservaPublica = {
      nome: this.nome.trim(), telefone: this.telefone.trim(), email: this.email.trim() || undefined,
      petNome: this.petNome.trim(), especie: this.especie, raca: this.raca.trim() || undefined, porte: this.porte(),
      servicoId: s.id, profissionalId: h.profissionalId, inicio: h.inicio,
    };
    this.enviando.set(true);
    this.erro.set('');
    this.http.post<ReservaConfirmada>(`${API}/publico/lojas/${this.slug}/agendamentos`, corpo).subscribe({
      next: c => { this.confirmacao.set(c); this.enviando.set(false); this.passo.set(5); },
      error: e => {
        this.enviando.set(false);
        this.erro.set(mensagemErro(e));
        // horário tomado por outra pessoa: volta para escolher outro
        if (e.status === 409) { this.passo.set(3); this.carregarHorarios(); }
      },
    });
  }

  novoAgendamento() {
    this.petNome = ''; this.raca = ''; this.servico.set(null); this.horario.set(null); this.confirmacao.set(null);
    this.passo.set(1);
  }
}
