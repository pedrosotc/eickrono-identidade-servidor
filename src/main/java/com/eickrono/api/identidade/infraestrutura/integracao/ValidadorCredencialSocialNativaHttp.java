package com.eickrono.api.identidade.infraestrutura.integracao;

import com.eickrono.api.identidade.aplicacao.modelo.CredencialSocialNativaValidada;
import com.eickrono.api.identidade.aplicacao.servico.ValidadorCredencialSocialNativa;
import com.eickrono.api.identidade.dominio.modelo.ProvedorVinculoSocial;
import com.eickrono.api.identidade.infraestrutura.configuracao.SocialNativoProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class ValidadorCredencialSocialNativaHttp implements ValidadorCredencialSocialNativa {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final SocialNativoProperties properties;

    public ValidadorCredencialSocialNativaHttp(final RestTemplateBuilder restTemplateBuilder,
                                               final ObjectMapper objectMapper,
                                               final SocialNativoProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties e obrigatorio");
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(this.properties.getTimeout())
                .setReadTimeout(this.properties.getTimeout())
                .build();
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper e obrigatorio");
    }

    @Override
    public CredencialSocialNativaValidada validar(final String provedor, final String tokenExterno) {
        ProvedorVinculoSocial provedorSocial = ProvedorVinculoSocial.fromAlias(provedor)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Provedor social nao suportado para autenticacao nativa."
                ));
        String tokenNormalizado = normalizarObrigatorio(tokenExterno, "tokenExterno");
        return switch (provedorSocial) {
            case GOOGLE -> validarGoogle(tokenNormalizado);
            case APPLE -> validarApple(tokenNormalizado);
            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Provedor social nao suportado para autenticacao nativa."
            );
        };
    }

    private CredencialSocialNativaValidada validarGoogle(final String tokenExterno) {
        JsonNode tokenInfo = consultarGoogleTokenInfo(tokenExterno);
        String identificadorExterno = normalizarTexto(
                tokenInfo.path("sub").asText(null),
                tokenInfo.path("user_id").asText(null)
        );
        if (!StringUtils.hasText(identificadorExterno)) {
            throw tokenSocialInvalido();
        }
        validarAudiencia("google", tokenInfo.path("aud").asText(null),
                properties.getGoogleAudienciasPermitidas());
        String email = normalizarEmail(tokenInfo.path("email").asText(null));
        String nomeExibicao = normalizarTexto(tokenInfo.path("name").asText(null));
        String nomeUsuario = resolverNomeUsuario(email, tokenInfo.path("preferred_username").asText(null));
        String urlAvatar = normalizarTexto(
                tokenInfo.path("picture").asText(null),
                tokenInfo.path("avatar_url").asText(null),
                tokenInfo.path("avatar").asText(null)
        );
        return new CredencialSocialNativaValidada(
                ProvedorVinculoSocial.GOOGLE,
                identificadorExterno,
                email,
                nomeUsuario,
                nomeExibicao,
                urlAvatar
        );
    }

    private JsonNode consultarGoogleTokenInfo(final String tokenExterno) {
        URI uri = UriComponentsBuilder.fromUriString(properties.getGoogleTokenInfoUrl())
                .queryParam("access_token", tokenExterno)
                .build()
                .encode()
                .toUri();
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw tokenSocialInvalido();
            }
            return objectMapper.readTree(Objects.requireNonNullElse(response.getBody(), "{}"));
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (RestClientException | java.io.IOException exception) {
            throw tokenSocialInvalido(exception);
        }
    }

    private CredencialSocialNativaValidada validarApple(final String tokenExterno) {
        try {
            JwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(properties.getAppleJwkSetUri()).build();
            Jwt jwt = decoder.decode(tokenExterno);
            String issuer = normalizarTexto(jwt.getIssuer() == null ? null : jwt.getIssuer().toString());
            if (!Objects.equals(issuer, properties.getAppleIssuer())) {
                throw tokenSocialInvalido();
            }
            validarAudiencia("apple", jwt.getAudience(), properties.getAppleAudienciasPermitidas());
            String identificadorExterno = normalizarTexto(jwt.getSubject());
            if (!StringUtils.hasText(identificadorExterno)) {
                throw tokenSocialInvalido();
            }
            String email = normalizarEmail(jwt.getClaimAsString("email"));
            String nomeExibicao = normalizarTexto(jwt.getClaimAsString("name"));
            String nomeUsuario = resolverNomeUsuario(email, jwt.getClaimAsString("preferred_username"));
            String urlAvatar = normalizarTexto(
                    jwt.getClaimAsString("picture"),
                    jwt.getClaimAsString("avatar_url"),
                    jwt.getClaimAsString("avatar")
            );
            return new CredencialSocialNativaValidada(
                    ProvedorVinculoSocial.APPLE,
                    identificadorExterno,
                    email,
                    nomeUsuario,
                    nomeExibicao,
                    urlAvatar
            );
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (JwtException exception) {
            throw tokenSocialInvalido(exception);
        }
    }

    private void validarAudiencia(final String provedor,
                                  final String audiencia,
                                  final List<String> audienciasPermitidas) {
        validarAudiencia(provedor, List.of(Objects.requireNonNullElse(audiencia, "")), audienciasPermitidas);
    }

    private void validarAudiencia(final String provedor,
                                  final List<String> audienciasToken,
                                  final List<String> audienciasPermitidas) {
        List<String> permitidasNormalizadas = normalizarLista(audienciasPermitidas);
        if (permitidasNormalizadas.isEmpty()) {
            return;
        }
        if (audienciasToken == null || audienciasToken.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Audiencia do token social " + provedor + " ausente."
            );
        }
        boolean permitida = audienciasToken.stream()
                .map(this::normalizarTexto)
                .filter(Objects::nonNull)
                .anyMatch(permitidasNormalizadas::contains);
        if (!permitida) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Audiencia do token social " + provedor + " nao permitida."
            );
        }
    }

    private List<String> normalizarLista(final List<String> valores) {
        if (valores == null || valores.isEmpty()) {
            return List.of();
        }
        return valores.stream()
                .map(this::normalizarTexto)
                .filter(Objects::nonNull)
                .toList();
    }

    private String resolverNomeUsuario(final String email, final String fallback) {
        String texto = normalizarTexto(fallback);
        if (texto != null) {
            return texto;
        }
        if (!StringUtils.hasText(email)) {
            return null;
        }
        int indiceArroba = email.indexOf('@');
        return indiceArroba > 0 ? email.substring(0, indiceArroba) : email;
    }

    private String normalizarObrigatorio(final String valor, final String campo) {
        String normalizado = normalizarTexto(valor);
        if (normalizado == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, campo + " e obrigatorio.");
        }
        return normalizado;
    }

    private String normalizarTexto(final String... valores) {
        if (valores == null) {
            return null;
        }
        for (String valor : valores) {
            if (StringUtils.hasText(valor)) {
                return valor.trim();
            }
        }
        return null;
    }

    private String normalizarEmail(final String valor) {
        String texto = normalizarTexto(valor);
        return texto == null ? null : texto.toLowerCase(Locale.ROOT);
    }

    private ResponseStatusException tokenSocialInvalido() {
        return tokenSocialInvalido(null);
    }

    private ResponseStatusException tokenSocialInvalido(final Throwable cause) {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Token social invalido.",
                cause
        );
    }
}
