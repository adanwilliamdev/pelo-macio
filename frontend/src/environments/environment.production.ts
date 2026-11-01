/**
 * Produção: por padrão a API fica atrás do mesmo domínio (nginx faz proxy de /api).
 * Para servir a API em outro domínio, troque por ex. 'https://api.seudominio.com/api'
 * e inclua a origem do frontend em PELOMACIO_CORS_ORIGINS no backend.
 */
export const environment = {
  production: true,
  apiUrl: '/api',
};
