import { DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { mensagemErro } from '../../core/erro';
import { GoogleConexao, GoogleStatus, Sincronizacao } from '../../core/models';
import { API } from '../../core/api';
import { IconeComponent } from '../../shared/icone.component';

const RESULTADOS: Record<string, { ok: boolean; texto: string }> = {
  ok: { ok: true, texto: 'Google Agenda conectado! Os próximos agendamentos já foram enviados.' },
  negado: { ok: false, texto: 'A autorização foi cancelada no Google. Nada foi alterado.' },
  permissao: { ok: false, texto: 'Para sincronizar é preciso permitir o acesso à agenda na tela do Google.' },
  falha: { ok: false, texto: 'Não foi possível concluir a conexão com o Google. Tente novamente.' },
  invalido: { ok: false, texto: 'O link de autorização expirou ou é inválido. Clique em Conectar de novo.' },
};

@Component({
  selector: 'app-google-calendar',
  standalone: true,
  imports: [DatePipe, IconeComponent],
  template: `
    <header class="topo">
      <div>
        <h1>Google Agenda</h1>
        <p class="sub">Cada profissional conecta a própria conta Google e os atendimentos aparecem na agenda dele.</p>
      </div>
    </header>

    @if (ok()) { <div class="sucesso" role="status">{{ ok() }}</div> }
    @if (erro()) { <div class="erro" role="alert">{{ erro() }}</div> }

    @if (status(); as s) {
      @if (!s.configurado) {
        <div class="card">
          <h2>Integração ainda não configurada</h2>
          <p>O servidor precisa das credenciais do Google para liberar a conexão. Defina
            <code>GOOGLE_CLIENT_ID</code> e <code>GOOGLE_CLIENT_SECRET</code> e reinicie o backend.</p>
          <p class="suave">Cadastre no Google Cloud Console, em “URIs de redirecionamento autorizados”, exatamente:
            <code>{{ s.redirectUri }}</code></p>
        </div>
      }

      @for (p of s.profissionais; track p.profissionalId) {
        <div class="card conexao">
          <div class="info">
            <h2>{{ p.profissionalNome }}</h2>
            @if (p.conectado) {
              <p><span class="badge ok">Conectado</span> {{ p.email || 'conta Google' }}
                @if (p.conectadoEm) { <span class="suave"> · desde {{ p.conectadoEm | date:'dd/MM/yyyy' }}</span> }</p>
              <p class="suave">Agenda usada: {{ p.calendarId === 'primary' ? 'principal' : p.calendarId }}</p>
            } @else if (p.reautorizar) {
              <p><span class="badge aviso">Reconexão necessária</span> O acesso foi revogado ou expirou no Google.</p>
            } @else {
              <p><span class="badge">Não conectado</span></p>
            }
          </div>
          <div class="botoes">
            @if (s.configurado) {
              <button class="primario" [disabled]="ocupado()" (click)="conectar(p)">{{ p.conectado ? 'Reconectar' : p.reautorizar ? 'Reconectar' : 'Conectar Google' }}</button>
            }
            @if (p.conectado) {
              <button [disabled]="ocupado()" (click)="sincronizar(p)"><app-icone nome="sincronizar" /> Sincronizar agora</button>
            }
            @if (p.conectado || p.reautorizar) {
              <button class="perigo" [disabled]="ocupado()" (click)="desconectar(p)">Desconectar</button>
            }
          </div>
        </div>
      } @empty {
        <div class="card vazio">
          @if (auth.isAdmin()) { Nenhuma profissional ativa cadastrada. }
          @else { Sua conta ainda não está vinculada a um profissional. Peça ao administrador para vincular em Usuários. }
        </div>
      }

      <div class="card">
        <h2>Como funciona</h2>
        <ul>
          <li>Ao agendar, remarcar ou trocar o profissional, o evento é criado ou atualizado no Google Agenda dele.</li>
          <li>Cancelamentos e faltas removem o evento; ao reativar o agendamento, ele volta.</li>
          <li>Compromissos já marcados no Google (com horário, não “livres”) bloqueiam o horário, a menos que o agendamento seja um encaixe. Eventos de dia inteiro são ignorados.</li>
          <li>Se o Google estiver fora do ar, o agendamento é salvo normalmente; use “Sincronizar agora” depois.</li>
          <li>Ao desconectar, os eventos já criados continuam na agenda do Google.</li>
        </ul>
      </div>
    }
  `,
  styles: [`
    .conexao { display: flex; flex-wrap: wrap; gap: 1rem 1.5rem; align-items: center; justify-content: space-between; }
    .conexao h2 { margin-bottom: .4rem; }
    .botoes { display: flex; flex-wrap: wrap; gap: .5rem; }
    code { background: var(--superficie-2); padding: .1rem .45rem; border-radius: var(--raio-p); word-break: break-all; }
    ul { margin: 0; padding-left: 1.2rem; display: grid; gap: .4rem; }
  `],
})
export class GoogleCalendarComponent implements OnInit {
  private http = inject(HttpClient);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  readonly auth = inject(AuthService);

  status = signal<GoogleStatus | null>(null);
  ok = signal('');
  erro = signal('');
  ocupado = signal(false);

  ngOnInit() {
    const resultado = this.route.snapshot.queryParamMap.get('google');
    if (resultado && RESULTADOS[resultado]) {
      const r = RESULTADOS[resultado];
      (r.ok ? this.ok : this.erro).set(r.texto);
      this.router.navigate([], { queryParams: {}, replaceUrl: true }); // limpa ?google= da URL
    }
    this.carregar();
  }

  carregar() {
    this.http.get<GoogleStatus>(`${API}/google/status`).subscribe({
      next: s => this.status.set(s),
      error: e => this.erro.set(mensagemErro(e)),
    });
  }

  private limpar() { this.ok.set(''); this.erro.set(''); }

  conectar(p: GoogleConexao) {
    this.limpar();
    this.ocupado.set(true);
    this.http.get<{ url: string }>(`${API}/google/profissionais/${p.profissionalId}/autorizacao`).subscribe({
      next: r => { window.location.href = r.url; },
      error: e => { this.ocupado.set(false); this.erro.set(mensagemErro(e)); },
    });
  }

  sincronizar(p: GoogleConexao) {
    this.limpar();
    this.ocupado.set(true);
    this.http.post<Sincronizacao>(`${API}/google/profissionais/${p.profissionalId}/sincronizar`, {}).subscribe({
      next: r => {
        this.ocupado.set(false);
        const resto = r.restantes > 0 ? ` Restam ${r.restantes}: clique de novo.` : '';
        const falhas = r.falhas > 0 ? ` ${r.falhas} falharam.` : '';
        this.ok.set(`${r.sincronizados} agendamento(s) enviados ao Google.${falhas}${resto}`);
        this.carregar();
      },
      error: e => { this.ocupado.set(false); this.erro.set(mensagemErro(e)); },
    });
  }

  desconectar(p: GoogleConexao) {
    if (!window.confirm(`Desconectar o Google Agenda de ${p.profissionalNome}? Os eventos já criados permanecem no Google.`)) return;
    this.limpar();
    this.ocupado.set(true);
    this.http.post(`${API}/google/profissionais/${p.profissionalId}/desconectar`, {}).subscribe({
      next: () => { this.ocupado.set(false); this.ok.set('Google Agenda desconectado.'); this.carregar(); },
      error: e => { this.ocupado.set(false); this.erro.set(mensagemErro(e)); },
    });
  }
}
