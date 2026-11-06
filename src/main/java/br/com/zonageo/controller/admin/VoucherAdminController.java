package br.com.zonageo.controller.admin;

import br.com.zonageo.dto.AdminDtos.ResgateRequest;
import br.com.zonageo.dto.AppDtos.VoucherDto;
import br.com.zonageo.service.VoucherService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Vouchers emitidos e conferência (resgate) no caixa. */
@RestController
@RequestMapping("/api/admin/vouchers")
public class VoucherAdminController {
    private final VoucherService vouchers;

    public VoucherAdminController(VoucherService vouchers) {
        this.vouchers = vouchers;
    }

    @GetMapping
    public List<VoucherDto> recentes() {
        return vouchers.recentes();
    }

    @PostMapping("/resgatar")
    public VoucherDto resgatar(@Valid @RequestBody ResgateRequest req) {
        return vouchers.resgatar(req.codigo());
    }
}
