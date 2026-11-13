CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE regioes (
  id BIGSERIAL PRIMARY KEY,
  nome VARCHAR(120) NOT NULL,
  tipo VARCHAR(60) NOT NULL DEFAULT 'Área de interesse',
  descricao VARCHAR(500),
  criada_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE localizacoes (
  id BIGSERIAL PRIMARY KEY,
  nome VARCHAR(120) NOT NULL,
  latitude DOUBLE PRECISION NOT NULL CHECK (latitude BETWEEN -90 AND 90),
  longitude DOUBLE PRECISION NOT NULL CHECK (longitude BETWEEN -180 AND 180),
  descricao VARCHAR(500),
  regiao_id BIGINT REFERENCES regioes(id),
  ponto GEOGRAPHY(POINT, 4326) GENERATED ALWAYS AS
    (ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)::geography) STORED,
  criada_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_localizacoes_ponto ON localizacoes USING GIST (ponto);

CREATE TABLE medicoes_ambientais (
  id BIGSERIAL PRIMARY KEY,
  localizacao_id BIGINT REFERENCES localizacoes(id),
  nome_local VARCHAR(120) NOT NULL,
  temperatura_c NUMERIC(6,2),
  umidade_percentual NUMERIC(5,2) CHECK (umidade_percentual BETWEEN 0 AND 100),
  condicao VARCHAR(120) NOT NULL DEFAULT 'Não informado',
  fonte VARCHAR(120) NOT NULL DEFAULT 'Registro manual',
  coletada_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  criada_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ocorrencias (
  id BIGSERIAL PRIMARY KEY,
  titulo VARCHAR(140) NOT NULL,
  categoria VARCHAR(80) NOT NULL,
  severidade VARCHAR(20) NOT NULL DEFAULT 'Moderado' CHECK (severidade IN ('Baixo','Moderado','Alto','Crítico')),
  latitude DOUBLE PRECISION CHECK (latitude BETWEEN -90 AND 90),
  longitude DOUBLE PRECISION CHECK (longitude BETWEEN -180 AND 180),
  descricao TEXT,
  status VARCHAR(30) NOT NULL DEFAULT 'Aberto' CHECK (status IN ('Aberto','Em análise','Validado','Em atendimento','Resolvido','Falso positivo','Cancelado')),
  criada_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  atualizada_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_ocorrencias_status ON ocorrencias(status);
