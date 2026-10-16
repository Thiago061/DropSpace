package br.com.zonageo.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "pontos_apoio")
public class PontoApoio {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 120)
    private String nome;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private TipoPonto tipo;
    @Column(nullable = false)
    private Double latitude;
    @Column(nullable = false)
    private Double longitude;
    @Column(length = 500)
    private String descricao;
    @Column(length = 200)
    private String endereco;
    @Column(length = 120)
    private String horario;
    @Column(nullable = false)
    private boolean gratuito = true;
    @Column(nullable = false)
    private boolean sombra = false;
    @Column(name = "voucher_descricao", length = 160)
    private String voucherDescricao;
    @Column(nullable = false)
    private boolean ativo = true;
    @Column(name = "criado_em", insertable = false, updatable = false)
    private Instant criadoEm;

    public boolean isParceiro() { return tipo == TipoPonto.MERCADO_PARCEIRO; }
    public boolean oferecaVoucher() {
        return isParceiro() && voucherDescricao != null && !voucherDescricao.isBlank();
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public TipoPonto getTipo() { return tipo; }
    public void setTipo(TipoPonto tipo) { this.tipo = tipo; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getEndereco() { return endereco; }
    public void setEndereco(String endereco) { this.endereco = endereco; }
    public String getHorario() { return horario; }
    public void setHorario(String horario) { this.horario = horario; }
    public boolean isGratuito() { return gratuito; }
    public void setGratuito(boolean gratuito) { this.gratuito = gratuito; }
    public boolean isSombra() { return sombra; }
    public void setSombra(boolean sombra) { this.sombra = sombra; }
    public String getVoucherDescricao() { return voucherDescricao; }
    public void setVoucherDescricao(String voucherDescricao) { this.voucherDescricao = voucherDescricao; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public Instant getCriadoEm() { return criadoEm; }
}
