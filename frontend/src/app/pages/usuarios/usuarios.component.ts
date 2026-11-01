import { Component, inject, OnInit, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/auth.service';
import { mensagemErro } from '../../core/erro';
import { OpcaoVinculo, OpcoesVinculo, Perfil, Usuario } from '../../core/models';
import { API } from '../../core/api';
import { IconeComponent } from '../../shared/icone.component';

const PERFIS: { valor: Perfil; rotulo: string; descricao: string }[] = [
  { valor: 'ADMINISTRADOR', rotulo: 'Administrador', descricao: 'Acesso total: financeiro, usuários, cadastros e exclusões.' },
  { valor: 'PROFISSIONAL', rotulo: 'Profissional', descricao: 'Agenda, tutores, pets e estoque. Pode conectar o próprio Google Agenda.' },
  { valor: 'CLIENTE', rotulo: 'Tutor', descricao: 'Reservado ao futuro portal do tutor: por enquanto só acessa "Minha conta".' },
];

interface FormUsuario {
  id?: number; emailOriginal?: string;
  nome: string; email: string; telefone: string; senha: string; ativo: boolean;
  perfis: Record<Perfil, boolean>;
  profissionalId: number | null; clienteId: number | null;
}

@Component({
  selector: 'app-usuarios',
  standalone: true,
  imports: [FormsModule, IconeComponent],
  template: `
    <header class="topo">
      <div>
        <h1>Usuários</h1>
        <p class="sub">{{ itens().length }} {{ itens().length === 1 ? 'usuário' : 'usuários' }} com acesso ao sistema</p>
      </div>
      <div class="acoes">
        <button class="primario" (click)="novo()"><app-icone nome="mais" /> Novo usuário</button>
      </div>
    </header>

    @if (ok()) { <div class="sucesso" role="status">{{ ok() }}</div> }
    @if (erro()) { <div class="erro" role="alert">{{ erro() }}</div> }

    @if (form(); as f) {
      <form class="card" (ngSubmit)="salvar()">
        <h2>{{ f.id ? 'Editar' : 'Novo' }} usuário</h2>
        <div class="form-grid">
          <div class="campo"><label for="u-nome">Nome *</label><input id="u-nome" name="nome" [(ngModel)]="f.nome" required></div>
          <div class="campo"><label for="u-email">E-mail (login) *</label><input id="u-email" name="email" type="email" [(ngModel)]="f.email" required autocomplete="off"></div>
          <div class="campo"><label for="u-tel">Telefone</label><input id="u-tel" name="telefone" type="tel" [(ngModel)]="f.telefone"></div>
          @if (!f.id) {
            <div class="campo"><label for="u-senha">Senha inicial * (mín. 8 caracteres)</label>
              <input id="u-senha" name="senha" type="password" [(ngModel)]="f.senha" required minlength="8" autocomplete="new-password"></div>
          }
        </div>

        <h2>Perfis de acesso</h2>
        <div class="perfis">
          @for (p of perfis; track p.valor) {
            <label class="perfil-opcao" [class.marcado]="f.perfis[p.valor]">
              <input type="checkbox" [name]="'perfil-' + p.valor" [(ngModel)]="f.perfis[p.valor]">
              <span><strong>{{ p.rotulo }}</strong><small>{{ p.descricao }}</small></span>
            </label>
          }
        </div>

        @if (f.perfis.PROFISSIONAL) {
          <div class="campo">
            <label for="u-prof">Vincular ao profissional</label>
            <select id="u-prof" name="profissionalId" [(ngModel)]="f.profissionalId">
              <option [ngValue]="null">Sem vínculo</option>
              @for (o of livres(opcoes().profissionais, f.id); track o.id) { <option [ngValue]="o.id">{{ o.nome }}</option> }
            </select>
            <p class="suave">O vínculo permite que ele conecte o próprio Google Agenda.</p>
          </div>
        }
        @if (f.perfis.CLIENTE) {
          <div class="campo">
            <label for="u-cli">Vincular ao cliente</label>
            <select id="u-cli" name="clienteId" [(ngModel)]="f.clienteId">
              <option [ngValue]="null">Sem vínculo</option>
              @for (o of livres(opcoes().clientes, f.id); track o.id) { <option [ngValue]="o.id">{{ o.nome }}</option> }
            </select>
          </div>
        }

        <div class="campo"><label for="u-ativo"><input id="u-ativo" name="ativo" type="checkbox" [(ngModel)]="f.ativo"> Usuário ativo (pode entrar no sistema)</label></div>

        <div class="form-acoes">
          <button class="primario" [disabled]="salvando()">{{ salvando() ? 'Salvando…' : 'Salvar' }}</button>
          <button type="button" (click)="cancelar()">Cancelar</button>
        </div>
      </form>
    }

    @if (redefinir(); as r) {
      <form class="card" (ngSubmit)="confirmarSenha()">
        <h2>Redefinir senha de {{ r.nome }}</h2>
        <div class="campo"><label for="r-senha">Nova senha (mín. 8 caracteres)</label>
          <input id="r-senha" name="novaSenha" type="password" [(ngModel)]="r.senha" required minlength="8" autocomplete="new-password"></div>
        <div class="form-acoes">
          <button class="primario" [disabled]="salvando()">Redefinir</button>
          <button type="button" (click)="redefinir.set(null)">Cancelar</button>
        </div>
      </form>
    }

    <div class="card tabela-wrap">
      <table>
        <thead><tr><th>Usuário</th><th>E-mail</th><th>Perfis</th><th>Vínculo</th><th>Situação</th><th></th></tr></thead>
        <tbody>
          @for (u of itens(); track u.id; let n = $index) {
            <tr>
              <td><div class="ident"><span class="inicial" [attr.data-tom]="n % 4">{{ u.nome.charAt(0).toUpperCase() }}</span><strong>{{ u.nome }}</strong></div></td>
              <td>{{ u.email }}</td>
              <td>@for (p of u.perfis; track p) { <span class="badge" [class.ok]="p === 'ADMINISTRADOR'">{{ rotuloPerfil(p) }}</span>{{ ' ' }} }</td>
              <td>{{ u.profissionalNome || u.clienteNome || '—' }}</td>
              <td><span class="badge" [class.ok]="u.ativo" [class.aviso]="!u.ativo">{{ u.ativo ? 'Ativo' : 'Inativo' }}</span></td>
              <td class="acoes-celula">
                <button class="mini" (click)="editar(u)">Editar</button>
                <button class="mini" (click)="abrirSenha(u)">Senha</button>
                <button class="mini" [class.perigo]="u.ativo" [disabled]="ehEu(u)" (click)="alternarAtivo(u)">{{ u.ativo ? 'Desativar' : 'Ativar' }}</button>
              </td>
            </tr>
          } @empty { <tr><td colspan="6">Nenhum usuário encontrado.</td></tr> }
        </tbody>
      </table>
    </div>
  `,
  styles: [`
    .perfis { display: grid; gap: .6rem; margin-bottom: 1.1rem; }
    .perfil-opcao {
      display: flex; align-items: flex-start; gap: .75rem; padding: .8rem 1rem; margin: 0; cursor: pointer;
      border: 1px solid var(--borda-forte); border-radius: var(--raio-m); background: var(--superficie-2);
      font-weight: 500;
    }
    .perfil-opcao.marcado { border-color: var(--acento-forte); background: var(--acento-bruma); }
    .perfil-opcao input { margin-top: .2rem; }
    .perfil-opcao span { display: flex; flex-direction: column; }
    .perfil-opcao small { color: var(--texto-suave); font-weight: 400; }
  `],
})
export class UsuariosComponent implements OnInit {
  private http = inject(HttpClient);
  private auth = inject(AuthService);
  readonly perfis = PERFIS;

  itens = signal<Usuario[]>([]);
  opcoes = signal<OpcoesVinculo>({ profissionais: [], clientes: [] });
  form = signal<FormUsuario | null>(null);
  redefinir = signal<{ id: number; nome: string; senha: string } | null>(null);
  erro = signal('');
  ok = signal('');
  salvando = signal(false);

  ngOnInit() { this.carregar(); }

  carregar() {
    this.http.get<Usuario[]>(`${API}/usuarios`).subscribe({
      next: l => this.itens.set(l),
      error: e => this.erro.set(mensagemErro(e)),
    });
    this.http.get<OpcoesVinculo>(`${API}/usuarios/opcoes-vinculo`).subscribe(o => this.opcoes.set(o));
  }

  rotuloPerfil(p: string) { return PERFIS.find(x => x.valor === p)?.rotulo ?? p; }
  ehEu(u: Usuario) { return u.email.toLowerCase() === this.auth.sessao()?.email.toLowerCase(); }

  /** Opções ainda sem usuário, mais a que já pertence ao usuário em edição. */
  livres(lista: OpcaoVinculo[], usuarioId?: number) {
    return lista.filter(o => o.usuarioId === null || o.usuarioId === usuarioId);
  }

  private limpar() { this.erro.set(''); this.ok.set(''); }

  novo() {
    this.limpar();
    this.redefinir.set(null);
    this.form.set({
      nome: '', email: '', telefone: '', senha: '', ativo: true,
      perfis: { ADMINISTRADOR: false, PROFISSIONAL: true, CLIENTE: false },
      profissionalId: null, clienteId: null,
    });
  }

  editar(u: Usuario) {
    this.limpar();
    this.redefinir.set(null);
    this.form.set({
      id: u.id, emailOriginal: u.email, nome: u.nome, email: u.email, telefone: u.telefone ?? '', senha: '', ativo: u.ativo,
      perfis: {
        ADMINISTRADOR: u.perfis.includes('ADMINISTRADOR'),
        PROFISSIONAL: u.perfis.includes('PROFISSIONAL'),
        CLIENTE: u.perfis.includes('CLIENTE'),
      },
      profissionalId: u.profissionalId ?? null, clienteId: u.clienteId ?? null,
    });
  }

  cancelar() { this.form.set(null); this.erro.set(''); }

  salvar() {
    const f = this.form();
    if (!f) return;
    const perfis = PERFIS.map(p => p.valor).filter(p => f.perfis[p]);
    if (perfis.length === 0) { this.erro.set('Selecione ao menos um perfil.'); return; }

    const corpo: Record<string, unknown> = {
      nome: f.nome, email: f.email, telefone: f.telefone || null, perfis, ativo: f.ativo,
      profissionalId: f.perfis.PROFISSIONAL ? f.profissionalId : null,
      clienteId: f.perfis.CLIENTE ? f.clienteId : null,
    };
    if (!f.id) corpo['senha'] = f.senha;

    this.limpar();
    this.salvando.set(true);
    const req = f.id ? this.http.put<Usuario>(`${API}/usuarios/${f.id}`, corpo) : this.http.post<Usuario>(`${API}/usuarios`, corpo);
    req.subscribe({
      next: () => {
        this.salvando.set(false);
        this.form.set(null);
        const alterouProprioEmail = !!f.id && f.emailOriginal?.toLowerCase() === this.auth.sessao()?.email.toLowerCase()
          && f.email.trim().toLowerCase() !== f.emailOriginal?.toLowerCase();
        if (alterouProprioEmail) { this.auth.logout(); return; } // o login mudou: entre novamente
        this.ok.set('Usuário salvo.');
        this.carregar();
        this.auth.sincronizar().subscribe({ error: () => {} }); // caso tenha editado a si mesma
      },
      error: e => { this.salvando.set(false); this.erro.set(mensagemErro(e)); },
    });
  }

  alternarAtivo(u: Usuario) {
    const acao = u.ativo ? 'Desativar' : 'Ativar';
    if (!window.confirm(`${acao} o acesso de ${u.nome}?`)) return;
    this.limpar();
    const corpo = {
      nome: u.nome, email: u.email, telefone: u.telefone ?? null, perfis: u.perfis, ativo: !u.ativo,
      profissionalId: u.profissionalId ?? null, clienteId: u.clienteId ?? null,
    };
    this.http.put(`${API}/usuarios/${u.id}`, corpo).subscribe({
      next: () => { this.ok.set(u.ativo ? 'Usuário desativado.' : 'Usuário ativado.'); this.carregar(); },
      error: e => this.erro.set(mensagemErro(e)),
    });
  }

  abrirSenha(u: Usuario) {
    this.limpar();
    this.form.set(null);
    this.redefinir.set({ id: u.id, nome: u.nome, senha: '' });
  }

  confirmarSenha() {
    const r = this.redefinir();
    if (!r) return;
    this.limpar();
    this.salvando.set(true);
    this.http.patch(`${API}/usuarios/${r.id}/senha`, { novaSenha: r.senha }).subscribe({
      next: () => { this.salvando.set(false); this.redefinir.set(null); this.ok.set(`Senha de ${r.nome} redefinida.`); },
      error: e => { this.salvando.set(false); this.erro.set(mensagemErro(e)); },
    });
  }
}
