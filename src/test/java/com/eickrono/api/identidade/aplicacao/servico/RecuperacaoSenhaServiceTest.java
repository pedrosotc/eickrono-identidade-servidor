package com.eickrono.api.identidade.aplicacao.servico;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Captor;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.eickrono.api.identidade.aplicacao.modelo.ConfirmacaoCodigoRecuperacaoSenhaRealizada;
import com.eickrono.api.identidade.aplicacao.modelo.ContextoSolicitacaoFluxoPublico;
import com.eickrono.api.identidade.aplicacao.modelo.ProjetoFluxoPublicoResolvido;
import com.eickrono.api.identidade.aplicacao.modelo.RecuperacaoSenhaIniciada;
import com.eickrono.api.identidade.aplicacao.modelo.UsuarioCadastroKeycloakExistente;
import com.eickrono.api.identidade.dominio.modelo.CadastroConta;
import com.eickrono.api.identidade.dominio.modelo.CanalValidacaoTelefoneCadastro;
import com.eickrono.api.identidade.dominio.modelo.MotivoRevogacaoToken;
import com.eickrono.api.identidade.dominio.modelo.RecuperacaoSenha;
import com.eickrono.api.identidade.dominio.modelo.TipoPessoaCadastro;
import com.eickrono.api.identidade.dominio.repositorio.CadastroContaRepositorio;
import com.eickrono.api.identidade.dominio.repositorio.RecuperacaoSenhaRepositorio;
import com.eickrono.api.identidade.infraestrutura.configuracao.DispositivoProperties;

@ExtendWith(MockitoExtension.class)
class RecuperacaoSenhaServiceTest {

    @Mock
    private RecuperacaoSenhaRepositorio recuperacaoSenhaRepositorio;

    @Mock
    private CadastroContaRepositorio cadastroContaRepositorio;

    @Mock
    private ClienteAdministracaoCadastroKeycloak clienteAdministracaoCadastroKeycloak;

    @Mock
    private CanalEnvioCodigoRecuperacaoSenhaEmail canalEnvioCodigoRecuperacaoSenhaEmail;
    @Mock
    private TokenDispositivoService tokenDispositivoService;
    @Mock
    private AuditoriaService auditoriaService;
    @Mock
    private ResolvedorProjetoFluxoPublico resolvedorProjetoFluxoPublico;

    @Captor
    private ArgumentCaptor<String> codigoCaptor;

    private RecuperacaoSenhaService servico;

    @BeforeEach
    void setUp() {
        DispositivoProperties dispositivoProperties = new DispositivoProperties();
        dispositivoProperties.getCodigo().setSegredoHmac("test-code-secret");
        dispositivoProperties.getCodigo().setTamanho(6);
        dispositivoProperties.getCodigo().setTentativasMaximas(5);
        dispositivoProperties.getCodigo().setReenviosMaximos(3);
        dispositivoProperties.getCodigo().setExpiracaoHoras(9);

        Clock clock = Clock.fixed(Instant.parse("2026-03-25T18:00:00Z"), ZoneOffset.UTC);
        servico = new RecuperacaoSenhaService(
                recuperacaoSenhaRepositorio,
                cadastroContaRepositorio,
                clienteAdministracaoCadastroKeycloak,
                canalEnvioCodigoRecuperacaoSenhaEmail,
                dispositivoProperties,
                clock,
                tokenDispositivoService,
                null,
                auditoriaService,
                resolvedorProjetoFluxoPublico
        );
        when(resolvedorProjetoFluxoPublico.resolverAtivo("eickrono-thimisu-app"))
                .thenReturn(new ProjetoFluxoPublicoResolvido(
                        7L,
                        "eickrono-thimisu-app",
                        "Eickrono Thimisu App",
                        "app",
                        "Thimisu",
                        "mobile",
                        false
                ));
        when(cadastroContaRepositorio.findAllByEmailPrincipal(anyString())).thenReturn(List.of());
    }

    @Test
    @DisplayName("deve persistir locale, timeZone e contexto de exibicao informados na recuperacao")
    void devePersistirContextoInformadoNaRecuperacao() {
        AtomicReference<RecuperacaoSenha> salvo = new AtomicReference<>();
        when(clienteAdministracaoCadastroKeycloak.buscarUsuarioPorEmail("ana@eickrono.com"))
                .thenReturn(Optional.of(new UsuarioCadastroKeycloakExistente(
                        "sub-ana",
                        "ana@eickrono.com",
                        true,
                        true,
                        1L
                )));
        when(recuperacaoSenhaRepositorio.save(any(RecuperacaoSenha.class))).thenAnswer(invocation -> {
            RecuperacaoSenha recuperacao = invocation.getArgument(0);
            salvo.set(recuperacao);
            return recuperacao;
        });

        servico.iniciar(
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

        assertThat(salvo.get().getLocaleSolicitante()).isEqualTo("es-AR");
        assertThat(salvo.get().getTimeZoneSolicitante()).isEqualTo("America/Argentina/Buenos_Aires");
        assertThat(salvo.get().getTipoProdutoExibicao()).isEqualTo("app");
        assertThat(salvo.get().getProdutoExibicao()).isEqualTo("Thimisu");
        assertThat(salvo.get().getCanalExibicao()).isEqualTo("ios");
        assertThat(salvo.get().getEmpresaExibicao()).isEqualTo("Eickrono");
        assertThat(salvo.get().getAmbienteExibicao()).isEqualTo("HML");
        assertThat(salvo.get().getClienteEcossistemaId()).isEqualTo(7L);
        assertThat(salvo.get().getExigeValidacaoTelefoneSnapshot()).isFalse();
    }

    @Test
    @DisplayName("deve reaproveitar o ultimo contexto conhecido quando a recuperacao nao informar locale ou timeZone")
    void deveReaproveitarUltimoContextoConhecidoQuandoRecuperacaoNaoInformarLocaleOuTimeZone() {
        AtomicReference<RecuperacaoSenha> salvo = new AtomicReference<>();
        when(clienteAdministracaoCadastroKeycloak.buscarUsuarioPorEmail("ana@eickrono.com"))
                .thenReturn(Optional.of(new UsuarioCadastroKeycloakExistente(
                        "sub-ana",
                        "ana@eickrono.com",
                        true,
                        true,
                        1L
                )));
        when(cadastroContaRepositorio.findTopByEmailPrincipalOrderByAtualizadoEmDesc("ana@eickrono.com"))
                .thenReturn(Optional.of(new CadastroConta(
                        java.util.UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
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
                        OffsetDateTime.parse("2026-03-25T18:00:00Z"),
                        new ContextoSolicitacaoFluxoPublico(
                                "pt-BR",
                                "America/Sao_Paulo",
                                "app",
                                "Thimisu",
                                "ios",
                                "Eickrono",
                                "HML"
                        )
                )));
        when(recuperacaoSenhaRepositorio.save(any(RecuperacaoSenha.class))).thenAnswer(invocation -> {
            RecuperacaoSenha recuperacao = invocation.getArgument(0);
            salvo.set(recuperacao);
            return recuperacao;
        });

        servico.iniciar(
                "eickrono-thimisu-app",
                "ana@eickrono.com",
                new ContextoSolicitacaoFluxoPublico(
                        null,
                        null,
                        null,
                        null,
                        "android",
                        null,
                        null
                )
        );

        assertThat(salvo.get().getLocaleSolicitante()).isEqualTo("pt-BR");
        assertThat(salvo.get().getTimeZoneSolicitante()).isEqualTo("America/Sao_Paulo");
        assertThat(salvo.get().getTipoProdutoExibicao()).isEqualTo("app");
        assertThat(salvo.get().getProdutoExibicao()).isEqualTo("Thimisu");
        assertThat(salvo.get().getCanalExibicao()).isEqualTo("android");
        assertThat(salvo.get().getEmpresaExibicao()).isEqualTo("Eickrono");
        assertThat(salvo.get().getAmbienteExibicao()).isEqualTo("HML");
        assertThat(salvo.get().getClienteEcossistemaId()).isEqualTo(7L);
        assertThat(salvo.get().getExigeValidacaoTelefoneSnapshot()).isFalse();
    }

    @Test
    @DisplayName("deve iniciar, confirmar e redefinir a senha quando o e-mail existir")
    void deveExecutarFluxoCompletoDeRecuperacao() {
        AtomicReference<RecuperacaoSenha> salvo = new AtomicReference<>();
        when(clienteAdministracaoCadastroKeycloak.buscarUsuarioPorEmail("ana@eickrono.com"))
                .thenReturn(Optional.of(new UsuarioCadastroKeycloakExistente(
                        "sub-ana",
                        "ana@eickrono.com",
                        true,
                        true,
                        1L
                )));
        when(recuperacaoSenhaRepositorio.save(any(RecuperacaoSenha.class))).thenAnswer(invocation -> {
            RecuperacaoSenha recuperacao = invocation.getArgument(0);
            salvo.set(recuperacao);
            return recuperacao;
        });

        RecuperacaoSenhaIniciada iniciada = servico.iniciar("eickrono-thimisu-app", "ana@eickrono.com");

        assertThat(iniciada.fluxoId()).isNotNull();
        assertThat(iniciada.cadastroId()).isNull();
        assertThat(iniciada.proximoPasso()).isEqualTo("VALIDAR_CODIGO_RECUPERACAO");
        assertThat(iniciada.requerNovaSenha()).isTrue();
        assertThat(salvo.get().getProtocoloSuporte()).startsWith("REC-");
        assertThat(salvo.get().getProtocoloSuporte()).doesNotContain(iniciada.fluxoId().toString());
        verify(canalEnvioCodigoRecuperacaoSenhaEmail).enviar(any(RecuperacaoSenha.class), codigoCaptor.capture());
        assertThat(codigoCaptor.getValue()).matches("\\d{6}");
        when(recuperacaoSenhaRepositorio.findByFluxoId(iniciada.fluxoId())).thenReturn(Optional.of(salvo.get()));

        ConfirmacaoCodigoRecuperacaoSenhaRealizada confirmacao =
                servico.confirmarCodigo(iniciada.fluxoId(), codigoCaptor.getValue());

        assertThat(confirmacao.codigoConfirmado()).isTrue();
        assertThat(confirmacao.podeDefinirSenha()).isTrue();

        servico.redefinirSenha(iniciada.fluxoId(), "SenhaNova@123", "SenhaNova@123");

        verify(clienteAdministracaoCadastroKeycloak).redefinirSenha("sub-ana", "SenhaNova@123");
        verify(clienteAdministracaoCadastroKeycloak).encerrarSessoesUsuario("sub-ana");
        verify(tokenDispositivoService).revogarTokensAtivos("sub-ana", MotivoRevogacaoToken.REDEFINICAO_SENHA);
        assertThat(salvo.get().senhaJaRedefinida()).isTrue();
    }

    @Test
    @DisplayName("deve redirecionar para validacao de contatos quando existir cadastro pendente no mesmo projeto")
    void deveRedirecionarParaValidacaoDeContatosQuandoExistirCadastroPendenteNoMesmoProjeto() {
        UUID cadastroId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        CadastroConta cadastroPendente = new CadastroConta(
                cadastroId,
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
        cadastroPendente.registrarProjetoFluxoPublico(7L, false, OffsetDateTime.parse("2026-03-25T18:00:00Z"));
        when(cadastroContaRepositorio.findAllByEmailPrincipal("ana@eickrono.com"))
                .thenReturn(List.of(cadastroPendente));

        RecuperacaoSenhaIniciada iniciada = servico.iniciar("eickrono-thimisu-app", "ana@eickrono.com");

        assertThat(iniciada.fluxoId()).isNull();
        assertThat(iniciada.cadastroId()).isEqualTo(cadastroId);
        assertThat(iniciada.proximoPasso()).isEqualTo("VALIDAR_CONTATOS");
        assertThat(iniciada.requerNovaSenha()).isFalse();
        verify(recuperacaoSenhaRepositorio, never()).save(any(RecuperacaoSenha.class));
        verify(canalEnvioCodigoRecuperacaoSenhaEmail, never()).enviar(any(), any());
        verify(clienteAdministracaoCadastroKeycloak, never()).buscarUsuarioPorEmail(anyString());
    }

    @Test
    @DisplayName("nao deve desviar a recuperacao quando o cadastro pendente existir apenas em outro projeto")
    void naoDeveDesviarARecuperacaoQuandoCadastroPendenteExistirApenasEmOutroProjeto() {
        AtomicReference<RecuperacaoSenha> salvo = new AtomicReference<>();
        CadastroConta cadastroOutroProjeto = new CadastroConta(
                UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
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
        cadastroOutroProjeto.registrarProjetoFluxoPublico(99L, false, OffsetDateTime.parse("2026-03-25T18:00:00Z"));
        when(cadastroContaRepositorio.findAllByEmailPrincipal("ana@eickrono.com"))
                .thenReturn(List.of(cadastroOutroProjeto));
        when(clienteAdministracaoCadastroKeycloak.buscarUsuarioPorEmail("ana@eickrono.com"))
                .thenReturn(Optional.of(new UsuarioCadastroKeycloakExistente(
                        "sub-ana",
                        "ana@eickrono.com",
                        true,
                        true,
                        1L
                )));
        when(recuperacaoSenhaRepositorio.save(any(RecuperacaoSenha.class))).thenAnswer(invocation -> {
            RecuperacaoSenha recuperacao = invocation.getArgument(0);
            salvo.set(recuperacao);
            return recuperacao;
        });

        RecuperacaoSenhaIniciada iniciada = servico.iniciar("eickrono-thimisu-app", "ana@eickrono.com");

        assertThat(iniciada.fluxoId()).isNotNull();
        assertThat(iniciada.cadastroId()).isNull();
        assertThat(iniciada.proximoPasso()).isEqualTo("VALIDAR_CODIGO_RECUPERACAO");
        verify(canalEnvioCodigoRecuperacaoSenhaEmail).enviar(any(RecuperacaoSenha.class), anyString());
        assertThat(salvo.get()).isNotNull();
    }

    @Test
    @DisplayName("deve traduzir falha de envio do codigo de recuperacao para erro publico estruturado")
    void deveTraduzirFalhaDeEnvioDoCodigoDeRecuperacao() {
        when(clienteAdministracaoCadastroKeycloak.buscarUsuarioPorEmail("ana@eickrono.com"))
                .thenReturn(Optional.of(new UsuarioCadastroKeycloakExistente(
                        "sub-ana",
                        "ana@eickrono.com",
                        true,
                        true,
                        1L
                )));
        when(recuperacaoSenhaRepositorio.save(any(RecuperacaoSenha.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new com.eickrono.api.identidade.aplicacao.excecao.EntregaEmailException(
                "recuperacao_email_indisponivel",
                "Não foi possível enviar o código de recuperação por e-mail agora. Tente novamente.",
                "Falha ao enviar o codigo de recuperacao de senha por SMTP.",
                new IllegalStateException("smtp")
        )).when(canalEnvioCodigoRecuperacaoSenhaEmail).enviar(any(RecuperacaoSenha.class), any(String.class));

        assertThatThrownBy(() -> servico.iniciar("eickrono-thimisu-app", "ana@eickrono.com"))
                .isInstanceOf(com.eickrono.api.identidade.aplicacao.excecao.FluxoPublicoException.class)
                .satisfies(erro -> {
                    com.eickrono.api.identidade.aplicacao.excecao.FluxoPublicoException excecao =
                            (com.eickrono.api.identidade.aplicacao.excecao.FluxoPublicoException) erro;
                    assertThat(excecao.getStatus()).isEqualTo(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(excecao.getCodigo()).isEqualTo("recuperacao_email_indisponivel");
                });

        verify(auditoriaService).registrarEvento(
                eq("RECUPERACAO_EMAIL_FALHA"),
                eq("sub-ana"),
                contains("codigo=recuperacao_email_indisponivel")
        );
    }

    @Test
    @DisplayName("deve concluir a redefinicao mesmo quando o logout administrativo falhar")
    void deveConcluirRedefinicaoMesmoQuandoEncerramentoDeSessaoFalhar() {
        AtomicReference<RecuperacaoSenha> salvo = new AtomicReference<>();
        when(clienteAdministracaoCadastroKeycloak.buscarUsuarioPorEmail("ana@eickrono.com"))
                .thenReturn(Optional.of(new UsuarioCadastroKeycloakExistente(
                        "sub-ana",
                        "ana@eickrono.com",
                        true,
                        true,
                        1L
                )));
        when(recuperacaoSenhaRepositorio.save(any(RecuperacaoSenha.class))).thenAnswer(invocation -> {
            RecuperacaoSenha recuperacao = invocation.getArgument(0);
            salvo.set(recuperacao);
            return recuperacao;
        });
        doThrow(new IllegalStateException("falha de logout"))
                .when(clienteAdministracaoCadastroKeycloak)
                .encerrarSessoesUsuario("sub-ana");

        RecuperacaoSenhaIniciada iniciada = servico.iniciar("eickrono-thimisu-app", "ana@eickrono.com");

        verify(canalEnvioCodigoRecuperacaoSenhaEmail).enviar(any(RecuperacaoSenha.class), codigoCaptor.capture());
        when(recuperacaoSenhaRepositorio.findByFluxoId(iniciada.fluxoId())).thenReturn(Optional.of(salvo.get()));
        servico.confirmarCodigo(iniciada.fluxoId(), codigoCaptor.getValue());

        servico.redefinirSenha(iniciada.fluxoId(), "SenhaNova@123", "SenhaNova@123");

        verify(clienteAdministracaoCadastroKeycloak).redefinirSenha("sub-ana", "SenhaNova@123");
        verify(clienteAdministracaoCadastroKeycloak).encerrarSessoesUsuario("sub-ana");
        verify(tokenDispositivoService).revogarTokensAtivos("sub-ana", MotivoRevogacaoToken.REDEFINICAO_SENHA);
        assertThat(salvo.get().senhaJaRedefinida()).isTrue();
    }

    @Test
    @DisplayName("deve responder de forma neutra quando o e-mail não existir")
    void deveResponderDeFormaNeutraQuandoEmailNaoExistir() {
        AtomicReference<RecuperacaoSenha> salvo = new AtomicReference<>();
        when(clienteAdministracaoCadastroKeycloak.buscarUsuarioPorEmail("desconhecido@eickrono.com"))
                .thenReturn(Optional.empty());
        when(recuperacaoSenhaRepositorio.save(any(RecuperacaoSenha.class))).thenAnswer(invocation -> {
            RecuperacaoSenha recuperacao = invocation.getArgument(0);
            salvo.set(recuperacao);
            return recuperacao;
        });

        RecuperacaoSenhaIniciada iniciada = servico.iniciar("eickrono-thimisu-app", "desconhecido@eickrono.com");

        assertThat(iniciada.fluxoId()).isNotNull();
        assertThat(iniciada.cadastroId()).isNull();
        assertThat(iniciada.proximoPasso()).isEqualTo("VALIDAR_CODIGO_RECUPERACAO");
        verify(canalEnvioCodigoRecuperacaoSenhaEmail, never()).enviar(any(), any());
        verify(tokenDispositivoService, never()).revogarTokensAtivos(any(), any());
        when(recuperacaoSenhaRepositorio.findByFluxoId(iniciada.fluxoId())).thenReturn(Optional.of(salvo.get()));

        assertThatThrownBy(() -> servico.confirmarCodigo(iniciada.fluxoId(), "123456"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("inválido");
    }
}
