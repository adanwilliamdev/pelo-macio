import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { mensagemErro } from '../../core/erro';
import { LogoComponent } from '../../shared/logo.component';

/** Onboarding: cria a loja e o administrador e já entra no painel. */
@Component({
  selector: 'app-cadastro',
  standalone: true,
  imports: [FormsModule, RouterLink, LogoComponent],
  template: `
    <main class="pagina">
      <form class="card formulario" (ngSubmit)="criar()">
        <app-logo class="marca" [tamanho]="40" />
        <h1>Crie a sua loja</h1>
        <p class="sub">Em um minuto você tem agenda, cadastro de pets e uma página para os tutores marcarem sozinhos.</p>

        @if (erro()) { <div class="erro" role="alert">{{ erro() }}</div> }

        <div class="campo"><label for="c-loja">Nome do pet shop *</label>
          <input id="c-loja" name="loja" [(ngModel)]="nomeLoja" required maxlength="120" placeholder="Ex.: Patas Felizes"></div>
        <div class="campo"><label for="c-nome">Seu nome *</label>
          <input id="c-nome" name="nome" [(ngModel)]="nome" required maxlength="120" autocomplete="name"></div>
        <div class="campo"><label for="c-email">E-mail *</label>
          <input id="c-email" name="email" type="email" [(ngModel)]="email" required autocomplete="username"></div>
        <div class="campo"><label for="c-senha">Senha * <span class="suave">(8 caracteres ou mais)</span></label>
          <input id="c-senha" name="senha" type="password" [(ngModel)]="senha" required minlength="8" maxlength="72" autocomplete="new-password"></div>

        <button class="primario criar" [disabled]="carregando()">{{ carregando() ? 'Criando…' : 'Criar loja' }}</button>
        <p class="rodape">Já tem conta? <a routerLink="/login">Entrar</a></p>
      </form>
    </main>
  `,
  styles: [`
    .pagina { min-height: 100vh; display: grid; place-items: center; padding: 2rem 1rem; }
    .formulario { width: min(28rem, 100%); padding: 2rem; }
    .marca { display: inline-flex; margin-bottom: 1.5rem; color: var(--marinho); }
    .sub { margin: .4rem 0 1.5rem; }
    .criar { width: 100%; padding: .7rem; font-size: 1rem; margin-top: .5rem; }
    .rodape { margin-top: 1.25rem; text-align: center; font-size: .9rem; color: var(--texto-suave); }
    .rodape a { font-weight: 600; }
  `],
})
export class CadastroComponent {
  private auth = inject(AuthService);
  private router = inject(Router);
  nomeLoja = '';
  nome = '';
  email = '';
  senha = '';
  erro = signal('');
  carregando = signal(false);

  criar() {
    this.erro.set('');
    this.carregando.set(true);
    this.auth.cadastrar({ nomeLoja: this.nomeLoja, nome: this.nome, email: this.email, senha: this.senha }).subscribe({
      next: () => this.router.navigate(['/loja']),
      error: e => { this.erro.set(mensagemErro(e)); this.carregando.set(false); },
    });
  }
}
