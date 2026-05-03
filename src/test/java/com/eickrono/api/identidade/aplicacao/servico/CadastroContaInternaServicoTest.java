package com.eickrono.api.identidade.aplicacao.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eickrono.api.identidade.aplicacao.excecao.FluxoPublicoException;
import com.eickrono.api.identidade.aplicacao.modelo.CadastroInternoRealizado;
import com.eickrono.api.identidade.aplicacao.modelo.CadastroKeycloakProvisionado;
import com.eickrono.api.identidade.aplicacao.modelo.ConfirmacaoEmailCadastroInternoRealizada;
import com.eickrono.api.identidade.aplicacao.modelo.ConfirmacaoEmailCadastroPublicoRealizada;
import com.eickrono.api.identidade.aplicacao.modelo.ConviteOrganizacionalValidado;
import com.eickrono.api.identidade.aplicacao.modelo.ContextoSolicitacaoFluxoPublico;
import com.eickrono.api.identidade.aplicacao.modelo.IdentidadeFederadaKeycloak;
import com.eickrono.api.identidade.aplicacao.modelo.ProjetoFluxoPublicoResolvido;
import com.eickrono.api.identidade.aplicacao.modelo.ProvisionamentoPerfilSistemaRealizado;
import com.eickrono.api.identidade.aplicacao.modelo.VinculoSocialPendenteCadastro;
import com.eickrono.api.identidade.aplicacao.modelo.ContextoPessoaPerfilSistema;
import com.eickrono.api.identidade.dominio.modelo.CanalValidacaoTelefoneCadastro;
import com.eickrono.api.identidade.dominio.modelo.CadastroConta;
import com.eickrono.api.identidade.dominio.modelo.FormaAcesso;
import com.eickrono.api.identidade.dominio.modelo.PerfilIdentidade;
import com.eickrono.api.identidade.dominio.modelo.Pessoa;
import com.eickrono.api.identidade.dominio.modelo.ProvedorVinculoSocial;
import com.eickrono.api.identidade.dominio.modelo.StatusCadastroConta;
import com.eickrono.api.identidade.dominio.modelo.StatusConviteOrganizacional;
import com.eickrono.api.identidade.dominio.modelo.TipoPessoaCadastro;
import com.eickrono.api.identidade.dominio.modelo.TipoFormaAcesso;
import com.eickrono.api.identidade.dominio.modelo.ConviteOrganizacional;
import com.eickrono.api.identidade.dominio.modelo.VinculoOrganizacional;
import com.eickrono.api.identidade.dominio.modelo.VinculoSocial;
import com.eickrono.api.identidade.dominio.repositorio.CadastroContaRepositorio;
import com.eickrono.api.identidade.dominio.repositorio.ConviteOrganizacionalRepositorio;
import com.eickrono.api.identidade.dominio.repositorio.FormaAcessoRepositorio;
import com.eickrono.api.identidade.dominio.repositorio.PerfilIdentidadeRepositorio;
import com.eickrono.api.identidade.dominio.repositorio.PessoaRepositorio;
import com.eickrono.api.identidade.dominio.repositorio.RecuperacaoSenhaRepositorio;
import com.eickrono.api.identidade.dominio.repositorio.VinculoOrganizacionalRepositorio;
import com.eickrono.api.identidade.dominio.repositorio.VinculoSocialRepositorio;
import com.eickrono.api.identidade.infraestrutura.configuracao.DispositivoProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.http.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class CadastroContaInternaServicoTest {

    @Mock
    private CadastroContaRepositorio cadastroContaRepositorio;

    @Mock
    private FormaAcessoRepositorio formaAcessoRepositorio;

    @Mock
    private RecuperacaoSenhaRepositorio recuperacaoSenhaRepositorio;

    @Mock
    private PessoaRepositorio pessoaRepositorio;

    @Mock
    private PerfilIdentidadeRepositorio perfilIdentidadeRepositorio;

    @Mock
    private VinculoSocialRepositorio vinculoSocialRepositorio;

    @Mock
    private ConviteOrganizacionalRepositorio conviteOrganizacionalRepositorio;

    @Mock
    private VinculoOrganizacionalRepositorio vinculoOrganizacionalRepositorio;

    @Mock
    private ClienteAdministracaoCadastroKeycloak clienteAdministracaoCadastroKeycloak;

    @Mock
    private ProvisionamentoIdentidadeService provisionamentoIdentidadeService;

    @Mock
    private CanalEnvioCodigoCadastroEmail canalEnvioCodigoCadastroEmail;

    @Mock
    private CanalEnvioCodigoCadastroTelefone canalEnvioCodigoCadastroTelefone;

    @Mock
    private CanalNotificacaoTentativaCadastroEmail canalNotificacaoTentativaCadastroEmail;

    @Mock
    private AuditoriaService auditoriaService;

    @Mock
    private ClienteContextoPessoaPerfilSistema clienteContextoPessoaPerfilSistema;

    @Mock
    private ProvisionadorPerfilSistemaServico provisionadorPerfilSistemaServico;

    @Mock
    private ConsultadorDisponibilidadeUsuarioSistemaServico consultadorDisponibilidadeUsuarioSistemaServico;

    @Captor
    private ArgumentCaptor<CadastroConta> cadastroCaptor;

    @Captor
    private ArgumentCaptor<String> codigoCaptor;

    @Captor
    private ArgumentCaptor<FormaAcesso> formaAcessoCaptor;

    @Captor
    private ArgumentCaptor<VinculoSocial> vinculoSocialCaptor;

    @Captor
    private ArgumentCaptor<VinculoOrganizacional> vinculoOrganizacionalCaptor;

    @Captor
    private ArgumentCaptor<ConviteOrganizacional> conviteOrganizacionalCaptor;

    private CadastroContaInternaServico servico;
    private CadastroContaInternaServico servicoPublico;

    private CadastroContaInternaServico servico() {
        if (servico != null) {
            return servico;
        }
        DispositivoProperties dispositivoProperties = criarDispositivoProperties();
        Clock clock = Clock.fixed(Instant.parse("2026-03-19T10:00:00Z"), ZoneOffset.UTC);
        servico = new CadastroContaInternaServico(
                cadastroContaRepositorio,
                recuperacaoSenhaRepositorio,
                formaAcessoRepositorio,
                clienteAdministracaoCadastroKeycloak,
                provisionamentoIdentidadeService,
                canalEnvioCodigoCadastroEmail,
                canalEnvioCodigoCadastroTelefone,
                dispositivoProperties,
                clock
        );
        return servico;
    }

    private CadastroContaInternaServico servicoPublico() {
        if (servicoPublico != null) {
            return servicoPublico;
        }
        DispositivoProperties dispositivoProperties = criarDispositivoProperties();
        Clock clock = Clock.fixed(Instant.parse("2026-03-19T10:00:00Z"), ZoneOffset.UTC);
        servicoPublico = new CadastroContaInternaServico(
                cadastroContaRepositorio,
                recuperacaoSenhaRepositorio,
                clienteContextoPessoaPerfilSistema,
                clienteAdministracaoCadastroKeycloak,
                pessoaRepositorio,
                perfilIdentidadeRepositorio,
                formaAcessoRepositorio,
                vinculoSocialRepositorio,
                conviteOrganizacionalRepositorio,
                vinculoOrganizacionalRepositorio,
                provisionadorPerfilSistemaServico,
                consultadorDisponibilidadeUsuarioSistemaServico,
                provisionamentoIdentidadeService,
                canalEnvioCodigoCadastroEmail,
                canalEnvioCodigoCadastroTelefone,
                canalNotificacaoTentativaCadastroEmail,
                dispositivoProperties,
                clock,
                null,
                auditoriaService
        );
        return servicoPublico;
    }

    private DispositivoProperties criarDispositivoProperties() {
        DispositivoProperties dispositivoProperties = new DispositivoProperties();
        dispositivoProperties.getCodigo().setSegredoHmac("test-code-secret");
        dispositivoProperties.getCodigo().setTamanho(6);
        dispositivoProperties.getCodigo().setTentativasMaximas(5);
        dispositivoProperties.getCodigo().setReenviosMaximos(3);
        dispositivoProperties.getCodigo().setExpiracaoHoras(9);
        return dispositivoProperties;
    }

    private Pessoa pessoaCanonica(final String sub, final String email, final String nome, final Long id) {
        Pessoa pessoa = org.mockito.Mockito.mock(Pessoa.class);
        when(pessoa.getId()).thenReturn(id);
        return pessoa;
    }

    @Test
    @DisplayName("deve persistir locale, timeZone e contexto de exibicao no cadastro publico")
    void devePersistirContextoInformadoNoCadastroPublico() {
        when(cadastroContaRepositorio.findByEmailPrincipal("ana@eickrono.com")).thenReturn(Optional.empty());
        when(cadastroContaRepositorio.findByUsuarioIgnoreCaseAndSistemaSolicitanteIgnoreCase(
                eq("ana.souza"),
                anyString())).thenReturn(Optional.empty());
        when(clienteContextoPessoaPerfilSistema.buscarPorEmail("ana@eickrono.com")).thenReturn(Optional.empty());
        when(consultadorDisponibilidadeUsuarioSistemaServico.usuarioDisponivel(eq("ana.souza"), anyString())).thenReturn(true);
        when(clienteAdministracaoCadastroKeycloak.criarUsuarioPendente(
                "Ana Souza", "ana@eickrono.com", "SenhaForte@123"))
                .thenReturn(new CadastroKeycloakProvisionado("sub-ana", "ana@eickrono.com", "Ana Souza"));
        when(cadastroContaRepositorio.save(any(CadastroConta.class))).thenAnswer(invocation -> invocation.getArgument(0));

        servicoPublico().cadastrarPublico(
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
                "SenhaForte@123",
                (VinculoSocialPendenteCadastro) null,
                (ConviteOrganizacionalValidado) null,
                "app-flutter-publico",
                "127.0.0.1",
                "JUnit",
                new ContextoSolicitacaoFluxoPublico(
                        "pt-BR",
                        "America/Sao_Paulo",
                        "app",
                        "Thimisu",
                        "ios",
                        "Eickrono",
                        "HML"
                )
        );

        verify(cadastroContaRepositorio).save(cadastroCaptor.capture());
        assertThat(cadastroCaptor.getValue().getLocaleSolicitante()).isEqualTo("pt-BR");
        assertThat(cadastroCaptor.getValue().getTimeZoneSolicitante()).isEqualTo("America/Sao_Paulo");
        assertThat(cadastroCaptor.getValue().getTipoProdutoExibicao()).isEqualTo("app");
        assertThat(cadastroCaptor.getValue().getProdutoExibicao()).isEqualTo("Thimisu");
        assertThat(cadastroCaptor.getValue().getCanalExibicao()).isEqualTo("ios");
        assertThat(cadastroCaptor.getValue().getEmpresaExibicao()).isEqualTo("Eickrono");
        assertThat(cadastroCaptor.getValue().getAmbienteExibicao()).isEqualTo("HML");
    }

    @Test
    @DisplayName("deve tratar telefone como nao obrigatorio quando o projeto nao exigir validacao")
    void deveLiberarCadastroPublicoAoConfirmarEmailQuandoProjetoNaoExigeTelefone() {
        DispositivoProperties propriedadesDispositivo = new DispositivoProperties();
        propriedadesDispositivo.getCodigo().setSegredoHmac("test-code-secret");
        propriedadesDispositivo.getCodigo().setTamanho(6);
        propriedadesDispositivo.getCodigo().setTentativasMaximas(5);
        propriedadesDispositivo.getCodigo().setReenviosMaximos(3);
        propriedadesDispositivo.getCodigo().setExpiracaoHoras(9);
        Clock relogioFixo = Clock.fixed(Instant.parse("2026-03-19T10:00:00Z"), ZoneOffset.UTC);

        CadastroContaInternaServico servicoProjetoSemTelefone = new CadastroContaInternaServico(
                cadastroContaRepositorio,
                clienteContextoPessoaPerfilSistema,
                clienteAdministracaoCadastroKeycloak,
                pessoaRepositorio,
                perfilIdentidadeRepositorio,
                formaAcessoRepositorio,
                vinculoSocialRepositorio,
                conviteOrganizacionalRepositorio,
                vinculoOrganizacionalRepositorio,
                provisionadorPerfilSistemaServico,
                consultadorDisponibilidadeUsuarioSistemaServico,
                provisionamentoIdentidadeService,
                canalEnvioCodigoCadastroEmail,
                canalEnvioCodigoCadastroTelefone,
                canalNotificacaoTentativaCadastroEmail,
                propriedadesDispositivo,
                relogioFixo,
                null,
                auditoriaService,
                new ResolvedorContextoFluxoPublico(cadastroContaRepositorio, recuperacaoSenhaRepositorio),
                aplicacaoId -> new ProjetoFluxoPublicoResolvido(
                        42L,
                        aplicacaoId,
                        "Thimisu",
                        "app",
                        "Thimisu",
                        "mobile",
                        false
                )
        );

        AtomicReference<CadastroConta> salvo = new AtomicReference<>();
        when(cadastroContaRepositorio.findByEmailPrincipal("ana@eickrono.com")).thenReturn(Optional.empty());
        when(cadastroContaRepositorio.findByUsuarioIgnoreCaseAndSistemaSolicitanteIgnoreCase(
                eq("ana.souza"),
                anyString())).thenReturn(Optional.empty());
        when(clienteContextoPessoaPerfilSistema.buscarPorEmail("ana@eickrono.com")).thenReturn(Optional.empty());
        when(consultadorDisponibilidadeUsuarioSistemaServico.usuarioDisponivel(eq("ana.souza"), anyString())).thenReturn(true);
        when(clienteAdministracaoCadastroKeycloak.criarUsuarioPendente(
                "Ana Souza", "ana@eickrono.com", "SenhaForte@123"))
                .thenReturn(new CadastroKeycloakProvisionado("sub-ana", "ana@eickrono.com", "Ana Souza"));
        when(cadastroContaRepositorio.save(any(CadastroConta.class))).thenAnswer(invocation -> {
            CadastroConta cadastro = invocation.getArgument(0);
            salvo.set(cadastro);
            return cadastro;
        });
        Pessoa pessoaCanonica = pessoaCanonica("sub-ana", "ana@eickrono.com", "Ana Souza", 77L);
        when(provisionamentoIdentidadeService.confirmarEmailCadastro(
                eq("sub-ana"),
                eq("ana@eickrono.com"),
                eq("Ana Souza"),
                any()
        ))
                .thenReturn(pessoaCanonica);
        when(provisionadorPerfilSistemaServico.provisionarCadastroConfirmado(any(CadastroConta.class), eq(77L)))
                .thenReturn(new com.eickrono.api.identidade.aplicacao.modelo.ProvisionamentoPerfilSistemaRealizado(
                        "usuario-001",
                        "LIBERADO"
                ));

        CadastroInternoRealizado cadastro = servicoProjetoSemTelefone.cadastrarPublico(
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
                "SenhaForte@123",
                "eickrono-thimisu-app",
                "127.0.0.1",
                "JUnit"
        );
        verify(canalEnvioCodigoCadastroEmail).enviar(any(CadastroConta.class), codigoCaptor.capture());
        verify(canalEnvioCodigoCadastroTelefone, never()).enviar(any(CadastroConta.class), any());
        when(cadastroContaRepositorio.findByCadastroId(cadastro.cadastroId())).thenReturn(Optional.of(salvo.get()));

        ConfirmacaoEmailCadastroPublicoRealizada confirmacao = servicoProjetoSemTelefone.confirmarEmailPublico(
                cadastro.cadastroId(),
                codigoCaptor.getValue(),
                null
        );

        assertThat(salvo.get().getClienteEcossistemaId()).isEqualTo(42L);
        assertThat(salvo.get().getExigeValidacaoTelefoneSnapshot()).isFalse();
        assertThat(salvo.get().getTelefonePrincipal()).isEqualTo("+5511999999999");
        assertThat(confirmacao.emailConfirmado()).isTrue();
        assertThat(confirmacao.telefoneObrigatorio()).isFalse();
        assertThat(confirmacao.telefoneConfirmado()).isFalse();
        assertThat(confirmacao.podeAutenticar()).isTrue();
        assertThat(confirmacao.proximoPasso()).isEqualTo("LOGIN");
        assertThat(salvo.get().getPessoaIdPerfil()).isEqualTo(77L);
        verify(clienteAdministracaoCadastroKeycloak).confirmarEmailEAtivarUsuario(
                eq("sub-ana"),
                eq("Ana Souza"),
                isNull()
        );
        InOrder inOrder = org.mockito.Mockito.inOrder(
                provisionamentoIdentidadeService,
                provisionadorPerfilSistemaServico
        );
        inOrder.verify(provisionamentoIdentidadeService).confirmarEmailCadastro(
                eq("sub-ana"),
                eq("ana@eickrono.com"),
                eq("Ana Souza"),
                any()
        );
        inOrder.verify(provisionadorPerfilSistemaServico).provisionarCadastroConfirmado(salvo.get(), 77L);
    }

    @Test
    @DisplayName("deve criar cadastro interno pendente e enviar o código de confirmação por e-mail")
    void deveCriarCadastroInternoPendente() {
        when(cadastroContaRepositorio.findByEmailPrincipal("ana@eickrono.com")).thenReturn(Optional.empty());
        when(formaAcessoRepositorio.findByTipoAndProvedorAndIdentificador(
                TipoFormaAcesso.EMAIL_SENHA, "EMAIL", "ana@eickrono.com")).thenReturn(Optional.empty());
        when(clienteAdministracaoCadastroKeycloak.criarUsuarioPendente(
                "Ana Souza", "ana@eickrono.com", "SenhaForte@123"))
                .thenReturn(new CadastroKeycloakProvisionado("sub-ana", "ana@eickrono.com", "Ana Souza"));
        when(cadastroContaRepositorio.save(any(CadastroConta.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CadastroInternoRealizado resultado = servico().cadastrar(
                "Ana Souza",
                "ana@eickrono.com",
                "+5511999999999",
                CanalValidacaoTelefoneCadastro.WHATSAPP,
                "SenhaForte@123",
                "thimisu-backend",
                "127.0.0.1",
                "JUnit"
        );

        assertThat(resultado.subjectRemoto()).isEqualTo("sub-ana");
        assertThat(resultado.emailPrincipal()).isEqualTo("ana@eickrono.com");
        assertThat(resultado.verificacaoEmailObrigatoria()).isTrue();

        verify(cadastroContaRepositorio).save(cadastroCaptor.capture());
        assertThat(cadastroCaptor.getValue().getStatus().name()).isEqualTo("PENDENTE_EMAIL");
        assertThat(cadastroCaptor.getValue().getSistemaSolicitante()).isEqualTo("thimisu-backend");
        assertThat(cadastroCaptor.getValue().getTelefonePrincipal()).isEqualTo("+5511999999999");
        assertThat(cadastroCaptor.getValue().getCanalValidacaoTelefone())
                .isEqualTo(CanalValidacaoTelefoneCadastro.WHATSAPP);

        verify(provisionamentoIdentidadeService).provisionarCadastroPendente(
                "sub-ana",
                "ana@eickrono.com",
                "Ana Souza",
                cadastroCaptor.getValue().getCriadoEm()
        );
        verify(canalEnvioCodigoCadastroEmail).enviar(any(CadastroConta.class), codigoCaptor.capture());
        assertThat(codigoCaptor.getValue()).matches("\\d{6}");
        verify(canalEnvioCodigoCadastroTelefone).enviar(any(CadastroConta.class), any());
    }

    @Test
    @DisplayName("deve traduzir falha de SMTP do cadastro publico para codigo estruturado")
    void deveTraduzirFalhaSmtpDoCadastroPublico() {
        when(cadastroContaRepositorio.findByEmailPrincipal("ana@eickrono.com")).thenReturn(Optional.empty());
        when(cadastroContaRepositorio.findByUsuarioIgnoreCaseAndSistemaSolicitanteIgnoreCase(
                eq("ana.souza"),
                anyString())).thenReturn(Optional.empty());
        when(clienteContextoPessoaPerfilSistema.buscarPorEmail("ana@eickrono.com")).thenReturn(Optional.empty());
        when(consultadorDisponibilidadeUsuarioSistemaServico.usuarioDisponivel(eq("ana.souza"), anyString())).thenReturn(true);
        when(clienteAdministracaoCadastroKeycloak.criarUsuarioPendente(
                "Ana Souza", "ana@eickrono.com", "SenhaForte@123"))
                .thenReturn(new CadastroKeycloakProvisionado("sub-ana", "ana@eickrono.com", "Ana Souza"));
        when(cadastroContaRepositorio.save(any(CadastroConta.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new com.eickrono.api.identidade.aplicacao.excecao.EntregaEmailException(
                "cadastro_email_indisponivel",
                "Não foi possível enviar o código de confirmação por e-mail agora. Tente novamente.",
                "Falha ao enviar o codigo de confirmacao do cadastro por SMTP.",
                new IllegalStateException("smtp")
        )).when(canalEnvioCodigoCadastroEmail).enviar(any(CadastroConta.class), any(String.class));

        assertThatThrownBy(() -> servicoPublico().cadastrarPublico(
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
                "SenhaForte@123",
                "eickrono-thimisu-app",
                "127.0.0.1",
                "JUnit"
        ))
                .isInstanceOf(FluxoPublicoException.class)
                .satisfies(erro -> {
                    FluxoPublicoException excecao = (FluxoPublicoException) erro;
                    assertThat(excecao.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(excecao.getCodigo()).isEqualTo("cadastro_email_indisponivel");
                    assertThat(excecao.getMessage()).isEqualTo(
                            "Não foi possível enviar o código de confirmação por e-mail agora. Tente novamente."
                    );
                });

        verify(auditoriaService).registrarEvento(
                eq("CADASTRO_EMAIL_FALHA"),
                eq("sub-ana"),
                contains("codigo=cadastro_email_indisponivel")
        );
        verify(clienteAdministracaoCadastroKeycloak).removerUsuarioPendente("sub-ana");
        verify(cadastroContaRepositorio).delete(any(CadastroConta.class));
    }

    @Test
    @DisplayName("deve traduzir conflito de e-mail do Keycloak para codigo estruturado no cadastro publico")
    void deveTraduzirConflitoEmailDoKeycloakNoCadastroPublico() {
        when(cadastroContaRepositorio.findByEmailPrincipal("ana@eickrono.com")).thenReturn(Optional.empty());
        when(cadastroContaRepositorio.findByUsuarioIgnoreCaseAndSistemaSolicitanteIgnoreCase(
                eq("ana.souza"),
                anyString())).thenReturn(Optional.empty());
        when(clienteContextoPessoaPerfilSistema.buscarPorEmail("ana@eickrono.com")).thenReturn(Optional.empty());
        when(consultadorDisponibilidadeUsuarioSistemaServico.usuarioDisponivel(eq("ana.souza"), anyString())).thenReturn(true);
        when(clienteAdministracaoCadastroKeycloak.criarUsuarioPendente(
                "Ana Souza", "ana@eickrono.com", "SenhaForte@123"))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Já existe um usuário de autenticação com o e-mail informado."
                ));

        assertThatThrownBy(() -> servicoPublico().cadastrarPublico(
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
                "SenhaForte@123",
                "eickrono-thimisu-app",
                "127.0.0.1",
                "JUnit"
        ))
                .isInstanceOf(FluxoPublicoException.class)
                .satisfies(erro -> {
                    FluxoPublicoException excecao = (FluxoPublicoException) erro;
                    assertThat(excecao.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(excecao.getCodigo()).isEqualTo("email_indisponivel");
                    assertThat(excecao.getMessage()).isEqualTo(
                            "Já existe uma conta com o e-mail informado. Entre ou recupere a senha."
                    );
                });

        verify(cadastroContaRepositorio, never()).save(any(CadastroConta.class));
        verify(canalEnvioCodigoCadastroEmail, never()).enviar(any(CadastroConta.class), any(String.class));
    }

    @Test
    @DisplayName("deve confirmar o e-mail do cadastro pendente e liberar autenticação")
    void deveConfirmarEmailDoCadastro() {
        AtomicReference<CadastroConta> salvo = new AtomicReference<>();
        when(cadastroContaRepositorio.findByEmailPrincipal("ana@eickrono.com")).thenReturn(Optional.empty());
        when(formaAcessoRepositorio.findByTipoAndProvedorAndIdentificador(
                TipoFormaAcesso.EMAIL_SENHA, "EMAIL", "ana@eickrono.com")).thenReturn(Optional.empty());
        when(clienteAdministracaoCadastroKeycloak.criarUsuarioPendente(
                "Ana Souza", "ana@eickrono.com", "SenhaForte@123"))
                .thenReturn(new CadastroKeycloakProvisionado("sub-ana", "ana@eickrono.com", "Ana Souza"));
        when(cadastroContaRepositorio.save(any(CadastroConta.class))).thenAnswer(invocation -> {
            CadastroConta cadastro = invocation.getArgument(0);
            salvo.set(cadastro);
            return cadastro;
        });

        CadastroInternoRealizado cadastro = servico().cadastrar(
                "Ana Souza",
                "ana@eickrono.com",
                "+5511999999999",
                CanalValidacaoTelefoneCadastro.SMS,
                "SenhaForte@123",
                "thimisu-backend",
                "127.0.0.1",
                "JUnit"
        );
        verify(canalEnvioCodigoCadastroEmail).enviar(any(CadastroConta.class), codigoCaptor.capture());
        when(cadastroContaRepositorio.findByCadastroId(cadastro.cadastroId())).thenReturn(Optional.of(salvo.get()));

        ConfirmacaoEmailCadastroInternoRealizada confirmacao = servico().confirmarEmail(
                cadastro.cadastroId(),
                codigoCaptor.getValue()
        );

        assertThat(confirmacao.emailConfirmado()).isTrue();
        assertThat(confirmacao.podeAutenticar()).isTrue();
        assertThat(salvo.get().emailJaConfirmado()).isTrue();
        verify(clienteAdministracaoCadastroKeycloak).confirmarEmailEAtivarUsuario(
                eq("sub-ana"),
                eq("Ana Souza"),
                isNull()
        );
        verify(provisionamentoIdentidadeService).confirmarEmailCadastro(
                eq("sub-ana"),
                eq("ana@eickrono.com"),
                eq("Ana Souza"),
                any()
        );
    }

    @Test
    @DisplayName("deve reenviar o código do cadastro pendente e atualizar o controle de reenvios")
    void deveReenviarCodigoEmailDoCadastro() {
        AtomicReference<CadastroConta> salvo = new AtomicReference<>();
        when(cadastroContaRepositorio.findByEmailPrincipal("ana@eickrono.com")).thenReturn(Optional.empty());
        when(formaAcessoRepositorio.findByTipoAndProvedorAndIdentificador(
                TipoFormaAcesso.EMAIL_SENHA, "EMAIL", "ana@eickrono.com")).thenReturn(Optional.empty());
        when(clienteAdministracaoCadastroKeycloak.criarUsuarioPendente(
                "Ana Souza", "ana@eickrono.com", "SenhaForte@123"))
                .thenReturn(new CadastroKeycloakProvisionado("sub-ana", "ana@eickrono.com", "Ana Souza"));
        when(cadastroContaRepositorio.save(any(CadastroConta.class))).thenAnswer(invocation -> {
            CadastroConta cadastro = invocation.getArgument(0);
            salvo.set(cadastro);
            return cadastro;
        });

        CadastroInternoRealizado cadastro = servico().cadastrar(
                "Ana Souza",
                "ana@eickrono.com",
                "+5511999999999",
                CanalValidacaoTelefoneCadastro.SMS,
                "SenhaForte@123",
                "thimisu-backend",
                "127.0.0.1",
                "JUnit"
        );
        String hashAnterior = salvo.get().getCodigoEmailHash();
        when(cadastroContaRepositorio.findByCadastroId(cadastro.cadastroId())).thenReturn(Optional.of(salvo.get()));

        servico().reenviarCodigoEmail(cadastro.cadastroId());

        assertThat(salvo.get().getReenviosEmail()).isEqualTo(1);
        assertThat(salvo.get().getCodigoEmailHash()).isNotEqualTo(hashAnterior);
        verify(canalEnvioCodigoCadastroEmail, times(2)).enviar(any(CadastroConta.class), codigoCaptor.capture());
        verify(canalEnvioCodigoCadastroTelefone, times(1)).enviar(any(CadastroConta.class), any());
        assertThat(codigoCaptor.getAllValues()).hasSize(2);
        assertThat(codigoCaptor.getAllValues().get(0)).matches("\\d{6}");
        assertThat(codigoCaptor.getAllValues().get(1)).matches("\\d{6}");
        assertThat(codigoCaptor.getAllValues().get(1)).isNotEqualTo(codigoCaptor.getAllValues().get(0));
    }

    @Test
    @DisplayName("deve manter o cadastro pendente de telefone quando confirmar apenas o e-mail")
    void deveExigirCodigoTelefoneNaConfirmacaoDoCadastroPublico() {
        AtomicReference<CadastroConta> salvo = new AtomicReference<>();
        when(cadastroContaRepositorio.findByUsuarioIgnoreCaseAndSistemaSolicitanteIgnoreCase(
                eq("ana.souza"),
                anyString())).thenReturn(Optional.empty());
        when(consultadorDisponibilidadeUsuarioSistemaServico.usuarioDisponivel(eq("ana.souza"), anyString())).thenReturn(true);
        when(cadastroContaRepositorio.findByEmailPrincipal("ana@eickrono.com")).thenReturn(Optional.empty());
        when(clienteContextoPessoaPerfilSistema.buscarPorEmail("ana@eickrono.com")).thenReturn(Optional.empty());
        when(clienteAdministracaoCadastroKeycloak.criarUsuarioPendente(
                "Ana Souza", "ana@eickrono.com", "SenhaForte@123"))
                .thenReturn(new CadastroKeycloakProvisionado("sub-ana", "ana@eickrono.com", "Ana Souza"));
        when(cadastroContaRepositorio.save(any(CadastroConta.class))).thenAnswer(invocation -> {
            CadastroConta cadastro = invocation.getArgument(0);
            salvo.set(cadastro);
            return cadastro;
        });

        CadastroInternoRealizado cadastro = servicoPublico().cadastrarPublico(
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
                "SenhaForte@123",
                "app-flutter-publico",
                "127.0.0.1",
                "JUnit"
        );
        verify(canalEnvioCodigoCadastroEmail).enviar(any(CadastroConta.class), codigoCaptor.capture());
        when(cadastroContaRepositorio.findByCadastroId(cadastro.cadastroId())).thenReturn(Optional.of(salvo.get()));

        ConfirmacaoEmailCadastroPublicoRealizada confirmacao = servicoPublico().confirmarEmailPublico(
                cadastro.cadastroId(),
                codigoCaptor.getValue(),
                null
        );

        assertThat(confirmacao.emailConfirmado()).isTrue();
        assertThat(confirmacao.telefoneConfirmado()).isFalse();
        assertThat(confirmacao.telefoneObrigatorio()).isTrue();
        assertThat(confirmacao.podeAutenticar()).isFalse();
        assertThat(confirmacao.proximoPasso()).isEqualTo("VALIDAR_TELEFONE");
    }

    @Test
    @DisplayName("deve reenviar tambem o codigo de telefone quando o cadastro possuir telefone principal")
    void deveReenviarTambemCodigoDeTelefoneNoCadastroPublico() {
        AtomicReference<CadastroConta> salvo = new AtomicReference<>();
        when(cadastroContaRepositorio.findByUsuarioIgnoreCaseAndSistemaSolicitanteIgnoreCase(
                eq("ana.souza"),
                anyString())).thenReturn(Optional.empty());
        when(consultadorDisponibilidadeUsuarioSistemaServico.usuarioDisponivel(eq("ana.souza"), anyString())).thenReturn(true);
        when(cadastroContaRepositorio.findByEmailPrincipal("ana@eickrono.com")).thenReturn(Optional.empty());
        when(clienteContextoPessoaPerfilSistema.buscarPorEmail("ana@eickrono.com")).thenReturn(Optional.empty());
        when(clienteAdministracaoCadastroKeycloak.criarUsuarioPendente(
                "Ana Souza", "ana@eickrono.com", "SenhaForte@123"))
                .thenReturn(new CadastroKeycloakProvisionado("sub-ana", "ana@eickrono.com", "Ana Souza"));
        when(cadastroContaRepositorio.save(any(CadastroConta.class))).thenAnswer(invocation -> {
            CadastroConta cadastro = invocation.getArgument(0);
            salvo.set(cadastro);
            return cadastro;
        });

        CadastroInternoRealizado cadastro = servicoPublico().cadastrarPublico(
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
                "SenhaForte@123",
                "app-flutter-publico",
                "127.0.0.1",
                "JUnit"
        );
        when(cadastroContaRepositorio.findByCadastroId(cadastro.cadastroId())).thenReturn(Optional.of(salvo.get()));

        servicoPublico().reenviarCodigoEmail(cadastro.cadastroId());

        verify(canalEnvioCodigoCadastroEmail, times(2)).enviar(any(CadastroConta.class), any());
        verify(canalEnvioCodigoCadastroTelefone, times(1)).enviar(any(CadastroConta.class), any());
    }

    @Test
    @DisplayName("deve manter o cadastro em validacao de telefone depois de confirmar apenas o e-mail")
    void deveManterCadastroPendenteTelefoneDepoisDeConfirmarEmail() {
        AtomicReference<CadastroConta> salvo = new AtomicReference<>();
        when(cadastroContaRepositorio.findByUsuarioIgnoreCaseAndSistemaSolicitanteIgnoreCase(
                eq("ana.souza"),
                anyString())).thenReturn(Optional.empty());
        when(consultadorDisponibilidadeUsuarioSistemaServico.usuarioDisponivel(eq("ana.souza"), anyString())).thenReturn(true);
        when(cadastroContaRepositorio.findByEmailPrincipal("ana@eickrono.com")).thenReturn(Optional.empty());
        when(clienteContextoPessoaPerfilSistema.buscarPorEmail("ana@eickrono.com")).thenReturn(Optional.empty());
        when(clienteAdministracaoCadastroKeycloak.criarUsuarioPendente(
                "Ana Souza", "ana@eickrono.com", "SenhaForte@123"))
                .thenReturn(new CadastroKeycloakProvisionado("sub-ana", "ana@eickrono.com", "Ana Souza"));
        when(cadastroContaRepositorio.save(any(CadastroConta.class))).thenAnswer(invocation -> {
            CadastroConta cadastro = invocation.getArgument(0);
            salvo.set(cadastro);
            return cadastro;
        });
        CadastroInternoRealizado cadastro = servicoPublico().cadastrarPublico(
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
                "SenhaForte@123",
                "app-flutter-publico",
                "127.0.0.1",
                "JUnit"
        );
        ArgumentCaptor<String> codigoEmailCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> codigoTelefoneCaptor = ArgumentCaptor.forClass(String.class);
        verify(canalEnvioCodigoCadastroEmail).enviar(any(CadastroConta.class), codigoEmailCaptor.capture());
        verify(canalEnvioCodigoCadastroTelefone).enviar(any(CadastroConta.class), codigoTelefoneCaptor.capture());
        when(cadastroContaRepositorio.findByCadastroId(cadastro.cadastroId())).thenReturn(Optional.of(salvo.get()));

        ConfirmacaoEmailCadastroPublicoRealizada confirmacao = servicoPublico().confirmarEmailPublico(
                cadastro.cadastroId(),
                codigoEmailCaptor.getValue(),
                null
        );

        assertThat(confirmacao.emailConfirmado()).isTrue();
        assertThat(confirmacao.telefoneConfirmado()).isFalse();
        assertThat(confirmacao.telefoneObrigatorio()).isTrue();
        assertThat(confirmacao.podeAutenticar()).isFalse();
        assertThat(confirmacao.proximoPasso()).isEqualTo("VALIDAR_TELEFONE");
        assertThat(salvo.get().emailJaConfirmado()).isTrue();
        assertThat(salvo.get().telefoneJaConfirmado()).isFalse();
        verify(clienteAdministracaoCadastroKeycloak, never()).confirmarEmailEAtivarUsuario(
                anyString(),
                anyString(),
                any()
        );
    }

    @Test
    @DisplayName("deve confirmar cadastro publico por telefone depois do e-mail ja confirmado")
    void deveConfirmarCadastroPublicoComTelefoneAposEmail() {
        AtomicReference<CadastroConta> salvo = new AtomicReference<>();
        when(cadastroContaRepositorio.findByUsuarioIgnoreCaseAndSistemaSolicitanteIgnoreCase(
                eq("ana.souza"),
                anyString())).thenReturn(Optional.empty());
        when(consultadorDisponibilidadeUsuarioSistemaServico.usuarioDisponivel(eq("ana.souza"), anyString())).thenReturn(true);
        when(cadastroContaRepositorio.findByEmailPrincipal("ana@eickrono.com")).thenReturn(Optional.empty());
        when(clienteContextoPessoaPerfilSistema.buscarPorEmail("ana@eickrono.com")).thenReturn(Optional.empty());
        when(clienteAdministracaoCadastroKeycloak.criarUsuarioPendente(
                "Ana Souza", "ana@eickrono.com", "SenhaForte@123"))
                .thenReturn(new CadastroKeycloakProvisionado("sub-ana", "ana@eickrono.com", "Ana Souza"));
        when(cadastroContaRepositorio.save(any(CadastroConta.class))).thenAnswer(invocation -> {
            CadastroConta cadastro = invocation.getArgument(0);
            salvo.set(cadastro);
            return cadastro;
        });
        Pessoa pessoaConfirmada = pessoaCanonica("sub-ana", "ana@eickrono.com", "Ana Souza", 10L);
        when(provisionamentoIdentidadeService.confirmarEmailCadastro(
                eq("sub-ana"),
                eq("ana@eickrono.com"),
                eq("Ana Souza"),
                any()
        ))
                .thenReturn(pessoaConfirmada);
        when(provisionadorPerfilSistemaServico.provisionarCadastroConfirmado(any(CadastroConta.class), eq(10L)))
                .thenReturn(new com.eickrono.api.identidade.aplicacao.modelo.ProvisionamentoPerfilSistemaRealizado(
                        "usuario-001",
                        "LIBERADO"
                ));

        CadastroInternoRealizado cadastro = servicoPublico().cadastrarPublico(
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
                "SenhaForte@123",
                "app-flutter-publico",
                "127.0.0.1",
                "JUnit"
        );
        ArgumentCaptor<String> codigoEmailCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> codigoTelefoneCaptor = ArgumentCaptor.forClass(String.class);
        verify(canalEnvioCodigoCadastroEmail).enviar(any(CadastroConta.class), codigoEmailCaptor.capture());
        verify(canalEnvioCodigoCadastroTelefone).enviar(any(CadastroConta.class), codigoTelefoneCaptor.capture());
        when(cadastroContaRepositorio.findByCadastroId(cadastro.cadastroId())).thenReturn(Optional.of(salvo.get()));

        servicoPublico().confirmarEmailPublico(
                cadastro.cadastroId(),
                codigoEmailCaptor.getValue(),
                null
        );

        ConfirmacaoEmailCadastroPublicoRealizada confirmacao = servicoPublico().confirmarTelefonePublico(
                cadastro.cadastroId(),
                codigoTelefoneCaptor.getValue()
        );

        assertThat(confirmacao.emailConfirmado()).isTrue();
        assertThat(confirmacao.telefoneConfirmado()).isTrue();
        assertThat(confirmacao.podeAutenticar()).isTrue();
        assertThat(confirmacao.proximoPasso()).isEqualTo("LOGIN");
        assertThat(salvo.get().telefoneJaConfirmado()).isTrue();
        verify(clienteAdministracaoCadastroKeycloak).confirmarEmailEAtivarUsuario(
                eq("sub-ana"),
                eq("Ana Souza"),
                isNull()
        );
    }

    @Test
    @DisplayName("deve rejeitar o cadastro publico quando o usuário já estiver indisponível")
    void deveRejeitarCadastroPublicoComUsuarioIndisponivel() {
        when(cadastroContaRepositorio.findByUsuarioIgnoreCaseAndSistemaSolicitanteIgnoreCase(
                eq("ana.souza"),
                anyString())).thenReturn(Optional.empty());
        when(consultadorDisponibilidadeUsuarioSistemaServico.usuarioDisponivel(eq("ana.souza"), anyString())).thenReturn(false);

        assertThatThrownBy(() -> servicoPublico().cadastrarPublico(
                TipoPessoaCadastro.FISICA,
                "Ana Souza",
                null,
                "Ana.Souza",
                null,
                null,
                null,
                "ana+novo@eickrono.com",
                "+5511999999999",
                CanalValidacaoTelefoneCadastro.SMS,
                "SenhaForte@123",
                "app-flutter-publico",
                "127.0.0.1",
                "JUnit"
        ))
                .isInstanceOf(FluxoPublicoException.class)
                .hasMessage("Este usuário não está disponível.");

        verify(clienteAdministracaoCadastroKeycloak, never()).criarUsuarioPendente(any(), any(), any());
        verify(canalNotificacaoTentativaCadastroEmail, never()).notificar(any());
    }

    @Test
    @DisplayName("deve informar a disponibilidade pública do usuário quando ele estiver livre")
    void deveInformarDisponibilidadePublicaDoUsuario() {
        when(cadastroContaRepositorio.findByUsuarioIgnoreCaseAndSistemaSolicitanteIgnoreCase(
                eq("ana.souza"),
                anyString())).thenReturn(Optional.empty());
        when(consultadorDisponibilidadeUsuarioSistemaServico.usuarioDisponivel(eq("ana.souza"), anyString())).thenReturn(true);

        boolean disponivel = servicoPublico().identificadorPublicoSistemaDisponivelPublico(
                " Ana.Souza ",
                " App-Flutter-Publico "
        );

        assertThat(disponivel).isTrue();
        verify(consultadorDisponibilidadeUsuarioSistemaServico).usuarioDisponivel(
                "ana.souza",
                "app-flutter-publico"
        );
    }

    @Test
    @DisplayName("deve responder com erro estruturado e avisar por e-mail quando já existir conta ativa para o e-mail")
    void deveAvisarPorEmailQuandoJaExistirContaAtivaNoEndereco() {
        when(cadastroContaRepositorio.findByUsuarioIgnoreCaseAndSistemaSolicitanteIgnoreCase(
                eq("ana.souza"),
                anyString())).thenReturn(Optional.empty());
        when(consultadorDisponibilidadeUsuarioSistemaServico.usuarioDisponivel(eq("ana.souza"), anyString())).thenReturn(true);
        when(cadastroContaRepositorio.findByEmailPrincipal("ana@eickrono.com")).thenReturn(Optional.empty());
        when(clienteContextoPessoaPerfilSistema.buscarPorEmail("ana@eickrono.com"))
                .thenReturn(Optional.of(new ContextoPessoaPerfilSistema(
                        10L,
                        "sub-ana",
                        "ana@eickrono.com",
                        "Ana Souza",
                        "usuario-001",
                        "LIBERADO"
                )));

        assertThatThrownBy(() -> servicoPublico().cadastrarPublico(
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
                "SenhaForte@123",
                "app-flutter-publico",
                "127.0.0.1",
                "JUnit"
        ))
                .isInstanceOf(FluxoPublicoException.class)
                .satisfies(erro -> {
                    FluxoPublicoException excecao = (FluxoPublicoException) erro;
                    assertThat(excecao.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(excecao.getCodigo()).isEqualTo("email_indisponivel");
                    assertThat(excecao.getMessage()).isEqualTo(
                            "Já existe uma conta com o e-mail informado. Entre ou recupere a senha."
                    );
                });

        verify(canalNotificacaoTentativaCadastroEmail).notificar("ana@eickrono.com");
        verify(clienteAdministracaoCadastroKeycloak, never()).criarUsuarioPendente(any(), any(), any());
    }

    @Test
    @DisplayName("deve cancelar cadastro pendente publico removendo o usuário pendente do Keycloak")
    void deveCancelarCadastroPendentePublico() {
        AtomicReference<CadastroConta> salvo = new AtomicReference<>();
        when(cadastroContaRepositorio.findByUsuarioIgnoreCaseAndSistemaSolicitanteIgnoreCase(
                eq("ana.souza"),
                anyString())).thenReturn(Optional.empty());
        when(consultadorDisponibilidadeUsuarioSistemaServico.usuarioDisponivel(eq("ana.souza"), anyString())).thenReturn(true);
        when(cadastroContaRepositorio.findByEmailPrincipal("ana@eickrono.com")).thenReturn(Optional.empty());
        when(clienteContextoPessoaPerfilSistema.buscarPorEmail("ana@eickrono.com")).thenReturn(Optional.empty());
        when(clienteAdministracaoCadastroKeycloak.criarUsuarioPendente(
                "Ana Souza", "ana@eickrono.com", "SenhaForte@123"))
                .thenReturn(new CadastroKeycloakProvisionado("sub-ana", "ana@eickrono.com", "Ana Souza"));
        when(cadastroContaRepositorio.save(any(CadastroConta.class))).thenAnswer(invocation -> {
            CadastroConta cadastro = invocation.getArgument(0);
            salvo.set(cadastro);
            return cadastro;
        });

        CadastroInternoRealizado cadastro = servicoPublico().cadastrarPublico(
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
                "SenhaForte@123",
                "app-flutter-publico",
                "127.0.0.1",
                "JUnit"
        );
        when(cadastroContaRepositorio.findByCadastroId(cadastro.cadastroId())).thenReturn(Optional.of(salvo.get()));

        servicoPublico().cancelarCadastroPendentePublico(cadastro.cadastroId());

        verify(clienteAdministracaoCadastroKeycloak).removerUsuarioPendente("sub-ana");
        verify(cadastroContaRepositorio).delete(salvo.get());
    }

    @Test
    @DisplayName("deve persistir o vínculo social pendente no cadastro público")
    void devePersistirVinculoSocialPendenteNoCadastroPublico() {
        when(cadastroContaRepositorio.findByUsuarioIgnoreCaseAndSistemaSolicitanteIgnoreCase(
                eq("ana.souza"),
                anyString())).thenReturn(Optional.empty());
        when(consultadorDisponibilidadeUsuarioSistemaServico.usuarioDisponivel(eq("ana.souza"), anyString())).thenReturn(true);
        when(cadastroContaRepositorio.findByEmailPrincipal("ana@eickrono.com")).thenReturn(Optional.empty());
        when(clienteContextoPessoaPerfilSistema.buscarPorEmail("ana@eickrono.com")).thenReturn(Optional.empty());
        when(clienteAdministracaoCadastroKeycloak.criarUsuarioPendente(
                "Ana Souza", "ana@eickrono.com", "SenhaForte@123"))
                .thenReturn(new CadastroKeycloakProvisionado("sub-ana", "ana@eickrono.com", "Ana Souza"));
        when(cadastroContaRepositorio.save(any(CadastroConta.class))).thenAnswer(invocation -> invocation.getArgument(0));

        servicoPublico().cadastrarPublico(
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
                "SenhaForte@123",
                new VinculoSocialPendenteCadastro(
                        ProvedorVinculoSocial.GOOGLE,
                        "google-user-123",
                        "ana.google"
                ),
                "app-flutter-publico",
                "127.0.0.1",
                "JUnit"
        );

        verify(cadastroContaRepositorio).save(cadastroCaptor.capture());
        CadastroConta cadastroSalvo = cadastroCaptor.getValue();
        assertThat(cadastroSalvo.getVinculoSocialPendenteProvedor()).isEqualTo("google");
        assertThat(cadastroSalvo.getVinculoSocialPendenteIdentificadorExterno()).isEqualTo("google-user-123");
        assertThat(cadastroSalvo.getVinculoSocialPendenteNomeUsuarioExterno()).isEqualTo("ana.google");
    }

    @Test
    @DisplayName("deve persistir o contexto do convite organizacional no cadastro público")
    void devePersistirContextoDoConviteOrganizacionalNoCadastroPublico() {
        when(cadastroContaRepositorio.findByUsuarioIgnoreCaseAndSistemaSolicitanteIgnoreCase(
                eq("jane.doe"),
                anyString())).thenReturn(Optional.empty());
        when(consultadorDisponibilidadeUsuarioSistemaServico.usuarioDisponivel(eq("jane.doe"), anyString())).thenReturn(true);
        when(cadastroContaRepositorio.findByEmailPrincipal("convite@acme.test")).thenReturn(Optional.empty());
        when(clienteContextoPessoaPerfilSistema.buscarPorEmail("convite@acme.test")).thenReturn(Optional.empty());
        when(clienteAdministracaoCadastroKeycloak.criarUsuarioPendente(
                "Jane Doe", "convite@acme.test", "SenhaForte@123"))
                .thenReturn(new CadastroKeycloakProvisionado("sub-jane", "convite@acme.test", "Jane Doe"));
        when(cadastroContaRepositorio.save(any(CadastroConta.class))).thenAnswer(invocation -> invocation.getArgument(0));

        servicoPublico().cadastrarPublico(
                TipoPessoaCadastro.FISICA,
                "Jane Doe",
                null,
                "jane.doe",
                null,
                null,
                null,
                "convite@acme.test",
                "+5511999999999",
                CanalValidacaoTelefoneCadastro.SMS,
                "SenhaForte@123",
                null,
                new ConviteOrganizacionalValidado(
                        "ORG-ACME-2026",
                        "org-acme",
                        "Acme Educacao",
                        "convite@acme.test",
                        "Jane Doe",
                        true,
                        false,
                        OffsetDateTime.parse("2026-05-01T00:00:00Z")
                ),
                "app-flutter-publico",
                "127.0.0.1",
                "JUnit"
        );

        verify(cadastroContaRepositorio).save(cadastroCaptor.capture());
        CadastroConta cadastroSalvo = cadastroCaptor.getValue();
        assertThat(cadastroSalvo.getConviteOrganizacionalCodigo()).isEqualTo("ORG-ACME-2026");
        assertThat(cadastroSalvo.getConviteOrganizacionalOrganizacaoId()).isEqualTo("org-acme");
        assertThat(cadastroSalvo.getConviteOrganizacionalNomeOrganizacao()).isEqualTo("Acme Educacao");
        assertThat(cadastroSalvo.getConviteOrganizacionalEmailConvidado()).isEqualTo("convite@acme.test");
        assertThat(cadastroSalvo.isConviteOrganizacionalExigeContaSeparada()).isTrue();
    }

    @Test
    @DisplayName("deve materializar vinculo organizacional e consumir convite ao confirmar cadastro público")
    void deveMaterializarVinculoOrganizacionalEConsumirConviteAoConfirmarCadastroPublico() {
        AtomicReference<CadastroConta> salvo = new AtomicReference<>();
        when(cadastroContaRepositorio.findByUsuarioIgnoreCaseAndSistemaSolicitanteIgnoreCase(
                eq("jane.doe"),
                anyString())).thenReturn(Optional.empty());
        when(consultadorDisponibilidadeUsuarioSistemaServico.usuarioDisponivel(eq("jane.doe"), anyString())).thenReturn(true);
        when(cadastroContaRepositorio.findByEmailPrincipal("convite@acme.test")).thenReturn(Optional.empty());
        when(clienteContextoPessoaPerfilSistema.buscarPorEmail("convite@acme.test")).thenReturn(Optional.empty());
        when(clienteAdministracaoCadastroKeycloak.criarUsuarioPendente(
                "Jane Doe", "convite@acme.test", "SenhaForte@123"))
                .thenReturn(new CadastroKeycloakProvisionado("sub-jane", "convite@acme.test", "Jane Doe"));
        when(cadastroContaRepositorio.save(any(CadastroConta.class))).thenAnswer(invocation -> {
            CadastroConta cadastro = invocation.getArgument(0);
            salvo.set(cadastro);
            return cadastro;
        });
        when(vinculoOrganizacionalRepositorio.findByOrganizacaoIdAndPerfilSistemaId("org-acme", "usuario-001"))
                .thenReturn(Optional.empty());
        when(vinculoOrganizacionalRepositorio.save(any(VinculoOrganizacional.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        ConviteOrganizacional convite = new ConviteOrganizacional(
                "ORG-ACME-2026",
                "org-acme",
                "Acme Educacao",
                "convite@acme.test",
                "Jane Doe",
                true,
                OffsetDateTime.parse("2026-05-01T00:00:00Z")
        );
        when(conviteOrganizacionalRepositorio.findByCodigoIgnoreCase("ORG-ACME-2026"))
                .thenReturn(Optional.of(convite));
        when(conviteOrganizacionalRepositorio.save(any(ConviteOrganizacional.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        Pessoa pessoaConfirmada = pessoaCanonica("sub-jane", "convite@acme.test", "Jane Doe", 10L);
        when(provisionamentoIdentidadeService.confirmarEmailCadastro(
                eq("sub-jane"),
                eq("convite@acme.test"),
                eq("Jane Doe"),
                any()
        ))
                .thenReturn(pessoaConfirmada);
        when(provisionadorPerfilSistemaServico.provisionarCadastroConfirmado(any(CadastroConta.class), eq(10L)))
                .thenReturn(new ProvisionamentoPerfilSistemaRealizado(
                        "usuario-001",
                        "LIBERADO"
                ));

        CadastroInternoRealizado cadastro = servicoPublico().cadastrarPublico(
                TipoPessoaCadastro.FISICA,
                "Jane Doe",
                null,
                "jane.doe",
                null,
                null,
                null,
                "convite@acme.test",
                "+5511999999999",
                CanalValidacaoTelefoneCadastro.SMS,
                "SenhaForte@123",
                null,
                new ConviteOrganizacionalValidado(
                        "ORG-ACME-2026",
                        "org-acme",
                        "Acme Educacao",
                        "convite@acme.test",
                        "Jane Doe",
                        true,
                        false,
                        OffsetDateTime.parse("2026-05-01T00:00:00Z")
                ),
                "app-flutter-publico",
                "127.0.0.1",
                "JUnit"
        );
        ArgumentCaptor<String> codigoEmailCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> codigoTelefoneCaptor = ArgumentCaptor.forClass(String.class);
        verify(canalEnvioCodigoCadastroEmail).enviar(any(CadastroConta.class), codigoEmailCaptor.capture());
        verify(canalEnvioCodigoCadastroTelefone).enviar(any(CadastroConta.class), codigoTelefoneCaptor.capture());
        when(cadastroContaRepositorio.findByCadastroId(cadastro.cadastroId())).thenReturn(Optional.of(salvo.get()));

        servicoPublico().confirmarEmailPublico(
                cadastro.cadastroId(),
                codigoEmailCaptor.getValue(),
                codigoTelefoneCaptor.getValue()
        );

        verify(vinculoOrganizacionalRepositorio).save(vinculoOrganizacionalCaptor.capture());
        VinculoOrganizacional vinculo = vinculoOrganizacionalCaptor.getValue();
        assertThat(vinculo.getCadastroId()).isEqualTo(cadastro.cadastroId());
        assertThat(vinculo.getPessoaIdPerfil()).isEqualTo(10L);
        assertThat(vinculo.getPerfilSistemaId()).isEqualTo("usuario-001");
        assertThat(vinculo.getOrganizacaoId()).isEqualTo("org-acme");
        assertThat(vinculo.getConviteCodigo()).isEqualTo("ORG-ACME-2026");
        assertThat(vinculo.isExigeContaSeparada()).isTrue();

        verify(conviteOrganizacionalRepositorio).save(conviteOrganizacionalCaptor.capture());
        assertThat(conviteOrganizacionalCaptor.getValue().getStatus()).isEqualTo(StatusConviteOrganizacional.CONSUMIDO);
    }

    @Test
    @DisplayName("deve vincular a identidade federada ao confirmar o e-mail do cadastro público")
    void deveVincularIdentidadeFederadaAoConfirmarEmailDoCadastroPublico() {
        AtomicReference<CadastroConta> salvo = new AtomicReference<>();
        when(cadastroContaRepositorio.findByUsuarioIgnoreCaseAndSistemaSolicitanteIgnoreCase(
                eq("ana.souza"),
                anyString())).thenReturn(Optional.empty());
        when(consultadorDisponibilidadeUsuarioSistemaServico.usuarioDisponivel(eq("ana.souza"), anyString())).thenReturn(true);
        when(cadastroContaRepositorio.findByEmailPrincipal("ana@eickrono.com")).thenReturn(Optional.empty());
        when(clienteContextoPessoaPerfilSistema.buscarPorEmail("ana@eickrono.com")).thenReturn(Optional.empty());
        Pessoa pessoa = new Pessoa("sub-ana", "ana@eickrono.com", "Ana Souza", java.util.Set.of(), java.util.Set.of(),
                OffsetDateTime.parse("2026-03-19T10:00:00Z"));
        org.springframework.test.util.ReflectionTestUtils.setField(pessoa, "id", 10L);
        PerfilIdentidade perfil = new PerfilIdentidade(
                "sub-ana",
                "ana@eickrono.com",
                "Ana Souza",
                java.util.Set.of(),
                java.util.Set.of(),
                OffsetDateTime.parse("2026-03-19T10:00:00Z"));
        when(clienteAdministracaoCadastroKeycloak.criarUsuarioPendente(
                "Ana Souza", "ana@eickrono.com", "SenhaForte@123"))
                .thenReturn(new CadastroKeycloakProvisionado("sub-ana", "ana@eickrono.com", "Ana Souza"));
        when(cadastroContaRepositorio.save(any(CadastroConta.class))).thenAnswer(invocation -> {
            CadastroConta cadastro = invocation.getArgument(0);
            salvo.set(cadastro);
            return cadastro;
        });
        when(pessoaRepositorio.findById(10L)).thenReturn(Optional.of(pessoa));
        when(perfilIdentidadeRepositorio.findBySub("sub-ana")).thenReturn(Optional.of(perfil));
        when(formaAcessoRepositorio.findByTipoAndProvedorAndIdentificador(
                TipoFormaAcesso.SOCIAL,
                "GOOGLE",
                "google-user-123")).thenReturn(Optional.empty());
        when(formaAcessoRepositorio.findByPessoa(pessoa)).thenReturn(List.of());
        when(vinculoSocialRepositorio.findByPerfil(perfil)).thenReturn(List.of());
        Pessoa pessoaConfirmada = pessoaCanonica("sub-ana", "ana@eickrono.com", "Ana Souza", 10L);
        when(provisionamentoIdentidadeService.confirmarEmailCadastro(
                eq("sub-ana"),
                eq("ana@eickrono.com"),
                eq("Ana Souza"),
                any()
        ))
                .thenReturn(pessoaConfirmada);
        when(provisionadorPerfilSistemaServico.provisionarCadastroConfirmado(any(CadastroConta.class), eq(10L)))
                .thenReturn(new com.eickrono.api.identidade.aplicacao.modelo.ProvisionamentoPerfilSistemaRealizado(
                        "usuario-001",
                        "LIBERADO"
                ));

        CadastroInternoRealizado cadastro = servicoPublico().cadastrarPublico(
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
                "SenhaForte@123",
                new VinculoSocialPendenteCadastro(
                        ProvedorVinculoSocial.GOOGLE,
                        "google-user-123",
                        "ana.google"
                ),
                "app-flutter-publico",
                "127.0.0.1",
                "JUnit"
        );
        ArgumentCaptor<String> codigoEmailCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> codigoTelefoneCaptor = ArgumentCaptor.forClass(String.class);
        verify(canalEnvioCodigoCadastroEmail).enviar(any(CadastroConta.class), codigoEmailCaptor.capture());
        verify(canalEnvioCodigoCadastroTelefone).enviar(any(CadastroConta.class), codigoTelefoneCaptor.capture());
        when(cadastroContaRepositorio.findByCadastroId(cadastro.cadastroId())).thenReturn(Optional.of(salvo.get()));

        servicoPublico().confirmarEmailPublico(
                cadastro.cadastroId(),
                codigoEmailCaptor.getValue(),
                codigoTelefoneCaptor.getValue()
        );

        verify(clienteAdministracaoCadastroKeycloak).vincularIdentidadeFederada(
                "sub-ana",
                new IdentidadeFederadaKeycloak(
                        ProvedorVinculoSocial.GOOGLE,
                        "google-user-123",
                        "ana.google"
                )
        );
        verify(formaAcessoRepositorio).save(formaAcessoCaptor.capture());
        assertThat(formaAcessoCaptor.getValue().getTipo()).isEqualTo(TipoFormaAcesso.SOCIAL);
        assertThat(formaAcessoCaptor.getValue().getProvedor()).isEqualTo("GOOGLE");
        assertThat(formaAcessoCaptor.getValue().getIdentificador()).isEqualTo("google-user-123");
        verify(vinculoSocialRepositorio).save(vinculoSocialCaptor.capture());
        assertThat(vinculoSocialCaptor.getValue().getProvedor()).isEqualTo("google");
        assertThat(vinculoSocialCaptor.getValue().getIdentificador()).isEqualTo("ana.google");
        assertThat(salvo.get().getVinculoSocialPendenteProvedor()).isNull();
        assertThat(salvo.get().getVinculoSocialPendenteIdentificadorExterno()).isNull();
        assertThat(salvo.get().getVinculoSocialPendenteNomeUsuarioExterno()).isNull();
    }

    @Test
    @DisplayName("deve expurgar automaticamente cadastros pendentes com mais de 48 horas")
    void deveExpurgarCadastrosPendentesExpirados() {
        CadastroConta expirado = new CadastroConta(
                java.util.UUID.randomUUID(),
                "sub-expirado",
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
                OffsetDateTime.parse("2026-03-16T09:00:00Z"),
                OffsetDateTime.parse("2026-03-16T18:00:00Z"),
                "app-flutter-publico",
                "127.0.0.1",
                "JUnit",
                OffsetDateTime.parse("2026-03-16T09:00:00Z"),
                OffsetDateTime.parse("2026-03-16T09:00:00Z")
        );
        assertThat(expirado.getStatus()).isEqualTo(StatusCadastroConta.PENDENTE_EMAIL);
        when(cadastroContaRepositorio.findByCriadoEmBefore(
                eq(OffsetDateTime.parse("2026-03-17T10:00:00Z"))))
                .thenReturn(List.of(expirado));

        int removidos = servicoPublico().expurgarCadastrosPendentesExpirados();

        assertThat(removidos).isEqualTo(1);
        verify(clienteAdministracaoCadastroKeycloak).removerUsuarioPendente("sub-expirado");
        verify(cadastroContaRepositorio).delete(expirado);
    }
}
