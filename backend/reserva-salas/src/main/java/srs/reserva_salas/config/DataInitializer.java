package srs.reserva_salas.config;

import srs.reserva_salas.domain.PerfilUsuario;
import srs.reserva_salas.domain.Usuario;
import srs.reserva_salas.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final AppProperties props;

    public DataInitializer(UsuarioRepository usuarios, PasswordEncoder encoder, AppProperties props) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (usuarios.existsByPerfil(PerfilUsuario.ADMINISTRADOR)) {
            return;
        }
        usuarios.save(new Usuario("Administrador", props.admin().email(),
                encoder.encode(props.admin().senha()), PerfilUsuario.ADMINISTRADOR));
        log.warn("Administrador inicial criado com o e-mail {}. Troque a senha padrão antes de ir para produção.",
                props.admin().email());
    }
}
