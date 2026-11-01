import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap } from 'rxjs';
import { Perfil, Sessao, Usuario } from './models';
import { API } from './api';

const CHAVE = 'pelomacio.sessao';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  readonly sessao = signal<Sessao | null>(this.carregar());
  readonly logado = computed(() => this.sessao() !== null);
  readonly isAdmin = computed(() => this.temPerfil('ADMINISTRADOR'));
  /** Acessa as telas internas (o cliente ainda não tem portal). */
  readonly isEquipe = computed(() => this.temPerfil('ADMINISTRADOR') || this.temPerfil('PROFISSIONAL'));

  login(email: string, senha: string) {
    return this.http.post<Sessao>(`${API}/auth/login`, { email, senha }).pipe(tap(s => this.iniciar(s)));
  }

  /** Cria uma loja nova (com o administrador) e já entra nela. */
  cadastrar(dados: { nomeLoja: string; nome: string; email: string; senha: string }) {
    return this.http.post<Sessao>(`${API}/publico/cadastro`, dados).pipe(tap(s => this.iniciar(s)));
  }

  private iniciar(s: Sessao) {
    localStorage.setItem(CHAVE, JSON.stringify(s));
    this.sessao.set(s);
  }

  logout() {
    localStorage.removeItem(CHAVE);
    this.sessao.set(null);
    this.router.navigate(['/login']);
  }

  /** Busca nome e perfis atuais no servidor (mudanças feitas por um administrador valem sem novo login). */
  sincronizar() {
    return this.http.get<Usuario>(`${API}/auth/me`).pipe(
      tap(u => this.atualizarSessao({ nome: u.nome, perfis: u.perfis }))
    );
  }

  atualizarSessao(parcial: Partial<Pick<Sessao, 'nome' | 'perfis'>>) {
    const atual = this.sessao();
    if (!atual) return;
    const nova = { ...atual, ...parcial };
    localStorage.setItem(CHAVE, JSON.stringify(nova));
    this.sessao.set(nova);
  }

  token(): string | null {
    return this.sessao()?.token ?? null;
  }

  temPerfil(perfil: Perfil): boolean {
    return this.sessao()?.perfis.includes(perfil) ?? false;
  }

  private carregar(): Sessao | null {
    try {
      const bruto = localStorage.getItem(CHAVE);
      return bruto ? (JSON.parse(bruto) as Sessao) : null;
    } catch {
      return null;
    }
  }
}
