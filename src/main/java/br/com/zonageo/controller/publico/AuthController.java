package br.com.zonageo.controller.publico;

import br.com.zonageo.dto.AuthDtos.*;
import br.com.zonageo.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) { this.auth = auth; }

    @PostMapping("/registro") @ResponseStatus(HttpStatus.CREATED)
    public TokenResposta registrar(@Valid @RequestBody RegistroRequest req, HttpServletRequest http) {
        return auth.registrar(req, http.getRemoteAddr());
    }

    @PostMapping("/login")
    public TokenResposta login(@Valid @RequestBody LoginRequest req, HttpServletRequest http) {
        return auth.login(req, http.getRemoteAddr());
    }
}
