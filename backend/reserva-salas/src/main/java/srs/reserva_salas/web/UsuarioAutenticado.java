package srs.reserva_salas.web;

import srs.reserva_salas.domain.PerfilUsuario;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

public final class UsuarioAutenticado {

    private UsuarioAutenticado() {
    }

    public static UUID id(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    public static boolean ehAdministrador(Jwt jwt) {
        return PerfilUsuario.ADMINISTRADOR.name().equals(jwt.getClaimAsString("perfil"));
    }
}
