package br.com.zonageo.security;

import br.com.zonageo.config.ZonaGeoProperties;
import br.com.zonageo.model.Usuario;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class TokenService {
    private final JwtEncoder encoder;
    private final ZonaGeoProperties props;

    public TokenService(JwtEncoder encoder, ZonaGeoProperties props) {
        this.encoder = encoder;
        this.props = props;
    }

    public record Emitido(String token, Instant expiraEm) {}

    public Emitido emitir(Usuario usuario) {
        Instant agora = Instant.now();
        Instant expira = agora.plus(props.jwt().ttl());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("zonageo")
                .issuedAt(agora)
                .expiresAt(expira)
                .subject(String.valueOf(usuario.getId()))
                .claim("papel", usuario.getPapel().name())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new Emitido(token, expira);
    }
}
