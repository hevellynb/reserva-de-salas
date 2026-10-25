package srs.reserva_salas.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(String timezone, JwtConfig jwt, AdminConfig admin, CorsConfig cors) {

    public record JwtConfig(String secret, long expiracaoMinutos) {}

    public record AdminConfig(String email, String senha) {}

    public record CorsConfig(List<String> origens) {}
}
