package com.eickrono.api.identidade.apresentacao.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import com.eickrono.api.identidade.dominio.modelo.CanalVerificacao;
import com.eickrono.api.identidade.dominio.modelo.CanalValidacaoTelefoneCadastro;
import com.eickrono.api.identidade.dominio.modelo.CadastroConta;
import com.eickrono.api.identidade.dominio.modelo.EventoOfflineDispositivo;
import com.eickrono.api.identidade.dominio.modelo.FormaAcesso;
import com.eickrono.api.identidade.dominio.modelo.MotivoRevogacaoToken;
import com.eickrono.api.identidade.dominio.modelo.PerfilIdentidade;
import com.eickrono.api.identidade.dominio.modelo.Pessoa;
import com.eickrono.api.identidade.dominio.modelo.ProvedorVinculoSocial;
import com.eickrono.api.identidade.dominio.modelo.SexoPessoaCadastro;
import com.eickrono.api.identidade.dominio.modelo.ConviteOrganizacional;
import com.eickrono.api.identidade.dominio.modelo.StatusConviteOrganizacional;
import com.eickrono.api.identidade.dominio.modelo.TipoEventoOfflineDispositivo;
import com.eickrono.api.identidade.dominio.modelo.TipoFormaAcesso;
import com.eickrono.api.identidade.dominio.modelo.TipoPessoaCadastro;
import com.eickrono.api.identidade.dominio.modelo.VinculoOrganizacional;
import com.eickrono.api.identidade.dominio.modelo.VinculoSocial;
import com.eickrono.api.identidade.AplicacaoApiIdentidade;
import com.eickrono.api.identidade.aplicacao.modelo.CadastroInternoRealizado;
import com.eickrono.api.identidade.aplicacao.modelo.ConfirmacaoEmailCadastroPublicoRealizada;
import com.eickrono.api.identidade.aplicacao.modelo.ConviteOrganizacionalValidado;
import com.eickrono.api.identidade.aplicacao.modelo.ContextoPessoaPerfil;
import com.eickrono.api.identidade.aplicacao.modelo.IdentidadeFederadaKeycloak;
import com.eickrono.api.identidade.aplicacao.modelo.ProvisionamentoPerfilRealizado;
import com.eickrono.api.identidade.aplicacao.modelo.VinculoSocialPendenteCadastro;
import com.eickrono.api.identidade.aplicacao.servico.CadastroContaInternaServico;
import com.eickrono.api.identidade.aplicacao.servico.CanalEnvioCodigoCadastroEmail;
import com.eickrono.api.identidade.aplicacao.servico.CanalEnvioCodigoCadastroTelefone;
import com.eickrono.api.identidade.aplicacao.servico.CanalNotificacaoTentativaCadastroEmail;
import com.eickrono.api.identidade.aplicacao.servico.ClienteContextoPessoaPerfil;
import com.eickrono.api.identidade.aplicacao.servico.ProvisionadorPerfilDominioServico;
import com.eickrono.api.identidade.support.ClienteAdministracaoCadastroKeycloakStubConfiguration;
import com.eickrono.api.identidade.support.InfraestruturaTesteIdentidade;
import com.eickrono.api.identidade.dominio.modelo.StatusRegistroDispositivo;
import com.eickrono.api.identidade.dominio.modelo.TokenDispositivo;
import com.eickrono.api.identidade.dominio.repositorio.CadastroContaRepositorio;
import com.eickrono.api.identidade.dominio.repositorio.ConviteOrganizacionalRepositorio;
import com.eickrono.api.identidade.dominio.repositorio.EventoOfflineDispositivoRepositorio;
import com.eickrono.api.identidade.dominio.repositorio.FormaAcessoRepositorio;
import com.eickrono.api.identidade.dominio.repositorio.PerfilIdentidadeRepositorio;
import com.eickrono.api.identidade.dominio.repositorio.PessoaRepositorio;
import com.eickrono.api.identidade.dominio.repositorio.TokenDispositivoRepositorio;
import com.eickrono.api.identidade.dominio.repositorio.VinculoOrganizacionalRepositorio;
import com.eickrono.api.identidade.dominio.repositorio.VinculoSocialRepositorio;
import com.eickrono.api.identidade.apresentacao.dto.ConfirmacaoRegistroResponse;
import com.eickrono.api.identidade.apresentacao.dto.PoliticaOfflineDispositivoResponse;
import com.eickrono.api.identidade.apresentacao.dto.RegistroDispositivoResponse;
import com.eickrono.api.identidade.apresentacao.dto.RegistroDispositivoSessaoResponse;
import com.eickrono.api.identidade.apresentacao.dto.ValidacaoTokenDispositivoResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest(classes = {
        AplicacaoApiIdentidade.class,
        RegistroDispositivoControllerITConfiguration.class
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(RegistroDispositivoControllerITConfiguration.class)
@ContextConfiguration(initializers = InfraestruturaTesteIdentidade.Initializer.class)
class RegistroDispositivoControllerIT {

    private static final String REGISTRO_ENDPOINT = "/identidade/dispositivos/registro";
    private static final String SUB_CONTA_PROJETO_ATUAL = "sub-conta-projeto-atual";
    private static final String EMAIL_CONTA_PROJETO_ATUAL = "entrar.vincular@eickrono.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CodigoCapturador codigoCapturador;

    @Autowired
    private TokenDispositivoRepositorio tokenDispositivoRepositorio;

    @Autowired
    private EventoOfflineDispositivoRepositorio eventoOfflineDispositivoRepositorio;

    @Autowired
    private CadastroContaInternaServico cadastroContaInternaServico;

    @Autowired
    private CadastroContaRepositorio cadastroContaRepositorio;

    @Autowired
    private PessoaRepositorio pessoaRepositorio;

    @Autowired
    private FormaAcessoRepositorio formaAcessoRepositorio;

    @Autowired
    private PerfilIdentidadeRepositorio perfilIdentidadeRepositorio;

    @Autowired
    private VinculoSocialRepositorio vinculoSocialRepositorio;

    @Autowired
    private ConviteOrganizacionalRepositorio conviteOrganizacionalRepositorio;

    @Autowired
    private VinculoOrganizacionalRepositorio vinculoOrganizacionalRepositorio;

    @Autowired
    private ClienteAdministracaoCadastroKeycloakStubConfiguration clienteAdministracaoCadastroKeycloakStub;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private ClienteContextoPessoaPerfil clienteContextoPessoaPerfil;

    @MockBean
    private CanalEnvioCodigoCadastroEmail canalEnvioCodigoCadastroEmail;

    @MockBean
    private CanalEnvioCodigoCadastroTelefone canalEnvioCodigoCadastroTelefone;

    @MockBean
    private CanalNotificacaoTentativaCadastroEmail canalNotificacaoTentativaCadastroEmail;

    @MockBean
    private ProvisionadorPerfilDominioServico provisionadorPerfilDominioServico;

    private final Map<UUID, String> codigosCadastroEmail = new ConcurrentHashMap<>();
    private final Map<UUID, String> codigosCadastroTelefone = new ConcurrentHashMap<>();

    @BeforeEach
    void setUp() {
        tokenDispositivoRepositorio().deleteAll();
        eventoOfflineDispositivoRepositorio().deleteAll();
        vinculoSocialRepositorio.deleteAll();
        jdbcTemplate.update(
                """
                DELETE FROM autenticacao.contextos_sociais_pendentes
                 WHERE usuario_id_sugerido IN (
                           SELECT id
                             FROM autenticacao.usuarios
                            WHERE sub_remoto = ?
                       )
                    OR login_sugerido = ?
                    OR email_social_normalizado = ?
                """,
                SUB_CONTA_PROJETO_ATUAL,
                EMAIL_CONTA_PROJETO_ATUAL,
                EMAIL_CONTA_PROJETO_ATUAL
        );
        jdbcTemplate.update("DELETE FROM autenticacao.contextos_sociais_pendentes");
        jdbcTemplate.update(
                "DELETE FROM autenticacao.usuarios_formas_acesso WHERE identificador_externo = ?",
                EMAIL_CONTA_PROJETO_ATUAL
        );
        jdbcTemplate.update(
                "DELETE FROM autenticacao.usuarios_clientes_ecossistema WHERE usuario_id IN (SELECT id FROM autenticacao.usuarios WHERE sub_remoto = ?)",
                SUB_CONTA_PROJETO_ATUAL
        );
        jdbcTemplate.update(
                "DELETE FROM autenticacao.usuarios WHERE sub_remoto = ?",
                SUB_CONTA_PROJETO_ATUAL
        );
        formaAcessoRepositorio.deleteAll();
        perfilIdentidadeRepositorio.deleteAll();
        pessoaRepositorio.deleteAll();
        cadastroContaRepositorio.deleteAll();
        codigoCapturador().limpar();
        codigosCadastroEmail.clear();
        codigosCadastroTelefone.clear();
        clienteAdministracaoCadastroKeycloakStub.limparIdentidadesFederadas();
        org.mockito.Mockito.doAnswer(invocacao -> {
            CadastroConta cadastroConta = invocacao.getArgument(0);
            String codigo = invocacao.getArgument(1);
            codigosCadastroEmail.put(cadastroConta.getCadastroId(), codigo);
            return null;
        }).when(canalEnvioCodigoCadastroEmail).enviar(
                org.mockito.ArgumentMatchers.any(CadastroConta.class),
                org.mockito.ArgumentMatchers.anyString()
        );
        org.mockito.Mockito.doAnswer(invocacao -> {
            CadastroConta cadastroConta = invocacao.getArgument(0);
            String codigo = invocacao.getArgument(1);
            codigosCadastroTelefone.put(cadastroConta.getCadastroId(), codigo);
            return null;
        }).when(canalEnvioCodigoCadastroTelefone).enviar(
                org.mockito.ArgumentMatchers.any(CadastroConta.class),
                org.mockito.ArgumentMatchers.anyString()
        );
        org.mockito.Mockito.doNothing().when(canalNotificacaoTentativaCadastroEmail)
                .notificar(org.mockito.ArgumentMatchers.anyString());
        when(provisionadorPerfilDominioServico.usuarioDisponivel(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(true);
        ContextoPessoaPerfil contexto = new ContextoPessoaPerfil(
                123L,
                "usuario-xyz",
                "teste@eickrono.com",
                "Usuario Teste",
                null,
                "ATIVO"
        );
        when(clienteContextoPessoaPerfil.buscarPorSub("usuario-xyz"))
                .thenReturn(Optional.of(contexto));
        when(clienteContextoPessoaPerfil.buscarPorEmail("teste@eickrono.com"))
                .thenReturn(Optional.of(contexto));
    }

    private MockMvc mockMvc() {
        return Objects.requireNonNull(mockMvc);
    }

    private ObjectMapper objectMapper() {
        return Objects.requireNonNull(objectMapper);
    }

    private CodigoCapturador codigoCapturador() {
        return Objects.requireNonNull(codigoCapturador);
    }

    private TokenDispositivoRepositorio tokenDispositivoRepositorio() {
        return Objects.requireNonNull(tokenDispositivoRepositorio);
    }

    private EventoOfflineDispositivoRepositorio eventoOfflineDispositivoRepositorio() {
        return Objects.requireNonNull(eventoOfflineDispositivoRepositorio);
    }

    @Test
    void fluxoCompletoDeRegistroConfirmacaoERevogacao() throws Exception {
        RegistroDispositivoResponse registro = solicitarRegistro();
        assertThat(registro.status()).isEqualTo(StatusRegistroDispositivo.PENDENTE);
        assertThat(registro.canaisConfirmacao()).containsExactlyInAnyOrder(CanalVerificacao.EMAIL, CanalVerificacao.SMS);

        String codigoSms = codigoCapturador().obterCodigo(registro.registroId(), CanalVerificacao.SMS)
                .orElseThrow(() -> new IllegalStateException("Código SMS não capturado"));
        String codigoEmail = codigoCapturador().obterCodigo(registro.registroId(), CanalVerificacao.EMAIL)
                .orElseThrow(() -> new IllegalStateException("Código e-mail não capturado"));

        ConfirmacaoRegistroResponse confirmacao = confirmarRegistro(registro.registroId(), codigoSms, codigoEmail);

        assertThat(confirmacao.tokenDispositivo()).isNotBlank();

        // GET autenticado com token válido deve passar pelo filtro e devolver snapshot vazio.
        mockMvc().perform(get("/identidade/vinculos-organizacionais")
                        .with(Objects.requireNonNull(clienteJwt()))
                        .header("X-Device-Token", confirmacao.tokenDispositivo()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vinculos").isArray());

        // Sem o cabeçalho obrigatório deve retornar 428
        mockMvc().perform(get("/identidade/vinculos-organizacionais")
                        .with(Objects.requireNonNull(clienteJwt())))
                .andExpect(status().isPreconditionRequired());
        MvcResult semCabecalho = mockMvc().perform(get("/identidade/vinculos-organizacionais")
                        .with(Objects.requireNonNull(clienteJwt())))
                .andExpect(status().isPreconditionRequired())
                .andReturn();
        assertThat(semCabecalho.getResponse().getContentAsString()).contains("DEVICE_TOKEN_MISSING");

        // Token inválido retorna 423
        MvcResult tokenInvalido = mockMvc().perform(get("/identidade/vinculos-organizacionais")
                        .with(Objects.requireNonNull(clienteJwt()))
                        .header("X-Device-Token", "token-invalido"))
                .andExpect(status().isLocked())
                .andReturn();
        assertThat(tokenInvalido.getResponse().getContentAsString()).contains("DEVICE_TOKEN_INVALID");

        MvcResult validacao = mockMvc().perform(get("/identidade/dispositivos/token/validacao")
                        .with(Objects.requireNonNull(clienteJwt()))
                        .header("X-Device-Token", confirmacao.tokenDispositivo()))
                .andExpect(status().isOk())
                .andReturn();
        ValidacaoTokenDispositivoResponse payloadValidacao = objectMapper().readValue(
                validacao.getResponse().getContentAsByteArray(),
                ValidacaoTokenDispositivoResponse.class);
        assertThat(payloadValidacao.valido()).isTrue();
        assertThat(payloadValidacao.codigo()).isEqualTo("DEVICE_TOKEN_VALID");

        MvcResult validacaoInterna = mockMvc().perform(get("/identidade/dispositivos/token/validacao/interna")
                        .with(Objects.requireNonNull(clienteJwtInterno()))
                        .header("X-Eickrono-Internal-Secret", "local-internal-secret")
                        .header("X-Usuario-Sub", "usuario-xyz")
                        .header("X-Device-Token", confirmacao.tokenDispositivo()))
                .andExpect(status().isOk())
                .andReturn();
        ValidacaoTokenDispositivoResponse payloadInterno = objectMapper().readValue(
                validacaoInterna.getResponse().getContentAsByteArray(),
                ValidacaoTokenDispositivoResponse.class);
        assertThat(payloadInterno.valido()).isTrue();

        MvcResult politicaOffline = mockMvc().perform(get("/identidade/dispositivos/offline/politica")
                        .with(Objects.requireNonNull(clienteJwt()))
                        .header("X-Device-Token", confirmacao.tokenDispositivo()))
                .andExpect(status().isOk())
                .andReturn();
        PoliticaOfflineDispositivoResponse politica = objectMapper().readValue(
                politicaOffline.getResponse().getContentAsByteArray(),
                PoliticaOfflineDispositivoResponse.class);
        assertThat(politica.permitido()).isTrue();
        assertThat(politica.exigeReconciliacao()).isTrue();
        assertThat(politica.condicoesBloqueio()).contains("TOKEN_REVOGADO");

        String payloadEventosOffline = Objects.requireNonNull(objectMapper().writeValueAsString(Map.of(
                "eventos", java.util.List.of(Map.of(
                        "tipoEvento", TipoEventoOfflineDispositivo.MODO_OFFLINE_ATIVADO.name(),
                        "detalhes", "usuario entrou em modo offline"
                )))));
        mockMvc().perform(post("/identidade/dispositivos/offline/eventos")
                        .with(Objects.requireNonNull(clienteJwt()))
                        .header("X-Device-Token", confirmacao.tokenDispositivo())
                        .contentType(Objects.requireNonNull(jsonMediaType()))
                        .content(payloadEventosOffline))
                .andExpect(status().isAccepted());

        assertThat(eventoOfflineDispositivoRepositorio().findAll())
                .extracting(EventoOfflineDispositivo::getTipoEvento)
                .contains(TipoEventoOfflineDispositivo.MODO_OFFLINE_ATIVADO);

        // Revogação
        mockMvc().perform(post("/identidade/dispositivos/revogar")
                        .with(Objects.requireNonNull(clienteJwt()))
                        .header("X-Device-Token", confirmacao.tokenDispositivo())
                        .contentType(Objects.requireNonNull(jsonMediaType()))
                        .content("{\"motivo\":\"SOLICITACAO_CLIENTE\"}"))
                .andExpect(status().isNoContent());

        MvcResult revogado = mockMvc().perform(get("/identidade/vinculos-organizacionais")
                        .with(Objects.requireNonNull(clienteJwt()))
                        .header("X-Device-Token", confirmacao.tokenDispositivo()))
                .andExpect(status().isLocked())
                .andReturn();
        assertThat(revogado.getResponse().getContentAsString()).contains("DEVICE_TOKEN_REVOKED");

        Optional<TokenDispositivo> tokenPersistido = tokenDispositivoRepositorio().findAll().stream().findFirst();
        assertThat(tokenPersistido).isPresent();
        assertThat(tokenPersistido.get().getMotivoRevogacao()).contains(MotivoRevogacaoToken.SOLICITACAO_CLIENTE);
    }

    @Test
    void reenviarCodigoRespeitaLimites() throws Exception {
        RegistroDispositivoResponse registro = solicitarRegistro();

        mockMvc().perform(post(REGISTRO_ENDPOINT + "/" + registro.registroId() + "/reenviar")
                        .contentType(Objects.requireNonNull(jsonMediaType()))
                        .content("{}"))
                .andExpect(status().isAccepted());
    }

    @Test
    void deveEmitirTokenDeDispositivoSilenciosoParaSessaoAutenticada() throws Exception {
        MvcResult resultado = mockMvc().perform(post(REGISTRO_ENDPOINT + "/silencioso")
                        .with(Objects.requireNonNull(clienteJwt()))
                        .contentType(Objects.requireNonNull(jsonMediaType()))
                        .content("""
                                {
                                  "plataforma": "IOS",
                                  "aplicacaoId": "eickrono-thimisu-app",
                                  "identificadorInstalacao": "instalacao-social-1",
                                  "modelo": "iPhone15,2",
                                  "fabricante": "Apple",
                                  "sistemaOperacional": "iOS",
                                  "versaoSistema": "iOS 18.0",
                                  "versaoApp": "1.2.3+45"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        RegistroDispositivoSessaoResponse resposta = objectMapper().readValue(
                resultado.getResponse().getContentAsByteArray(),
                RegistroDispositivoSessaoResponse.class
        );

        assertThat(resposta.tokenDispositivo()).isNotBlank();
        assertThat(resposta.tokenDispositivoExpiraEm()).isNotNull();
    }

    @Test
    void devePermitirPrimeiroLoginSocialAposConfirmacaoDoCadastroPublicoVinculado() throws Exception {
        CadastroInternoRealizado cadastro = cadastroContaInternaServico.cadastrarPublico(
                TipoPessoaCadastro.FISICA,
                "Ana Souza",
                null,
                "ana.souza",
                SexoPessoaCadastro.FEMININO,
                "BR",
                java.time.LocalDate.parse("1990-05-10"),
                "ana@eickrono.com",
                "+5511999999999",
                CanalValidacaoTelefoneCadastro.SMS,
                "SenhaForte@123",
                new VinculoSocialPendenteCadastro(
                        ProvedorVinculoSocial.GOOGLE,
                        "google-user-123",
                        "ana.google"
                ),
                "eickrono-thimisu-app",
                "app-flutter-publico",
                "127.0.0.1",
                "JUnit"
        );
        String codigo = Optional.ofNullable(codigosCadastroEmail.get(cadastro.cadastroId()))
                .orElseThrow(() -> new IllegalStateException("Codigo de cadastro nao capturado"));
        Pessoa pessoaPendente = pessoaRepositorio.save(new Pessoa(
                cadastro.subjectRemoto(),
                cadastro.emailPrincipal(),
                "Ana Souza",
                java.util.Set.of(),
                java.util.Set.of(),
                OffsetDateTime.parse("2026-03-19T10:00:00Z")
        ));
        perfilIdentidadeRepositorio.save(new PerfilIdentidade(
                cadastro.subjectRemoto(),
                cadastro.emailPrincipal(),
                "Ana Souza",
                java.util.Set.of(),
                java.util.Set.of(),
                OffsetDateTime.parse("2026-03-19T10:00:00Z")
        ));
        when(provisionadorPerfilDominioServico.provisionarCadastroConfirmado(
                org.mockito.ArgumentMatchers.any(CadastroConta.class)))
                .thenReturn(new ProvisionamentoPerfilRealizado(
                        pessoaPendente.getId(),
                        "usuario-ana-001",
                        "ATIVO"
                ));

        ConfirmacaoEmailCadastroPublicoRealizada confirmacao =
                cadastroContaInternaServico.confirmarEmailPublico(cadastro.cadastroId(), codigo, null);

        CadastroConta cadastroPersistido = cadastroContaRepositorio.findByCadastroId(cadastro.cadastroId())
                .orElseThrow(() -> new IllegalStateException("Cadastro nao persistido"));
        Pessoa pessoa = pessoaRepositorio.findById(cadastroPersistido.getPessoaIdPerfil())
                .orElseThrow(() -> new IllegalStateException("Pessoa nao provisionada"));
        PerfilIdentidade perfil = perfilIdentidadeRepositorio.findBySub(confirmacao.subjectRemoto())
                .orElseThrow(() -> new IllegalStateException("Perfil nao provisionado"));

        assertThat(formaAcessoRepositorio.findByPessoa(pessoa))
                .extracting(FormaAcesso::getTipo, FormaAcesso::getProvedor, FormaAcesso::getIdentificador)
                .contains(org.assertj.core.groups.Tuple.tuple(
                        TipoFormaAcesso.SOCIAL,
                        "GOOGLE",
                        "google-user-123"
                ));
        assertThat(vinculoSocialRepositorio.findByPerfil(perfil))
                .extracting(VinculoSocial::getProvedor, VinculoSocial::getIdentificador)
                .contains(org.assertj.core.groups.Tuple.tuple("google", "ana.google"));

        when(clienteContextoPessoaPerfil.buscarPorSub("social-google-sub-1"))
                .thenReturn(Optional.empty());
        when(clienteContextoPessoaPerfil.buscarPorPessoaId(pessoa.getId()))
                .thenReturn(Optional.of(new ContextoPessoaPerfil(
                        pessoa.getId(),
                        pessoa.getSub(),
                        pessoa.getEmail(),
                        pessoa.getNome(),
                        confirmacao.usuarioId(),
                        confirmacao.statusUsuario()
                )));
        clienteAdministracaoCadastroKeycloakStub.definirIdentidadesFederadas(
                "social-google-sub-1",
                java.util.List.of(new IdentidadeFederadaKeycloak(
                        ProvedorVinculoSocial.GOOGLE,
                        "google-user-123",
                        "ana.google"
                ))
        );

        MvcResult resultado = mockMvc().perform(post(REGISTRO_ENDPOINT + "/silencioso")
                        .with(Objects.requireNonNull(clienteJwt("social-google-sub-1", "ana@eickrono.com", "Ana Souza")))
                        .contentType(Objects.requireNonNull(jsonMediaType()))
                        .content("""
                                {
                                  "plataforma": "IOS",
                                  "aplicacaoId": "eickrono-thimisu-app",
                                  "identificadorInstalacao": "instalacao-social-confirmado-1",
                                  "modelo": "iPhone15,2",
                                  "fabricante": "Apple",
                                  "sistemaOperacional": "iOS",
                                  "versaoSistema": "iOS 18.0",
                                  "versaoApp": "1.2.3+45"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        RegistroDispositivoSessaoResponse resposta = objectMapper().readValue(
                resultado.getResponse().getContentAsByteArray(),
                RegistroDispositivoSessaoResponse.class
        );

        assertThat(resposta.tokenDispositivo()).isNotBlank();
        assertThat(resposta.tokenDispositivoExpiraEm()).isNotNull();
    }

    @Test
    void deveConsumirConviteERegistrarVinculoOrganizacionalAoConfirmarCadastroPublico() throws Exception {
        conviteOrganizacionalRepositorio.save(new ConviteOrganizacional(
                "ORG-ACME-2026",
                "org-acme",
                "Acme Educacao",
                "convite@acme.test",
                "Jane Doe",
                true,
                OffsetDateTime.parse("2026-05-01T00:00:00Z")
        ));
        CadastroInternoRealizado cadastro = cadastroContaInternaServico.cadastrarPublico(
                TipoPessoaCadastro.FISICA,
                "Jane Doe",
                null,
                "jane.doe",
                SexoPessoaCadastro.FEMININO,
                "BR",
                java.time.LocalDate.parse("1990-05-10"),
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
                "eickrono-thimisu-app",
                "app-flutter-publico",
                "127.0.0.1",
                "JUnit"
        );
        String codigo = Optional.ofNullable(codigosCadastroEmail.get(cadastro.cadastroId()))
                .orElseThrow(() -> new IllegalStateException("Codigo de cadastro nao capturado"));
        Pessoa pessoaPendente = pessoaRepositorio.save(new Pessoa(
                cadastro.subjectRemoto(),
                cadastro.emailPrincipal(),
                "Jane Doe",
                java.util.Set.of(),
                java.util.Set.of(),
                OffsetDateTime.parse("2026-03-19T10:00:00Z")
        ));
        perfilIdentidadeRepositorio.save(new PerfilIdentidade(
                cadastro.subjectRemoto(),
                cadastro.emailPrincipal(),
                "Jane Doe",
                java.util.Set.of(),
                java.util.Set.of(),
                OffsetDateTime.parse("2026-03-19T10:00:00Z")
        ));
        when(provisionadorPerfilDominioServico.provisionarCadastroConfirmado(
                org.mockito.ArgumentMatchers.any(CadastroConta.class)))
                .thenReturn(new ProvisionamentoPerfilRealizado(
                        pessoaPendente.getId(),
                        "usuario-jane-001",
                        "ATIVO"
                ));

        cadastroContaInternaServico.confirmarEmailPublico(cadastro.cadastroId(), codigo, null);

        VinculoOrganizacional vinculo = vinculoOrganizacionalRepositorio
                .findByOrganizacaoIdAndUsuarioIdPerfil("org-acme", "usuario-jane-001")
                .orElseThrow(() -> new IllegalStateException("Vinculo organizacional nao criado"));
        assertThat(vinculo.getCadastroId()).isEqualTo(cadastro.cadastroId());
        assertThat(vinculo.getNomeOrganizacao()).isEqualTo("Acme Educacao");
        assertThat(vinculo.isExigeContaSeparada()).isTrue();

        ConviteOrganizacional conviteAtualizado = conviteOrganizacionalRepositorio.findByCodigoIgnoreCase("ORG-ACME-2026")
                .orElseThrow(() -> new IllegalStateException("Convite organizacional nao encontrado"));
        assertThat(conviteAtualizado.getStatus()).isEqualTo(StatusConviteOrganizacional.CONSUMIDO);
    }

    @Test
    void deveResponderErroEstruturadoQuandoSessaoSocialNaoPossuiContextoLocal() throws Exception {
        when(clienteContextoPessoaPerfil.buscarPorSub("usuario-xyz"))
                .thenReturn(Optional.empty());
        clienteAdministracaoCadastroKeycloakStub.definirIdentidadesFederadas(
                "usuario-xyz",
                java.util.List.of(new IdentidadeFederadaKeycloak(
                        ProvedorVinculoSocial.GOOGLE,
                        "google-user-123",
                        "ana.google"
                ))
        );

        String resposta = mockMvc().perform(post(REGISTRO_ENDPOINT + "/silencioso")
                        .with(Objects.requireNonNull(clienteJwt(
                                "usuario-xyz",
                                "social.sem.conta.local@eickrono.com",
                                "Usuario Teste"
                        )))
                        .contentType(Objects.requireNonNull(jsonMediaType()))
                        .content("""
                                {
                                  "plataforma": "IOS",
                                  "aplicacaoId": "eickrono-thimisu-app",
                                  "identificadorInstalacao": "instalacao-social-1",
                                  "modelo": "iPhone15,2",
                                  "fabricante": "Apple",
                                  "sistemaOperacional": "iOS",
                                  "versaoSistema": "iOS 18.0",
                                  "versaoApp": "1.2.3+45"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("social_sem_conta_local"))
                .andExpect(jsonPath("$.mensagem").value("Esta rede social foi autenticada com sucesso, mas ainda nao esta ligada a uma conta local. Deseja abrir o cadastro com os dados recebidos?"))
                .andExpect(jsonPath("$.detalhes.sub").value("usuario-xyz"))
                .andExpect(jsonPath("$.detalhes.acaoSugerida").value("ABRIR_CADASTRO"))
                .andExpect(jsonPath("$.detalhes.provedor").value("google"))
                .andExpect(jsonPath("$.detalhes.identificadorExterno").value("google-user-123"))
                .andExpect(jsonPath("$.detalhes.nomeUsuarioExterno").value("ana.google"))
                .andExpect(jsonPath("$.detalhes.contextoSocialPendenteId").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String contextoId = objectMapper.readTree(resposta)
                .path("detalhes")
                .path("contextoSocialPendenteId")
                .asText();
        Map<String, Object> contextoPersistido = jdbcTemplate.queryForMap("""
                SELECT modo_pendente,
                       usuario_id_sugerido,
                       email_social_normalizado
                  FROM autenticacao.contextos_sociais_pendentes
                 WHERE id = ?::uuid
                """, contextoId);
        assertThat(contextoPersistido.get("modo_pendente")).isEqualTo("ABRIR_CADASTRO");
        assertThat(contextoPersistido.get("usuario_id_sugerido")).isNull();
        assertThat(contextoPersistido.get("email_social_normalizado")).isEqualTo("social.sem.conta.local@eickrono.com");
    }

    @Test
    void deveResponderContaNaoLiberadaQuandoSessaoSocialEncontrarContaAindaPendente() throws Exception {
        CadastroConta cadastro = cadastroContaRepositorio.save(new CadastroConta(
                UUID.randomUUID(),
                "usuario-xyz",
                TipoPessoaCadastro.FISICA,
                "Usuario Teste",
                null,
                "usuario.teste",
                null,
                null,
                null,
                "teste@eickrono.com",
                null,
                null,
                "hash-cadastro",
                OffsetDateTime.parse("2026-04-01T10:00:00Z"),
                OffsetDateTime.parse("2026-04-01T16:00:00Z"),
                "ios",
                "127.0.0.1",
                "JUnit",
                OffsetDateTime.parse("2026-04-01T10:00:00Z"),
                OffsetDateTime.parse("2026-04-01T10:00:00Z")
        ));
        when(clienteContextoPessoaPerfil.buscarPorSub("usuario-xyz"))
                .thenReturn(Optional.of(new ContextoPessoaPerfil(
                        123L,
                        "usuario-xyz",
                        "teste@eickrono.com",
                        "Usuario Teste",
                        "usuario-001",
                        "PENDENTE_EMAIL"
                )));

        mockMvc().perform(post(REGISTRO_ENDPOINT + "/silencioso")
                        .with(Objects.requireNonNull(clienteJwt()))
                        .contentType(Objects.requireNonNull(jsonMediaType()))
                        .content("""
                                {
                                  "plataforma": "IOS",
                                  "aplicacaoId": "eickrono-thimisu-app",
                                  "identificadorInstalacao": "instalacao-social-bloqueado-1",
                                  "modelo": "iPhone15,2",
                                  "fabricante": "Apple",
                                  "sistemaOperacional": "iOS",
                                  "versaoSistema": "iOS 18.0",
                                  "versaoApp": "1.2.3+45"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("conta_nao_liberada"))
                .andExpect(jsonPath("$.mensagem").value("A conta ainda não está liberada para utilizar o aplicativo."))
                .andExpect(jsonPath("$.detalhes.sub").value("usuario-xyz"))
                .andExpect(jsonPath("$.detalhes.statusUsuario").value("PENDENTE_EMAIL"))
                .andExpect(jsonPath("$.detalhes.email").value("teste@eickrono.com"))
                .andExpect(jsonPath("$.detalhes.cadastroId").value(cadastro.getCadastroId().toString()));
    }

    @Test
    void deveSugerirEntrarEVincularQuandoSessaoSocialEncontrarContaLocalNoProjetoAtualPorEmail() throws Exception {
        when(clienteContextoPessoaPerfil.buscarPorSub("usuario-sem-conta-local"))
                .thenReturn(Optional.empty());
        vincularContaLocalAoProjetoAtual(SUB_CONTA_PROJETO_ATUAL, EMAIL_CONTA_PROJETO_ATUAL);
        clienteAdministracaoCadastroKeycloakStub.definirIdentidadesFederadas(
                "usuario-sem-conta-local",
                java.util.List.of(new IdentidadeFederadaKeycloak(
                        ProvedorVinculoSocial.GOOGLE,
                        "google-user-123",
                        "ana.google"
                ))
        );

        String resposta = mockMvc().perform(post(REGISTRO_ENDPOINT + "/silencioso")
                        .with(Objects.requireNonNull(clienteJwt(
                                "usuario-sem-conta-local",
                                EMAIL_CONTA_PROJETO_ATUAL,
                                "Usuario Teste"
                        )))
                        .contentType(Objects.requireNonNull(jsonMediaType()))
                        .content("""
                                {
                                  "plataforma": "IOS",
                                  "aplicacaoId": "eickrono-thimisu-app",
                                  "identificadorInstalacao": "instalacao-social-1",
                                  "modelo": "iPhone15,2",
                                  "fabricante": "Apple",
                                  "sistemaOperacional": "iOS",
                                  "versaoSistema": "iOS 18.0",
                                  "versaoApp": "1.2.3+45"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("social_sem_conta_local"))
                .andExpect(jsonPath("$.mensagem").value("Ja existe uma conta neste projeto com o mesmo e-mail desta rede social. Deseja entrar e vincular agora?"))
                .andExpect(jsonPath("$.detalhes.sub").value("usuario-sem-conta-local"))
                .andExpect(jsonPath("$.detalhes.acaoSugerida").value("ENTRAR_E_VINCULAR"))
                .andExpect(jsonPath("$.detalhes.email").value(EMAIL_CONTA_PROJETO_ATUAL))
                .andExpect(jsonPath("$.detalhes.loginSugerido").value(EMAIL_CONTA_PROJETO_ATUAL))
                .andExpect(jsonPath("$.detalhes.emailContaExistente").value(EMAIL_CONTA_PROJETO_ATUAL))
                .andExpect(jsonPath("$.detalhes.provedor").value("google"))
                .andExpect(jsonPath("$.detalhes.identificadorExterno").value("google-user-123"))
                .andExpect(jsonPath("$.detalhes.nomeUsuarioExterno").value("ana.google"))
                .andExpect(jsonPath("$.detalhes.contextoSocialPendenteId").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String contextoId = objectMapper.readTree(resposta)
                .path("detalhes")
                .path("contextoSocialPendenteId")
                .asText();
        Map<String, Object> contextoPersistido = jdbcTemplate.queryForMap("""
                SELECT modo_pendente,
                       usuario_id_sugerido,
                       login_sugerido,
                       email_social_normalizado
                  FROM autenticacao.contextos_sociais_pendentes
                 WHERE id = ?::uuid
                """, contextoId);
        assertThat(contextoPersistido.get("modo_pendente")).isEqualTo("ENTRAR_E_VINCULAR");
        assertThat(contextoPersistido.get("usuario_id_sugerido")).isNotNull();
        assertThat(contextoPersistido.get("login_sugerido")).isEqualTo(EMAIL_CONTA_PROJETO_ATUAL);
        assertThat(contextoPersistido.get("email_social_normalizado")).isEqualTo(EMAIL_CONTA_PROJETO_ATUAL);
    }

    @Test
    void deveResponderContaDesabilitadaQuandoSessaoSocialEncontrarContaBloqueada() throws Exception {
        when(clienteContextoPessoaPerfil.buscarPorSub("usuario-xyz"))
                .thenReturn(Optional.of(new ContextoPessoaPerfil(
                        123L,
                        "usuario-xyz",
                        "teste@eickrono.com",
                        "Usuario Teste",
                        "usuario-001",
                        "DESABILITADO"
                )));

        mockMvc().perform(post(REGISTRO_ENDPOINT + "/silencioso")
                        .with(Objects.requireNonNull(clienteJwt()))
                        .contentType(Objects.requireNonNull(jsonMediaType()))
                        .content("""
                                {
                                  "plataforma": "IOS",
                                  "aplicacaoId": "eickrono-thimisu-app",
                                  "identificadorInstalacao": "instalacao-social-desabilitado-1",
                                  "modelo": "iPhone15,2",
                                  "fabricante": "Apple",
                                  "sistemaOperacional": "iOS",
                                  "versaoSistema": "iOS 18.0",
                                  "versaoApp": "1.2.3+45"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("conta_desabilitada"))
                .andExpect(jsonPath("$.mensagem").value("A conta está desabilitada para autenticação."))
                .andExpect(jsonPath("$.detalhes.sub").value("usuario-xyz"))
                .andExpect(jsonPath("$.detalhes.statusUsuario").value("DESABILITADO"))
                .andExpect(jsonPath("$.detalhes.email").value("teste@eickrono.com"))
                .andExpect(jsonPath("$.detalhes.motivoBloqueio").value("STATUS_DESABILITADO"))
                .andExpect(jsonPath("$.detalhes.acaoSugerida").value("SUPORTE"));
    }

    void deveBloquearSessaoSocialQuandoContaLocalEncontradaAindaNaoPossuirEmailVerificado() throws Exception {
        Pessoa pessoa = pessoaRepositorio.save(new Pessoa(
                "sub-conta-local",
                "face@eickrono.com",
                "Pessoa Face",
                java.util.Set.of(),
                java.util.Set.of(),
                OffsetDateTime.parse("2026-04-01T10:00:00Z")
        ));
        formaAcessoRepositorio.save(new FormaAcesso(
                pessoa,
                TipoFormaAcesso.EMAIL_SENHA,
                "EMAIL",
                "face@eickrono.com",
                true,
                OffsetDateTime.parse("2026-04-01T10:00:00Z"),
                null
        ));
        formaAcessoRepositorio.save(new FormaAcesso(
                pessoa,
                TipoFormaAcesso.SOCIAL,
                "FACEBOOK",
                "facebook-user-123",
                false,
                OffsetDateTime.parse("2026-04-01T10:00:00Z"),
                OffsetDateTime.parse("2026-04-01T10:00:00Z")
        ));
        CadastroConta cadastro = cadastroContaRepositorio.save(new CadastroConta(
                UUID.randomUUID(),
                "sub-conta-local",
                TipoPessoaCadastro.FISICA,
                "Pessoa Face",
                null,
                "pessoa.face",
                null,
                null,
                null,
                "face@eickrono.com",
                null,
                null,
                "hash-cadastro",
                OffsetDateTime.parse("2026-04-01T10:00:00Z"),
                OffsetDateTime.parse("2026-04-01T16:00:00Z"),
                "ios",
                "127.0.0.1",
                "JUnit",
                OffsetDateTime.parse("2026-04-01T10:00:00Z"),
                OffsetDateTime.parse("2026-04-01T10:00:00Z")
        ));

        when(clienteContextoPessoaPerfil.buscarPorSub("usuario-social-facebook"))
                .thenReturn(Optional.empty());
        when(clienteContextoPessoaPerfil.buscarPorPessoaId(pessoa.getId()))
                .thenReturn(Optional.of(new ContextoPessoaPerfil(
                        pessoa.getId(),
                        pessoa.getSub(),
                        pessoa.getEmail(),
                        pessoa.getNome(),
                        "usuario-001",
                        "ATIVO"
                )));
        clienteAdministracaoCadastroKeycloakStub.definirIdentidadesFederadas(
                "usuario-social-facebook",
                java.util.List.of(new IdentidadeFederadaKeycloak(
                        ProvedorVinculoSocial.FACEBOOK,
                        "facebook-user-123",
                        "face.user"
                ))
        );

        mockMvc().perform(post(REGISTRO_ENDPOINT + "/silencioso")
                        .with(Objects.requireNonNull(clienteJwt(
                                "usuario-social-facebook",
                                "face@facebook.com",
                                "Pessoa Face"
                        )))
                        .contentType(Objects.requireNonNull(jsonMediaType()))
                        .content("""
                                {
                                  "plataforma": "IOS",
                                  "aplicacaoId": "eickrono-thimisu-app",
                                  "identificadorInstalacao": "instalacao-social-facebook-1",
                                  "modelo": "iPhone15,2",
                                  "fabricante": "Apple",
                                  "sistemaOperacional": "iOS",
                                  "versaoSistema": "iOS 18.0",
                                  "versaoApp": "1.2.3+45"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("conta_nao_liberada"))
                .andExpect(jsonPath("$.detalhes.sub").value("usuario-social-facebook"))
                .andExpect(jsonPath("$.detalhes.statusUsuario").value("ATIVO"))
                .andExpect(jsonPath("$.detalhes.email").value("face@eickrono.com"))
                .andExpect(jsonPath("$.detalhes.emailVerificado").value(false))
                .andExpect(jsonPath("$.detalhes.motivoBloqueio").value("EMAIL_NAO_CONFIRMADO"))
                .andExpect(jsonPath("$.detalhes.cadastroId").value(cadastro.getCadastroId().toString()));
    }

    @Test
    void deveReconhecerContaJaVinculadaPorFormaAcessoSocialQuandoSubAindaNaoPossuiContextoLocal() throws Exception {
        when(clienteContextoPessoaPerfil.buscarPorSub("usuario-xyz"))
                .thenReturn(Optional.empty());
        Pessoa pessoa = pessoaRepositorio.save(new Pessoa(
                "sub-conta-local",
                "teste@eickrono.com",
                "Usuario Teste",
                java.util.Set.of(),
                java.util.Set.of(),
                OffsetDateTime.parse("2026-03-19T10:00:00Z")
        ));
        formaAcessoRepositorio.save(new FormaAcesso(
                pessoa,
                TipoFormaAcesso.EMAIL_SENHA,
                "EMAIL",
                "teste@eickrono.com",
                true,
                OffsetDateTime.parse("2026-03-19T10:00:00Z"),
                OffsetDateTime.parse("2026-03-19T10:00:00Z")
        ));
        formaAcessoRepositorio.save(new FormaAcesso(
                pessoa,
                TipoFormaAcesso.SOCIAL,
                "GOOGLE",
                "google-user-123",
                false,
                OffsetDateTime.parse("2026-03-19T10:05:00Z"),
                OffsetDateTime.parse("2026-03-19T10:05:00Z")
        ));
        when(clienteContextoPessoaPerfil.buscarPorPessoaId(pessoa.getId()))
                .thenReturn(Optional.of(new ContextoPessoaPerfil(
                        pessoa.getId(),
                        pessoa.getSub(),
                        pessoa.getEmail(),
                        pessoa.getNome(),
                        "usuario-001",
                        "ATIVO"
                )));
        clienteAdministracaoCadastroKeycloakStub.definirIdentidadesFederadas(
                "usuario-xyz",
                java.util.List.of(new IdentidadeFederadaKeycloak(
                        ProvedorVinculoSocial.GOOGLE,
                        "google-user-123",
                        "ana.google"
                ))
        );

        MvcResult resultado = mockMvc().perform(post(REGISTRO_ENDPOINT + "/silencioso")
                        .with(Objects.requireNonNull(clienteJwt()))
                        .contentType(Objects.requireNonNull(jsonMediaType()))
                        .content("""
                                {
                                  "plataforma": "IOS",
                                  "aplicacaoId": "eickrono-thimisu-app",
                                  "identificadorInstalacao": "instalacao-social-2",
                                  "modelo": "iPhone15,2",
                                  "fabricante": "Apple",
                                  "sistemaOperacional": "iOS",
                                  "versaoSistema": "iOS 18.0",
                                  "versaoApp": "1.2.3+45"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        RegistroDispositivoSessaoResponse resposta = objectMapper().readValue(
                resultado.getResponse().getContentAsByteArray(),
                RegistroDispositivoSessaoResponse.class
        );

        assertThat(resposta.tokenDispositivo()).isNotBlank();
        assertThat(resposta.tokenDispositivoExpiraEm()).isNotNull();
    }

    private RegistroDispositivoResponse solicitarRegistro() throws Exception {
        String payload = """
                {
                  "email": "teste@eickrono.com",
                  "telefone": "+55-11-99999-0000",
                  "fingerprint": "ios|iphone14,3|device",
                  "plataforma": "iOS",
                  "versaoAplicativo": "1.0.0"
                }
                """;

        MvcResult resultado = mockMvc().perform(post(REGISTRO_ENDPOINT)
                        .with(Objects.requireNonNull(clienteJwt()))
                        .contentType(Objects.requireNonNull(jsonMediaType()))
                        .content(payload))
                .andExpect(status().isAccepted())
                .andReturn();

        return objectMapper().readValue(resultado.getResponse().getContentAsByteArray(), RegistroDispositivoResponse.class);
    }

    private ConfirmacaoRegistroResponse confirmarRegistro(UUID registroId, String codigoSms, String codigoEmail) throws Exception {
        String payload = Objects.requireNonNull(objectMapper().writeValueAsString(Map.of(
                "codigoSms", codigoSms,
                "codigoEmail", codigoEmail
        )));

        MvcResult resultado = mockMvc().perform(post(REGISTRO_ENDPOINT + "/" + registroId + "/confirmacao")
                        .with(Objects.requireNonNull(clienteJwt()))
                        .contentType(Objects.requireNonNull(jsonMediaType()))
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper().readValue(resultado.getResponse().getContentAsByteArray(), ConfirmacaoRegistroResponse.class);
    }

    private RequestPostProcessor clienteJwt() {
        return clienteJwt("usuario-xyz", "teste@eickrono.com", "Usuario Teste");
    }

    private RequestPostProcessor clienteJwt(final String subject, final String email, final String nome) {
        return Objects.requireNonNull(jwt().jwt(builder -> builder
                        .subject(subject)
                        .claim("email", email)
                        .claim("name", nome)
                        .claim("preferred_username", "usuario.teste")
                        .claim("scope", "identidade:ler"))
                .authorities(
                        new SimpleGrantedAuthority("ROLE_cliente"),
                        new SimpleGrantedAuthority("SCOPE_identidade:ler")));
    }

    private RequestPostProcessor clienteJwtInterno() {
        return Objects.requireNonNull(jwt().jwt(builder -> builder
                        .subject("service-account-servidor-autorizacao")
                        .claim("azp", "servidor-autorizacao")
                        .claim("preferred_username", "service-account-servidor-autorizacao")));
    }

    private MediaType jsonMediaType() {
        return Objects.requireNonNull(MediaType.APPLICATION_JSON);
    }

    private void vincularContaLocalAoProjetoAtual(final String subRemoto, final String email) {
        final Long clienteEcossistemaId = buscarClienteEcossistemaId("eickrono-thimisu-app");
        final UUID usuarioId = UUID.randomUUID();
        final UUID pessoaId = UUID.randomUUID();
        final OffsetDateTime agora = OffsetDateTime.parse("2026-03-19T10:00:00Z");
        final String emailNormalizado = email.trim().toLowerCase();

        jdbcTemplate.update("""
                INSERT INTO autenticacao.usuarios (
                    id,
                    pessoa_id,
                    sub_remoto,
                    status_global,
                    credencial_local_habilitada,
                    ultimo_login_em,
                    criado_em,
                    atualizado_em
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                usuarioId,
                pessoaId,
                subRemoto,
                "ATIVO",
                true,
                null,
                agora,
                agora);
        jdbcTemplate.update("""
                INSERT INTO autenticacao.usuarios_clientes_ecossistema (
                    id,
                    usuario_id,
                    cliente_ecossistema_id,
                    status_vinculo,
                    identificador_publico_cliente,
                    ultimo_acesso_em,
                    vinculado_em,
                    atualizado_em,
                    revogado_em,
                    motivo_revogacao
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                UUID.randomUUID(),
                usuarioId,
                clienteEcossistemaId,
                "ATIVO",
                null,
                agora,
                agora,
                agora,
                null,
                null);
        jdbcTemplate.update("""
                INSERT INTO autenticacao.usuarios_formas_acesso (
                    id,
                    usuario_id,
                    email_id,
                    tipo,
                    provedor,
                    identificador_externo,
                    principal,
                    verificado_em,
                    vinculado_em,
                    desvinculado_em
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                UUID.randomUUID(),
                usuarioId,
                UUID.randomUUID(),
                "EMAIL_SENHA",
                "EMAIL",
                emailNormalizado,
                true,
                agora,
                agora,
                null);
    }

    private Long buscarClienteEcossistemaId(final String codigoProjeto) {
        return Objects.requireNonNull(jdbcTemplate.queryForObject(
                "SELECT id FROM catalogo.clientes_ecossistema WHERE codigo = ?",
                Long.class,
                codigoProjeto
        ));
    }

    static class CodigoCapturador {
        private final Map<CanalVerificacao, Map<UUID, String>> mapa = new ConcurrentHashMap<>();

        void registrar(UUID registroId, CanalVerificacao canal, String codigo) {
            mapa.computeIfAbsent(canal, c -> new ConcurrentHashMap<>())
                    .put(registroId, codigo);
        }

        Optional<String> obterCodigo(UUID registroId, CanalVerificacao canal) {
            return Optional.ofNullable(mapa.getOrDefault(canal, Map.of()).get(registroId));
        }

        void limpar() {
            mapa.clear();
        }
    }
}
