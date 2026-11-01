import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { DEMO } from '../../core/demo';
import { AuthService } from '../../core/auth.service';
import { mensagemErro } from '../../core/erro';
import { IconeComponent } from '../../shared/icone.component';
import { LogoComponent } from '../../shared/logo.component';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, RouterLink, LogoComponent, IconeComponent],
  template: `
    <div class="tela">
      <section class="arte" aria-hidden="true">
        <div class="conteudo">
          <app-logo [tamanho]="44" />
          <div class="texto">
            <h2>Seu pet shop organizado, do banho ao fechamento do caixa.</h2>
            <ul>
              <li><app-icone nome="agenda" /> Agenda sem conflito e agendamento online</li>
              <li><app-icone nome="pata" /> Tutores e pets em um só lugar</li>
              <li><app-icone nome="financeiro" /> Estoque, comissões e caixa do mês</li>
            </ul>
          </div>
          <small>© Pelo Macio</small>
        </div>
      </section>

      <main class="lado-form">
        <form class="formulario" (ngSubmit)="entrar()">
          <app-logo class="marca" [tamanho]="40" />
          <h1>Bem-vindo de volta</h1>
          <p class="sub">Entre para ver a agenda de hoje e quem está na mesa.</p>

          @if (erro()) { <div class="erro" role="alert">{{ erro() }}</div> }

          <div class="campo">
            <label for="email">E-mail</label>
            <div class="campo-icone">
              <app-icone nome="email" />
              <input id="email" name="email" type="email" [(ngModel)]="email" required autocomplete="username" placeholder="voce@petshop.com">
            </div>
          </div>
          <div class="campo">
            <label for="senha">Senha</label>
            <div class="campo-icone">
              <app-icone nome="cadeado" />
              <input id="senha" name="senha" type="password" [(ngModel)]="senha" required autocomplete="current-password" placeholder="Sua senha">
            </div>
          </div>
          <button class="primario entrar" [disabled]="carregando()">
            {{ carregando() ? 'Entrando…' : 'Entrar' }}
          </button>

          <p class="rodape-form">Ainda não tem loja? <a routerLink="/cadastro">Criar minha loja</a></p>
          @if (demo.ativo) {
            <p class="demo">Quer só conhecer? <button type="button" class="link" (click)="usarDemo()">Entrar com a conta de demonstração</button></p>
          }
        </form>
      </main>
    </div>
  `,
  styles: [`
    .tela { min-height: 100vh; display: grid; grid-template-columns: minmax(0, 1.05fr) minmax(0, 1fr); background: var(--superficie); }

    /* lado esquerdo: para usar uma foto, defina --foto-login: url(...) no :root; o degradê marinho fica por cima */
    .arte {
      position: relative; overflow: hidden; color: #fff; min-height: 100vh;
      background:
        linear-gradient(160deg, rgba(15, 23, 42, .93) 0%, rgba(15, 23, 42, .86) 55%, rgba(30, 41, 59, .94) 100%),
        var(--foto-login, none),
        var(--marinho);
      background-size: cover; background-position: center;
    }
    .arte::before {
      content: ''; position: absolute; width: 34rem; height: 34rem; right: -12rem; top: -10rem; border-radius: 50%;
      background: radial-gradient(closest-side, rgba(245, 158, 11, .28), transparent);
    }
    .conteudo { position: relative; height: 100%; min-height: 100vh; display: flex; flex-direction: column; justify-content: space-between; padding: 2.5rem 3rem; }
    .texto { max-width: 28rem; }
    .texto h2 { margin: 0 0 1.75rem; color: #fff; font-size: clamp(1.5rem, 2.4vw, 2rem); line-height: 1.25; font-weight: 700; letter-spacing: -.02em; }
    .texto ul { list-style: none; margin: 0; padding: 0; display: grid; gap: .9rem; color: #cbd5e1; }
    .texto li { display: flex; align-items: center; gap: .75rem; }
    .texto li app-icone { color: var(--acento); }
    .conteudo small { color: var(--marinho-txt); }

    .lado-form { display: grid; place-items: center; padding: 2rem 1.5rem; background: var(--superficie); }
    .formulario { width: min(24rem, 100%); }
    .marca { display: none; margin-bottom: 2rem; color: var(--marinho); }
    .formulario .sub { margin: .5rem 0 1.75rem; }
    .entrar { width: 100%; padding: .7rem; margin-top: .5rem; font-size: 1rem; }
    .rodape-form, .demo { margin-top: 1.25rem; text-align: center; font-size: .9rem; color: var(--texto-suave); }
    .demo { margin-top: .5rem; }
    a { font-weight: 600; }
    button.link { display: inline; padding: 0; border: 0; background: none; box-shadow: none; color: var(--acento-texto); font-weight: 600; text-decoration: underline; }
    button.link:hover { background: none; }

    /* celular: o painel some e a marca volta para o topo do formulário */
    @media (max-width: 800px) {
      .tela { grid-template-columns: 1fr; }
      .arte { display: none; }
      .marca { display: inline-flex; }
      .lado-form { align-items: start; padding: 2.5rem 1.25rem; }
    }
  `],
})
export class LoginComponent {
  private auth = inject(AuthService);
  private router = inject(Router);
  readonly demo = DEMO;
  email = '';
  senha = '';
  erro = signal('');
  carregando = signal(false);

  usarDemo() {
    this.email = DEMO.email;
    this.senha = DEMO.senha;
    this.entrar();
  }

  entrar() {
    this.erro.set('');
    this.carregando.set(true);
    this.auth.login(this.email, this.senha).subscribe({
      next: () => this.router.navigate(['/dashboard']),
      error: e => { this.erro.set(mensagemErro(e)); this.carregando.set(false); },
    });
  }
}
