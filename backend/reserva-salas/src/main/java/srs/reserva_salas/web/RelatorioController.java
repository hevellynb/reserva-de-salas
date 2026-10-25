package srs.reserva_salas.web;

import srs.reserva_salas.dto.ContagemItem;
import srs.reserva_salas.service.RelatorioService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/relatorios")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class RelatorioController {

    private final RelatorioService service;

    public RelatorioController(RelatorioService service) {
        this.service = service;
    }

    @GetMapping("/reservas-por-sala")
    public List<ContagemItem> porSala(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @AuthenticationPrincipal Jwt jwt) {
        return service.reservasPorSala(UsuarioAutenticado.id(jwt), inicio, fim);
    }

    @GetMapping("/reservas-por-usuario")
    public List<ContagemItem> porUsuario(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @AuthenticationPrincipal Jwt jwt) {
        return service.reservasPorUsuario(UsuarioAutenticado.id(jwt), inicio, fim);
    }
}
