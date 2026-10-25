package srs.reserva_salas.dto;

import srs.reserva_salas.domain.PerfilUsuario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class AuthDto {

    private AuthDto() {
    }

    public record LoginRequest(
            @NotBlank(message = "Informe o e-mail.") @Size(max = 255) String email,
            @NotBlank(message = "Informe a senha.") @Size(max = 200) String senha) {
    }

    public record LoginResponse(String token, String tipo, Instant expiraEm, String nome, PerfilUsuario perfil) {
    }
}
