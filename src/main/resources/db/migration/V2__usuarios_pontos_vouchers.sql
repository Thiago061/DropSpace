-- Usuários (app mobile e administradores)
CREATE TABLE usuarios (
  id BIGSERIAL PRIMARY KEY,
  nome VARCHAR(120) NOT NULL,
  login VARCHAR(160) NOT NULL UNIQUE,
  senha_hash VARCHAR(100) NOT NULL,
  papel VARCHAR(20) NOT NULL DEFAULT 'USER' CHECK (papel IN ('USER','ADMIN')),
  plano VARCHAR(20) NOT NULL DEFAULT 'FREE' CHECK (plano IN ('FREE','PRO')),
  plano_expira_em TIMESTAMPTZ,
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Pontos de apoio: água, descanso/sombra, abrigo contra frio e mercados parceiros
CREATE TABLE pontos_apoio (
  id BIGSERIAL PRIMARY KEY,
  nome VARCHAR(120) NOT NULL,
  tipo VARCHAR(30) NOT NULL CHECK (tipo IN ('PONTO_AGUA','DESCANSO_SOMBRA','ABRIGO_FRIO','MERCADO_PARCEIRO')),
  latitude DOUBLE PRECISION NOT NULL CHECK (latitude BETWEEN -90 AND 90),
  longitude DOUBLE PRECISION NOT NULL CHECK (longitude BETWEEN -180 AND 180),
  descricao VARCHAR(500),
  endereco VARCHAR(200),
  horario VARCHAR(120),
  gratuito BOOLEAN NOT NULL DEFAULT TRUE,
  sombra BOOLEAN NOT NULL DEFAULT FALSE,
  voucher_descricao VARCHAR(160),
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  ponto GEOGRAPHY(POINT, 4326) GENERATED ALWAYS AS
    (ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)::geography) STORED,
  criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_pontos_apoio_ponto ON pontos_apoio USING GIST (ponto);

-- Vouchers do plano Pro (troca por produto em mercado parceiro)
CREATE TABLE vouchers (
  id BIGSERIAL PRIMARY KEY,
  codigo VARCHAR(20) NOT NULL UNIQUE,
  usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
  ponto_id BIGINT NOT NULL REFERENCES pontos_apoio(id),
  descricao VARCHAR(160) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ATIVO' CHECK (status IN ('ATIVO','RESGATADO')),
  emitido_em TIMESTAMPTZ NOT NULL,
  expira_em TIMESTAMPTZ NOT NULL,
  resgatado_em TIMESTAMPTZ
);
CREATE INDEX idx_vouchers_usuario ON vouchers(usuario_id, emitido_em DESC);

-- Consultas espaciais de ocorrências
ALTER TABLE ocorrencias ADD COLUMN ponto GEOGRAPHY(POINT, 4326) GENERATED ALWAYS AS (
  CASE WHEN latitude IS NOT NULL AND longitude IS NOT NULL
       THEN ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)::geography END) STORED;
CREATE INDEX idx_ocorrencias_ponto ON ocorrencias USING GIST (ponto);

-- Mantém atualizada_em coerente quando o status muda
CREATE FUNCTION atualiza_timestamp() RETURNS trigger AS $$
BEGIN NEW.atualizada_em = CURRENT_TIMESTAMP; RETURN NEW; END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER trg_ocorrencias_atualizada BEFORE UPDATE ON ocorrencias
  FOR EACH ROW EXECUTE FUNCTION atualiza_timestamp();
