package br.com.zonageo.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "vouchers")
public class Voucher {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 20)
    private String codigo;
    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;
    @Column(name = "ponto_id", nullable = false)
    private Long pontoId;
    @Column(nullable = false, length = 160)
    private String descricao;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private StatusVoucher status = StatusVoucher.ATIVO;
    @Column(name = "emitido_em", nullable = false)
    private Instant emitidoEm;
    @Column(name = "expira_em", nullable = false)
    private Instant expiraEm;
    @Column(name = "resgatado_em")
    private Instant resgatadoEm;

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
    public Long getPontoId() { return pontoId; }
    public void setPontoId(Long pontoId) { this.pontoId = pontoId; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public StatusVoucher getStatus() { return status; }
    public void setStatus(StatusVoucher status) { this.status = status; }
    public Instant getEmitidoEm() { return emitidoEm; }
    public void setEmitidoEm(Instant emitidoEm) { this.emitidoEm = emitidoEm; }
    public Instant getExpiraEm() { return expiraEm; }
    public void setExpiraEm(Instant expiraEm) { this.expiraEm = expiraEm; }
    public Instant getResgatadoEm() { return resgatadoEm; }
    public void setResgatadoEm(Instant resgatadoEm) { this.resgatadoEm = resgatadoEm; }
}
