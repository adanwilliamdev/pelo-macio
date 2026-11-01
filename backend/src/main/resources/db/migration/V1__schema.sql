-- Pelo Macio: schema multi-loja (discriminador loja_id).
--
-- Regras gerais
--  * Toda tabela de negócio carrega loja_id. A aplicação filtra por ele automaticamente (Hibernate @TenantId).
--  * Os relacionamentos entre tabelas de negócio usam chave estrangeira COMPOSTA (id, loja_id): mesmo que um bug
--    na aplicação tente ligar um registro de uma loja a outro de outra loja, o banco recusa.
--  * Usuários são globais (o e-mail identifica a conta em qualquer loja) e apontam para a sua loja.

CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE lojas (
    id                 BIGSERIAL PRIMARY KEY,
    nome               VARCHAR(120) NOT NULL,
    slug               VARCHAR(60)  NOT NULL UNIQUE,
    telefone           VARCHAR(30),
    abertura           TIME         NOT NULL DEFAULT '09:00',
    fechamento         TIME         NOT NULL DEFAULT '18:00',
    intervalo_minutos  INTEGER      NOT NULL DEFAULT 30 CHECK (intervalo_minutos BETWEEN 5 AND 240),
    -- dias da semana em que a loja abre, no padrão ISO (1 = segunda ... 7 = domingo), separados por vírgula
    dias_funcionamento VARCHAR(20)  NOT NULL DEFAULT '1,2,3,4,5,6',
    agendamento_online BOOLEAN      NOT NULL DEFAULT TRUE,
    criada_em          TIMESTAMP    NOT NULL DEFAULT now(),
    CHECK (abertura < fechamento)
);

CREATE TABLE usuarios (
    id        BIGSERIAL PRIMARY KEY,
    loja_id   BIGINT       NOT NULL REFERENCES lojas(id),
    nome      VARCHAR(120) NOT NULL,
    email     VARCHAR(160) NOT NULL,
    senha     VARCHAR(100) NOT NULL,
    telefone  VARCHAR(30),
    ativo     BOOLEAN      NOT NULL DEFAULT TRUE,
    UNIQUE (id, loja_id)
);
CREATE UNIQUE INDEX uq_usuarios_email ON usuarios (lower(email));
CREATE INDEX idx_usuarios_loja ON usuarios (loja_id);

CREATE TABLE usuario_perfis (
    usuario_id BIGINT      NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    perfil     VARCHAR(30) NOT NULL,
    PRIMARY KEY (usuario_id, perfil)
);

CREATE TABLE clientes (
    id          BIGSERIAL PRIMARY KEY,
    loja_id     BIGINT       NOT NULL REFERENCES lojas(id),
    nome        VARCHAR(120) NOT NULL,
    telefone    VARCHAR(30),
    email       VARCHAR(160),
    usuario_id  BIGINT,
    observacoes TEXT,
    ativo       BOOLEAN      NOT NULL DEFAULT TRUE,
    UNIQUE (id, loja_id),
    FOREIGN KEY (usuario_id, loja_id) REFERENCES usuarios (id, loja_id)
);
CREATE INDEX idx_clientes_loja ON clientes (loja_id, nome);
CREATE INDEX idx_clientes_telefone ON clientes (loja_id, telefone);
CREATE UNIQUE INDEX uq_clientes_usuario ON clientes (usuario_id) WHERE usuario_id IS NOT NULL;

CREATE TABLE pets (
    id          BIGSERIAL PRIMARY KEY,
    loja_id     BIGINT       NOT NULL REFERENCES lojas(id),
    cliente_id  BIGINT       NOT NULL,
    nome        VARCHAR(80)  NOT NULL,
    especie     VARCHAR(20)  NOT NULL,   -- CACHORRO, GATO, OUTRO
    raca        VARCHAR(80),
    porte       VARCHAR(20)  NOT NULL,   -- PEQUENO, MEDIO, GRANDE
    pelagem     VARCHAR(40),
    alergias    VARCHAR(255),
    observacoes TEXT,
    ativo       BOOLEAN      NOT NULL DEFAULT TRUE,
    UNIQUE (id, loja_id),
    FOREIGN KEY (cliente_id, loja_id) REFERENCES clientes (id, loja_id) ON DELETE CASCADE
);
CREATE INDEX idx_pets_cliente ON pets (loja_id, cliente_id);

CREATE TABLE profissionais (
    id                 BIGSERIAL PRIMARY KEY,
    loja_id            BIGINT       NOT NULL REFERENCES lojas(id),
    nome               VARCHAR(120) NOT NULL,
    especialidade      VARCHAR(120),
    usuario_id         BIGINT,
    comissao_padrao    NUMERIC(5,2) NOT NULL DEFAULT 0,
    google_calendar_id VARCHAR(200),
    ativo              BOOLEAN      NOT NULL DEFAULT TRUE,
    UNIQUE (id, loja_id),
    FOREIGN KEY (usuario_id, loja_id) REFERENCES usuarios (id, loja_id)
);
CREATE INDEX idx_profissionais_loja ON profissionais (loja_id);
CREATE UNIQUE INDEX uq_profissionais_usuario ON profissionais (usuario_id) WHERE usuario_id IS NOT NULL;

CREATE TABLE produtos (
    id                 BIGSERIAL PRIMARY KEY,
    loja_id            BIGINT        NOT NULL REFERENCES lojas(id),
    nome               VARCHAR(120)  NOT NULL,
    tipo               VARCHAR(60),
    quantidade_estoque NUMERIC(12,3) NOT NULL DEFAULT 0,
    unidade            VARCHAR(20),
    custo_unitario     NUMERIC(10,2) NOT NULL DEFAULT 0,
    estoque_minimo     NUMERIC(12,3) NOT NULL DEFAULT 0,
    UNIQUE (id, loja_id)
);
CREATE INDEX idx_produtos_loja ON produtos (loja_id);

-- Preço e duração valem para o porte PEQUENO; médio e grande só precisam ser preenchidos se forem diferentes.
CREATE TABLE servicos (
    id               BIGSERIAL PRIMARY KEY,
    loja_id          BIGINT        NOT NULL REFERENCES lojas(id),
    nome             VARCHAR(120)  NOT NULL,
    categoria        VARCHAR(60),
    duracao_minutos  INTEGER       NOT NULL,
    preco            NUMERIC(10,2) NOT NULL,
    duracao_medio    INTEGER,
    preco_medio      NUMERIC(10,2),
    duracao_grande   INTEGER,
    preco_grande     NUMERIC(10,2),
    ativo            BOOLEAN       NOT NULL DEFAULT TRUE,
    UNIQUE (id, loja_id)
);
CREATE INDEX idx_servicos_loja ON servicos (loja_id);

-- Insumos só são alcançados a partir do serviço (que já é filtrado por loja).
CREATE TABLE servico_insumos (
    id         BIGSERIAL PRIMARY KEY,
    servico_id BIGINT        REFERENCES servicos(id) ON DELETE CASCADE,
    produto_id BIGINT        NOT NULL REFERENCES produtos(id),
    quantidade NUMERIC(12,3) NOT NULL
);

CREATE TABLE agendamentos (
    id               BIGSERIAL PRIMARY KEY,
    loja_id          BIGINT      NOT NULL REFERENCES lojas(id),
    cliente_id       BIGINT      NOT NULL,
    pet_id           BIGINT      NOT NULL,
    profissional_id  BIGINT      NOT NULL,
    servico_id       BIGINT      NOT NULL,
    data_hora_inicio TIMESTAMP   NOT NULL,
    data_hora_fim    TIMESTAMP   NOT NULL,
    status           VARCHAR(20) NOT NULL,
    origem           VARCHAR(10) NOT NULL DEFAULT 'INTERNA',   -- INTERNA ou ONLINE (página pública)
    encaixe          BOOLEAN     NOT NULL DEFAULT FALSE,
    lembrete_enviado BOOLEAN     NOT NULL DEFAULT FALSE,
    google_event_id  VARCHAR(200),
    CHECK (data_hora_fim > data_hora_inicio),
    FOREIGN KEY (cliente_id, loja_id)      REFERENCES clientes (id, loja_id),
    FOREIGN KEY (pet_id, loja_id)          REFERENCES pets (id, loja_id),
    FOREIGN KEY (profissional_id, loja_id) REFERENCES profissionais (id, loja_id),
    FOREIGN KEY (servico_id, loja_id)      REFERENCES servicos (id, loja_id),
    UNIQUE (id, loja_id),
    -- Garantia no banco: um profissional não pode ter dois atendimentos sobrepostos, salvo encaixe.
    -- A aplicação checa antes para dar uma mensagem amigável; isto fecha a janela de corrida entre duas
    -- reservas simultâneas do mesmo horário (por exemplo, dois clientes na página pública).
    CONSTRAINT ex_agendamento_sem_sobreposicao
        EXCLUDE USING gist (profissional_id WITH =, tsrange(data_hora_inicio, data_hora_fim) WITH &&)
        WHERE (NOT encaixe AND status NOT IN ('CANCELADO', 'FALTOU'))
);
CREATE INDEX idx_agendamentos_loja_inicio ON agendamentos (loja_id, data_hora_inicio);
CREATE INDEX idx_agendamentos_prof ON agendamentos (profissional_id, data_hora_inicio);

CREATE TABLE lancamentos (
    id              BIGSERIAL PRIMARY KEY,
    loja_id         BIGINT        NOT NULL REFERENCES lojas(id),
    tipo            VARCHAR(10)   NOT NULL,
    categoria       VARCHAR(30)   NOT NULL,
    descricao       VARCHAR(200)  NOT NULL,
    valor           NUMERIC(12,2) NOT NULL,
    data            DATE          NOT NULL,
    agendamento_id  BIGINT,
    profissional_id BIGINT,
    FOREIGN KEY (profissional_id, loja_id) REFERENCES profissionais (id, loja_id),
    FOREIGN KEY (agendamento_id, loja_id)  REFERENCES agendamentos (id, loja_id) ON DELETE SET NULL (agendamento_id)
);
CREATE INDEX idx_lancamentos_loja_data ON lancamentos (loja_id, data);

-- Conexão OAuth2 de cada profissional com a própria conta Google.
-- refresh_token fica cifrado (AES-GCM) pela aplicação; nunca é exposto pela API.
CREATE TABLE google_conexoes (
    id              BIGSERIAL PRIMARY KEY,
    profissional_id BIGINT       NOT NULL UNIQUE REFERENCES profissionais(id) ON DELETE CASCADE,
    email           VARCHAR(160),
    refresh_token   VARCHAR(1000) NOT NULL,
    reautorizar     BOOLEAN      NOT NULL DEFAULT FALSE,
    conectado_em    TIMESTAMP    NOT NULL
);
