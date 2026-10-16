package br.com.zonageo.config;

import br.com.zonageo.model.Papel;
import br.com.zonageo.model.PontoApoio;
import br.com.zonageo.model.TipoPonto;
import br.com.zonageo.model.Usuario;
import br.com.zonageo.repository.PontoApoioRepository;
import br.com.zonageo.repository.UsuarioRepository;
import br.com.zonageo.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Cria o administrador inicial (a partir do ambiente) e, opcionalmente, dados de demonstração. */
@Component
public class DataInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final ZonaGeoProperties props;
    private final UsuarioRepository usuarios;
    private final PontoApoioRepository pontos;
    private final PasswordEncoder encoder;

    public DataInitializer(ZonaGeoProperties props, UsuarioRepository usuarios,
                           PontoApoioRepository pontos, PasswordEncoder encoder) {
        this.props = props;
        this.usuarios = usuarios;
        this.pontos = pontos;
        this.encoder = encoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        criarAdmin();
        if (props.seedDemo()) criarDemo();
    }

    private void criarAdmin() {
        String user = props.admin().user();
        String senha = props.admin().password();
        if (user == null || user.isBlank() || senha == null || senha.isBlank()) {
            log.warn("ZONAGEO_ADMIN_USER/ZONAGEO_ADMIN_PASSWORD não definidos: nenhum administrador será criado.");
            return;
        }
        String login = AuthService.normalizarLogin(user);
        if (usuarios.existsByLogin(login)) return; // nunca sobrescreve a senha de quem já existe
        if (senha.length() < 10) {
            log.warn("A senha do administrador tem menos de 10 caracteres. Use uma senha mais forte.");
        }
        Usuario admin = new Usuario();
        admin.setNome("Administrador");
        admin.setLogin(login);
        admin.setSenhaHash(encoder.encode(senha));
        admin.setPapel(Papel.ADMIN);
        usuarios.save(admin);
        log.info("Administrador '{}' criado.", login);
    }

    private void criarDemo() {
        if (pontos.count() > 0) return;
        // Coordenadas fictícias perto do centro de Brasília, só para testar. Substitua por locais reais.
        demo("Bebedouro público (demo)", TipoPonto.PONTO_AGUA, -15.7915, -47.8850, true, false, null);
        demo("Praça com sombra (demo)", TipoPonto.DESCANSO_SOMBRA, -15.7960, -47.8800, true, true, null);
        demo("Abrigo aquecido (demo)", TipoPonto.ABRIGO_FRIO, -15.7990, -47.8870, true, false, null);
        demo("Atacadão Dia a Dia", TipoPonto.MERCADO_PARCEIRO, -15.7930, -47.8840, false, false, "1 água mineral 500 ml");
        demo("Supermercado Açaí", TipoPonto.MERCADO_PARCEIRO, -15.7975, -47.8815, false, false, "1 água mineral 500 ml");
        demo("Mercearia do Seu Zé", TipoPonto.MERCADO_PARCEIRO, -15.7945, -47.8895, false, false, "1 água mineral 500 ml");
        log.info("Dados de demonstração criados (pontos de apoio).");
    }

    private void demo(String nome, TipoPonto tipo, double lat, double lon, boolean gratuito,
                      boolean sombra, String voucher) {
        PontoApoio p = new PontoApoio();
        p.setNome(nome);
        p.setTipo(tipo);
        p.setLatitude(lat);
        p.setLongitude(lon);
        p.setGratuito(gratuito);
        p.setSombra(sombra);
        p.setVoucherDescricao(voucher);
        p.setDescricao("Dado de demonstração. Substitua por um local real.");
        pontos.save(p);
    }
}
