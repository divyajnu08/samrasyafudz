package in.samrasyafudz.commonsecurity;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class CommonSecurityAutoConfiguration {

    private final String jwtSecret;
    private final long jwtExpirationMs;

    public CommonSecurityAutoConfiguration(
            @Value("${jwt.secret}") String jwtSecret,
            @Value("${jwt.expiration-ms:3600000}") long jwtExpirationMs) {
        this.jwtSecret = jwtSecret;
        this.jwtExpirationMs = jwtExpirationMs;
    }

    @Bean
    public JwtService jwtService() {
        return new JwtService(jwtSecret, jwtExpirationMs);
    }

    @Bean
    public JwtAuthFilter jwtAuthFilter(JwtService jwtService) {
        return new JwtAuthFilter(jwtService);
    }
}