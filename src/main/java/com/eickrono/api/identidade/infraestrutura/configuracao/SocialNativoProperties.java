package com.eickrono.api.identidade.infraestrutura.configuracao;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "identidade.social.nativo")
public class SocialNativoProperties {

    private String googleTokenInfoUrl = "https://oauth2.googleapis.com/tokeninfo";
    private List<String> googleAudienciasPermitidas = new ArrayList<>();
    private String appleIssuer = "https://appleid.apple.com";
    private String appleJwkSetUri = "https://appleid.apple.com/auth/keys";
    private List<String> appleAudienciasPermitidas = new ArrayList<>();
    private Duration timeout = Duration.ofSeconds(5);

    public String getGoogleTokenInfoUrl() {
        return googleTokenInfoUrl;
    }

    public void setGoogleTokenInfoUrl(final String googleTokenInfoUrl) {
        this.googleTokenInfoUrl = googleTokenInfoUrl;
    }

    public List<String> getGoogleAudienciasPermitidas() {
        return List.copyOf(googleAudienciasPermitidas);
    }

    public void setGoogleAudienciasPermitidas(final List<String> googleAudienciasPermitidas) {
        this.googleAudienciasPermitidas = copiarLista(googleAudienciasPermitidas);
    }

    public String getAppleIssuer() {
        return appleIssuer;
    }

    public void setAppleIssuer(final String appleIssuer) {
        this.appleIssuer = appleIssuer;
    }

    public String getAppleJwkSetUri() {
        return appleJwkSetUri;
    }

    public void setAppleJwkSetUri(final String appleJwkSetUri) {
        this.appleJwkSetUri = appleJwkSetUri;
    }

    public List<String> getAppleAudienciasPermitidas() {
        return List.copyOf(appleAudienciasPermitidas);
    }

    public void setAppleAudienciasPermitidas(final List<String> appleAudienciasPermitidas) {
        this.appleAudienciasPermitidas = copiarLista(appleAudienciasPermitidas);
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(final Duration timeout) {
        this.timeout = timeout == null ? Duration.ofSeconds(5) : timeout;
    }

    private List<String> copiarLista(final List<String> valores) {
        return valores == null ? new ArrayList<>() : new ArrayList<>(valores);
    }
}
