import { DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { mensagemErro } from '../../core/erro';
import { Agendamento, AgendamentoRequest, Cliente, Pet, Profissional, Servico, StatusAgendamento } from '../../core/models';
import { API } from '../../core/api';
import { IconeComponent } from '../../shared/icone.component';
import { statusRotulo } from '../../core/rotulos';

function hoje(): string {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

@Component({
  selector: 'app-agenda',
  standalone: true,
  imports: [FormsModule, DatePipe, IconeComponent],
  template: `
    <header class="topo">
      <div>
        <h1>Agenda</h1>
        <p class="sub primeira-maiuscula">{{ data | date:"EEEE, d 'de' MMMM" }}</p>
      </div>
      <div class="acoes">
        <button class="icone-btn" type="button" aria-label="Dia anterior" (click)="mudarDia(-1)"><app-icone nome="esquerda" /></button>
        <input type="date" aria-label="Data" [(ngModel)]="data" (ngModelChange)="carregar()">
        <button class="icone-btn" type="button" aria-label="Próximo dia" (click)="mudarDia(1)"><app-icone nome="direita" /></button>
        <select aria-label="Profissional" [(ngModel)]="filtroProfissional" (ngModelChange)="carregar()">
          <option [ngValue]="null">Todos os profissionais</option>
          @for (p of profissionais(); track p.id) { <option [ngValue]="p.id">{{ p.nome }}</option> }
        </select>
        <button class="primario" (click)="novo()"><app-icone nome="mais" /> Agendar</button>
      </div>
    </header>

    @if (erro()) { <div class="erro" role="alert">{{ erro() }}</div> }

    @if (form(); as f) {
      <form class="card" (ngSubmit)="salvar()">
        <h2>Novo agendamento</h2>
        <div class="form-grid">
          <div class="campo"><label for="ag-cliente">Tutor *</label>
            <select id="ag-cliente" name="cliente" [(ngModel)]="f.clienteId" (ngModelChange)="escolherTutor($event)" required>
              <option [ngValue]="null">Selecione…</option>
              @for (c of clientes(); track c.id) { <option [ngValue]="c.id">{{ c.nome }}</option> }
            </select></div>
          <div class="campo"><label for="ag-pet">Pet *</label>
            <select id="ag-pet" name="pet" [(ngModel)]="f.petId" required [disabled]="!f.clienteId">
              <option [ngValue]="null">{{ f.clienteId && pets().length === 0 ? 'Este tutor não tem pets cadastrados' : 'Selecione…' }}</option>
              @for (p of pets(); track p.id) { <option [ngValue]="p.id">{{ p.nome }} ({{ porteRotulo(p) }})</option> }
            </select></div>
          <div class="campo"><label for="ag-prof">Profissional *</label>
            <select id="ag-prof" name="profissional" [(ngModel)]="f.profissionalId" required>
              <option [ngValue]="null">Selecione…</option>
              @for (p of profissionais(); track p.id) { <option [ngValue]="p.id">{{ p.nome }}</option> }
            </select></div>
          <div class="campo"><label for="ag-serv">Serviço *</label>
            <select id="ag-serv" name="servico" [(ngModel)]="f.servicoId" required>
              <option [ngValue]="null">Selecione…</option>
              @for (s of servicos(); track s.id) { <option [ngValue]="s.id">{{ s.nome }}{{ rotuloPreco(s, f.petId) }}</option> }
            </select></div>
          <div class="campo"><label for="ag-inicio">Data e hora *</label>
            <input id="ag-inicio" type="datetime-local" name="inicio" [(ngModel)]="f.inicio" required></div>
        </div>
        <div class="campo">
          <label for="ag-encaixe"><input id="ag-encaixe" type="checkbox" name="encaixe" [(ngModel)]="f.encaixe"> Encaixe: permite atender junto com outro pet do mesmo profissional</label>
        </div>
        <div class="form-acoes">
          <button class="primario" [disabled]="salvando()">{{ salvando() ? 'Salvando…' : 'Agendar' }}</button>
          <button type="button" (click)="form.set(null); erro.set('')">Cancelar</button>
        </div>
      </form>
    }

    <ul class="livro">
      @for (a of agendamentos(); track a.id) {
        <li class="atend" [class.apagado]="a.status === 'CANCELADO' || a.status === 'FALTOU'" [class.agora]="emAndamento(a)">
          <div class="hora">
            <strong>{{ a.dataHoraInicio | date:'HH:mm' }}</strong>
            <small>até {{ a.dataHoraFim | date:'HH:mm' }}</small>
          </div>
          <div class="quem">
            <h3>{{ a.pet.nome }} <span class="tutor">· {{ a.cliente.nome }}</span></h3>
            <p class="suave">{{ a.servico.nome }} com {{ a.profissional.nome }}</p>
          </div>
          <div class="marcas">
            @if (emAndamento(a)) { <span class="badge agora">Em atendimento</span> }
            @if (a.origem === 'ONLINE') { <span class="badge online" title="Marcado pelo tutor na página pública">Online</span> }
            @if (a.encaixe) { <span class="badge encaixe">Encaixe</span> }
            @if (a.googleEventId) { <span class="badge google" title="Evento criado no Google Agenda">Google</span> }
            <span [class]="'badge ' + a.status">{{ rotulo(a.status) }}</span>
          </div>
          <div class="botoes">
            @if (a.status === 'AGENDADO') { <button class="mini" (click)="status(a, 'CONFIRMADO')">Confirmar</button> }
            @if (a.status === 'AGENDADO' || a.status === 'CONFIRMADO') {
              <button class="mini primario" (click)="status(a, 'CONCLUIDO')">Concluir</button>
              <button class="mini" (click)="status(a, 'FALTOU')">Faltou</button>
              <button class="mini perigo" (click)="status(a, 'CANCELADO')">Cancelar</button>
            }
          </div>
        </li>
      } @empty {
        <li class="atend estado-vazio">
          <span class="ico"><app-icone nome="agenda" /></span>
          <strong>Nenhum atendimento neste dia</strong>
          <p>Use “Agendar” para marcar o primeiro horário.</p>
        </li>
      }
    </ul>
  `,
  styles: [`
    /* a agenda é uma lista de horários: linhas finas, hora à esquerda */
    .livro { list-style: none; margin: 0; padding: 0; overflow: hidden; background: var(--superficie); border: 1px solid var(--borda); border-radius: var(--raio-g); box-shadow: var(--sombra); }
    .atend {
      position: relative; display: grid; grid-template-columns: 6rem 1fr auto auto; align-items: center; gap: 1.1rem;
      padding: 1rem 1.25rem 1rem 1.4rem; border-top: 1px solid #f1f5f9; transition: background .15s;
    }
    .atend:first-child { border-top: 0; }
    .atend:hover { background: var(--fundo); }
    .atend.apagado { opacity: .6; }
    .atend.apagado .quem h3 { text-decoration: line-through; text-decoration-thickness: 1px; }
    .atend.agora { background: var(--acento-bruma); }
    .atend.agora::before { content: ''; position: absolute; left: 0; top: 0; bottom: 0; width: 3px; background: var(--acento); }
    .hora { display: flex; flex-direction: column; line-height: 1.15; }
    .hora strong { font-size: 1.3rem; font-weight: 700; font-variant-numeric: tabular-nums; letter-spacing: -.01em; }
    .hora small { color: var(--texto-suave); font-size: .75rem; margin-top: .15rem; }
    .quem h3 { margin-bottom: .1rem; }
    .tutor { color: var(--texto-suave); font-weight: 400; }
    .marcas { display: flex; gap: .4rem; flex-wrap: wrap; justify-content: flex-end; }
    .botoes { display: flex; gap: .4rem; flex-wrap: wrap; justify-content: flex-end; }
    .atend.estado-vazio { display: flex; border: 0; }
    .atend.estado-vazio:hover { background: transparent; }
    @media (max-width: 760px) {
      .atend { grid-template-columns: 5rem 1fr; padding: .9rem 1rem .9rem 1.2rem; }
      .marcas, .botoes { grid-column: 1 / -1; justify-content: flex-start; }
    }
  `],
})
export class AgendaComponent implements OnInit {
  readonly rotulo = statusRotulo;
  private http = inject(HttpClient);

  data = hoje();
  filtroProfissional: number | null = null;
  agendamentos = signal<Agendamento[]>([]);
  clientes = signal<Cliente[]>([]);
  pets = signal<Pet[]>([]);
  profissionais = signal<Profissional[]>([]);
  servicos = signal<Servico[]>([]);
  form = signal<{ clienteId: number | null; petId: number | null; profissionalId: number | null; servicoId: number | null; inicio: string; encaixe: boolean } | null>(null);
  erro = signal('');
  salvando = signal(false);

  ngOnInit() {
    this.http.get<Cliente[]>(`${API}/clientes`).subscribe(l => this.clientes.set(l.filter(c => c.ativo !== false)));
    this.http.get<Profissional[]>(`${API}/profissionais`).subscribe(l => this.profissionais.set(l.filter(p => p.ativo !== false)));
    this.http.get<Servico[]>(`${API}/servicos`).subscribe(l => this.servicos.set(l.filter(s => s.ativo !== false)));
    this.carregar();
  }

  carregar() {
    let params = new HttpParams().set('de', this.data).set('ate', this.data);
    if (this.filtroProfissional) params = params.set('profissionalId', this.filtroProfissional);
    this.http.get<Agendamento[]>(`${API}/agendamentos`, { params }).subscribe({
      next: l => this.agendamentos.set(l),
      error: e => this.erro.set(mensagemErro(e)),
    });
  }

  /** Carrega os pets do tutor escolhido. */
  escolherTutor(clienteId: number | null) {
    this.form.update(f => f && { ...f, petId: null });
    this.pets.set([]);
    if (!clienteId) return;
    this.http.get<Pet[]>(`${API}/pets`, { params: { clienteId } }).subscribe(l => this.pets.set(l.filter(p => p.ativo !== false)));
  }

  porteRotulo(p: Pet): string {
    return p.porte === 'GRANDE' ? 'grande' : p.porte === 'MEDIO' ? 'médio' : 'pequeno';
  }

  /** Mostra preço e duração do serviço para o porte do pet escolhido. */
  rotuloPreco(s: Servico, petId: number | null): string {
    const pet = this.pets().find(p => p.id === petId);
    if (!pet) return ` (a partir de ${s.duracaoMinutos} min)`;
    const grande = pet.porte === 'GRANDE', medio = pet.porte === 'MEDIO';
    const dur = (grande ? s.duracaoGrande : medio ? s.duracaoMedio : null) ?? s.duracaoMinutos;
    const preco = (grande ? s.precoGrande : medio ? s.precoMedio : null) ?? s.preco;
    return ` (${dur} min · ${Number(preco).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })})`;
  }

  /** Atendimento acontecendo agora (hoje, entre início e fim, ainda não encerrado). */
  emAndamento(a: Agendamento): boolean {
    if (a.status !== 'AGENDADO' && a.status !== 'CONFIRMADO') return false;
    const agora = Date.now();
    return new Date(a.dataHoraInicio).getTime() <= agora && agora < new Date(a.dataHoraFim).getTime();
  }

  mudarDia(delta: number) {
    const d = new Date(this.data + 'T12:00:00');
    d.setDate(d.getDate() + delta);
    this.data = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
    this.carregar();
  }

  novo() {
    this.erro.set('');
    this.pets.set([]);
    this.form.set({ clienteId: null, petId: null, profissionalId: this.filtroProfissional, servicoId: null, inicio: `${this.data}T09:00`, encaixe: false });
  }

  salvar() {
    const f = this.form();
    if (!f || !f.petId || !f.profissionalId || !f.servicoId) return;
    const corpo: AgendamentoRequest = {
      petId: f.petId, profissionalId: f.profissionalId, servicoId: f.servicoId, inicio: f.inicio, encaixe: f.encaixe,
    };
    this.salvando.set(true);
    this.erro.set('');
    this.http.post(`${API}/agendamentos`, corpo).subscribe({
      next: () => { this.salvando.set(false); this.data = f.inicio.slice(0, 10); this.form.set(null); this.carregar(); },
      error: e => { this.salvando.set(false); this.erro.set(mensagemErro(e)); },
    });
  }

  status(a: Agendamento, status: StatusAgendamento) {
    if (status === 'CANCELADO' && !window.confirm('Cancelar este agendamento?')) return;
    this.erro.set('');
    this.http.patch(`${API}/agendamentos/${a.id}/status`, { status }).subscribe({
      next: () => this.carregar(),
      error: e => this.erro.set(mensagemErro(e)),
    });
  }
}
