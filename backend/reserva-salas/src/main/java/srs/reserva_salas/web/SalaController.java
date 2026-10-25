package srs.reserva_salas.web;

import srs.reserva_salas.dto.SalaDto;
import srs.reserva_salas.service.SalaService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/salas")
public class SalaController {

    private final SalaService service;

    public SalaController(SalaService service) {
        this.service = service;
    }

    @GetMapping
    public List<SalaDto.Response> listar(@AuthenticationPrincipal Jwt jwt) {
        return service.listar(UsuarioAutenticado.ehAdministrador(jwt));
    }

    /** Ex.: /api/salas/disponiveis?data=2026-11-03&inicio=14:00&fim=16:00 */
    @GetMapping("/disponiveis")
    public List<SalaDto.Response> disponiveis(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime fim) {
        return service.disponiveis(data, inicio, fim);
    }

    @GetMapping("/{id}")
    public SalaDto.Response buscar(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return service.buscar(id, UsuarioAutenticado.ehAdministrador(jwt));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public SalaDto.Response criar(@Valid @RequestBody SalaDto.Request req, @AuthenticationPrincipal Jwt jwt) {
        return service.criar(UsuarioAutenticado.id(jwt), req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public SalaDto.Response editar(@PathVariable UUID id, @Valid @RequestBody SalaDto.Request req,
                                   @AuthenticationPrincipal Jwt jwt) {
        return service.editar(UsuarioAutenticado.id(jwt), id, req);
    }

    @PatchMapping("/{id}/desativar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public SalaDto.Response desativar(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return service.desativar(UsuarioAutenticado.id(jwt), id);
    }
}
