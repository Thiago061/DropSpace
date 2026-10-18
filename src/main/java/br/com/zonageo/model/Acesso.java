package br.com.zonageo.model;

import jakarta.persistence.*;
import java.time.Instant;

/** Registro de uma tentativa de entrada (sucesso ou falha). */
@Entity
@Table(name = "acessos")
public class Acesso {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "usuario_id")
    private Long usuarioId;
    @Column(nullable = false, length = 160)
    private String login;
    @Column(nullable = false)
    private boolean sucesso;
    @Column(length = 64)
    private String ip;
    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();

    protected Acesso() {}

    public Acesso(Long usuarioId, String login, boolean sucesso, String ip) {
        this.usuarioId = usuarioId;
        this.login = login;
        this.sucesso = sucesso;
        this.ip = ip;
    }

    public Long getId() { return id; }
    public Long getUsuarioId() { return usuarioId; }
    public String getLogin() { return login; }
    public boolean isSucesso() { return sucesso; }
    public String getIp() { return ip; }
    public Instant getCriadoEm() { return criadoEm; }
}
