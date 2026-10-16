package br.com.zonageo.service;

import br.com.zonageo.config.ZonaGeoProperties;
import br.com.zonageo.dto.AppDtos;
import br.com.zonageo.dto.AppDtos.VoucherDto;
import br.com.zonageo.geo.Geo;
import br.com.zonageo.model.PontoApoio;
import br.com.zonageo.model.StatusVoucher;
import br.com.zonageo.model.Usuario;
import br.com.zonageo.model.Voucher;
import br.com.zonageo.repository.PontoApoioRepository;
import br.com.zonageo.repository.UsuarioRepository;
import br.com.zonageo.repository.VoucherRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class VoucherService {
    /** Sem 0/O/1/I para evitar confusão na hora de digitar o código. */
    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final VoucherRepository vouchers;
    private final PontoApoioRepository pontos;
    private final UsuarioRepository usuarios;
    private final ZonaGeoProperties props;

    public VoucherService(VoucherRepository vouchers, PontoApoioRepository pontos,
                          UsuarioRepository usuarios, ZonaGeoProperties props) {
        this.vouchers = vouchers;
        this.pontos = pontos;
        this.usuarios = usuarios;
        this.props = props;
    }

    @Transactional
    public VoucherDto emitir(Long usuarioId, Long pontoId, double lat, double lon) {
        // trava a linha do usuário: duas requisições simultâneas não furam o limite diário
        Usuario usuario = usuarios.buscarComTrava(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida."));
        if (!usuario.isPro()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vouchers são exclusivos do plano Pro.");
        }
        PontoApoio ponto = pontos.findById(pontoId).filter(PontoApoio::isAtivo).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Ponto não encontrado."));
        if (!ponto.oferecaVoucher()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Este local não oferece voucher.");
        }
        double distancia = Geo.distanciaM(lat, lon, ponto.getLatitude(), ponto.getLongitude());
        if (distancia > props.voucher().raioValidacaoM()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Chegue mais perto do local (até " + props.voucher().raioValidacaoM() + " m) para gerar o voucher.");
        }
        Instant agora = Instant.now();
        // se já existe um voucher ativo para este local, devolve o mesmo
        Optional<Voucher> existente = vouchers.findFirstByUsuarioIdAndPontoIdAndStatusAndExpiraEmAfter(
                usuarioId, pontoId, StatusVoucher.ATIVO, agora);
        if (existente.isPresent()) return paraDto(existente.get(), ponto.getNome());

        Instant inicioDoDia = LocalDate.now(FUSO).atStartOfDay(FUSO).toInstant();
        if (vouchers.countByUsuarioIdAndEmitidoEmGreaterThanEqual(usuarioId, inicioDoDia) >= props.voucher().limiteDiario()) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Limite diário de vouchers atingido. Volte amanhã.");
        }
        Voucher v = new Voucher();
        v.setCodigo(gerarCodigo());
        v.setUsuarioId(usuarioId);
        v.setPontoId(pontoId);
        v.setDescricao(ponto.getVoucherDescricao());
        v.setEmitidoEm(agora);
        v.setExpiraEm(agora.plusSeconds(props.voucher().validadeMinutos() * 60L));
        return paraDto(vouchers.save(v), ponto.getNome());
    }

    @Transactional(readOnly = true)
    public List<VoucherDto> doUsuario(Long usuarioId) {
        return paraDtos(vouchers.findTop50ByUsuarioIdOrderByEmitidoEmDesc(usuarioId));
    }

    @Transactional(readOnly = true)
    public List<VoucherDto> recentes() {
        return paraDtos(vouchers.findTop100ByOrderByEmitidoEmDesc());
    }

    /** Resgate feito pelo parceiro/administrador ao conferir o código apresentado pelo cliente. */
    @Transactional
    public VoucherDto resgatar(String codigoBruto) {
        String codigo = codigoBruto.trim().toUpperCase(java.util.Locale.ROOT);
        Voucher v = vouchers.findByCodigo(codigo).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Voucher não encontrado."));
        int alterados = vouchers.resgatar(codigo, Instant.now());
        if (alterados == 0) {
            if (v.getStatus() == StatusVoucher.RESGATADO) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Este voucher já foi utilizado.");
            }
            throw new ResponseStatusException(HttpStatus.GONE, "Este voucher expirou.");
        }
        Voucher atualizado = vouchers.findByCodigo(codigo).orElse(v);
        String nome = pontos.findById(atualizado.getPontoId()).map(PontoApoio::getNome).orElse("");
        return paraDto(atualizado, nome);
    }

    private List<VoucherDto> paraDtos(List<Voucher> lista) {
        Map<Long, String> nomes = pontos.findAllById(lista.stream().map(Voucher::getPontoId).distinct().toList())
                .stream().collect(Collectors.toMap(PontoApoio::getId, PontoApoio::getNome, (a, b) -> a));
        return lista.stream().map(v -> paraDto(v, nomes.getOrDefault(v.getPontoId(), ""))).toList();
    }

    private static VoucherDto paraDto(Voucher v, String pontoNome) {
        return new VoucherDto(v.getId(), v.getCodigo(), v.getPontoId(), pontoNome, v.getDescricao(),
                AppDtos.statusEfetivo(v.getStatus(), v.getExpiraEm()), v.getEmitidoEm(), v.getExpiraEm(),
                v.getResgatadoEm(), v.getUsuarioId());
    }

    private static String gerarCodigo() {
        StringBuilder sb = new StringBuilder("ZG-");
        for (int i = 0; i < 8; i++) {
            if (i == 4) sb.append('-');
            sb.append(ALFABETO.charAt(RANDOM.nextInt(ALFABETO.length())));
        }
        return sb.toString();
    }
}
