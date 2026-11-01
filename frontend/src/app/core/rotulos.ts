import { StatusAgendamento } from './models';

const STATUS: Record<StatusAgendamento, string> = {
  AGENDADO: 'Agendado', CONFIRMADO: 'Confirmado', CONCLUIDO: 'Concluído', CANCELADO: 'Cancelado', FALTOU: 'Faltou',
};
const CATEGORIA: Record<string, string> = {
  OUTROS: 'Outros', SERVICO: 'Serviço', COMPRA_ESTOQUE: 'Compra de estoque', COMISSAO: 'Comissão',
};

/** "COMPRA_ESTOQUE" -> "Compra estoque" quando não há rótulo cadastrado. */
function humanizar(v: string): string {
  const t = v.replace(/_/g, ' ').toLowerCase();
  return t.charAt(0).toUpperCase() + t.slice(1);
}

export const statusRotulo = (s: string) => STATUS[s as StatusAgendamento] ?? humanizar(s);
export const tipoRotulo = (t: string) => humanizar(t);
export const categoriaRotulo = (c: string) => CATEGORIA[c] ?? humanizar(c);
