import { HttpClient } from '@angular/common/http';
import { API } from '../core/api';

export interface Campo {
  chave: string;
  rotulo: string;
  tipo: 'text' | 'email' | 'tel' | 'number' | 'textarea' | 'select' | 'checkbox';
  obrigatorio?: boolean;
  passo?: string;
  padrao?: unknown;
  /** select com opções fixas */
  opcoes?: { valor: string; rotulo: string }[];
  /** select carregado da API; o valor enviado é { id } */
  origem?: { url: string; rotulo: string };
}

export interface Coluna {
  rotulo: string;
  valor: (item: any) => string | number;
}

export interface AcaoExtra {
  rotulo: string;
  executar: (item: any, ctx: { http: HttpClient; recarregar: () => void; erro: (e: unknown) => void }) => void;
}

export interface CrudConfig {
  titulo: string;
  singular: string;
  url: string;
  campos: Campo[];
  colunas: Coluna[];
  busca?: boolean;
  /** Apenas ADMINISTRADOR cria/edita (a exclusão é sempre só do administrador). */
  escritaSomenteAdmin?: boolean;
  /** Usa "Nova/esta" no lugar de "Novo/este" nos títulos e confirmações. */
  feminino?: boolean;
  acoes?: AcaoExtra[];
}

export const CLIENTES: CrudConfig = {
  titulo: 'Tutores', singular: 'tutor', url: `${API}/clientes`, busca: true,
  campos: [
    { chave: 'nome', rotulo: 'Nome', tipo: 'text', obrigatorio: true },
    { chave: 'telefone', rotulo: 'Telefone (WhatsApp)', tipo: 'tel' },
    { chave: 'email', rotulo: 'E-mail', tipo: 'email' },
    { chave: 'ativo', rotulo: 'Ativo', tipo: 'checkbox', padrao: true },
    { chave: 'observacoes', rotulo: 'Observações', tipo: 'textarea' },
  ],
  colunas: [
    { rotulo: 'Nome', valor: c => c.nome },
    { rotulo: 'Telefone', valor: c => c.telefone ?? '' },
    { rotulo: 'E-mail', valor: c => c.email ?? '' },
    { rotulo: 'Ativo', valor: c => (c.ativo ? 'Sim' : 'Não') },
  ],
};

const ESPECIES = [{ valor: 'CACHORRO', rotulo: 'Cachorro' }, { valor: 'GATO', rotulo: 'Gato' }, { valor: 'OUTRO', rotulo: 'Outro' }];
const PORTES = [{ valor: 'PEQUENO', rotulo: 'Pequeno' }, { valor: 'MEDIO', rotulo: 'Médio' }, { valor: 'GRANDE', rotulo: 'Grande' }];
const rotuloDe = (lista: { valor: string; rotulo: string }[], v?: string) => lista.find(o => o.valor === v)?.rotulo ?? '';

export const PETS: CrudConfig = {
  titulo: 'Pets', singular: 'pet', url: `${API}/pets`, busca: true,
  campos: [
    { chave: 'cliente', rotulo: 'Tutor', tipo: 'select', obrigatorio: true, origem: { url: `${API}/clientes`, rotulo: 'nome' } },
    { chave: 'nome', rotulo: 'Nome do pet', tipo: 'text', obrigatorio: true },
    { chave: 'especie', rotulo: 'Espécie', tipo: 'select', obrigatorio: true, opcoes: ESPECIES, padrao: 'CACHORRO' },
    { chave: 'raca', rotulo: 'Raça', tipo: 'text' },
    { chave: 'porte', rotulo: 'Porte (define preço e duração)', tipo: 'select', obrigatorio: true, opcoes: PORTES, padrao: 'PEQUENO' },
    { chave: 'pelagem', rotulo: 'Pelagem (curta, longa, dupla…)', tipo: 'text' },
    { chave: 'alergias', rotulo: 'Alergias', tipo: 'text' },
    { chave: 'ativo', rotulo: 'Ativo', tipo: 'checkbox', padrao: true },
    { chave: 'observacoes', rotulo: 'Cuidados (temperamento, vacinas, medicamentos)', tipo: 'textarea' },
  ],
  colunas: [
    { rotulo: 'Pet', valor: p => p.nome },
    { rotulo: 'Tutor', valor: p => p.cliente?.nome ?? '' },
    { rotulo: 'Espécie', valor: p => rotuloDe(ESPECIES, p.especie) },
    { rotulo: 'Raça', valor: p => p.raca ?? '' },
    { rotulo: 'Porte', valor: p => rotuloDe(PORTES, p.porte) },
  ],
};

export const PROFISSIONAIS: CrudConfig = {
  titulo: 'Equipe', singular: 'profissional', url: `${API}/profissionais`, escritaSomenteAdmin: true,
  campos: [
    { chave: 'nome', rotulo: 'Nome', tipo: 'text', obrigatorio: true },
    { chave: 'especialidade', rotulo: 'Especialidade', tipo: 'text' },
    { chave: 'comissaoPadrao', rotulo: 'Comissão (%)', tipo: 'number', passo: '0.01', obrigatorio: true, padrao: 0 },
    { chave: 'googleCalendarId', rotulo: 'Agenda do Google (vazio = principal)', tipo: 'text' },
    { chave: 'ativo', rotulo: 'Ativo', tipo: 'checkbox', padrao: true },
  ],
  colunas: [
    { rotulo: 'Nome', valor: p => p.nome },
    { rotulo: 'Especialidade', valor: p => p.especialidade ?? '' },
    { rotulo: 'Comissão', valor: p => `${p.comissaoPadrao}%` },
    { rotulo: 'Ativo', valor: p => (p.ativo ? 'Sim' : 'Não') },
  ],
};

export const PRODUTOS: CrudConfig = {
  titulo: 'Estoque', singular: 'produto', url: `${API}/produtos`, busca: true, escritaSomenteAdmin: true,
  campos: [
    { chave: 'nome', rotulo: 'Produto', tipo: 'text', obrigatorio: true },
    { chave: 'tipo', rotulo: 'Tipo', tipo: 'text' },
    { chave: 'unidade', rotulo: 'Unidade (g, ml, un)', tipo: 'text' },
    { chave: 'quantidadeEstoque', rotulo: 'Quantidade em estoque', tipo: 'number', passo: '0.001', obrigatorio: true, padrao: 0 },
    { chave: 'estoqueMinimo', rotulo: 'Estoque mínimo', tipo: 'number', passo: '0.001', obrigatorio: true, padrao: 0 },
    { chave: 'custoUnitario', rotulo: 'Custo unitário (R$)', tipo: 'number', passo: '0.01', obrigatorio: true, padrao: 0 },
  ],
  colunas: [
    { rotulo: 'Produto', valor: p => p.nome },
    { rotulo: 'Tipo', valor: p => p.tipo ?? '' },
    { rotulo: 'Estoque', valor: p => `${p.quantidadeEstoque} ${p.unidade ?? ''}` },
    { rotulo: 'Mínimo', valor: p => p.estoqueMinimo },
    { rotulo: 'Custo unit.', valor: p => Number(p.custoUnitario).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' }) },
  ],
  acoes: [{
    rotulo: 'Entrada',
    executar: (p, { http, recarregar, erro }) => {
      const q = window.prompt(`Quantidade a dar entrada de "${p.nome}" (${p.unidade ?? 'un'}):`);
      const quantidade = q ? Number(q.replace(',', '.')) : NaN;
      if (!(quantidade > 0)) return;
      const c = window.prompt('Custo total da compra em R$ (opcional, deixe vazio para não lançar despesa):');
      const custoTotal = c ? Number(c.replace(',', '.')) : null;
      http.post(`${API}/produtos/${p.id}/entrada`, { quantidade, custoTotal }).subscribe({ next: recarregar, error: erro });
    },
  }],
};
