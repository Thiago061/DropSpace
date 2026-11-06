package br.com.zonageo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "medicoes_ambientais")
public class MedicaoAmbiental {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "localizacao_id")
    private Long localizacaoId;
    @NotBlank @Size(max = 120) @Column(name = "nome_local", nullable = false)
    private String nomeLocal;
    @Digits(integer = 4, fraction = 2) @Column(name = "temperatura_c")
    private BigDecimal temperaturaC;
    @DecimalMin("0.0") @DecimalMax("100.0") @Digits(integer = 3, fraction = 2)
    @Column(name = "umidade_percentual")
    private BigDecimal umidadePercentual;
    @Size(max = 120) @Column(nullable = false)
    private String condicao = "Não informado";
    @Size(max = 120) @Column(nullable = false)
    private String fonte = "Registro manual";
    @Column(name = "coletada_em", nullable = false)
    private Instant coletadaEm = Instant.now();
    @Column(name = "criada_em", insertable = false, updatable = false)
    private Instant criadaEm;

    public Long getId() { return id; }
    public Long getLocalizacaoId() { return localizacaoId; }
    public void setLocalizacaoId(Long localizacaoId) { this.localizacaoId = localizacaoId; }
    public String getNomeLocal() { return nomeLocal; }
    public void setNomeLocal(String nomeLocal) { this.nomeLocal = nomeLocal; }
    public BigDecimal getTemperaturaC() { return temperaturaC; }
    public void setTemperaturaC(BigDecimal temperaturaC) { this.temperaturaC = temperaturaC; }
    public BigDecimal getUmidadePercentual() { return umidadePercentual; }
    public void setUmidadePercentual(BigDecimal umidadePercentual) { this.umidadePercentual = umidadePercentual; }
    public String getCondicao() { return condicao; }
    public void setCondicao(String condicao) { this.condicao = condicao; }
    public String getFonte() { return fonte; }
    public void setFonte(String fonte) { this.fonte = fonte; }
    public Instant getColetadaEm() { return coletadaEm; }
    public void setColetadaEm(Instant coletadaEm) { this.coletadaEm = coletadaEm; }
    public Instant getCriadaEm() { return criadaEm; }
}
