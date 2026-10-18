-- Produtos das ofertas passam a guardar unidade e preço sugerido.
ALTER TABLE ofertas_voucher
  ADD COLUMN unidade VARCHAR(80),
  ADD COLUMN valor NUMERIC(10, 2);

-- A disponibilidade da oferta passa a durar até o administrador desativá-la.
ALTER TABLE ofertas_voucher DROP COLUMN valido_ate;
