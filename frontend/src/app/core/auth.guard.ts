import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.logado() ? true : inject(Router).createUrlTree(['/login']);
};

export const adminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  if (!auth.logado()) return inject(Router).createUrlTree(['/login']);
  return auth.isAdmin() ? true : inject(Router).createUrlTree(['/dashboard']);
};

/** Telas internas: só ADMINISTRADOR ou PROFISSIONAL. Quem só tem o perfil CLIENTE vai para "Minha conta". */
export const equipeGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  if (!auth.logado()) return inject(Router).createUrlTree(['/login']);
  return auth.isEquipe() ? true : inject(Router).createUrlTree(['/conta']);
};
