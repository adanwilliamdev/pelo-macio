export type Perfil = 'ADMINISTRADOR' | 'PROFISSIONAL' | 'CLIENTE';
export type StatusAgendamento = 'AGENDADO' | 'CONFIRMADO' | 'CONCLUIDO' | 'CANCELADO' | 'FALTOU';

export type Porte = 'PEQUENO' | 'MEDIO' | 'GRANDE';
export type Especie = 'CACHORRO' | 'GATO' | 'OUTRO';

export interface Sessao { token: string; nome: string; email: string; perfis: Perfil[]; lojaNome: string; lojaSlug: string; }

export interface Cliente { id?: number; nome: string; telefone?: string; email?: string; observacoes?: string; ativo?: boolean; }
export interface Pet {
  id?: number; cliente?: Cliente; nome: string; especie: Especie; raca?: string; porte: Porte;
  pelagem?: string; alergias?: string; observacoes?: string; ativo?: boolean;
}
export interface Profissional { id?: number; nome: string; especialidade?: string; comissaoPadrao: number; googleCalendarId?: string; ativo?: boolean; }
export interface Produto { id?: number; nome: string; tipo?: string; quantidadeEstoque: number; unidade?: string; custoUnitario: number; estoqueMinimo: number; }
export interface ServicoInsumo { id?: number; produto: { id: number; nome?: string; unidade?: string }; quantidade: number; }
export interface Servico {
  id?: number; nome: string; categoria?: string; ativo?: boolean; insumos: ServicoInsumo[];
  /** preço e duração do porte pequeno (base); médio e grande, se vazios, valem a base */
  duracaoMinutos: number; preco: number;
  duracaoMedio?: number | null; precoMedio?: number | null; duracaoGrande?: number | null; precoGrande?: number | null;
}

export interface Agendamento {
  id: number; cliente: Cliente; pet: Pet; profissional: Profissional; servico: Servico;
  dataHoraInicio: string; dataHoraFim: string; status: StatusAgendamento; encaixe: boolean;
  origem: 'INTERNA' | 'ONLINE'; googleEventId?: string | null;
}
export interface AgendamentoRequest { petId: number; profissionalId: number; servicoId: number; inicio: string; encaixe: boolean; }

export interface Loja {
  id: number; nome: string; slug: string; telefone?: string | null; abertura: string; fechamento: string;
  intervaloMinutos: number; diasFuncionamento: number[]; agendamentoOnline: boolean;
}

// ───── página pública de agendamento ─────
export interface LojaPublica { nome: string; slug: string; telefone?: string | null; abertura: string; fechamento: string; diasFuncionamento: number[]; }
export interface ServicoPublico {
  id: number; nome: string; categoria?: string;
  precoPequeno: number; duracaoPequeno: number; precoMedio: number; duracaoMedio: number; precoGrande: number; duracaoGrande: number;
}
export interface ProfissionalPublico { id: number; nome: string; especialidade?: string; }
export interface HorarioLivre { inicio: string; profissionalId: number; profissionalNome: string; }
export interface ReservaPublica {
  nome: string; telefone: string; email?: string; petNome: string; especie: Especie; raca?: string; porte: Porte;
  servicoId: number; profissionalId: number | null; inicio: string;
}
export interface ReservaConfirmada { protocolo: number; inicio: string; loja: string; servico: string; profissional: string; pet: string; valor: number; }

export interface Lancamento {
  id?: number; tipo: 'RECEITA' | 'DESPESA'; categoria: string; descricao: string; valor: number; data: string;
  profissional?: Profissional | null;
}
export interface ResumoFinanceiro { receitas: number; despesas: number; saldo: number; }
export interface ComissaoResumo { profissionalId: number; profissional: string; total: number; }

export interface Dashboard {
  agendamentosHoje: number; clientesAtivos: number; receitaMes: number | null; receitaSerie: number[] | null;
  proximos: Agendamento[]; estoqueBaixo: Produto[];
}

export interface Usuario {
  id: number; nome: string; email: string; telefone?: string | null; ativo: boolean; perfis: Perfil[];
  profissionalId?: number | null; profissionalNome?: string | null; clienteId?: number | null; clienteNome?: string | null;
}
export interface OpcaoVinculo { id: number; nome: string; usuarioId: number | null; }
export interface OpcoesVinculo { profissionais: OpcaoVinculo[]; clientes: OpcaoVinculo[]; }

export interface GoogleConexao {
  profissionalId: number; profissionalNome: string; conectado: boolean; reautorizar: boolean;
  email?: string | null; conectadoEm?: string | null; calendarId: string;
}
export interface GoogleStatus { configurado: boolean; redirectUri: string; profissionais: GoogleConexao[]; }
export interface Sincronizacao { sincronizados: number; falhas: number; restantes: number; }
