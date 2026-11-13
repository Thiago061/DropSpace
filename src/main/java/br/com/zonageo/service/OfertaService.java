package br.com.zonageo.service;

import br.com.zonageo.dto.AdminDtos.OfertaDto;
import br.com.zonageo.dto.AdminDtos.OfertaRequest;
import br.com.zonageo.dto.AdminDtos.ProdutoCatalogoDto;
import br.com.zonageo.model.OfertaVoucher;
import br.com.zonageo.model.PontoApoio;
import br.com.zonageo.repository.OfertaVoucherRepository;
import br.com.zonageo.repository.PontoApoioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** Catálogo de ofertas de voucher dos mercados parceiros. */
@Service
public class OfertaService {
    private static final List<ProdutoCatalogoDto> CATALOGO = List.of(
            produto("Água mineral 500 ml", "Garrafa", "1.50", "Água mineral em garrafa de 500 ml."),
            produto("Água mineral 1,5 L", "Garrafa", "3.50", "Água mineral em garrafa de 1,5 litro."),
            produto("Água mineral 20 L", "Galão/recarga", "12.00", "Galão ou recarga de água mineral de 20 litros."),
            produto("Gelo em cubos", "Saco de 5 kg", "12.00", "Saco de gelo em cubos com 5 kg."),
            produto("Bolsa térmica", "Unidade", "45.00", "Bolsa térmica para conservar alimentos e bebidas."),
            produto("Bolsa de gelo reutilizável", "Unidade", "15.00", "Bolsa de gelo reutilizável."),
            produto("Protetor solar FPS 50", "Frasco de 200 ml", "50.00", "Protetor solar FPS 50 em frasco de 200 ml."),
            produto("Creme de proteção para a pele", "Pote de 200 g", "35.00", "Creme de proteção para a pele em pote de 200 g."),
            produto("Creme hidratante", "Frasco de 200 ml", "20.00", "Creme hidratante em frasco de 200 ml."),
            produto("Soro fisiológico", "Frasco de 500 ml", "8.00", "Soro fisiológico em frasco de 500 ml."),
            produto("Boné com proteção UV", "Unidade", "30.00", "Boné com proteção contra raios UV."),
            produto("Luvas de proteção", "Par", "15.00", "Um par de luvas de proteção."),
            produto("Óculos de proteção UV", "Unidade", "25.00", "Óculos com proteção contra raios UV."),
            produto("Máscara de proteção respiratória", "Unidade", "5.00", "Máscara de proteção respiratória."),
            produto("Kit de primeiros socorros", "Kit", "80.00", "Kit básico de primeiros socorros.")
    );
    private final OfertaVoucherRepository ofertas;
    private final PontoApoioRepository pontos;

    public OfertaService(OfertaVoucherRepository ofertas, PontoApoioRepository pontos) {
        this.ofertas = ofertas;
        this.pontos = pontos;
    }

    @Transactional(readOnly = true)
    public List<ProdutoCatalogoDto> catalogoProdutos() { return CATALOGO; }

    private static ProdutoCatalogoDto produto(String nome, String unidade, String valor, String descricao) {
        return new ProdutoCatalogoDto(nome, unidade, new BigDecimal(valor), descricao);
    }

    // ----------------------------------------------------------- consulta (app)

    /** Título da primeira oferta vigente de cada ponto. Pontos sem oferta não aparecem no mapa. */
    @Transactional(readOnly = true)
    public Map<Long, String> tituloPrincipalPorPonto(Collection<Long> pontoIds) {
        Map<Long, String> resultado = new HashMap<>();
        if (pontoIds.isEmpty()) return resultado;
        for (OfertaVoucher o : ofertas.vigentesDosPontos(pontoIds)) {
            resultado.putIfAbsent(o.getPontoId(), o.getTitulo());
        }
        return resultado;
    }

    /** Oferta vigente do ponto: a pedida ou, se não houver pedido, a primeira. */
    @Transactional(readOnly = true)
    public Optional<OfertaVoucher> ofertaVigente(Long pontoId, Long ofertaId) {
        List<OfertaVoucher> vigentes = ofertas.vigentesDosPontos(List.of(pontoId));
        if (ofertaId == null) return vigentes.stream().findFirst();
        return vigentes.stream().filter(o -> o.getId().equals(ofertaId)).findFirst();
    }

    // ---------------------------------------------------------- administração

    @Transactional(readOnly = true)
    public List<OfertaDto> listar() {
        Map<Long, String> nomes = pontos.findAll().stream()
                .collect(Collectors.toMap(PontoApoio::getId, PontoApoio::getNome, (a, b) -> a));
        return ofertas.findAllByOrderByIdDesc().stream()
                .map(o -> OfertaDto.de(o, nomes.getOrDefault(o.getPontoId(), "(ponto removido)")))
                .toList();
    }

    @Transactional
    public OfertaDto criar(OfertaRequest req) {
        PontoApoio ponto = exigirMercadoParceiro(req.pontoId());
        OfertaVoucher o = new OfertaVoucher();
        aplicar(o, req);
        return OfertaDto.de(ofertas.save(o), ponto.getNome());
    }

    @Transactional
    public OfertaDto atualizar(Long id, OfertaRequest req) {
        OfertaVoucher o = buscar(id);
        PontoApoio ponto = exigirMercadoParceiro(req.pontoId());
        aplicar(o, req);
        return OfertaDto.de(ofertas.save(o), ponto.getNome());
    }

    @Transactional
    public OfertaDto alternarAtivo(Long id, boolean ativo) {
        OfertaVoucher o = buscar(id);
        o.setAtivo(ativo);
        String nome = pontos.findById(o.getPontoId()).map(PontoApoio::getNome).orElse("");
        return OfertaDto.de(ofertas.save(o), nome);
    }

    private OfertaVoucher buscar(Long id) {
        return ofertas.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Oferta não encontrada."));
    }

    private PontoApoio exigirMercadoParceiro(Long pontoId) {
        PontoApoio p = pontos.findById(pontoId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Ponto não encontrado."));
        if (!p.isParceiro()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Só mercados parceiros podem ter vouchers. Mude o tipo do ponto para \"Mercado parceiro\".");
        }
        return p;
    }

    private static void aplicar(OfertaVoucher o, OfertaRequest req) {
        o.setPontoId(req.pontoId());
        o.setTitulo(req.titulo().trim());
        o.setDescricao(req.descricao() == null || req.descricao().isBlank() ? null : req.descricao().trim());
        o.setUnidade(req.unidade().trim());
        o.setValor(req.valor());
    }
}
