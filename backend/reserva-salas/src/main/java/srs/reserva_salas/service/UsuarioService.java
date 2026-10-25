package srs.reserva_salas.service;

import srs.reserva_salas.domain.Usuario;
import srs.reserva_salas.dto.UsuarioDto;
import srs.reserva_salas.exception.ApiException;
import srs.reserva_salas.repository.UsuarioRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final AuditoriaService auditoria;

    public UsuarioService(UsuarioRepository usuarios, PasswordEncoder encoder, AuditoriaService auditoria) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.auditoria = auditoria;
    }

    @Transactional
    public UsuarioDto.Response criar(UUID adminId, UsuarioDto.CriarRequest req) {
        String email = req.email().trim();
        if (usuarios.existsByEmailIgnoreCase(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Já existe um usuário cadastrado com este e-mail.");
        }
        Usuario novo = usuarios.save(
                new Usuario(req.nome().trim(), email, encoder.encode(req.senha()), req.perfil()));
        auditoria.registrar(usuarios.getReferenceById(adminId), "USUARIO_CRIADO", "Usuario",
                novo.getId(), "perfil=" + novo.getPerfil());
        return UsuarioDto.Response.from(novo);
    }
}