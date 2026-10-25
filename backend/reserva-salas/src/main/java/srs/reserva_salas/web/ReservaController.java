package srs.reserva_salas.web;

import srs.reserva_salas.dto.ReservaDto;
import srs.reserva_salas.service.ReservaService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    private final ReservaService service;

    public ReservaController(ReservaService service) {
        this.service = service;
    }

    @GetMapping("/minhas")
    public List<ReservaDto.Response> minhas(@AuthenticationPrincipal Jwt jwt) {
        return service.minhas(UsuarioAutenticado.id(jwt));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservaDto.Response agendar(@Valid @RequestBody ReservaDto.CriarRequest req,
                                       @AuthenticationPrincipal Jwt jwt) {
        return service.agendar(UsuarioAutenticado.id(jwt), req);
    }

    @PostMapping("/{id}/cancelar")
    public ReservaDto.Response cancelar(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return service.cancelar(UsuarioAutenticado.id(jwt), id);
    }

    @PostMapping("/check-in")
    public ReservaDto.Response checkIn(@Valid @RequestBody ReservaDto.CheckInRequest req,
                                       @AuthenticationPrincipal Jwt jwt) {
        return service.checkIn(UsuarioAutenticado.id(jwt), req.codigoQr());
    }

    @PostMapping("/{id}/saida")
    public ReservaDto.Response confirmarSaida(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return service.confirmarSaida(UsuarioAutenticado.id(jwt), id);
    }
}
