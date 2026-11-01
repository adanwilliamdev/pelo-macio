import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';
import { API } from './api';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.token();
  const publica = req.url.startsWith(`${API}/publico/`);
  const autenticada = token && !publica && !req.url.endsWith(`${API}/auth/login`)
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(autenticada).pipe(
    catchError((e: HttpErrorResponse) => {
      if (e.status === 401 && !publica && !req.url.endsWith(`${API}/auth/login`)) auth.logout();
      return throwError(() => e);
    })
  );
};
