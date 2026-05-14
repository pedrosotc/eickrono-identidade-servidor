package com.eickrono.api.identidade.infraestrutura.integracao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eickrono.api.identidade.aplicacao.modelo.CredencialSocialNativaValidada;
import com.eickrono.api.identidade.dominio.modelo.ProvedorVinculoSocial;
import com.eickrono.api.identidade.infraestrutura.configuracao.SocialNativoProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class ValidadorCredencialSocialNativaHttpTest {

    private MockWebServer servidorGoogle;

    @BeforeEach
    void setUp() throws IOException {
        servidorGoogle = new MockWebServer();
        servidorGoogle.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        servidorGoogle.shutdown();
    }

    @Test
    void deveValidarTokenGoogleSemUsarKeycloak() throws Exception {
        servidorGoogle.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("""
                        {
                          "sub": "google-user-123",
                          "aud": "app-flutter-hml",
                          "email": "Pessoa.Google@Eickrono.com",
                          "name": "Pessoa Google",
                          "picture": "https://img/google.png"
                        }
                        """)
                .addHeader("Content-Type", "application/json"));

        ValidadorCredencialSocialNativaHttp validador = criarValidador(List.of("app-flutter-hml"));

        CredencialSocialNativaValidada credencial = validador.validar("google", "google-access-token");

        assertThat(credencial.provedor()).isEqualTo(ProvedorVinculoSocial.GOOGLE);
        assertThat(credencial.identificadorExterno()).isEqualTo("google-user-123");
        assertThat(credencial.email()).isEqualTo("pessoa.google@eickrono.com");
        assertThat(credencial.nomeUsuarioExterno()).isEqualTo("pessoa.google");
        assertThat(credencial.nomeExibicaoExterno()).isEqualTo("Pessoa Google");
        assertThat(credencial.urlAvatarExterno()).isEqualTo("https://img/google.png");
        assertThat(servidorGoogle.takeRequest().getPath()).contains("/tokeninfo");
    }

    @Test
    void deveRejeitarGoogleQuandoAudienciaNaoForPermitida() {
        servidorGoogle.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("""
                        {
                          "sub": "google-user-123",
                          "aud": "outro-client-id",
                          "email": "pessoa@eickrono.com"
                        }
                        """)
                .addHeader("Content-Type", "application/json"));

        ValidadorCredencialSocialNativaHttp validador = criarValidador(List.of("app-flutter-hml"));

        assertThatThrownBy(() -> validador.validar("google", "google-access-token"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private ValidadorCredencialSocialNativaHttp criarValidador(final List<String> audienciasPermitidas) {
        SocialNativoProperties properties = new SocialNativoProperties();
        properties.setGoogleTokenInfoUrl(servidorGoogle.url("/tokeninfo").toString());
        properties.setGoogleAudienciasPermitidas(audienciasPermitidas);
        return new ValidadorCredencialSocialNativaHttp(
                new RestTemplateBuilder(),
                new ObjectMapper(),
                properties
        );
    }
}
