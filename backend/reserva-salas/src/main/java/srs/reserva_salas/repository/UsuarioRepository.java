package srs.reserva_salas.repository;

import srs.reserva_salas.domain.PerfilUsuario;
import srs.reserva_salas.domain.Usuario;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPerfil(PerfilUsuario perfil);

    /** Trava a linha do usuário para que duas reservas simultâneas não furem o limite diário (RN002). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.id = :id")
    Optional<Usuario> buscarParaAtualizar(@Param("id") UUID id);
}
