package com.regenta.gateway.seguridad;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * Validacion del token en el borde. HU-007.
 *
 * <p>El gateway solo comprueba que el token exista, este firmado por nosotros,
 * no haya expirado y venga del emisor esperado. Quien puede hacer que, con que
 * plan y sobre que sucursal lo decide cada servicio; aqui no.
 */
@Configuration
@EnableWebFluxSecurity
@EnableConfigurationProperties(PropiedadesDeSeguridad.class)
public class ConfiguracionDeSeguridad {

    private static final Logger registro = LoggerFactory.getLogger(ConfiguracionDeSeguridad.class);

    /** HS256 exige una clave de 256 bits: 32 caracteres. */
    static final int LARGO_MINIMO_DEL_SECRETO = 32;

    /**
     * Decodificador HS256. El secreto lo comparte el emisor (servicio-usuarios,
     * HU-013). Si falta, el contexto no levanta: un gateway que no puede validar
     * firmas no debe atender ni una peticion.
     */
    @Bean
    public ReactiveJwtDecoder decodificadorDeJwt(PropiedadesDeSeguridad propiedades) {
        String secreto = propiedades.getJwt().getSecreto();
        if (secreto == null || secreto.trim().length() < LARGO_MINIMO_DEL_SECRETO) {
            throw new IllegalStateException(
                    "Falta el secreto del JWT: definir la variable JWT_SECRETO"
                            + " (regenta.seguridad.jwt.secreto) con al menos "
                            + LARGO_MINIMO_DEL_SECRETO + " caracteres");
        }
        SecretKey clave = new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        NimbusReactiveJwtDecoder decodificador = NimbusReactiveJwtDecoder.withSecretKey(clave)
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
    public SecurityWebFilterChain cadenaDeSeguridad(ServerHttpSecurity http,
            PropiedadesDeSeguridad propiedades) {
        String[] publicas = propiedades.getRutasPublicas().toArray(new String[0]);
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .authorizeExchange(intercambios -> intercambios
                        .pathMatchers(publicas).permitAll()
                        .anyExchange().authenticated())
                .oauth2ResourceServer(recursos -> recursos
                        .authenticationEntryPoint(this::rechazar)
                        .jwt(Customizer.withDefaults()))
                .exceptionHandling(manejo -> manejo.authenticationEntryPoint(this::rechazar))
                .build();
    }

    /** Token ausente, expirado, con firma rota o de otro emisor: 401 y al log con la traza. */
    private Mono<Void> rechazar(ServerWebExchange intercambio, AuthenticationException fallo) {
        String traza = Trazas.de(intercambio);
        registro.warn("401 en el gateway: {} metodo={} ruta={} trace_id={}",
                fallo.getMessage(),
                intercambio.getRequest().getMethod(),
                intercambio.getRequest().getPath().value(),
                traza);
        return RespuestaDeError.escribir(intercambio, HttpStatus.UNAUTHORIZED,
                "token ausente o invalido");
    }
}
