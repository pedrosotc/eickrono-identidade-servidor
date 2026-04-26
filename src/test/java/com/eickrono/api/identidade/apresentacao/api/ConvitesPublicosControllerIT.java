package com.eickrono.api.identidade.apresentacao.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eickrono.api.identidade.AplicacaoApiIdentidade;
import com.eickrono.api.identidade.aplicacao.excecao.FluxoPublicoException;
import com.eickrono.api.identidade.aplicacao.modelo.ConviteOrganizacionalValidado;
import com.eickrono.api.identidade.aplicacao.servico.ConviteOrganizacionalService;
import com.eickrono.api.identidade.support.InfraestruturaTesteIdentidade;
import java.time.OffsetDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = AplicacaoApiIdentidade.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ContextConfiguration(initializers = InfraestruturaTesteIdentidade.Initializer.class)
class ConvitesPublicosControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ConviteOrganizacionalService conviteOrganizacionalService;

    @Test
    void deveConsultarConviteOrganizacionalPublicoValido() throws Exception {
        when(conviteOrganizacionalService.consultarPublico("ORG-ACME-2026"))
                .thenReturn(new ConviteOrganizacionalValidado(
                        "ORG-ACME-2026",
                        "org-acme",
                        "Acme Educacao",
                        "convite@acme.test",
                        "Jane Doe",
                        true,
                        true,
                        OffsetDateTime.parse("2026-05-01T00:00:00Z")
                ));

        mockMvc.perform(get("/api/publica/convites/{codigo}", "ORG-ACME-2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("ORG-ACME-2026"))
                .andExpect(jsonPath("$.organizacaoId").value("org-acme"))
                .andExpect(jsonPath("$.nomeOrganizacao").value("Acme Educacao"))
                .andExpect(jsonPath("$.emailConvidado").value("convite@acme.test"))
                .andExpect(jsonPath("$.nomeConvidado").value("Jane Doe"))
                .andExpect(jsonPath("$.exigeContaSeparada").value(true))
                .andExpect(jsonPath("$.contaExistenteDetectada").value(true));
    }

    @Test
    void deveTraduzirConviteInvalidoComContratoDeErroPublico() throws Exception {
        when(conviteOrganizacionalService.consultarPublico("ORG-EXPIRADO-2026"))
                .thenThrow(new FluxoPublicoException(
                        HttpStatus.GONE,
                        "convite_invalido",
                        "O convite informado nao esta mais disponivel.",
                        Map.of("motivo", "expirado")
                ));

        mockMvc.perform(get("/api/publica/convites/{codigo}", "ORG-EXPIRADO-2026"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.codigo").value("convite_invalido"))
                .andExpect(jsonPath("$.detalhes.motivo").value("expirado"));
    }
}
