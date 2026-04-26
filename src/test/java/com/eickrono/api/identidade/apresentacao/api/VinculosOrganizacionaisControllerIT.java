package com.eickrono.api.identidade.apresentacao.api;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eickrono.api.identidade.AplicacaoApiIdentidade;
import com.eickrono.api.identidade.aplicacao.modelo.ContextoPessoaPerfil;
import com.eickrono.api.identidade.aplicacao.servico.ClienteContextoPessoaPerfil;
import com.eickrono.api.identidade.dominio.modelo.VinculoOrganizacional;
import com.eickrono.api.identidade.dominio.repositorio.VinculoOrganizacionalRepositorio;
import com.eickrono.api.identidade.support.ClienteAdministracaoCadastroKeycloakStubConfiguration;
import com.eickrono.api.identidade.support.InfraestruturaTesteIdentidade;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = AplicacaoApiIdentidade.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ClienteAdministracaoCadastroKeycloakStubConfiguration.class)
@ContextConfiguration(initializers = InfraestruturaTesteIdentidade.Initializer.class)
class VinculosOrganizacionaisControllerIT {

    private static final String ENDPOINT = "/identidade/vinculos-organizacionais";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VinculoOrganizacionalRepositorio vinculoOrganizacionalRepositorio;

    @MockBean
    private ClienteContextoPessoaPerfil clienteContextoPessoaPerfil;

    @BeforeEach
    @SuppressWarnings("unused")
    void limparEstado() {
        vinculoOrganizacionalRepositorio.deleteAll();
    }

    @Test
    void deveListarVinculosOrganizacionaisDoUsuarioAutenticado() throws Exception {
        vinculoOrganizacionalRepositorio.save(new VinculoOrganizacional(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                10L,
                "usuario-001",
                "org-acme",
                "Acme Educacao",
                "ORG-ACME-2026",
                "convite@acme.test",
                true,
                OffsetDateTime.parse("2026-04-01T10:00:00Z")
        ));
        vinculoOrganizacionalRepositorio.save(new VinculoOrganizacional(
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                10L,
                "usuario-001",
                "org-beta",
                "Beta Labs",
                "ORG-BETA-2026",
                null,
                false,
                OffsetDateTime.parse("2026-04-02T10:00:00Z")
        ));
        when(clienteContextoPessoaPerfil.buscarPorSub("sub-123"))
                .thenReturn(Optional.of(new ContextoPessoaPerfil(
                        10L,
                        "sub-123",
                        "jane@empresa.test",
                        "Jane Doe",
                        "usuario-001",
                        "ATIVO"
                )));

        mockMvc.perform(get(ENDPOINT).with(jwtEscopo("vinculos:ler")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vinculos[0].organizacaoId").value("org-acme"))
                .andExpect(jsonPath("$.vinculos[0].nomeOrganizacao").value("Acme Educacao"))
                .andExpect(jsonPath("$.vinculos[0].conviteCodigo").value("ORG-ACME-2026"))
                .andExpect(jsonPath("$.vinculos[0].emailConvidado").value("convite@acme.test"))
                .andExpect(jsonPath("$.vinculos[0].exigeContaSeparada").value(true))
                .andExpect(jsonPath("$.vinculos[1].organizacaoId").value("org-beta"))
                .andExpect(jsonPath("$.vinculos[1].nomeOrganizacao").value("Beta Labs"))
                .andExpect(jsonPath("$.vinculos[1].exigeContaSeparada").value(false));
    }

    @Test
    void deveRetornarListaVaziaQuandoNaoHouverContextoLocal() throws Exception {
        when(clienteContextoPessoaPerfil.buscarPorSub("sub-123"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get(ENDPOINT).with(jwtEscopo("vinculos:ler")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vinculos").isArray())
                .andExpect(jsonPath("$.vinculos").isEmpty());
    }

    @Test
    void deveNegarListagemSemEscopo() throws Exception {
        mockMvc.perform(get(ENDPOINT).with(jwtSemEscopo()))
                .andExpect(status().isForbidden());
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor jwtEscopo(final String escopo) {
        return jwt().jwt(builder -> builder
                        .subject("sub-123")
                        .claim("email", "teste@eickrono.com")
                        .claim("name", "Pessoa Teste"))
                .authorities(new SimpleGrantedAuthority("SCOPE_" + Objects.requireNonNull(escopo)));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor jwtSemEscopo() {
        return jwt().jwt(builder -> builder
                .subject("sub-123")
                .claim("email", "teste@eickrono.com")
                .claim("name", "Pessoa Teste"));
    }
}
