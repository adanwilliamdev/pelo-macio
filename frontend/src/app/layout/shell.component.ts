import { Component, computed, inject, OnInit } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { IconeComponent, NomeIcone } from '../shared/icone.component';
import { LogoComponent } from '../shared/logo.component';

interface ItemMenu { rota: string; rotulo: string; icone: NomeIcone; acesso?: 'equipe' | 'admin'; }

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, IconeComponent, LogoComponent],
  template: `
    <div class="app">
      <aside class="lateral">
        <a class="marca" routerLink="/dashboard" aria-label="Pelo Macio, ir para o painel">
          <app-logo [tamanho]="36" />
        </a>

        <nav aria-label="Principal">
          @for (item of menu; track item.rota) {
            @if (visivel(item)) {
              <a [routerLink]="item.rota" routerLinkActive="ativo">
                <app-icone [nome]="item.icone" />
                <span>{{ item.rotulo }}</span>
              </a>
            }
          }
        </nav>

        <div class="perfil">
          <span class="avatar">{{ inicial() }}</span>
          <div class="quem">
            <strong>{{ auth.sessao()?.nome }}</strong>
            <small [title]="papel()">{{ auth.sessao()?.lojaNome }}</small>
          </div>
          <button class="sair" type="button" aria-label="Sair" title="Sair" (click)="auth.logout()">
            <app-icone nome="sair" />
          </button>
        </div>
      </aside>

      <main><router-outlet /></main>
    </div>
  `,
  styles: [`
    .app { display: flex; min-height: 100vh; }

    /* barra lateral em marinho */
    .lateral {
      position: sticky; top: 0; flex: none; width: 248px; height: 100vh;
      display: flex; flex-direction: column; gap: 1.5rem; padding: 1.4rem .85rem 1rem;
      color: var(--marinho-txt); overflow: hidden; background: var(--marinho);
    }
    .marca { display: flex; align-items: center; padding: 0 .55rem; color: #fff; text-decoration: none; }

    nav { display: flex; flex-direction: column; gap: 2px; flex: 1; overflow-y: auto; scrollbar-width: none; }
    nav a {
      position: relative; display: flex; align-items: center; gap: .8rem; padding: .6rem .75rem;
      border-radius: var(--raio-m); color: var(--marinho-txt); text-decoration: none; font-size: .9rem; font-weight: 500;
      transition: background .15s, color .15s;
    }
    nav a:hover { background: rgba(255, 255, 255, .06); color: #fff; }
    /* item ativo: pílula suave + indicador âmbar na lateral */
    nav a.ativo { color: #fff; background: rgba(255, 255, 255, .1); }
    nav a.ativo::before {
      content: ''; position: absolute; left: -.85rem; top: 22%; bottom: 22%; width: 3px;
      border-radius: 0 3px 3px 0; background: var(--acento);
    }
    nav a.ativo app-icone { color: var(--acento); }
    nav a:focus-visible, .sair:focus-visible, .marca:focus-visible { outline-color: var(--acento); outline-offset: -2px; }

    .perfil { display: flex; align-items: center; gap: .7rem; padding: .75rem .55rem 0; border-top: 1px solid rgba(255, 255, 255, .08); }
    .avatar {
      flex: none; width: 2.25rem; height: 2.25rem; display: grid; place-items: center;
      border-radius: 50%; background: var(--acento); color: var(--marinho);
      font-size: .95rem; font-weight: 700;
    }
    .quem { display: flex; flex-direction: column; min-width: 0; flex: 1; line-height: 1.25; }
    .quem strong { font-size: .875rem; font-weight: 600; color: #fff; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .quem small { color: var(--marinho-txt); font-size: .75rem; }
    .sair { flex: none; width: 2.25rem; height: 2.25rem; padding: 0; color: var(--marinho-txt); background: transparent; border-color: transparent; box-shadow: none; }
    .sair:hover { background: rgba(255, 255, 255, .1); border-color: transparent; color: #fff; }

    main { flex: 1; min-width: 0; max-width: 1180px; margin: 0 auto; padding: 2rem 2rem 3rem; }

    /* celular e tablet: a barra vira uma faixa fixa na base */
    @media (max-width: 900px) {
      .app { flex-direction: column; padding-bottom: 5rem; }
      .lateral {
        position: fixed; z-index: 30; top: auto; left: 0; right: 0; bottom: 0; width: auto; height: auto;
        flex-direction: row; align-items: center; gap: .25rem; padding: .4rem .5rem;
        border-top: 1px solid rgba(255, 255, 255, .08);
      }
      .marca, .quem { display: none; }
      nav { flex-direction: row; overflow-x: auto; overflow-y: hidden; gap: .15rem; }
      nav a { flex: none; flex-direction: column; gap: .1rem; padding: .4rem .75rem; font-size: .68rem; }
      nav a.ativo::before { top: 0; bottom: auto; left: 25%; right: 25%; width: auto; height: 3px; border-radius: 0 0 3px 3px; }
      .perfil { flex: none; padding: 0; border: 0; }
      .avatar { display: none; }
      main { padding: 1.4rem 1rem 1rem; }
    }
  `],
})
export class ShellComponent implements OnInit {
  auth = inject(AuthService);

  menu: ItemMenu[] = [
    { rota: '/dashboard', rotulo: 'Painel', icone: 'painel', acesso: 'equipe' },
    { rota: '/agenda', rotulo: 'Agenda', icone: 'agenda', acesso: 'equipe' },
    { rota: '/clientes', rotulo: 'Tutores', icone: 'clientes', acesso: 'equipe' },
    { rota: '/pets', rotulo: 'Pets', icone: 'pata', acesso: 'equipe' },
    { rota: '/profissionais', rotulo: 'Equipe', icone: 'profissionais', acesso: 'equipe' },
    { rota: '/servicos', rotulo: 'Serviços', icone: 'servicos', acesso: 'equipe' },
    { rota: '/estoque', rotulo: 'Estoque', icone: 'estoque', acesso: 'equipe' },
    { rota: '/financeiro', rotulo: 'Financeiro', icone: 'financeiro', acesso: 'admin' },
    { rota: '/google-calendar', rotulo: 'Google Agenda', icone: 'sincronizar', acesso: 'equipe' },
    { rota: '/usuarios', rotulo: 'Usuários', icone: 'usuarios', acesso: 'admin' },
    { rota: '/loja', rotulo: 'Minha loja', icone: 'loja', acesso: 'admin' },
    { rota: '/conta', rotulo: 'Minha conta', icone: 'conta' },
  ];

  ngOnInit() {
    // atualiza nome/perfis; se a conta foi desativada o interceptor recebe 401 e desloga
    this.auth.sincronizar().subscribe({ error: () => {} });
  }

  visivel(item: ItemMenu): boolean {
    if (item.acesso === 'admin') return this.auth.isAdmin();
    if (item.acesso === 'equipe') return this.auth.isEquipe();
    return true;
  }


  inicial = computed(() => (this.auth.sessao()?.nome ?? '?').trim().charAt(0).toUpperCase());
  papel = computed(() => (this.auth.isAdmin() ? 'Administrador' : this.auth.temPerfil('PROFISSIONAL') ? 'Profissional' : 'Tutor'));
}
