package br.com.zonageo.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

/** Uma oferta que um mercado parceiro dá a assinantes Pro (ex.: "1 água mineral 500 ml"). */
@Entity
@Table(name = "ofertas_voucher")
public class OfertaVoucher {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "ponto_id", nullable = false)
    private Long pontoId;
    @Column(nullable = false, length = 120)
    private String titulo;
    @Column(length = 300)
    private String descricao;
    @Column(length = 80)
    private String unidade;
    @Column(precision = 10, scale = 2)
    private BigDecimal valor;
    @Column(nullable = false)
    private boolean ativo = true;
    @Column(name = "criado_em", insertable = false, updatable = false)
    private Instant criadoEm;

    /** Oferta permanece disponível enquanto o administrador mantiver o cadastro ativo. */
    public boolean vigente() { return ativo; }

    public Long getId() { return id; }
    public Long getPontoId() { return pontoId; }
    public void setPontoId(Long pontoId) { this.pontoId = pontoId; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getUnidade() { return unidade; }
    public void setUnidade(String unidade) { this.unidade = unidade; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public Instant getCriadoEm() { return criadoEm; }
}
