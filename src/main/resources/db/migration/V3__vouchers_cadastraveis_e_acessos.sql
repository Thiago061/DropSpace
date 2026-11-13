-- Acompanhamento de usuários: último acesso e contagem de acessos
ALTER TABLE usuarios
  ADD COLUMN ultimo_login_em TIMESTAMPTZ,
  ADD COLUMN total_logins INTEGER NOT NULL DEFAULT 0;

-- Histórico de tentativas de entrada (sucesso e falha), para auditoria
CREATE TABLE acessos (
  id BIGSERIAL PRIMARY KEY,
  usuario_id BIGINT REFERENCES usuarios(id),
  login VARCHAR(160) NOT NULL,
  sucesso BOOLEAN NOT NULL,
  ip VARCHAR(64),
  criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_acessos_criado_em ON acessos (criado_em DESC);

-- Catálogo de ofertas de voucher: um mercado parceiro pode ter várias
CREATE TABLE ofertas_voucher (
  id BIGSERIAL PRIMARY KEY,
  ponto_id BIGINT NOT NULL REFERENCES pontos_apoio(id),
  titulo VARCHAR(120) NOT NULL,
  descricao VARCHAR(300),
  valido_ate DATE,
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_ofertas_ponto ON ofertas_voucher (ponto_id);

-- Aproveita o que já estava cadastrado no campo antigo do ponto
INSERT INTO ofertas_voucher (ponto_id, titulo)
SELECT id, btrim(voucher_descricao) FROM pontos_apoio
WHERE voucher_descricao IS NOT NULL AND btrim(voucher_descricao) <> '';

ALTER TABLE pontos_apoio DROP COLUMN voucher_descricao;
ALTER TABLE vouchers ADD COLUMN oferta_id BIGINT REFERENCES ofertas_voucher(id);
