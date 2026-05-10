package com.eickrono.api.identidade.apresentacao.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eickrono.api.identidade.AplicacaoApiIdentidade;
import com.eickrono.api.identidade.aplicacao.servico.ProvisionamentoIdentidadeService;
import com.eickrono.api.identidade.dominio.modelo.Pessoa;
import com.eickrono.api.identidade.dominio.repositorio.PessoaRepositorio;
import com.eickrono.api.identidade.support.InfraestruturaTesteIdentidade;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest(classes = AplicacaoApiIdentidade.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ContextConfiguration(initializers = InfraestruturaTesteIdentidade.Initializer.class)
class PessoasInternaControllerIT {

    private static final String SEGREDO_INTERNO = "local-internal-secret";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProvisionamentoIdentidadeService provisionamentoIdentidadeService;

    @Autowired
    private PessoaRepositorio pessoaRepositorio;

    @Test
    void deveConfirmarPessoaDeCadastroPendentePorChamadaInterna() throws Exception {
        Pessoa pessoaPendente = provisionamentoIdentidadeService.provisionarCadastroPendente(
                "sub-pessoa-interna-001",
                "pendente@eickrono.com",
                "Pessoa Pendente",
                OffsetDateTime.parse("2026-05-03T03:00:00Z")
        );

        mockMvc.perform(post("/identidade/pessoas/interna/confirmacoes-email")
                        .with(jwtInternoAutenticacao())
                        .header("X-Eickrono-Internal-Secret", SEGREDO_INTERNO)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "sub": "sub-pessoa-interna-001",
                                  "email": "confirmado@eickrono.com",
                                  "nomeCompleto": "Pessoa Confirmada",
                                  "confirmadoEm": "2026-05-03T03:10:00Z"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pessoaId").value(pessoaPendente.getId()))
                .andExpect(jsonPath("$.sub").value("sub-pessoa-interna-001"))
                .andExpect(jsonPath("$.emailPrincipal").value("confirmado@eickrono.com"));

        assertThat(pessoaRepositorio.findBySub("sub-pessoa-interna-001"))
                .get()
                .extracting(Pessoa::getEmail)
                .isEqualTo("confirmado@eickrono.com");
    }

    @Test
    void deveRecusarConfirmacaoInternaComSegredoInvalido() throws Exception {
        mockMvc.perform(post("/identidade/pessoas/interna/confirmacoes-email")
                        .with(jwtInternoAutenticacao())
                        .header("X-Eickrono-Internal-Secret", "segredo-invalido")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "sub": "sub-pessoa-interna-002",
                                  "email": "confirmado@eickrono.com",
                                  "nomeCompleto": "Pessoa Confirmada",
                                  "confirmadoEm": "2026-05-03T03:10:00Z"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    private RequestPostProcessor jwtInternoAutenticacao() {
        return jwt().jwt(jwt -> jwt
                .subject("service-account-eickrono-autenticacao-interno")
                .claim("azp", "eickrono-autenticacao-interno")
                .claim("preferred_username", "service-account-eickrono-autenticacao-interno"));
    }
}
