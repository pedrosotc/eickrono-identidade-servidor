package com.eickrono.api.identidade.apresentacao.api;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
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
import com.eickrono.api.identidade.aplicacao.modelo.ContextoSolicitacaoFluxoPublico;
import com.eickrono.api.identidade.aplicacao.modelo.CredencialSocialNativaValidada;
import com.eickrono.api.identidade.aplicacao.modelo.PerfilSistemaProjetoPorEmailResolvido;
import com.eickrono.api.identidade.aplicacao.modelo.ProjetoFluxoPublicoResolvido;
import com.eickrono.api.identidade.aplicacao.servico.AtestacaoAppServico;
import com.eickrono.api.identidade.aplicacao.servico.AvaliacaoSegurancaAplicativoService;
import com.eickrono.api.identidade.aplicacao.servico.AutenticacaoSessaoInternaServico;
import com.eickrono.api.identidade.aplicacao.servico.CadastroContaInternaServico;
import com.eickrono.api.identidade.aplicacao.servico.ClienteContextoPessoaPerfilSistema;
import com.eickrono.api.identidade.aplicacao.servico.ContextoSocialPendenteJdbc;
import com.eickrono.api.identidade.aplicacao.servico.ConviteOrganizacionalService;
import com.eickrono.api.identidade.aplicacao.servico.LocalizadorPerfilSistemaProjetoPorEmailJdbc;
import com.eickrono.api.identidade.aplicacao.servico.RecuperacaoSenhaService;
import com.eickrono.api.identidade.aplicacao.servico.RegistroDispositivoLoginSilenciosoService;
import com.eickrono.api.identidade.aplicacao.servico.ResolvedorProjetoFluxoPublicoJdbc;
import com.eickrono.api.identidade.aplicacao.servico.ValidadorCredencialSocialNativa;
import com.eickrono.api.identidade.aplicacao.servico.VinculoSocialService;
import com.eickrono.api.identidade.aplicacao.modelo.DispositivoSessaoRegistrado;
import com.eickrono.api.identidade.aplicacao.modelo.ContextoPessoaPerfilSistema;
import com.eickrono.api.identidade.aplicacao.modelo.RecuperacaoSenhaIniciada;
import com.eickrono.api.identidade.aplicacao.modelo.VinculoSocialPendenteCadastro;
import com.eickrono.api.identidade.aplicacao.modelo.StatusCadastroPublico;
import com.eickrono.api.identidade.dominio.modelo.CanalValidacaoTelefoneCadastro;
import com.eickrono.api.identidade.dominio.modelo.CadastroConta;
import com.eickrono.api.identidade.dominio.modelo.ProvedorVinculoSocial;
import com.eickrono.api.identidade.dominio.modelo.TipoFormaAcesso;
import com.eickrono.api.identidade.dominio.modelo.TipoPessoaCadastro;
import com.eickrono.api.identidade.dominio.repositorio.FormaAcessoRepositorio;
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
    private ClienteContextoPessoaPerfilSistema clienteContextoPessoaPerfilSistema;

    @MockBean
    private RecuperacaoSenhaService recuperacaoSenhaService;

    @MockBean
    private RegistroDispositivoLoginSilenciosoService registroDispositivoLoginSilenciosoService;

    @MockBean
    private ConviteOrganizacionalService conviteOrganizacionalService;

    @MockBean
    private ContextoSocialPendenteJdbc contextoSocialPendenteJdbc;

    @MockBean
    private ResolvedorProjetoFluxoPublicoJdbc resolvedorProjetoFluxoPublico;

    @MockBean
    private LocalizadorPerfilSistemaProjetoPorEmailJdbc localizadorPerfilSistemaProjetoPorEmail;

    @MockBean
    private FormaAcessoRepositorio formaAcessoRepositorio;

    @MockBean
    private VinculoSocialService vinculoSocialService;

    @MockBean
    private ValidadorCredencialSocialNativa validadorCredencialSocialNativa;

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
        when(resolvedorProjetoFluxoPublico.resolverAtivo("eickrono-thimisu-app"))
                .thenReturn(new ProjetoFluxoPublicoResolvido(
                        7L,
                        "eickrono-thimisu-app",
                        "Thimisu",
                        "app",
                        "Thimisu",
                        "ios",
                        false
                ));
        when(contextoSocialPendenteJdbc.buscarAtivo(isNull(), eq(7L)))
                .thenReturn(Optional.empty());
        when(localizadorPerfilSistemaProjetoPorEmail.localizar(any(), anyString()))
                .thenReturn(Optional.empty());
        when(formaAcessoRepositorio.findByTipoAndProvedorAndIdentificador(any(), anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(validadorCredencialSocialNativa.validar(eq("google"), anyString()))
                .thenReturn(new CredencialSocialNativaValidada(
                        ProvedorVinculoSocial.GOOGLE,
                        "google-user-123",
                        "google@eickrono.com",
                        "google",
                        "Pessoa Google",
                        "https://img/google.png"
                ));
        when(validadorCredencialSocialNativa.validar(eq("apple"), anyString()))
                .thenReturn(new CredencialSocialNativaValidada(
                        ProvedorVinculoSocial.APPLE,
                        "apple-user-123",
                        "estudiantemeduba@gmail.com",
                        "estudiantemeduba",
                        "Estudiante Meduba",
                        null
                ));
    }

    @Test
    void deveConsultarDisponibilidadePublicaDoUsuario() throws Exception {
        setUp();
        when(cadastroContaInternaServico.identificadorPublicoSistemaDisponivelPublico(
                "ana.souza",
                "eickrono-thimisu-app"
        ))
                .thenReturn(false);

        mockMvc.perform(get("/api/publica/cadastros/usuarios/disponibilidade")
                        .param("usuario", " Ana.Souza ")
                        .param("aplicacaoId", " Eickrono-Thimisu-App "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario").value("ana.souza"))
                .andExpect(jsonPath("$.disponivel").value(false));

        verify(cadastroContaInternaServico).identificadorPublicoSistemaDisponivelPublico(
                "ana.souza",
                "eickrono-thimisu-app"
        );
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
    void deveCancelarContextoSocialPendenteNoProjetoAtual() throws Exception {
        UUID contextoId = UUID.fromString("77777777-7777-7777-7777-777777777777");

        mockMvc.perform(delete("/api/publica/sessoes/contextos-sociais-pendentes/{contextoId}", contextoId)
                        .param("aplicacaoId", "eickrono-thimisu-app"))
                .andExpect(status().isNoContent());

        verify(contextoSocialPendenteJdbc).cancelar(contextoId, 7L, "USUARIO_DESISTIU");
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
    void deveMapearFalhaDeAutenticacaoSocialSemUsarCredenciaisInvalidas() throws Exception {
        when(validadorCredencialSocialNativa.validar("google", "google-access-token"))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token social rejeitado"));

        mockMvc.perform(post("/api/publica/sessoes/sociais")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "aplicacaoId": "eickrono-thimisu-app",
                                  "provedor": "google",
                                  "tokenExterno": "google-access-token",
                                  "dispositivo": {
                                    "plataforma": "IOS",
                                    "identificadorInstalacao": "instalacao-social",
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
                .andExpect(jsonPath("$.codigo").value("autenticacao_social_invalida"))
                .andExpect(jsonPath("$.mensagem")
                        .value("Não foi possível concluir a autenticação com a rede social informada."));
    }

    @Test
    void deveClassificarConflitoSocialComoEntrarEVincularQuandoEmailJaPossuiContaNoProjeto() throws Exception {
        UUID contextoId = UUID.fromString("99999999-9999-9999-9999-999999999999");
        when(localizadorPerfilSistemaProjetoPorEmail.localizar(7L, "estudiantemeduba@gmail.com"))
                .thenReturn(Optional.of(new PerfilSistemaProjetoPorEmailResolvido(
                        UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                        "estudiantemeduba@gmail.com",
                        "pedroso_tc"
                )));
        when(contextoSocialPendenteJdbc.registrarOuAtualizar(
                any(ProjetoFluxoPublicoResolvido.class),
                eq("apple"),
                eq("apple-user-123"),
                eq("estudiantemeduba@gmail.com"),
                eq("estudiantemeduba"),
                eq("Estudiante Meduba"),
                eq("https://img/apple.png"),
                eq(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")),
                eq("pedroso_tc")
        )).thenReturn(contextoId);

        mockMvc.perform(post("/api/publica/sessoes/sociais")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "aplicacaoId": "eickrono-thimisu-app",
                                  "provedor": "apple",
                                  "tokenExterno": "apple-id-token",
                                  "identificadorExterno": "apple-user-123",
                                  "email": "estudiantemeduba@gmail.com",
                                  "nomeUsuarioExterno": "estudiantemeduba",
                                  "nomeCompleto": "Estudiante Meduba",
                                  "urlAvatarExterno": "https://img/apple.png",
                                  "dispositivo": {
                                    "plataforma": "IOS",
                                    "identificadorInstalacao": "instalacao-social",
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
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("social_sem_conta_local"))
                .andExpect(jsonPath("$.detalhes.acaoSugerida").value("ENTRAR_E_VINCULAR"))
                .andExpect(jsonPath("$.detalhes.loginSugerido").value("pedroso_tc"))
                .andExpect(jsonPath("$.detalhes.emailContaExistente").value("estudiantemeduba@gmail.com"))
                .andExpect(jsonPath("$.detalhes.contextoSocialPendenteId").value(contextoId.toString()));
        verify(autenticacaoSessaoInternaServico, never()).autenticarSocial(anyString(), anyString());
    }

    @Test
    void deveClassificarSocialSemContaLocalComoAbrirCadastroSemCriarSessaoKeycloak() throws Exception {
        UUID contextoId = UUID.fromString("88888888-8888-8888-8888-888888888888");
        when(contextoSocialPendenteJdbc.registrarOuAtualizar(
                any(ProjetoFluxoPublicoResolvido.class),
                eq("apple"),
                eq("apple-user-123"),
                eq("estudiantemeduba@gmail.com"),
                eq("estudiantemeduba"),
                eq("Estudiante Meduba"),
                isNull(),
                isNull(),
                isNull()
        )).thenReturn(contextoId);

        mockMvc.perform(post("/api/publica/sessoes/sociais")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "aplicacaoId": "eickrono-thimisu-app",
                                  "provedor": "apple",
                                  "tokenExterno": "apple-id-token",
                                  "dispositivo": {
                                    "plataforma": "IOS",
                                    "identificadorInstalacao": "instalacao-social",
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
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("social_sem_conta_local"))
                .andExpect(jsonPath("$.detalhes.acaoSugerida").value("ABRIR_CADASTRO"))
                .andExpect(jsonPath("$.detalhes.contextoSocialPendenteId").value(contextoId.toString()));
        verify(autenticacaoSessaoInternaServico, never()).autenticarSocial(anyString(), anyString());
    }

    @Test
    void deveEmitirSessaoSocialQuandoIdentidadeJaEstaVinculadaLocalmente() throws Exception {
        when(formaAcessoRepositorio.findByTipoAndProvedorAndIdentificador(
                TipoFormaAcesso.SOCIAL,
                "APPLE",
                "apple-user-123"
        )).thenReturn(Optional.of(org.mockito.Mockito.mock(com.eickrono.api.identidade.dominio.modelo.FormaAcesso.class)));
        when(autenticacaoSessaoInternaServico.autenticarSocial("apple", "apple-id-token"))
                .thenReturn(new com.eickrono.api.identidade.aplicacao.modelo.SessaoInternaAutenticada(
                        true,
                        "Bearer",
                        "access-token-social",
                        "refresh-token-social",
                        3600
                ));

        mockMvc.perform(post("/api/publica/sessoes/sociais")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "aplicacaoId": "eickrono-thimisu-app",
                                  "provedor": "apple",
                                  "tokenExterno": "apple-id-token",
                                  "identificadorExterno": "apple-user-123",
                                  "email": "estudiantemeduba@gmail.com",
                                  "nomeUsuarioExterno": "estudiantemeduba",
                                  "nomeCompleto": "Estudiante Meduba",
                                  "dispositivo": {
                                    "plataforma": "IOS",
                                    "identificadorInstalacao": "instalacao-social",
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
                .andExpect(jsonPath("$.accessToken").value("access-token-social"));
    }

    @Test
    void deveCancelarVinculacaoSocialPendenteQuandoLoginInformadoNaoCorrespondeAContaSugerida() throws Exception {
        UUID contextoId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        when(contextoSocialPendenteJdbc.buscarAtivo(contextoId, 7L))
                .thenReturn(Optional.of(new ContextoSocialPendenteJdbc.ContextoSocialPendenteAtivo(
                        contextoId,
                        7L,
                        UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                        "usuario.sugerido@eickrono.com",
                        "ENTRAR_E_VINCULAR",
                        0,
                        3
                )));

        mockMvc.perform(post("/api/publica/sessoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "aplicacaoId": "eickrono-thimisu-app",
                                  "login": "outra.conta@eickrono.com",
                                  "senha": "SenhaErrada123",
                                  "contextoSocialPendenteId": "44444444-4444-4444-4444-444444444444",
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
                .andExpect(jsonPath("$.codigo").value("vinculacao_social_pendente_cancelada"))
                .andExpect(jsonPath("$.detalhes.contextoSocialPendenteId").value(contextoId.toString()))
                .andExpect(jsonPath("$.detalhes.motivoCancelamento").value("LOGIN_DIVERGENTE"));

        verify(contextoSocialPendenteJdbc).cancelar(contextoId, 7L, "LOGIN_DIVERGENTE");
    }

    @Test
    void deveCancelarVinculacaoSocialPendenteNaTerceiraFalhaDeCredenciais() throws Exception {
        UUID contextoId = UUID.fromString("55555555-5555-5555-5555-555555555555");
        when(contextoSocialPendenteJdbc.buscarAtivo(contextoId, 7L))
                .thenReturn(Optional.of(new ContextoSocialPendenteJdbc.ContextoSocialPendenteAtivo(
                        contextoId,
                        7L,
                        UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
                        "usuario.sugerido@eickrono.com",
                        "ENTRAR_E_VINCULAR",
                        2,
                        3
                )));
        when(autenticacaoSessaoInternaServico.autenticar("usuario.sugerido@eickrono.com", "SenhaErrada123"))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid user credentials"));
        when(contextoSocialPendenteJdbc.registrarFalha(contextoId, 7L))
                .thenReturn(new ContextoSocialPendenteJdbc.ResultadoTentativaFalha(3, 0, true));

        mockMvc.perform(post("/api/publica/sessoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "aplicacaoId": "eickrono-thimisu-app",
                                  "login": "usuario.sugerido@eickrono.com",
                                  "senha": "SenhaErrada123",
                                  "contextoSocialPendenteId": "55555555-5555-5555-5555-555555555555",
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
                .andExpect(jsonPath("$.codigo").value("vinculacao_social_pendente_cancelada"))
                .andExpect(jsonPath("$.detalhes.contextoSocialPendenteId").value(contextoId.toString()))
                .andExpect(jsonPath("$.detalhes.motivoCancelamento").value("LIMITE_TENTATIVAS"));
    }

    @Test
    void deveOferecerRegularizacaoComNovaSenhaQuandoContaPendenteDoProjetoAtualTiverSenhaInvalida() throws Exception {
        CadastroConta cadastro = new CadastroConta(
                UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"),
                "sub-ana",
                TipoPessoaCadastro.FISICA,
                "Ana Souza",
                null,
                "ana.souza",
                null,
                null,
                null,
                "ana@eickrono.com",
                "+5511999999999",
                CanalValidacaoTelefoneCadastro.SMS,
                "hash",
                OffsetDateTime.parse("2026-03-25T18:00:00Z"),
                OffsetDateTime.parse("2026-03-26T03:00:00Z"),
                "thimisu-backend",
                "127.0.0.1",
                "JUnit",
                OffsetDateTime.parse("2026-03-25T18:00:00Z"),
                OffsetDateTime.parse("2026-03-25T18:00:00Z")
        );
        cadastro.registrarProjetoFluxoPublico(7L, true, OffsetDateTime.parse("2026-03-25T18:00:00Z"));
        when(autenticacaoSessaoInternaServico.autenticar("ana@eickrono.com", "SenhaErrada123"))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid user credentials"));
        when(cadastroContaInternaServico.buscarCadastroPendenteEmailPublicoPorProjeto(
                "eickrono-thimisu-app",
                "ana@eickrono.com"
        )).thenReturn(Optional.of(cadastro));

        mockMvc.perform(post("/api/publica/sessoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "aplicacaoId": "eickrono-thimisu-app",
                                  "login": "ana@eickrono.com",
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
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("conta_pendente_redefinir_senha"))
                .andExpect(jsonPath("$.detalhes.cadastroId").value(cadastro.getCadastroId().toString()))
                .andExpect(jsonPath("$.detalhes.email").value("ana@eickrono.com"))
                .andExpect(jsonPath("$.detalhes.requerNovaSenha").value(true));
    }

    @Test
    void deveAceitarLoginPublicoComUsuarioEAutenticarNoKeycloakComEmailCanonico() throws Exception {
        when(clienteContextoPessoaPerfilSistema.buscarPorIdentificadorPublicoSistema("ana.souza"))
                .thenReturn(Optional.of(new ContextoPessoaPerfilSistema(
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

        verify(clienteContextoPessoaPerfilSistema).buscarPorIdentificadorPublicoSistema("ana.souza");
        verify(autenticacaoSessaoInternaServico).autenticar("ana@eickrono.com", "SenhaForte123");
        verify(clienteContextoPessoaPerfilSistema)
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
                anyString(),
                nullable(String.class),
                nullable(ContextoSolicitacaoFluxoPublico.class)
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
                                  "locale": "pt-BR",
                                  "timeZone": "America/Sao_Paulo",
                                  "tipoProdutoExibicao": "app",
                                  "produtoExibicao": "Thimisu",
                                  "canalExibicao": "ios",
                                  "empresaExibicao": "Eickrono",
                                  "ambienteExibicao": "HML",
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
                eq("eickrono-thimisu-app"),
                eq("eickrono-thimisu-app"),
                eq("127.0.0.1"),
                nullable(String.class),
                eq(new ContextoSolicitacaoFluxoPublico(
                        "pt-BR",
                        "America/Sao_Paulo",
                        "app",
                        "Thimisu",
                        "ios",
                        "Eickrono",
                        "HML"
                ))
        );
    }

    @Test
    void deveRegistrarTodosOsVinculosSociaisPendentesAoCriarCadastroPublico() throws Exception {
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
                anyString(),
                nullable(String.class),
                nullable(ContextoSolicitacaoFluxoPublico.class)
        );

        mockMvc.perform(post("/api/publica/cadastros")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", "127.0.0.1")
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
                                    "contextoSocialPendenteId": "google-contexto-1",
                                    "nomeUsuarioExterno": "ana.google",
                                    "email": "ana@eickrono.com",
                                    "nomeCompleto": "Ana Google",
                                    "urlAvatarExterno": "https://img/google.png"
                                  },
                                  "vinculosSociaisPendentes": [
                                    {
                                      "provedor": "google",
                                      "identificadorExterno": "google-user-123",
                                      "contextoSocialPendenteId": "google-contexto-1",
                                      "nomeUsuarioExterno": "ana.google",
                                      "email": "ana@eickrono.com",
                                      "nomeCompleto": "Ana Google",
                                      "urlAvatarExterno": "https://img/google.png"
                                    },
                                    {
                                      "provedor": "apple",
                                      "identificadorExterno": "apple-user-123",
                                      "contextoSocialPendenteId": "apple-contexto-1",
                                      "nomeUsuarioExterno": "ana.apple",
                                      "email": "ana@eickrono.com",
                                      "nomeCompleto": "Ana Apple",
                                      "urlAvatarExterno": "https://img/apple.png"
                                    }
                                  ],
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
                .andExpect(status().isCreated());

        verify(contextoSocialPendenteJdbc).registrarOuAtualizar(
                argThat(projeto -> projeto != null && projeto.clienteEcossistemaId() == 7L),
                eq("google"),
                eq("google-user-123"),
                eq("ana@eickrono.com"),
                eq("ana.google"),
                eq("Ana Google"),
                eq("https://img/google.png"),
                isNull(),
                isNull()
        );
        verify(contextoSocialPendenteJdbc).registrarOuAtualizar(
                argThat(projeto -> projeto != null && projeto.clienteEcossistemaId() == 7L),
                eq("apple"),
                eq("apple-user-123"),
                eq("ana@eickrono.com"),
                eq("ana.apple"),
                eq("Ana Apple"),
                eq("https://img/apple.png"),
                isNull(),
                isNull()
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
                anyString(),
                nullable(String.class),
                nullable(ContextoSolicitacaoFluxoPublico.class)
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
                                  "locale": "pt-BR",
                                  "timeZone": "America/Sao_Paulo",
                                  "tipoProdutoExibicao": "app",
                                  "produtoExibicao": "Thimisu",
                                  "canalExibicao": "ios",
                                  "empresaExibicao": "Eickrono",
                                  "ambienteExibicao": "HML",
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
                eq("eickrono-thimisu-app"),
                eq("eickrono-thimisu-app"),
                eq("127.0.0.1"),
                nullable(String.class),
                eq(new ContextoSolicitacaoFluxoPublico(
                        "pt-BR",
                        "America/Sao_Paulo",
                        "app",
                        "Thimisu",
                        "ios",
                        "Eickrono",
                        "HML"
                ))
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
                anyString(),
                nullable(String.class),
                nullable(ContextoSolicitacaoFluxoPublico.class)
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
                                  "locale": "pt-BR",
                                  "timeZone": "America/Sao_Paulo",
                                  "tipoProdutoExibicao": "app",
                                  "produtoExibicao": "Thimisu",
                                  "canalExibicao": "ios",
                                  "empresaExibicao": "Eickrono",
                                  "ambienteExibicao": "HML",
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
                eq("eickrono-thimisu-app"),
                eq("eickrono-thimisu-app"),
                eq("127.0.0.1"),
                nullable(String.class),
                eq(new ContextoSolicitacaoFluxoPublico(
                        "pt-BR",
                        "America/Sao_Paulo",
                        "app",
                        "Thimisu",
                        "ios",
                        "Eickrono",
                        "HML"
                ))
        );
    }

    @Test
    void deveIniciarRecuperacaoSenhaComContextoDeLocaleEFuso() throws Exception {
        when(recuperacaoSenhaService.iniciar(
                eq("eickrono-thimisu-app"),
                eq("ana@eickrono.com"),
                eq(new ContextoSolicitacaoFluxoPublico(
                        "es-AR",
                        "America/Argentina/Buenos_Aires",
                        "app",
                        "Thimisu",
                        "ios",
                        "Eickrono",
                        "HML"
                ))
        )).thenReturn(RecuperacaoSenhaIniciada.validarCodigoRecuperacao(
                UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc")
        ));

        mockMvc.perform(post("/api/publica/recuperacoes-senha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "aplicacaoId": "eickrono-thimisu-app",
                                  "emailPrincipal": "ana@eickrono.com",
                                  "locale": "es-AR",
                                  "timeZone": "America/Argentina/Buenos_Aires",
                                  "tipoProdutoExibicao": "app",
                                  "produtoExibicao": "Thimisu",
                                  "canalExibicao": "ios",
                                  "empresaExibicao": "Eickrono",
                                  "ambienteExibicao": "HML"
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.fluxoId").value("cccccccc-cccc-cccc-cccc-cccccccccccc"))
                .andExpect(jsonPath("$.cadastroId").doesNotExist())
                .andExpect(jsonPath("$.proximoPasso").value("VALIDAR_CODIGO_RECUPERACAO"))
                .andExpect(jsonPath("$.requerNovaSenha").value(true));

        verify(recuperacaoSenhaService).iniciar(
                "eickrono-thimisu-app",
                "ana@eickrono.com",
                new ContextoSolicitacaoFluxoPublico(
                        "es-AR",
                        "America/Argentina/Buenos_Aires",
                        "app",
                        "Thimisu",
                        "ios",
                        "Eickrono",
                        "HML"
                )
        );
    }

    @Test
    void deveIniciarRecuperacaoSenhaComValidacaoDeContatosQuandoContaEstiverPendenteNoProjetoAtual() throws Exception {
        when(recuperacaoSenhaService.iniciar(
                eq("eickrono-thimisu-app"),
                eq("ana@eickrono.com"),
                eq(new ContextoSolicitacaoFluxoPublico(
                        "pt-BR",
                        "America/Sao_Paulo",
                        "app",
                        "Thimisu",
                        "ios",
                        "Eickrono",
                        "HML"
                ))
        )).thenReturn(RecuperacaoSenhaIniciada.validarContatosCadastro(
                UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd"),
                false
        ));

        mockMvc.perform(post("/api/publica/recuperacoes-senha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "aplicacaoId": "eickrono-thimisu-app",
                                  "emailPrincipal": "ana@eickrono.com",
                                  "locale": "pt-BR",
                                  "timeZone": "America/Sao_Paulo",
                                  "tipoProdutoExibicao": "app",
                                  "produtoExibicao": "Thimisu",
                                  "canalExibicao": "ios",
                                  "empresaExibicao": "Eickrono",
                                  "ambienteExibicao": "HML"
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.fluxoId").doesNotExist())
                .andExpect(jsonPath("$.cadastroId").value("dddddddd-dddd-dddd-dddd-dddddddddddd"))
                .andExpect(jsonPath("$.proximoPasso").value("VALIDAR_CONTATOS"))
                .andExpect(jsonPath("$.requerNovaSenha").value(false));
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
                true,
                true,
                true
                ,
                "LOGIN"
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
                .andExpect(jsonPath("$.telefoneConfirmado").value(true))
                .andExpect(jsonPath("$.telefoneObrigatorio").value(true))
                .andExpect(jsonPath("$.liberadoParaLogin").value(true))
                .andExpect(jsonPath("$.proximoPasso").value("LOGIN"));
    }

    @Test
    void deveConfirmarTelefoneDoCadastroPublicoNaRotaDedicada() throws Exception {
        UUID cadastroId = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
        when(cadastroContaInternaServico.confirmarTelefonePublico(
                eq(cadastroId),
                eq("654321")
        )).thenReturn(new ConfirmacaoEmailCadastroPublicoRealizada(
                cadastroId,
                "sub-ana",
                "ana@eickrono.com",
                "usuario-001",
                "LIBERADO",
                true,
                true,
                true,
                true,
                "LOGIN"
        ));

        mockMvc.perform(post("/api/publica/cadastros/{cadastroId}/confirmacoes/telefone", cadastroId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "codigo": "654321"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cadastroId").value(cadastroId.toString()))
                .andExpect(jsonPath("$.telefoneConfirmado").value(true))
                .andExpect(jsonPath("$.liberadoParaLogin").value(true))
                .andExpect(jsonPath("$.proximoPasso").value("LOGIN"));
    }

    @Test
    void deveDefinirSenhaDoCadastroPendenteNaRotaPublica() throws Exception {
        UUID cadastroId = UUID.fromString("12121212-1212-1212-1212-121212121212");

        mockMvc.perform(post("/api/publica/cadastros/{cadastroId}/senha", cadastroId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "senha": "SenhaNova@123",
                                  "confirmacaoSenha": "SenhaNova@123"
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(cadastroContaInternaServico).definirSenhaCadastroPendentePublico(
                cadastroId,
                "SenhaNova@123",
                "SenhaNova@123"
        );
    }

    @Test
    void deveConsultarStatusDoCadastroPublicoPendente() throws Exception {
        UUID cadastroId = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
        when(cadastroContaInternaServico.consultarStatusCadastroPublico(cadastroId))
                .thenReturn(new StatusCadastroPublico(
                        cadastroId,
                        "ana@eickrono.com",
                        "+5511999999999",
                        true,
                        false,
                        true,
                        false,
                        "VALIDAR_TELEFONE"
                ));

        mockMvc.perform(get("/api/publica/cadastros/{cadastroId}/status", cadastroId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cadastroId").value(cadastroId.toString()))
                .andExpect(jsonPath("$.emailConfirmado").value(true))
                .andExpect(jsonPath("$.telefoneConfirmado").value(false))
                .andExpect(jsonPath("$.telefoneObrigatorio").value(true))
                .andExpect(jsonPath("$.liberadoParaLogin").value(false))
                .andExpect(jsonPath("$.proximoPasso").value("VALIDAR_TELEFONE"));
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
    void deveConcluirVinculoSocialPendenteAoCriarSessaoComLoginLocal() throws Exception {
        UUID contextoId = UUID.fromString("66666666-6666-6666-6666-666666666666");
        ContextoSocialPendenteJdbc.ContextoSocialPendenteAtivo contextoPendente =
                new ContextoSocialPendenteJdbc.ContextoSocialPendenteAtivo(
                        contextoId,
                        7L,
                        "apple",
                        "apple-user-id",
                        "apple-user",
                        "Pessoa Apple",
                        null,
                        UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
                        "a@a.com",
                        "ENTRAR_E_VINCULAR",
                        0,
                        3
                );
        when(contextoSocialPendenteJdbc.buscarAtivo(contextoId, 7L))
                .thenReturn(Optional.of(contextoPendente));
        when(autenticacaoSessaoInternaServico.autenticar("a@a.com", "SenhaForte123"))
                .thenReturn(new com.eickrono.api.identidade.aplicacao.modelo.SessaoInternaAutenticada(
                        true,
                        "Bearer",
                        "access-token",
                        "refresh-token",
                        3600
                ));
        when(clienteContextoPessoaPerfilSistema.buscarPorEmail("a@a.com"))
                .thenReturn(Optional.of(new ContextoPessoaPerfilSistema(
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
                                  "contextoSocialPendenteId": "66666666-6666-6666-6666-666666666666",
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

        verify(vinculoSocialService).vincularContextoPendenteAposLoginLocal(
                eq("access-token"),
                eq(contextoPendente),
                eq("eickrono-thimisu-app")
        );
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
        when(clienteContextoPessoaPerfilSistema.buscarPorEmail("a@a.com"))
                .thenReturn(Optional.of(new ContextoPessoaPerfilSistema(
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
