import { Routes } from '@angular/router';
import { adminGuard, authGuard, equipeGuard } from './core/auth.guard';
import { CLIENTES, PETS, PROFISSIONAIS, PRODUTOS } from './shared/crud-configs';

export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./pages/login/login.component').then(m => m.LoginComponent) },
  { path: 'cadastro', loadComponent: () => import('./pages/cadastro/cadastro.component').then(m => m.CadastroComponent) },
  // página pública de agendamento: sem login, a loja vem do endereço
  { path: 'agendar/:slug', loadComponent: () => import('./pages/agendar/agendar.component').then(m => m.AgendarComponent) },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./layout/shell.component').then(m => m.ShellComponent),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      // qualquer usuário autenticado
      { path: 'conta', loadComponent: () => import('./pages/conta/conta.component').then(m => m.ContaComponent) },
      // telas internas (ADMINISTRADOR ou PROFISSIONAL)
      {
        path: '',
        canActivate: [equipeGuard],
        children: [
          { path: 'dashboard', loadComponent: () => import('./pages/dashboard/dashboard.component').then(m => m.DashboardComponent) },
          { path: 'agenda', loadComponent: () => import('./pages/agenda/agenda.component').then(m => m.AgendaComponent) },
          { path: 'clientes', data: { config: CLIENTES }, loadComponent: () => import('./shared/crud-page.component').then(m => m.CrudPageComponent) },
          { path: 'pets', data: { config: PETS }, loadComponent: () => import('./shared/crud-page.component').then(m => m.CrudPageComponent) },
          { path: 'profissionais', data: { config: PROFISSIONAIS }, loadComponent: () => import('./shared/crud-page.component').then(m => m.CrudPageComponent) },
          { path: 'servicos', loadComponent: () => import('./pages/servicos/servicos.component').then(m => m.ServicosComponent) },
          { path: 'estoque', data: { config: PRODUTOS }, loadComponent: () => import('./shared/crud-page.component').then(m => m.CrudPageComponent) },
          { path: 'google-calendar', loadComponent: () => import('./pages/google/google-calendar.component').then(m => m.GoogleCalendarComponent) },
          { path: 'financeiro', canActivate: [adminGuard], loadComponent: () => import('./pages/financeiro/financeiro.component').then(m => m.FinanceiroComponent) },
          { path: 'loja', canActivate: [adminGuard], loadComponent: () => import('./pages/loja/loja.component').then(m => m.LojaComponent) },
          { path: 'usuarios', canActivate: [adminGuard], loadComponent: () => import('./pages/usuarios/usuarios.component').then(m => m.UsuariosComponent) },
        ],
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
