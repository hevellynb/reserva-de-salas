package srs.reserva_salas.dto;

import srs.reserva_salas.domain.PerfilUsuario;
import srs.reserva_salas.domain.Usuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public final class UsuarioDto {

    private UsuarioDto() {
    }

    public record CriarRequest(
            @NotBlank @Size(max = 150) String nome,
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(min = 8, max = 100, message = "A senha deve ter entre 8 e 100 caracteres.") String senha,
            @NotNull PerfilUsuario perfil) {
    }

    public record Response(UUID id, String nome, String email, PerfilUsuario perfil, boolean ativo) {

        public static Response from(Usuario u) {
            return new Response(u.getId(), u.getNome(), u.getEmail(), u.getPerfil(), u.isAtivo());
        }
    }
}
