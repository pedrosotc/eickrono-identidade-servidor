package com.eickrono.api.identidade.apresentacao.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eickrono.api.identidade.AplicacaoApiIdentidade;
import com.eickrono.api.identidade.dominio.modelo.DesafioAtestacaoApp;
import com.eickrono.api.identidade.dominio.repositorio.DesafioAtestacaoAppRepositorio;
import com.eickrono.api.identidade.support.InfraestruturaTesteIdentidade;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = AplicacaoApiIdentidade.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ContextConfiguration(initializers = InfraestruturaTesteIdentidade.Initializer.class)
class AtestacaoAppPublicaControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DesafioAtestacaoAppRepositorio desafioRepositorio;

    @Test
    void deveAceitarAplicacaoIdNoDesafioPublico() throws Exception {
        desafioRepositorio.deleteAll();

        mockMvc.perform(post("/api/publica/atestacoes/desafios")
                        .header("User-Agent", "JUnit/MockMvc")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "aplicacaoId": "eickrono-thimisu-app",
                                  "operacao": "LOGIN",
                                  "plataforma": "IOS"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.operacao").value("LOGIN"))
                .andExpect(jsonPath("$.plataforma").value("IOS"))
                .andExpect(jsonPath("$.provedorEsperado").value("APPLE_APP_ATTEST"));

        assertThat(desafioRepositorio.findAll())
                .singleElement()
                .extracting(
                        DesafioAtestacaoApp::getIpSolicitante,
                        DesafioAtestacaoApp::getUserAgentSolicitante,
                        DesafioAtestacaoApp::getOperacao,
                        DesafioAtestacaoApp::getPlataforma)
                .containsExactly(
                        "127.0.0.1",
                        "JUnit/MockMvc",
                        com.eickrono.api.identidade.dominio.modelo.OperacaoAtestacaoApp.LOGIN,
                        com.eickrono.api.identidade.dominio.modelo.PlataformaAtestacaoApp.IOS);
    }
}
