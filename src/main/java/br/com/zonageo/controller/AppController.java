package br.com.zonageo.controller;

import br.com.zonageo.dto.AppDtos.*;
import br.com.zonageo.dto.AuthDtos.UsuarioDto;
import br.com.zonageo.model.Usuario;
import br.com.zonageo.security.UsuarioAtual;
import br.com.zonageo.service.LocalidadeService;
import br.com.zonageo.service.RotaService;
import br.com.zonageo.service.VoucherService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** Endpoints consumidos pelo aplicativo Android (usuário autenticado). */
@RestController
@RequestMapping("/api/app")
public class AppController {
    private static final int RAIO_PADRAO_M = 3000;
    private static final int RAIO_MAX_M = 10000;

    private final UsuarioAtual atual;
    private final LocalidadeService localidade;
    private final RotaService rotas;
    private final VoucherService vouchers;

    public AppController(UsuarioAtual atual, LocalidadeService localidade, RotaService rotas, VoucherService vouchers) {
        this.atual = atual;
        this.localidade = localidade;
        this.rotas = rotas;
        this.vouchers = vouchers;
    }

    @GetMapping("/me")
    public UsuarioDto me(@AuthenticationPrincipal Jwt jwt) {
        return UsuarioDto.de(atual.exigir(jwt));
    }

    /** Clima, recomendações, ocorrências e pontos de apoio perto da posição informada. */
    @GetMapping("/localidade")
    public LocalidadeResposta localidade(@AuthenticationPrincipal Jwt jwt,
                                         @RequestParam double lat, @RequestParam double lon,
                                         @RequestParam(required = false) Integer raio) {
        validarCoordenadas(lat, lon);
        int raioM = raio == null ? RAIO_PADRAO_M : Math.max(200, Math.min(raio, RAIO_MAX_M));
        return localidade.consultar(atual.exigir(jwt), lat, lon, raioM);
    }

    /** Rota com paradas de apoio. Exclusivo do plano Pro. */
    @GetMapping("/rotas/segura")
    public RotaResposta rota(@AuthenticationPrincipal Jwt jwt,
                             @RequestParam double origemLat, @RequestParam double origemLon,
                             @RequestParam double destinoLat, @RequestParam double destinoLon) {
        Usuario u = atual.exigir(jwt);
        if (!u.isPro()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Rotas com paradas de apoio são exclusivas do plano Pro.");
        }
        validarCoordenadas(origemLat, origemLon);
        validarCoordenadas(destinoLat, destinoLon);
        if (!RotaService.distanciaValida(origemLat, origemLon, destinoLat, destinoLon)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A distância máxima para a rota é de 20 km.");
        }
        return rotas.montar(origemLat, origemLon, destinoLat, destinoLon);
    }

    /** Gera o voucher do plano Pro (exige estar perto do mercado parceiro). */
    @PostMapping("/vouchers") @ResponseStatus(HttpStatus.CREATED)
    public VoucherDto emitirVoucher(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody VoucherRequest req) {
        Usuario u = atual.exigir(jwt);
        return vouchers.emitir(u.getId(), req.pontoId(), req.latitude(), req.longitude());
    }

    @GetMapping("/vouchers")
    public List<VoucherDto> meusVouchers(@AuthenticationPrincipal Jwt jwt) {
        return vouchers.doUsuario(atual.exigir(jwt).getId());
    }

    private static void validarCoordenadas(double lat, double lon) {
        if (Double.isNaN(lat) || Double.isNaN(lon) || lat < -90 || lat > 90 || lon < -180 || lon > 180) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coordenadas inválidas.");
        }
    }
}
