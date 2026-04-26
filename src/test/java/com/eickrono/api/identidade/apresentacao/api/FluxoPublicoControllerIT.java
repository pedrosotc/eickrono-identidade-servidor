package com.eickrono.api.identidade.apresentacao.api;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eickrono.api.identidade.AplicacaoApiIdentidade;
import com.eickrono.api.identidade.aplicacao.modelo.CadastroInternoRealizado;
import com.eickrono.api.identidade.aplicacao.modelo.ConfirmacaoEmailCadastroPublicoRealizada;
import com.eickrono.api.identidade.aplicacao.modelo.ConviteOrganizacionalValidado;
import com.eickrono.api.identidade.aplicacao.servico.AtestacaoAppServico;
import com.eickrono.api.identidade.aplicacao.servico.AvaliacaoSegurancaAplicativoService;
import com.eickrono.api.identidade.aplicacao.servico.AutenticacaoSessaoInternaServico;
import com.eickrono.api.identidade.aplicacao.servico.CadastroContaInternaServico;
import com.eickrono.api.identidade.aplicacao.servico.ClienteContextoPessoaPerfil;
import com.eickrono.api.identidade.aplicacao.servico.ConviteOrganizacionalService;
import com.eickrono.api.identidade.aplicacao.servico.RecuperacaoSenhaService;
import com.eickrono.api.identidade.aplicacao.servico.RegistroDispositivoLoginSilenciosoService;
import com.eickrono.api.identidade.aplicacao.modelo.DispositivoSessaoRegistrado;
import com.eickrono.api.identidade.aplicacao.modelo.ContextoPessoaPerfil;
import com.eickrono.api.identidade.aplicacao.modelo.VinculoSocialPendenteCadastro;
import com.eickrono.api.identidade.dominio.modelo.CanalValidacaoTelefoneCadastro;
import com.eickrono.api.identidade.dominio.modelo.ProvedorVinculoSocial;
import com.eickrono.api.identidade.dominio.modelo.TipoPessoaCadastro;
import com.eickrono.api.identidade.support.InfraestruturaTesteIdentidade;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.anyString;

@SpringBootTest(classes = AplicacaoApiIdentidade.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ContextConfiguration(initializers = InfraestruturaTesteIdentidade.Initializer.class)
class FluxoPublicoControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CadastroContaInternaServico cadastroContaInternaServico;

    @MockBean
    private AtestacaoAppServico atestacaoAppServico;

    @MockBean
    private AvaliacaoSegurancaAplicativoService avaliacaoSegurancaAplicativoService;

    @MockBean
    private AutenticacaoSessaoInternaServico autenticacaoSessaoInternaServico;

    @MockBean
    private ClienteContextoPessoaPerfil clienteContextoPessoaPerfil;

    @MockBean
    private RecuperacaoSenhaService recuperacaoSenhaService;

    @MockBean
    private RegistroDispositivoLoginSilenciosoService registroDispositivoLoginSilenciosoService;

    @MockBean
    private ConviteOrganizacionalService conviteOrganizacionalService;

    @BeforeEach
    void setUp() {
        when(atestacaoAppServico.validarComprovante(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new com.eickrono.api.identidade.aplicacao.modelo.ValidacaoAtestacaoAppConcluida(
                        null,
                        com.eickrono.api.identidade.aplicacao.modelo.ValidacaoOficialAtestacaoAppResultado.naoExecutada(
                                "validacao oficial nao executada no teste"
                        ),
                        com.eickrono.api.identidade.aplicacao.modelo.StatusValidacaoAtestacaoApp.VALIDADA_LOCALMENTE
                ));
        org.mockito.Mockito.doAnswer(invocacao -> new com.eickrono.api.identidade.aplicacao.modelo
                        .AvaliacaoSegurancaAplicativoRealizada(false, true, 0, java.util.List.of()))
                .when(avaliacaoSegurancaAplicativoService)
                .avaliar(
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.anyString()
                );
        when(registroDispositivoLoginSilenciosoService.registrar(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()))
                .thenReturn(new DispositivoSessaoRegistrado(
                        "device-token-teste",
                        OffsetDateTime.parse("2026-03-27T20:00:00Z")
                ));
    }

    @Test
    void deveConsultarDisponibilidadePublicaDoUsuario() throws Exception {
        when(cadastroContaInternaServico.usuarioDisponivelPublico("ana.souza")).thenReturn(false);

        mockMvc.perform(get("/api/publica/cadastros/usuarios/disponibilidade")
                        .param("usuario", " Ana.Souza "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario").value("ana.souza"))
                .andExpect(jsonPath("$.disponivel").value(false));

        verify(cadastroContaInternaServico).usuarioDisponivelPublico("ana.souza");
    }

    @Test
    void deveCancelarCadastroPendentePublico() throws Exception {
        mockMvc.perform(delete("/api/publica/cadastros/{cadastroId}",
                        "11111111-1111-1111-1111-111111111111"))
                .andExpect(status().isNoContent());

        verify(cadastroContaInternaServico).cancelarCadastroPendentePublico(
                java.util.UUID.fromString("11111111-1111-1111-1111-111111111111"));
    }

    @Test
    void deveMapearContaNaoLiberadaQuandoKeycloakRetornaContaDesabilitada() throws Exception {
        UUID cadastroId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        when(autenticacaoSessaoInternaServico.autenticar("b@b.com", "SenhaForte123"))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account disabled"));
        when(cadastroContaInternaServico.buscarCadastroPendenteEmailPublico("b@b.com"))
                .thenReturn(Optional.of(cadastroId));

        mockMvc.perform(post("/api/publica/sessoes")
                        .contentType(MediaType.APPLICATION_JSON)
                .content("""
                                {
                                  "aplicacaoId": "eickrono-thimisu-app",
                                  "login": "b@b.com",
                                  "senha": "SenhaForte123",
                                  "dispositivo": {
                                    "plataforma": "IOS",
                                    "identificadorInstalacao": "instalacao-teste",
                                    "modelo": "simulador",
                                    "sistemaOperacional": "ios",
                                    "versaoSistema": "18",
                                    "versaoApp": "1.0.0"
                                  },
                                  "atestacao": {
                                    "plataforma": "IOS",
                                    "provedor": "APPLE_APP_ATTEST",
                                    "tipoComprovante": "OBJETO_ASSERCAO",
                                    "identificadorDesafio": "desafio",
                                    "desafioBase64": "ZGVzYWZpbw==",
                                    "conteudoComprovante": "Y29tcHJvdmFudGU=",
                                    "geradoEm": "2026-03-26T20:00:00Z",
                                    "chaveId": "chave"
                                  },
                                  "segurancaAplicativo": {
                                    "plataforma": "IOS",
                                    "provedorAtestacao": "APPLE_APP_ATTEST",
                                    "rootOuJailbreak": false,
                                    "debuggerDetectado": false,
                                    "hookingSuspeito": false,
                                    "tamperSuspeito": false,
                                    "riscoCapturaTela": false,
                                    "assinaturaValida": true,
                                    "identidadeAplicativoValida": true,
                                    "sinaisRisco": [],
                                    "scoreRiscoLocal": 0,
                                    "bundleIdentifier": "com.eickrono.thimisu",
                                    "teamIdentifier": "TEAM123"
                                  }
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("conta_nao_liberada"))
                .andExpect(jsonPath("$.detalhes.cadastroId").value(cadastroId.toString()));
    }

    @Test
    void deveMapearCredenciaisInvalidasQuandoKeycloakRetornaSenhaInvalida() throws Exception {
        when(autenticacaoSessaoInternaServico.autenticar("a@a.com", "SenhaErrada123"))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid user credentials"));

        mockMvc.perform(post("/api/publica/sessoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "aplicacaoId": "eickrono-thimisu-app",
                                  "login": "a@a.com",
                                  "senha": "SenhaErrada123",
                                  "dispositivo": {
                                    "plataforma": "IOS",
                                    "identificadorInstalacao": "instalacao-teste",
                                    "modelo": "simulador",
                                    "sistemaOperacional": "ios",
                                    "versaoSistema": "18",
                                    "versaoApp": "1.0.0"
                                  },
                                  "atestacao": {
                                    "plataforma": "IOS",
                                    "provedor": "APPLE_APP_ATTEST",
                                    "tipoComprovante": "OBJETO_ASSERCAO",
                                    "identificadorDesafio": "desafio",
                                    "desafioBase64": "ZGVzYWZpbw==",
                                    "conteudoComprovante": "Y29tcHJvdmFudGU=",
                                    "geradoEm": "2026-03-26T20:00:00Z",
                                    "chaveId": "chave"
                                  },
                                  "segurancaAplicativo": {
                                    "plataforma": "IOS",
                                    "provedorAtestacao": "APPLE_APP_ATTEST",
                                    "rootOuJailbreak": false,
                                    "debuggerDetectado": false,
                                    "hookingSuspeito": false,
                                    "tamperSuspeito": false,
                                    "riscoCapturaTela": false,
                                    "assinaturaValida": true,
                                    "identidadeAplicativoValida": true,
                                    "sinaisRisco": [],
                                    "scoreRiscoLocal": 0,
                                    "bundleIdentifier": "com.eickrono.thimisu",
                                    "teamIdentifier": "TEAM123"
                                  }
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("credenciais_invalidas"));
    }

    @Test
    void deveAceitarLoginPublicoComUsuarioEAutenticarNoKeycloakComEmailCanonico() throws Exception {
        when(clienteContextoPessoaPerfil.buscarPorUsuario("ana.souza"))
                .thenReturn(Optional.of(new ContextoPessoaPerfil(
                        10L,
                        "sub-ana-souza",
                        "ana@eickrono.com",
                        "Ana Souza",
                        "usuario-id-ana",
                        "LIBERADO"
                )));
        when(autenticacaoSessaoInternaServico.autenticar("ana@eickrono.com", "SenhaForte123"))
                .thenReturn(new com.eickrono.api.identidade.aplicacao.modelo.SessaoInternaAutenticada(
                        true,
                        "Bearer",
                        "access-token",
                        "refresh-token",
                        3600
                ));

        mockMvc.perform(post("/api/publica/sessoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "aplicacaoId": "eickrono-thimisu-app",
                                  "login": " Ana.Souza ",
                                  "senha": "SenhaForte123",
                                  "dispositivo": {
                                    "plataforma": "IOS",
                                    "identificadorInstalacao": "instalacao-teste",
                                    "modelo": "simulador",
                                    "sistemaOperacional": "ios",
                                    "versaoSistema": "18",
                                    "versaoApp": "1.0.0"
                                  },
                                  "atestacao": {
                                    "plataforma": "IOS",
                                    "provedor": "APPLE_APP_ATTEST",
                                    "tipoComprovante": "OBJETO_ASSERCAO",
                                    "identificadorDesafio": "desafio",
                                    "desafioBase64": "ZGVzYWZpbw==",
                                    "conteudoComprovante": "Y29tcHJvdmFudGU=",
                                    "geradoEm": "2026-03-26T20:00:00Z",
                                    "chaveId": "chave"
                                  },
                                  "segurancaAplicativo": {
                                    "plataforma": "IOS",
                                    "provedorAtestacao": "APPLE_APP_ATTEST",
                                    "rootOuJailbreak": false,
                                    "debuggerDetectado": false,
                                    "hookingSuspeito": false,
                                    "tamperSuspeito": false,
                                    "riscoCapturaTela": false,
                                    "assinaturaValida": true,
                                    "identidadeAplicativoValida": true,
                                    "sinaisRisco": [],
                                    "scoreRiscoLocal": 0,
                                    "bundleIdentifier": "com.eickrono.thimisu",
                                    "teamIdentifier": "TEAM123"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.autenticado").value(true))
                .andExpect(jsonPath("$.statusUsuario").value("LIBERADO"))
                .andExpect(jsonPath("$.tokenDispositivo").value("device-token-teste"));

        verify(clienteContextoPessoaPerfil).buscarPorUsuario("ana.souza");
        verify(autenticacaoSessaoInternaServico).autenticar("ana@eickrono.com", "SenhaForte123");
        verify(clienteContextoPessoaPerfil)
                .buscarPorEmail(argThat(email -> "ana@eickrono.com".equalsIgnoreCase(email)));
    }

    @Test
    void deveAceitarCadastroPublicoComVinculoSocialPendente() throws Exception {
        doReturn(new CadastroInternoRealizado(
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                "sub-ana",
                "ana@eickrono.com",
                true
        )).when(cadastroContaInternaServico).cadastrarPublico(
                any(TipoPessoaCadastro.class),
                anyString(),
                nullable(String.class),
                anyString(),
                any(com.eickrono.api.identidade.dominio.modelo.SexoPessoaCadastro.class),
                anyString(),
                any(java.time.LocalDate.class),
                anyString(),
                anyString(),
                any(CanalValidacaoTelefoneCadastro.class),
                anyString(),
                nullable(VinculoSocialPendenteCadastro.class),
                nullable(ConviteOrganizacionalValidado.class),
                anyString(),
                anyString(),
                nullable(String.class)
        );

        mockMvc.perform(post("/api/publica/cadastros")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "aplicacaoId": "eickrono-thimisu-app",
                                  "tipoPessoa": "FISICA",
                                  "nomeCompleto": "Ana Souza",
                                  "usuario": "ana.souza",
                                  "sexo": "FEMININO",
                                  "paisNascimento": "BR",
                                  "dataNascimento": "1990-05-10",
                                  "emailPrincipal": "ana@eickrono.com",
                                  "telefone": "+5511999999999",
                                  "tipoValidacaoTelefone": "SMS",
                                  "senha": "SenhaForte@123",
                                  "confirmacaoSenha": "SenhaForte@123",
                                  "aceitouTermos": true,
                                  "aceitouPrivacidade": true,
                                  "plataformaApp": "IOS",
                                  "vinculoSocialPendente": {
                                    "provedor": "google",
                                    "identificadorExterno": "google-user-123",
                                    "nomeUsuarioExterno": "ana.google"
                                  },
                                  "atestacao": {
                                    "plataforma": "IOS",
                                    "provedor": "APPLE_APP_ATTEST",
                                    "tipoComprovante": "OBJETO_ASSERCAO",
                                    "identificadorDesafio": "desafio",
                                    "desafioBase64": "ZGVzYWZpbw==",
                                    "conteudoComprovante": "Y29tcHJvdmFudGU=",
                                    "geradoEm": "2026-03-26T20:00:00Z",
                                    "chaveId": "chave"
                                  },
                                  "segurancaAplicativo": {
                                    "plataforma": "IOS",
                                    "provedorAtestacao": "APPLE_APP_ATTEST",
                                    "rootOuJailbreak": false,
                                    "debuggerDetectado": false,
                                    "hookingSuspeito": false,
                                    "tamperSuspeito": false,
                                    "riscoCapturaTela": false,
                                    "assinaturaValida": true,
                                    "identidadeAplicativoValida": true,
                                    "sinaisRisco": [],
                                    "scoreRiscoLocal": 0,
                                    "bundleIdentifier": "com.eickrono.thimisu",
                                    "teamIdentifier": "TEAM123"
                                  }
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cadastroId").value("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"))
                .andExpect(jsonPath("$.emailPrincipal").value("ana@eickrono.com"))
                .andExpect(jsonPath("$.proximoPasso").value("VALIDAR_CONTATOS"));

        verify(cadastroContaInternaServico).cadastrarPublico(
                eq(TipoPessoaCadastro.FISICA),
                eq("Ana Souza"),
                isNull(),
                eq("ana.souza"),
                eq(com.eickrono.api.identidade.dominio.modelo.SexoPessoaCadastro.FEMININO),
                eq("BR"),
                eq(java.time.LocalDate.parse("1990-05-10")),
                eq("ana@eickrono.com"),
                eq("+5511999999999"),
                eq(CanalValidacaoTelefoneCadastro.SMS),
                eq("SenhaForte@123"),
                eq(new VinculoSocialPendenteCadastro(
                        ProvedorVinculoSocial.GOOGLE,
                        "google-user-123",
                        "ana.google"
                )),
                isNull(),
                eq("app-flutter-publico"),
                eq("127.0.0.1"),
                nullable(String.class)
        );
    }

    @Test
    void deveAceitarCadastroPublicoPorConviteOrganizacional() throws Exception {
        ConviteOrganizacionalValidado convite = new ConviteOrganizacionalValidado(
                "ORG-ACME-2026",
                "org-acme",
                "Acme Educacao",
                "convite@acme.test",
                "Jane Doe",
                true,
                false,
                OffsetDateTime.parse("2026-05-01T00:00:00Z")
        );
        when(conviteOrganizacionalService.consultarPublico("ORG-ACME-2026")).thenReturn(convite);
        doReturn(new CadastroInternoRealizado(
                UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
                "sub-jane",
                "convite@acme.test",
                true
        )).when(cadastroContaInternaServico).cadastrarPublico(
                any(TipoPessoaCadastro.class),
                anyString(),
                nullable(String.class),
                anyString(),
                any(com.eickrono.api.identidade.dominio.modelo.SexoPessoaCadastro.class),
                anyString(),
                any(java.time.LocalDate.class),
                anyString(),
                anyString(),
                any(CanalValidacaoTelefoneCadastro.class),
                anyString(),
                nullable(VinculoSocialPendenteCadastro.class),
                any(ConviteOrganizacionalValidado.class),
                anyString(),
                anyString(),
                nullable(String.class)
        );

        mockMvc.perform(post("/api/publica/convites/{codigo}/cadastros", "ORG-ACME-2026")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "aplicacaoId": "eickrono-thimisu-app",
                                  "tipoPessoa": "FISICA",
                                  "nomeCompleto": "Jane Doe",
                                  "usuario": "jane.doe",
                                  "sexo": "FEMININO",
                                  "paisNascimento": "BR",
                                  "dataNascimento": "1990-05-10",
                                  "emailPrincipal": "convite@acme.test",
                                  "telefone": "+5511999999999",
                                  "tipoValidacaoTelefone": "SMS",
                                  "senha": "SenhaForte@123",
                                  "confirmacaoSenha": "SenhaForte@123",
                                  "aceitouTermos": true,
                                  "aceitouPrivacidade": true,
                                  "plataformaApp": "IOS",
                                  "atestacao": {
                                    "plataforma": "IOS",
                                    "provedor": "APPLE_APP_ATTEST",
                                    "tipoComprovante": "OBJETO_ASSERCAO",
                                    "identificadorDesafio": "desafio",
                                    "desafioBase64": "ZGVzYWZpbw==",
                                    "conteudoComprovante": "Y29tcHJvdmFudGU=",
                                    "geradoEm": "2026-03-26T20:00:00Z",
                                    "chaveId": "chave"
                                  },
                                  "segurancaAplicativo": {
                                    "plataforma": "IOS",
                                    "provedorAtestacao": "APPLE_APP_ATTEST",
                                    "rootOuJailbreak": false,
                                    "debuggerDetectado": false,
                                    "hookingSuspeito": false,
                                    "tamperSuspeito": false,
                                    "riscoCapturaTela": false,
                                    "assinaturaValida": true,
                                    "identidadeAplicativoValida": true,
                                    "sinaisRisco": [],
                                    "scoreRiscoLocal": 0,
                                    "bundleIdentifier": "com.eickrono.thimisu",
                                    "teamIdentifier": "TEAM123"
                                  }
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cadastroId").value("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"))
                .andExpect(jsonPath("$.emailPrincipal").value("convite@acme.test"))
                .andExpect(jsonPath("$.proximoPasso").value("VALIDAR_CONTATOS"));

        verify(conviteOrganizacionalService).consultarPublico("ORG-ACME-2026");
        verify(cadastroContaInternaServico).cadastrarPublico(
                eq(TipoPessoaCadastro.FISICA),
                eq("Jane Doe"),
                isNull(),
                eq("jane.doe"),
                eq(com.eickrono.api.identidade.dominio.modelo.SexoPessoaCadastro.FEMININO),
                eq("BR"),
                eq(java.time.LocalDate.parse("1990-05-10")),
                eq("convite@acme.test"),
                eq("+5511999999999"),
                eq(CanalValidacaoTelefoneCadastro.SMS),
                eq("SenhaForte@123"),
                isNull(),
                eq(convite),
                eq("app-flutter-publico"),
                eq("127.0.0.1"),
                nullable(String.class)
        );
    }

    @Test
    void deveAceitarCadastroPublicoPorConviteComEmailAlternativoQuandoContaSeparadaForObrigatoria() throws Exception {
        ConviteOrganizacionalValidado convite = new ConviteOrganizacionalValidado(
                "ORG-ACME-2026",
                "org-acme",
                "Acme Educacao",
                "convite@acme.test",
                "Jane Doe",
                true,
                true,
                OffsetDateTime.parse("2026-05-01T00:00:00Z")
        );
        when(conviteOrganizacionalService.consultarPublico("ORG-ACME-2026")).thenReturn(convite);
        doReturn(new CadastroInternoRealizado(
                UUID.fromString("abababab-abab-abab-abab-abababababab"),
                "sub-jane-org",
                "jane.empresa@acme.test",
                true
        )).when(cadastroContaInternaServico).cadastrarPublico(
                any(TipoPessoaCadastro.class),
                anyString(),
                nullable(String.class),
                anyString(),
                any(com.eickrono.api.identidade.dominio.modelo.SexoPessoaCadastro.class),
                anyString(),
                any(java.time.LocalDate.class),
                anyString(),
                anyString(),
                any(CanalValidacaoTelefoneCadastro.class),
                anyString(),
                nullable(VinculoSocialPendenteCadastro.class),
                any(ConviteOrganizacionalValidado.class),
                anyString(),
                anyString(),
                nullable(String.class)
        );

        mockMvc.perform(post("/api/publica/convites/{codigo}/cadastros", "ORG-ACME-2026")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "aplicacaoId": "eickrono-thimisu-app",
                                  "tipoPessoa": "FISICA",
                                  "nomeCompleto": "Jane Doe",
                                  "usuario": "jane.org",
                                  "sexo": "FEMININO",
                                  "paisNascimento": "BR",
                                  "dataNascimento": "1990-05-10",
                                  "emailPrincipal": "jane.empresa@acme.test",
                                  "telefone": "+5511999999999",
                                  "tipoValidacaoTelefone": "SMS",
                                  "senha": "SenhaForte@123",
                                  "confirmacaoSenha": "SenhaForte@123",
                                  "aceitouTermos": true,
                                  "aceitouPrivacidade": true,
                                  "plataformaApp": "IOS",
                                  "atestacao": {
                                    "plataforma": "IOS",
                                    "provedor": "APPLE_APP_ATTEST",
                                    "tipoComprovante": "OBJETO_ASSERCAO",
                                    "identificadorDesafio": "desafio",
                                    "desafioBase64": "ZGVzYWZpbw==",
                                    "conteudoComprovante": "Y29tcHJvdmFudGU=",
                                    "geradoEm": "2026-03-26T20:00:00Z",
                                    "chaveId": "chave"
                                  },
                                  "segurancaAplicativo": {
                                    "plataforma": "IOS",
                                    "provedorAtestacao": "APPLE_APP_ATTEST",
                                    "rootOuJailbreak": false,
                                    "debuggerDetectado": false,
                                    "hookingSuspeito": false,
                                    "tamperSuspeito": false,
                                    "riscoCapturaTela": false,
                                    "assinaturaValida": true,
                                    "identidadeAplicativoValida": true,
                                    "sinaisRisco": [],
                                    "scoreRiscoLocal": 0,
                                    "bundleIdentifier": "com.eickrono.thimisu",
                                    "teamIdentifier": "TEAM123"
                                  }
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cadastroId").value("abababab-abab-abab-abab-abababababab"))
                .andExpect(jsonPath("$.emailPrincipal").value("jane.empresa@acme.test"));

        verify(cadastroContaInternaServico).cadastrarPublico(
                eq(TipoPessoaCadastro.FISICA),
                eq("Jane Doe"),
                isNull(),
                eq("jane.org"),
                eq(com.eickrono.api.identidade.dominio.modelo.SexoPessoaCadastro.FEMININO),
                eq("BR"),
                eq(java.time.LocalDate.parse("1990-05-10")),
                eq("jane.empresa@acme.test"),
                eq("+5511999999999"),
                eq(CanalValidacaoTelefoneCadastro.SMS),
                eq("SenhaForte@123"),
                isNull(),
                eq(convite),
                eq("app-flutter-publico"),
                eq("127.0.0.1"),
                nullable(String.class)
        );
    }

    @Test
    void deveRejeitarCadastroPublicoPorConviteQuandoContaSeparadaUsaEmailOriginal() throws Exception {
        ConviteOrganizacionalValidado convite = new ConviteOrganizacionalValidado(
                "ORG-ACME-2026",
                "org-acme",
                "Acme Educacao",
                "convite@acme.test",
                "Jane Doe",
                true,
                true,
                OffsetDateTime.parse("2026-05-01T00:00:00Z")
        );
        when(conviteOrganizacionalService.consultarPublico("ORG-ACME-2026")).thenReturn(convite);

        mockMvc.perform(post("/api/publica/convites/{codigo}/cadastros", "ORG-ACME-2026")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "aplicacaoId": "eickrono-thimisu-app",
                                  "tipoPessoa": "FISICA",
                                  "nomeCompleto": "Jane Doe",
                                  "usuario": "jane.doe",
                                  "sexo": "FEMININO",
                                  "paisNascimento": "BR",
                                  "dataNascimento": "1990-05-10",
                                  "emailPrincipal": "convite@acme.test",
                                  "telefone": "+5511999999999",
                                  "tipoValidacaoTelefone": "SMS",
                                  "senha": "SenhaForte@123",
                                  "confirmacaoSenha": "SenhaForte@123",
                                  "aceitouTermos": true,
                                  "aceitouPrivacidade": true,
                                  "plataformaApp": "IOS",
                                  "atestacao": {
                                    "plataforma": "IOS",
                                    "provedor": "APPLE_APP_ATTEST",
                                    "tipoComprovante": "OBJETO_ASSERCAO",
                                    "identificadorDesafio": "desafio",
                                    "desafioBase64": "ZGVzYWZpbw==",
                                    "conteudoComprovante": "Y29tcHJvdmFudGU=",
                                    "geradoEm": "2026-03-26T20:00:00Z",
                                    "chaveId": "chave"
                                  },
                                  "segurancaAplicativo": {
                                    "plataforma": "IOS",
                                    "provedorAtestacao": "APPLE_APP_ATTEST",
                                    "rootOuJailbreak": false,
                                    "debuggerDetectado": false,
                                    "hookingSuspeito": false,
                                    "tamperSuspeito": false,
                                    "riscoCapturaTela": false,
                                    "assinaturaValida": true,
                                    "identidadeAplicativoValida": true,
                                    "sinaisRisco": [],
                                    "scoreRiscoLocal": 0,
                                    "bundleIdentifier": "com.eickrono.thimisu",
                                    "teamIdentifier": "TEAM123"
                                  }
                                }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("conta_separada_obrigatoria"))
                .andExpect(jsonPath("$.detalhes.emailConvidado").value("convite@acme.test"));
    }

    @Test
    void deveConfirmarCadastroPublicoComCodigoEmailETelefone() throws Exception {
        UUID cadastroId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
        when(cadastroContaInternaServico.confirmarEmailPublico(
                eq(cadastroId),
                eq("123456"),
                eq("654321")
        )).thenReturn(new ConfirmacaoEmailCadastroPublicoRealizada(
                cadastroId,
                "sub-ana",
                "ana@eickrono.com",
                "usuario-001",
                "LIBERADO",
                true,
                true
        ));

        mockMvc.perform(post("/api/publica/cadastros/{cadastroId}/confirmacoes/email", cadastroId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "codigo": "123456",
                                  "codigoTelefone": "654321"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cadastroId").value(cadastroId.toString()))
                .andExpect(jsonPath("$.emailConfirmado").value(true))
                .andExpect(jsonPath("$.liberadoParaLogin").value(true))
                .andExpect(jsonPath("$.proximoPasso").value("LOGIN"));
    }

    @Test
    void deveMapearContaNaoLiberadaQuandoKeycloakRetornaContaNaoConfigurada() throws Exception {
        UUID cadastroId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        when(autenticacaoSessaoInternaServico.autenticar("b@b.com", "SenhaForte123"))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account is not fully set up"));
        when(cadastroContaInternaServico.buscarCadastroPendenteEmailPublico("b@b.com"))
                .thenReturn(Optional.of(cadastroId));

        mockMvc.perform(post("/api/publica/sessoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "aplicacaoId": "eickrono-thimisu-app",
                                  "login": "b@b.com",
                                  "senha": "SenhaForte123",
                                  "dispositivo": {
                                    "plataforma": "IOS",
                                    "identificadorInstalacao": "instalacao-teste",
                                    "modelo": "simulador",
                                    "sistemaOperacional": "ios",
                                    "versaoSistema": "18",
                                    "versaoApp": "1.0.0"
                                  },
                                  "atestacao": {
                                    "plataforma": "IOS",
                                    "provedor": "APPLE_APP_ATTEST",
                                    "tipoComprovante": "OBJETO_ASSERCAO",
                                    "identificadorDesafio": "desafio",
                                    "desafioBase64": "ZGVzYWZpbw==",
                                    "conteudoComprovante": "Y29tcHJvdmFudGU=",
                                    "geradoEm": "2026-03-26T20:00:00Z",
                                    "chaveId": "chave"
                                  },
                                  "segurancaAplicativo": {
                                    "plataforma": "IOS",
                                    "provedorAtestacao": "APPLE_APP_ATTEST",
                                    "rootOuJailbreak": false,
                                    "debuggerDetectado": false,
                                    "hookingSuspeito": false,
                                    "tamperSuspeito": false,
                                    "riscoCapturaTela": false,
                                    "assinaturaValida": true,
                                    "identidadeAplicativoValida": true,
                                    "sinaisRisco": [],
                                    "scoreRiscoLocal": 0,
                                    "bundleIdentifier": "com.eickrono.thimisu",
                                    "teamIdentifier": "TEAM123"
                                  }
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("conta_nao_liberada"))
                .andExpect(jsonPath("$.detalhes.cadastroId").value(cadastroId.toString()));
    }

    @Test
    void deveMapearContaIncompletaQuandoKeycloakRetornaContaNaoConfiguradaSemCadastroPendente() throws Exception {
        when(autenticacaoSessaoInternaServico.autenticar("c@c.com", "SenhaForte123"))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account is not fully set up"));
        when(cadastroContaInternaServico.buscarCadastroPendenteEmailPublico("c@c.com"))
                .thenReturn(Optional.empty());

        mockMvc.perform(post("/api/publica/sessoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "aplicacaoId": "eickrono-thimisu-app",
                                  "login": "c@c.com",
                                  "senha": "SenhaForte123",
                                  "dispositivo": {
                                    "plataforma": "IOS",
                                    "identificadorInstalacao": "instalacao-teste",
                                    "modelo": "simulador",
                                    "sistemaOperacional": "ios",
                                    "versaoSistema": "18",
                                    "versaoApp": "1.0.0"
                                  },
                                  "atestacao": {
                                    "plataforma": "IOS",
                                    "provedor": "APPLE_APP_ATTEST",
                                    "tipoComprovante": "OBJETO_ASSERCAO",
                                    "identificadorDesafio": "desafio",
                                    "desafioBase64": "ZGVzYWZpbw==",
                                    "conteudoComprovante": "Y29tcHJvdmFudGU=",
                                    "geradoEm": "2026-03-26T20:00:00Z",
                                    "chaveId": "chave"
                                  },
                                  "segurancaAplicativo": {
                                    "plataforma": "IOS",
                                    "provedorAtestacao": "APPLE_APP_ATTEST",
                                    "rootOuJailbreak": false,
                                    "debuggerDetectado": false,
                                    "hookingSuspeito": false,
                                    "tamperSuspeito": false,
                                    "riscoCapturaTela": false,
                                    "assinaturaValida": true,
                                    "identidadeAplicativoValida": true,
                                    "sinaisRisco": [],
                                    "scoreRiscoLocal": 0,
                                    "bundleIdentifier": "com.eickrono.thimisu",
                                    "teamIdentifier": "TEAM123"
                                  }
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("conta_incompleta"));
    }

    @Test
    void deveEmitirTokenDispositivoJaNoLoginPublico() throws Exception {
        when(autenticacaoSessaoInternaServico.autenticar("a@a.com", "SenhaForte123"))
                .thenReturn(new com.eickrono.api.identidade.aplicacao.modelo.SessaoInternaAutenticada(
                        true,
                        "Bearer",
                        "access-token",
                        "refresh-token",
                        3600
                ));
        when(clienteContextoPessoaPerfil.buscarPorEmail("a@a.com"))
                .thenReturn(Optional.of(new ContextoPessoaPerfil(
                        10L,
                        "sub-123",
                        "a@a.com",
                        "Ana",
                        "usuario-1",
                        "LIBERADO"
                )));

        mockMvc.perform(post("/api/publica/sessoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "aplicacaoId": "eickrono-thimisu-app",
                                  "login": "a@a.com",
                                  "senha": "SenhaForte123",
                                  "dispositivo": {
                                    "plataforma": "IOS",
                                    "identificadorInstalacao": "instalacao-teste",
                                    "modelo": "simulador",
                                    "sistemaOperacional": "ios",
                                    "versaoSistema": "18",
                                    "versaoApp": "1.0.0"
                                  },
                                  "atestacao": {
                                    "plataforma": "IOS",
                                    "provedor": "APPLE_APP_ATTEST",
                                    "tipoComprovante": "OBJETO_ASSERCAO",
                                    "identificadorDesafio": "desafio",
                                    "desafioBase64": "ZGVzYWZpbw==",
                                    "conteudoComprovante": "Y29tcHJvdmFudGU=",
                                    "geradoEm": "2026-03-26T20:00:00Z",
                                    "chaveId": "chave"
                                  },
                                  "segurancaAplicativo": {
                                    "plataforma": "IOS",
                                    "provedorAtestacao": "APPLE_APP_ATTEST",
                                    "rootOuJailbreak": false,
                                    "debuggerDetectado": false,
                                    "hookingSuspeito": false,
                                    "tamperSuspeito": false,
                                    "riscoCapturaTela": false,
                                    "assinaturaValida": true,
                                    "identidadeAplicativoValida": true,
                                    "sinaisRisco": [],
                                    "scoreRiscoLocal": 0,
                                    "bundleIdentifier": "com.eickrono.thimisu",
                                    "teamIdentifier": "TEAM123"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.autenticado").value(true))
                .andExpect(jsonPath("$.tokenDispositivo").value("device-token-teste"));
    }
}
