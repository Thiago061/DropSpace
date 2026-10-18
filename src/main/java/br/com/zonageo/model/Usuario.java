package br.com.zonageo.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "usuarios")
public class Usuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 120)
    private String nome;
    @Column(nullable = false, unique = true, length = 160)
    private String login;
    @Column(name = "senha_hash", nullable = false, length = 100)
    private String senhaHash;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private Papel papel = Papel.USER;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private Plano plano = Plano.FREE;
    @Column(name = "plano_expira_em")
    private Instant planoExpiraEm;
    @Column(nullable = false)
    private boolean ativo = true;
    @Column(name = "criado_em", insertable = false, updatable = false)
    private Instant criadoEm;
    @Column(name = "ultimo_login_em")
    private Instant ultimoLoginEm;
    @Column(name = "total_logins", nullable = false)
    private int totalLogins = 0;

    /** Plano realmente válido agora: um Pro vencido volta a ser Free. */
    public Plano planoEfetivo() {
        if (plano == Plano.PRO && (planoExpiraEm == null || planoExpiraEm.isAfter(Instant.now()))) {
            return Plano.PRO;
        }
        return Plano.FREE;
    }
    public boolean isPro() { return planoEfetivo() == Plano.PRO; }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }
    public String getSenhaHash() { return senhaHash; }
    public void setSenhaHash(String senhaHash) { this.senhaHash = senhaHash; }
    public Papel getPapel() { return papel; }
    public void setPapel(Papel papel) { this.papel = papel; }
    public Plano getPlano() { return plano; }
    public void setPlano(Plano plano) { this.plano = plano; }
    public Instant getPlanoExpiraEm() { return planoExpiraEm; }
    public void setPlanoExpiraEm(Instant planoExpiraEm) { this.planoExpiraEm = planoExpiraEm; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public Instant getCriadoEm() { return criadoEm; }
    public Instant getUltimoLoginEm() { return ultimoLoginEm; }
    public int getTotalLogins() { return totalLogins; }

    /** Registra um acesso bem-sucedido. */
    public void registrarAcesso() {
        this.ultimoLoginEm = Instant.now();
        this.totalLogins++;
    }
}
