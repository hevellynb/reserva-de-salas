package srs.reserva_salas.service;

import srs.reserva_salas.config.AppProperties;
import srs.reserva_salas.domain.Usuario;
import srs.reserva_salas.dto.AuthDto;
import srs.reserva_salas.exception.ApiException;
import srs.reserva_salas.repository.UsuarioRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final JwtEncoder jwtEncoder;
    private final AuditoriaService auditoria;
    private final AppProperties props;
    private final Clock clock;

    public AuthService(UsuarioRepository usuarios, PasswordEncoder encoder, JwtEncoder jwtEncoder,
                       AuditoriaService auditoria, AppProperties props, Clock clock) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.jwtEncoder = jwtEncoder;
        this.auditoria = auditoria;
        this.props = props;
        this.clock = clock;
    }

    public AuthDto.LoginResponse login(AuthDto.LoginRequest req) {
        Usuario usuario = usuarios.findByEmailIgnoreCase(req.email().trim()).orElse(null);

        if (usuario == null || !encoder.matches(req.senha(), usuario.getSenhaHash())) {
            auditoria.registrar(usuario, "LOGIN_FALHA", "Usuario",
                    usuario != null ? usuario.getId() : null, "email=" + req.email());
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Usuário ou senha inválidos. Tente novamente.");
        }
        if (!usuario.isAtivo()) {
            auditoria.registrar(usuario, "LOGIN_FALHA", "Usuario", usuario.getId(), "conta inativa");
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Sua conta está inativa. Entre em contato com o administrador.");
        }

        Instant agora = Instant.now(clock);
        Instant expiraEm = agora.plus(Duration.ofMinutes(props.jwt().expiracaoMinutos()));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("srs")
                .issuedAt(agora)
                .expiresAt(expiraEm)
                .subject(usuario.getId().toString())
                .claim("perfil", usuario.getPerfil().name())
                .build();
        String token = jwtEncoder.encode(
                JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

        auditoria.registrar(usuario, "LOGIN_SUCESSO", "Usuario", usuario.getId(), null);
        return new AuthDto.LoginResponse(token, "Bearer", expiraEm, usuario.getNome(), usuario.getPerfil());
    }
}
