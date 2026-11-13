package br.com.zonageo.controller.admin;

import br.com.zonageo.dto.AdminDtos.ResultadoBusca;
import br.com.zonageo.service.LugarService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Busca de lugares por nome, para preencher coordenadas nos cadastros. */
@RestController
@RequestMapping("/api/admin/lugares")
public class LugarAdminController {
    private final LugarService lugares;

    public LugarAdminController(LugarService lugares) {
        this.lugares = lugares;
    }

    @GetMapping
    public ResultadoBusca buscar(@RequestParam("q") String consulta) {
        String q = consulta == null ? "" : consulta.trim();
        if (q.length() < 3 || q.length() > 120) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Digite de 3 a 120 caracteres para buscar.");
        }
        return lugares.buscar(q);
    }
}
