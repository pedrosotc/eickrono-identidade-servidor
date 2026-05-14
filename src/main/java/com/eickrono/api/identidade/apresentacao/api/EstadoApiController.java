package com.eickrono.api.identidade.apresentacao.api;

import com.eickrono.api.identidade.apresentacao.dto.EstadoApiResposta;
import java.time.Instant;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class EstadoApiController {

    private static final String STATUS_OK = "ok";
    private static final String VALOR_DESCONHECIDO = "desconhecida";

    private final String nomeAplicacao;
    private final ObjectProvider<BuildProperties> buildPropertiesProvider;

    public EstadoApiController(@Value("${spring.application.name}") final String nomeAplicacao,
                               final ObjectProvider<BuildProperties> buildPropertiesProvider) {
        this.nomeAplicacao = nomeAplicacao;
        this.buildPropertiesProvider = buildPropertiesProvider;
    }

    @GetMapping("/estado")
    public EstadoApiResposta consultarEstado() {
        return new EstadoApiResposta(
                nomeAplicacao,
                STATUS_OK,
                resolverVersao(),
                resolverBuildTime()
        );
    }

    private String resolverVersao() {
        BuildProperties buildProperties = buildPropertiesProvider.getIfAvailable();
        if (buildProperties != null && StringUtils.hasText(buildProperties.getVersion())) {
            return buildProperties.getVersion();
        }
        return VALOR_DESCONHECIDO;
    }

    private String resolverBuildTime() {
        BuildProperties buildProperties = buildPropertiesProvider.getIfAvailable();
        if (buildProperties == null) {
            return VALOR_DESCONHECIDO;
        }
        Instant tempoBuild = buildProperties.getTime();
        return tempoBuild == null ? VALOR_DESCONHECIDO : tempoBuild.toString();
    }
}
