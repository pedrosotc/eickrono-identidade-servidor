package com.eickrono.api.identidade.infraestrutura.integracao;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;

import com.eickrono.api.identidade.aplicacao.servico.ConsultadorDisponibilidadeUsuarioSistemaServico;
import com.eickrono.api.identidade.infraestrutura.configuracao.ConfiguradorRestTemplateBackchannelMtls;
import com.eickrono.api.identidade.infraestrutura.configuracao.IntegracaoInternaProperties;
import com.eickrono.api.identidade.infraestrutura.configuracao.PerfilDominioBackchannelProperties;
import java.net.URI;
import java.util.Locale;
import java.util.Objects;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ConsultadorDisponibilidadeUsuarioSistemaHttp implements ConsultadorDisponibilidadeUsuarioSistemaServico {

    private static final String HEADER_SEGREDO_INTERNO = "X-Eickrono-Internal-Secret";
    private static final String HEADER_SISTEMA_SOLICITANTE = "X-Eickrono-Calling-System";
    private static final String CAMINHO_DISPONIBILIDADE_USUARIO =
            "/identidade/perfis-sistema/interna/disponibilidade";
    private static final DefaultResponseErrorHandler NO_OP_ERROR_HANDLER = new NoOpResponseErrorHandler();

    private final RestTemplate restTemplate;
    private final String disponibilidadeUrlBase;
    private final String segredoInterno;
    private final ClienteTokenBackchannelPerfilKeycloak clienteTokenBackchannelPerfilKeycloak;

    public ConsultadorDisponibilidadeUsuarioSistemaHttp(
            final RestTemplateBuilder restTemplateBuilder,
            final PerfilDominioBackchannelProperties properties,
            final IntegracaoInternaProperties integracaoInternaProperties,
            final ConfiguradorRestTemplateBackchannelMtls configuradorRestTemplateBackchannelMtls,
            final ClienteTokenBackchannelPerfilKeycloak clienteTokenBackchannelPerfilKeycloak) {
        PerfilDominioBackchannelProperties configuracao = Objects.requireNonNull(properties, "properties e obrigatorio");
        this.disponibilidadeUrlBase = Objects.requireNonNull(
                configuracao.getDisponibilidadeUrlBase(),
                "integracao.perfil.disponibilidade-url-base e obrigatorio"
        );
        this.restTemplate = Objects.requireNonNull(
                        configuradorRestTemplateBackchannelMtls,
                        "configuradorRestTemplateBackchannelMtls e obrigatorio")
                .configurar(restTemplateBuilder, this.disponibilidadeUrlBase, configuracao.getTimeout())
                .errorHandler(NO_OP_ERROR_HANDLER)
                .build();
        this.segredoInterno = Objects.requireNonNull(integracaoInternaProperties, "integracaoInternaProperties e obrigatorio")
                .getSegredo();
        this.clienteTokenBackchannelPerfilKeycloak = Objects.requireNonNull(
                clienteTokenBackchannelPerfilKeycloak,
                "clienteTokenBackchannelPerfilKeycloak e obrigatorio");
    }

    @Override
    public boolean usuarioDisponivel(final String usuario, final String sistemaSolicitante) {
        String usuarioNormalizado = Objects.requireNonNull(usuario, "usuario e obrigatorio")
                .trim()
                .toLowerCase(Locale.ROOT);
        String sistemaNormalizado = Objects.requireNonNull(sistemaSolicitante, "sistemaSolicitante e obrigatorio")
                .trim()
                .toLowerCase(Locale.ROOT);
        if (usuarioNormalizado.isBlank()) {
            return false;
        }
        if (sistemaNormalizado.isBlank()) {
            throw new ResponseStatusException(BAD_GATEWAY, "Sistema solicitante ausente na validacao central do usuario.");
        }
        ResponseEntity<DisponibilidadeUsuarioInternaResponse> response = restTemplate.exchange(
                URI.create(disponibilidadeUrlBase + CAMINHO_DISPONIBILIDADE_USUARIO
                        + "?identificadorPublicoSistema=" + UriEscapers.escapeQueryParam(usuarioNormalizado)),
                HttpMethod.GET,
                new HttpEntity<>(cabecalhosDisponibilidade(sistemaNormalizado)),
                DisponibilidadeUsuarioInternaResponse.class
        );
        DisponibilidadeUsuarioInternaResponse body = response.getBody();
        if (!response.getStatusCode().is2xxSuccessful() || body == null) {
            throw new ResponseStatusException(
                    BAD_GATEWAY,
                    "Nao foi possivel validar a disponibilidade central do usuario para o sistema solicitado."
            );
        }
        return body.disponivel();
    }

    private HttpHeaders cabecalhosDisponibilidade(final String sistemaSolicitante) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HEADER_SEGREDO_INTERNO, segredoInterno);
        headers.setBearerAuth(clienteTokenBackchannelPerfilKeycloak.obterTokenBearer());
        headers.set(HEADER_SISTEMA_SOLICITANTE, sistemaSolicitante);
        return headers;
    }

    private record DisponibilidadeUsuarioInternaResponse(
            String identificadorPublicoSistema,
            boolean disponivel
    ) {
    }

    private static final class NoOpResponseErrorHandler extends DefaultResponseErrorHandler {
        @Override
        public boolean hasError(@NonNull final ClientHttpResponse response) {
            return false;
        }
    }

    private static final class UriEscapers {
        private UriEscapers() {
        }

        private static String escapeQueryParam(final String value) {
            return value.replace(" ", "%20");
        }
    }
}
