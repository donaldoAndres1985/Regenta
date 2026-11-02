package com.regenta.shared.security;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.regenta.shared.tenancy.AislamientoPorNegocio;
import com.regenta.shared.tenancy.FijadorDeNegocio;

import jakarta.persistence.EntityManagerFactory;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties({PropiedadesDeSeguridad.class, PropiedadesCors.class})
@EnableTransactionManagement(order = 0, proxyTargetClass = true)
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class ConfiguracionDeSeguridad {

    static final int LARGO_MINIMO_DEL_SECRETO = 32;

    @Bean
    @ConditionalOnMissingBean
    public JwtDecoder jwtDecoder(PropiedadesDeSeguridad propiedades) {
        String secreto = propiedades.getJwt().getSecreto();
        if (secreto == null || secreto.trim().length() < LARGO_MINIMO_DEL_SECRETO) {
            // Valor por defecto para tests si no viene configurado
            secreto = "regenta-secreto-super-seguro-de-al-menos-32-bytes!";
        }
        SecretKey clave = new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        NimbusJwtDecoder decodificador = NimbusJwtDecoder.withSecretKey(clave)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        List<OAuth2TokenValidator<Jwt>> validaciones = new ArrayList<>();
        validaciones.add(new JwtTimestampValidator());
        String emisor = propiedades.getJwt().getEmisor();
        if (emisor != null && !emisor.isBlank()) {
            validaciones.add(new JwtIssuerValidator(emisor));
        }
        decodificador.setJwtValidator(new DelegatingOAuth2TokenValidator<>(validaciones));
        return decodificador;
    }

    @Bean
    public FiltroDeContexto filtroDeContexto() {
        return new FiltroDeContexto();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, PropiedadesDeSeguridad propiedades,
            PropiedadesCors propiedadesCors, FiltroDeContexto filtroDeContexto) throws Exception {
        String[] publicas = propiedades.getRutasPublicas().toArray(new String[0]);

        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource(propiedades, propiedadesCors)))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(publicas).permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(Customizer.withDefaults())
                .authenticationEntryPoint((request, response, authException) -> {
                    String traceId = Trazas.de(request);
                    RespuestaDeError.escribir(response, HttpStatus.UNAUTHORIZED,
                            authException.getMessage() != null ? authException.getMessage() : "No autenticado",
                            traceId);
                })
            )
            .addFilterAfter(filtroDeContexto, BearerTokenAuthenticationFilter.class);

        return http.build();
    }

    private CorsConfigurationSource corsConfigurationSource(PropiedadesDeSeguridad propiedades,
            PropiedadesCors propiedadesCors) {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origenes = (propiedadesCors != null && propiedadesCors.getAllowedOriginPatterns() != null && !propiedadesCors.getAllowedOriginPatterns().isEmpty())
                ? propiedadesCors.getAllowedOriginPatterns()
                : propiedades.getCorsOrigenes();
        config.setAllowedOriginPatterns(origenes);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With", Trazas.CABECERA));
        config.setExposedHeaders(List.of(Trazas.CABECERA));
        config.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    @ConditionalOnMissingBean
    public GuardiaDeModulos guardiaDeModulos() {
        return new GuardiaDeModulos();
    }

    @Bean
    @ConditionalOnMissingBean
    public GuardiaDePermisos guardiaDePermisos() {
        return new GuardiaDePermisos();
    }

    @Bean
    @ConditionalOnMissingBean
    public AislamientoPorNegocio aislamientoPorNegocio(EntityManagerFactory fabrica) {
        return new AislamientoPorNegocio(fabrica, 10);
    }

    @Bean
    @ConditionalOnMissingBean
    public FijadorDeNegocio fijadorDeNegocio(EntityManagerFactory fabrica) {
        return new FijadorDeNegocio(fabrica);
    }
}
