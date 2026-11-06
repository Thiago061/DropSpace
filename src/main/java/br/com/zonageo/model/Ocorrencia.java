package br.com.zonageo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.Instant;

@Entity
@Table(name = "ocorrencias")
public class Ocorrencia {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank @Size(max = 140)
    private String titulo;
    @NotBlank @Size(max = 80)
    private String categoria;
    @NotBlank @Pattern(regexp = "Baixo|Moderado|Alto|Crítico")
    private String severidade = "Moderado";
    @DecimalMin("-90.0") @DecimalMax("90.0")
    private Double latitude;
    @DecimalMin("-180.0") @DecimalMax("180.0")
    private Double longitude;
    @Column(columnDefinition = "text")
    private String descricao;
    @NotBlank @Pattern(regexp = "Aberto|Em análise|Validado|Em atendimento|Resolvido|Falso positivo|Cancelado")
    private String status = "Aberto";
    @Column(name = "criada_em", insertable = false, updatable = false)
    private Instant criadaEm;
    @Column(name = "atualizada_em", insertable = false, updatable = false)
    private Instant atualizadaEm;

    public Long getId() { return id; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public String getSeveridade() { return severidade; }
    public void setSeveridade(String severidade) { this.severidade = severidade; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCriadaEm() { return criadaEm; }
    public Instant getAtualizadaEm() { return atualizadaEm; }
}
