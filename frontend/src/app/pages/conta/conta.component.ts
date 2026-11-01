import { Component, inject, OnInit, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/auth.service';
import { mensagemErro } from '../../core/erro';
import { Usuario } from '../../core/models';
import { API } from '../../core/api';

@Component({
  selector: 'app-conta',
  standalone: true,
  imports: [FormsModule],
  template: `
    <header class="topo">
      <div>
        <h1>Minha conta</h1>
        <p class="sub">Seus dados de acesso ao Pelo Macio.</p>
      </div>
    </header>

    @if (!auth.isEquipe()) {
      <div class="card vazio">O portal do cliente ainda não está disponível. Por enquanto você pode manter seus dados e sua senha aqui.</div>
    }

    <form class="card" (ngSubmit)="salvarDados()">
      <h2>Meus dados</h2>
      @if (okDados()) { <div class="sucesso" role="status">{{ okDados() }}</div> }
      @if (erroDados()) { <div class="erro" role="alert">{{ erroDados() }}</div> }
      <div class="form-grid">
        <div class="campo"><label for="c-nome">Nome *</label><input id="c-nome" name="nome" [(ngModel)]="nome" required></div>
        <div class="campo"><label for="c-email">E-mail (login)</label><input id="c-email" name="email" [ngModel]="email" disabled></div>
        <div class="campo"><label for="c-tel">Telefone</label><input id="c-tel" name="telefone" type="tel" [(ngModel)]="telefone"></div>
      </div>
      <p class="suave">Perfis: {{ perfis }}. Para trocar o e-mail ou os perfis, fale com o administrador.</p>
      <div class="form-acoes"><button class="primario" [disabled]="salvandoDados()">Salvar dados</button></div>
    </form>

    <form class="card" (ngSubmit)="alterarSenha()">
      <h2>Alterar senha</h2>
      @if (okSenha()) { <div class="sucesso" role="status">{{ okSenha() }}</div> }
      @if (erroSenha()) { <div class="erro" role="alert">{{ erroSenha() }}</div> }
      <div class="form-grid">
        <div class="campo"><label for="s-atual">Senha atual *</label><input id="s-atual" name="atual" type="password" [(ngModel)]="senhaAtual" required autocomplete="current-password"></div>
        <div class="campo"><label for="s-nova">Nova senha * (mín. 8)</label><input id="s-nova" name="nova" type="password" [(ngModel)]="novaSenha" required minlength="8" autocomplete="new-password"></div>
        <div class="campo"><label for="s-conf">Confirmar nova senha *</label><input id="s-conf" name="conf" type="password" [(ngModel)]="confirmacao" required autocomplete="new-password"></div>
      </div>
      <div class="form-acoes"><button class="primario" [disabled]="salvandoSenha()">Alterar senha</button></div>
    </form>
  `,
})
export class ContaComponent implements OnInit {
  private http = inject(HttpClient);
  readonly auth = inject(AuthService);

  nome = ''; email = ''; telefone = ''; perfis = '';
  senhaAtual = ''; novaSenha = ''; confirmacao = '';
  okDados = signal(''); erroDados = signal(''); salvandoDados = signal(false);
  okSenha = signal(''); erroSenha = signal(''); salvandoSenha = signal(false);

  ngOnInit() {
    this.http.get<Usuario>(`${API}/auth/me`).subscribe({
      next: u => {
        this.nome = u.nome; this.email = u.email; this.telefone = u.telefone ?? '';
        this.perfis = u.perfis.map(p => p.charAt(0) + p.slice(1).toLowerCase()).join(', ');
      },
      error: e => this.erroDados.set(mensagemErro(e)),
    });
  }

  salvarDados() {
    this.okDados.set(''); this.erroDados.set(''); this.salvandoDados.set(true);
    this.http.put<Usuario>(`${API}/auth/me`, { nome: this.nome, telefone: this.telefone || null }).subscribe({
      next: u => {
        this.salvandoDados.set(false);
        this.okDados.set('Dados atualizados.');
        this.auth.atualizarSessao({ nome: u.nome });
      },
      error: e => { this.salvandoDados.set(false); this.erroDados.set(mensagemErro(e)); },
    });
  }

  alterarSenha() {
    this.okSenha.set(''); this.erroSenha.set('');
    if (this.novaSenha !== this.confirmacao) { this.erroSenha.set('A confirmação não confere com a nova senha.'); return; }
    this.salvandoSenha.set(true);
    this.http.post(`${API}/auth/senha`, { senhaAtual: this.senhaAtual, novaSenha: this.novaSenha }).subscribe({
      next: () => {
        this.salvandoSenha.set(false);
        this.senhaAtual = ''; this.novaSenha = ''; this.confirmacao = '';
        this.okSenha.set('Senha alterada.');
      },
      error: e => { this.salvandoSenha.set(false); this.erroSenha.set(mensagemErro(e)); },
    });
  }
}
