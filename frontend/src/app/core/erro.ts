import { HttpErrorResponse } from '@angular/common/http';

/** Converte a resposta de erro da API ({message, campos}) em texto legível. */
export function mensagemErro(e: unknown): string {
  if (e instanceof HttpErrorResponse) {
    if (e.status === 0) return 'Não foi possível conectar ao servidor.';
    if (e.status === 403) return 'Você não tem permissão para esta ação.';
    const corpo = e.error;
    if (corpo?.campos) {
      return Object.values<string>(corpo.campos).join(' · ');
    }
    if (corpo?.message) return corpo.message;
  }
  return 'Ocorreu um erro inesperado.';
}
