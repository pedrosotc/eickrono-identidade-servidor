package com.eickrono.api.identidade.aplicacao.servico;

import com.eickrono.api.identidade.aplicacao.excecao.EntregaEmailException;
import com.eickrono.api.identidade.aplicacao.excecao.FluxoPublicoException;
import com.eickrono.api.identidade.aplicacao.modelo.CadastroInternoRealizado;
import com.eickrono.api.identidade.aplicacao.modelo.CadastroKeycloakProvisionado;
import com.eickrono.api.identidade.aplicacao.modelo.ConfirmacaoEmailCadastroInternoRealizada;
import com.eickrono.api.identidade.aplicacao.modelo.ConfirmacaoEmailCadastroPublicoRealizada;
import com.eickrono.api.identidade.aplicacao.modelo.ConviteOrganizacionalValidado;
import com.eickrono.api.identidade.aplicacao.modelo.ContextoSolicitacaoFluxoPublico;
import com.eickrono.api.identidade.aplicacao.modelo.ContextoPessoaPerfilSistema;
import com.eickrono.api.identidade.aplicacao.modelo.IdentidadeFederadaKeycloak;
import com.eickrono.api.identidade.aplicacao.modelo.ProjetoFluxoPublicoResolvido;
import com.eickrono.api.identidade.aplicacao.modelo.ProvisionamentoPerfilSistemaRealizado;
import com.eickrono.api.identidade.aplicacao.modelo.StatusCadastroPublico;
import com.eickrono.api.identidade.aplicacao.modelo.VinculoSocialPendenteCadastro;
import com.eickrono.api.identidade.dominio.modelo.CadastroConta;
import com.eickrono.api.identidade.dominio.modelo.CanalValidacaoTelefoneCadastro;
import com.eickrono.api.identidade.dominio.modelo.FormaAcesso;
import com.eickrono.api.identidade.dominio.modelo.PerfilIdentidade;
import com.eickrono.api.identidade.dominio.modelo.Pessoa;
import com.eickrono.api.identidade.dominio.modelo.ProvedorVinculoSocial;
import com.eickrono.api.identidade.dominio.modelo.SexoPessoaCadastro;
import com.eickrono.api.identidade.dominio.modelo.StatusConviteOrganizacional;
import com.eickrono.api.identidade.dominio.modelo.TipoFormaAcesso;
import com.eickrono.api.identidade.dominio.modelo.TipoPessoaCadastro;
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
import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class CadastroContaInternaServico {

    private static final String HMAC_ALG = "HmacSHA256";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Logger LOGGER = LoggerFactory.getLogger(CadastroContaInternaServico.class);

    private final CadastroContaRepositorio cadastroContaRepositorio;
    private final ClienteContextoPessoaPerfilSistema clienteContextoPessoaPerfilSistema;
    private final ClienteAdministracaoCadastroKeycloak clienteAdministracaoCadastroKeycloak;
    private final PessoaRepositorio pessoaRepositorio;
    private final PerfilIdentidadeRepositorio perfilIdentidadeRepositorio;
    private final FormaAcessoRepositorio formaAcessoRepositorio;
    private final VinculoSocialRepositorio vinculoSocialRepositorio;
    private final ConviteOrganizacionalRepositorio conviteOrganizacionalRepositorio;
    private final VinculoOrganizacionalRepositorio vinculoOrganizacionalRepositorio;
    private final ProvisionadorPerfilSistemaServico provisionadorPerfilSistemaServico;
    private final ConsultadorDisponibilidadeUsuarioSistemaServico consultadorDisponibilidadeUsuarioSistemaServico;
    private final CanalEnvioCodigoCadastroEmail canalEnvioCodigoCadastroEmail;
    private final CanalEnvioCodigoCadastroTelefone canalEnvioCodigoCadastroTelefone;
    private final CanalNotificacaoTentativaCadastroEmail canalNotificacaoTentativaCadastroEmail;
    private final DispositivoProperties dispositivoProperties;
    private final Clock clock;
    private final SincronizacaoModeloMultiappService sincronizacaoModeloMultiappService;
    private final AuditoriaService auditoriaService;
    private final ContextoSocialPendenteJdbc contextoSocialPendenteJdbc;
    private final ResolvedorContextoFluxoPublico resolvedorContextoFluxoPublico;
    private final ResolvedorProjetoFluxoPublico resolvedorProjetoFluxoPublico;
    private final boolean tolerarFalhaDisponibilidadeCentralUsuario;
    private ProvisionamentoIdentidadeService provisionamentoIdentidadeServiceCompat;
    private final HexFormat hexFormat = HexFormat.of();

    @Autowired
    public CadastroContaInternaServico(final CadastroContaRepositorio cadastroContaRepositorio,
                                       final ClienteContextoPessoaPerfilSistema clienteContextoPessoaPerfilSistema,
                                       final ClienteAdministracaoCadastroKeycloak clienteAdministracaoCadastroKeycloak,
                                       final PessoaRepositorio pessoaRepositorio,
                                       final PerfilIdentidadeRepositorio perfilIdentidadeRepositorio,
                                       final FormaAcessoRepositorio formaAcessoRepositorio,
                                       final VinculoSocialRepositorio vinculoSocialRepositorio,
                                       final ConviteOrganizacionalRepositorio conviteOrganizacionalRepositorio,
                                       final VinculoOrganizacionalRepositorio vinculoOrganizacionalRepositorio,
                                       final ProvisionadorPerfilSistemaServico provisionadorPerfilSistemaServico,
                                       final ConsultadorDisponibilidadeUsuarioSistemaServico consultadorDisponibilidadeUsuarioSistemaServico,
                                       final ProvisionamentoIdentidadeService provisionamentoIdentidadeService,
                                       final CanalEnvioCodigoCadastroEmail canalEnvioCodigoCadastroEmail,
                                       final CanalEnvioCodigoCadastroTelefone canalEnvioCodigoCadastroTelefone,
                                       final CanalNotificacaoTentativaCadastroEmail canalNotificacaoTentativaCadastroEmail,
                                       final DispositivoProperties dispositivoProperties,
                                       final Clock clock,
                                       final SincronizacaoModeloMultiappService sincronizacaoModeloMultiappService,
                                       final AuditoriaService auditoriaService,
                                       final ContextoSocialPendenteJdbc contextoSocialPendenteJdbc,
                                       final ResolvedorContextoFluxoPublico resolvedorContextoFluxoPublico,
                                       final ResolvedorProjetoFluxoPublico resolvedorProjetoFluxoPublico,
                                       @Value("${identidade.cadastro.tolerar-falha-disponibilidade-central:false}")
                                       final boolean tolerarFalhaDisponibilidadeCentralUsuario) {
        this(
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
                dispositivoProperties,
                clock,
                sincronizacaoModeloMultiappService,
                auditoriaService,
                contextoSocialPendenteJdbc,
                resolvedorContextoFluxoPublico,
                resolvedorProjetoFluxoPublico,
                tolerarFalhaDisponibilidadeCentralUsuario,
                true
        );
    }

    public CadastroContaInternaServico(final CadastroContaRepositorio cadastroContaRepositorio,
                                       final ClienteContextoPessoaPerfilSistema clienteContextoPessoaPerfilSistema,
                                       final ClienteAdministracaoCadastroKeycloak clienteAdministracaoCadastroKeycloak,
                                       final PessoaRepositorio pessoaRepositorio,
                                       final PerfilIdentidadeRepositorio perfilIdentidadeRepositorio,
                                       final FormaAcessoRepositorio formaAcessoRepositorio,
                                       final VinculoSocialRepositorio vinculoSocialRepositorio,
                                       final ConviteOrganizacionalRepositorio conviteOrganizacionalRepositorio,
                                       final VinculoOrganizacionalRepositorio vinculoOrganizacionalRepositorio,
                                       final ProvisionadorPerfilSistemaServico provisionadorPerfilSistemaServico,
                                       final ConsultadorDisponibilidadeUsuarioSistemaServico consultadorDisponibilidadeUsuarioSistemaServico,
                                       final ProvisionamentoIdentidadeService provisionamentoIdentidadeService,
                                       final CanalEnvioCodigoCadastroEmail canalEnvioCodigoCadastroEmail,
                                       final CanalEnvioCodigoCadastroTelefone canalEnvioCodigoCadastroTelefone,
                                       final CanalNotificacaoTentativaCadastroEmail canalNotificacaoTentativaCadastroEmail,
                                       final DispositivoProperties dispositivoProperties,
                                       final Clock clock,
                                       final SincronizacaoModeloMultiappService sincronizacaoModeloMultiappService,
                                       final AuditoriaService auditoriaService,
                                       final ResolvedorContextoFluxoPublico resolvedorContextoFluxoPublico,
                                       final ResolvedorProjetoFluxoPublico resolvedorProjetoFluxoPublico) {
        this(
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
                dispositivoProperties,
                clock,
                sincronizacaoModeloMultiappService,
                auditoriaService,
                null,
                resolvedorContextoFluxoPublico,
                resolvedorProjetoFluxoPublico,
                false,
                true
        );
    }

    public CadastroContaInternaServico(final CadastroContaRepositorio cadastroContaRepositorio,
                                       final ClienteContextoPessoaPerfilSistema clienteContextoPessoaPerfilSistema,
                                       final ClienteAdministracaoCadastroKeycloak clienteAdministracaoCadastroKeycloak,
                                       final PessoaRepositorio pessoaRepositorio,
                                       final PerfilIdentidadeRepositorio perfilIdentidadeRepositorio,
                                       final FormaAcessoRepositorio formaAcessoRepositorio,
                                       final VinculoSocialRepositorio vinculoSocialRepositorio,
                                       final ConviteOrganizacionalRepositorio conviteOrganizacionalRepositorio,
                                       final VinculoOrganizacionalRepositorio vinculoOrganizacionalRepositorio,
                                       final ProvisionadorPerfilSistemaServico provisionadorPerfilSistemaServico,
                                       final ConsultadorDisponibilidadeUsuarioSistemaServico consultadorDisponibilidadeUsuarioSistemaServico,
                                       final ProvisionamentoIdentidadeService provisionamentoIdentidadeService,
                                       final CanalEnvioCodigoCadastroEmail canalEnvioCodigoCadastroEmail,
                                       final CanalEnvioCodigoCadastroTelefone canalEnvioCodigoCadastroTelefone,
                                       final CanalNotificacaoTentativaCadastroEmail canalNotificacaoTentativaCadastroEmail,
                                       final DispositivoProperties dispositivoProperties,
                                       final Clock clock,
                                       final SincronizacaoModeloMultiappService sincronizacaoModeloMultiappService,
                                       final AuditoriaService auditoriaService,
                                       final ContextoSocialPendenteJdbc contextoSocialPendenteJdbc,
                                       final ResolvedorContextoFluxoPublico resolvedorContextoFluxoPublico,
                                       final ResolvedorProjetoFluxoPublico resolvedorProjetoFluxoPublico) {
        this(
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
                dispositivoProperties,
                clock,
                sincronizacaoModeloMultiappService,
                auditoriaService,
                contextoSocialPendenteJdbc,
                resolvedorContextoFluxoPublico,
                resolvedorProjetoFluxoPublico,
                false,
                true
        );
    }

    public CadastroContaInternaServico(final CadastroContaRepositorio cadastroContaRepositorio,
                                       final RecuperacaoSenhaRepositorio recuperacaoSenhaRepositorio,
                                       final FormaAcessoRepositorio formaAcessoRepositorio,
                                       final ClienteAdministracaoCadastroKeycloak clienteAdministracaoCadastroKeycloak,
                                       final ProvisionamentoIdentidadeService provisionamentoIdentidadeService,
                                       final CanalEnvioCodigoCadastroEmail canalEnvioCodigoCadastroEmail,
                                       final CanalEnvioCodigoCadastroTelefone canalEnvioCodigoCadastroTelefone,
                                       final DispositivoProperties dispositivoProperties,
                                       final Clock clock) {
        this(
                cadastroContaRepositorio,
                new ClienteContextoPessoaPerfilSistemaLegado(
                        Objects.requireNonNull(formaAcessoRepositorio, "formaAcessoRepositorio é obrigatório"),
                        Objects.requireNonNull(provisionamentoIdentidadeService, "provisionamentoIdentidadeService é obrigatório")),
                clienteAdministracaoCadastroKeycloak,
                null,
                null,
                formaAcessoRepositorio,
                null,
                null,
                null,
                null,
                null,
                provisionamentoIdentidadeService,
                canalEnvioCodigoCadastroEmail,
                canalEnvioCodigoCadastroTelefone,
                email -> {
                },
                dispositivoProperties,
                clock,
                null,
                null,
                null,
                new ResolvedorContextoFluxoPublico(cadastroContaRepositorio, recuperacaoSenhaRepositorio),
                aplicacaoId -> new ProjetoFluxoPublicoResolvido(0L, aplicacaoId, aplicacaoId, null, null, null, true),
                false,
                false
        );
    }

    public CadastroContaInternaServico(final CadastroContaRepositorio cadastroContaRepositorio,
                                       final RecuperacaoSenhaRepositorio recuperacaoSenhaRepositorio,
                                       final ClienteContextoPessoaPerfilSistema clienteContextoPessoaPerfilSistema,
                                       final ClienteAdministracaoCadastroKeycloak clienteAdministracaoCadastroKeycloak,
                                       final PessoaRepositorio pessoaRepositorio,
                                       final PerfilIdentidadeRepositorio perfilIdentidadeRepositorio,
                                       final FormaAcessoRepositorio formaAcessoRepositorio,
                                       final VinculoSocialRepositorio vinculoSocialRepositorio,
                                       final ConviteOrganizacionalRepositorio conviteOrganizacionalRepositorio,
                                       final VinculoOrganizacionalRepositorio vinculoOrganizacionalRepositorio,
                                       final ProvisionadorPerfilSistemaServico provisionadorPerfilSistemaServico,
                                       final ConsultadorDisponibilidadeUsuarioSistemaServico consultadorDisponibilidadeUsuarioSistemaServico,
                                       final ProvisionamentoIdentidadeService provisionamentoIdentidadeService,
                                       final CanalEnvioCodigoCadastroEmail canalEnvioCodigoCadastroEmail,
                                       final CanalEnvioCodigoCadastroTelefone canalEnvioCodigoCadastroTelefone,
                                       final CanalNotificacaoTentativaCadastroEmail canalNotificacaoTentativaCadastroEmail,
                                       final DispositivoProperties dispositivoProperties,
                                       final Clock clock) {
        this(
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
                null
        );
    }

    public CadastroContaInternaServico(final CadastroContaRepositorio cadastroContaRepositorio,
                                       final RecuperacaoSenhaRepositorio recuperacaoSenhaRepositorio,
                                       final ClienteContextoPessoaPerfilSistema clienteContextoPessoaPerfilSistema,
                                       final ClienteAdministracaoCadastroKeycloak clienteAdministracaoCadastroKeycloak,
                                       final PessoaRepositorio pessoaRepositorio,
                                       final PerfilIdentidadeRepositorio perfilIdentidadeRepositorio,
                                       final FormaAcessoRepositorio formaAcessoRepositorio,
                                       final VinculoSocialRepositorio vinculoSocialRepositorio,
                                       final ConviteOrganizacionalRepositorio conviteOrganizacionalRepositorio,
                                       final VinculoOrganizacionalRepositorio vinculoOrganizacionalRepositorio,
                                       final ProvisionadorPerfilSistemaServico provisionadorPerfilSistemaServico,
                                       final ConsultadorDisponibilidadeUsuarioSistemaServico consultadorDisponibilidadeUsuarioSistemaServico,
                                       final ProvisionamentoIdentidadeService provisionamentoIdentidadeService,
                                       final CanalEnvioCodigoCadastroEmail canalEnvioCodigoCadastroEmail,
                                       final CanalEnvioCodigoCadastroTelefone canalEnvioCodigoCadastroTelefone,
                                       final CanalNotificacaoTentativaCadastroEmail canalNotificacaoTentativaCadastroEmail,
                                       final DispositivoProperties dispositivoProperties,
                                       final Clock clock,
                                       final SincronizacaoModeloMultiappService sincronizacaoModeloMultiappService,
                                       final AuditoriaService auditoriaService) {
        this(
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
                dispositivoProperties,
                clock,
                sincronizacaoModeloMultiappService,
                auditoriaService,
                null,
                new ResolvedorContextoFluxoPublico(cadastroContaRepositorio, recuperacaoSenhaRepositorio),
                aplicacaoId -> new ProjetoFluxoPublicoResolvido(0L, aplicacaoId, aplicacaoId, null, null, null, true),
                false,
                true
        );
    }

    private CadastroContaInternaServico(final CadastroContaRepositorio cadastroContaRepositorio,
                                        final ClienteContextoPessoaPerfilSistema clienteContextoPessoaPerfilSistema,
                                        final ClienteAdministracaoCadastroKeycloak clienteAdministracaoCadastroKeycloak,
                                        final PessoaRepositorio pessoaRepositorio,
                                        final PerfilIdentidadeRepositorio perfilIdentidadeRepositorio,
                                        final FormaAcessoRepositorio formaAcessoRepositorio,
                                        final VinculoSocialRepositorio vinculoSocialRepositorio,
                                        final ConviteOrganizacionalRepositorio conviteOrganizacionalRepositorio,
                                        final VinculoOrganizacionalRepositorio vinculoOrganizacionalRepositorio,
                                        final ProvisionadorPerfilSistemaServico provisionadorPerfilSistemaServico,
                                        final ConsultadorDisponibilidadeUsuarioSistemaServico consultadorDisponibilidadeUsuarioSistemaServico,
                                        final ProvisionamentoIdentidadeService provisionamentoIdentidadeServiceCompat,
                                        final CanalEnvioCodigoCadastroEmail canalEnvioCodigoCadastroEmail,
                                        final CanalEnvioCodigoCadastroTelefone canalEnvioCodigoCadastroTelefone,
                                        final CanalNotificacaoTentativaCadastroEmail canalNotificacaoTentativaCadastroEmail,
                                        final DispositivoProperties dispositivoProperties,
                                        final Clock clock,
                                        final SincronizacaoModeloMultiappService sincronizacaoModeloMultiappService,
                                        final AuditoriaService auditoriaService,
                                        final ContextoSocialPendenteJdbc contextoSocialPendenteJdbc,
                                        final ResolvedorContextoFluxoPublico resolvedorContextoFluxoPublico,
                                        final ResolvedorProjetoFluxoPublico resolvedorProjetoFluxoPublico,
                                        final boolean tolerarFalhaDisponibilidadeCentralUsuario,
                                        final boolean exigirProvisionadorPerfil) {
        this.cadastroContaRepositorio = Objects.requireNonNull(cadastroContaRepositorio, "cadastroContaRepositorio é obrigatório");
        this.clienteContextoPessoaPerfilSistema = Objects.requireNonNull(
                clienteContextoPessoaPerfilSistema, "clienteContextoPessoaPerfilSistema é obrigatório");
        this.clienteAdministracaoCadastroKeycloak = Objects.requireNonNull(
                clienteAdministracaoCadastroKeycloak, "clienteAdministracaoCadastroKeycloak é obrigatório");
        this.pessoaRepositorio = pessoaRepositorio;
        this.perfilIdentidadeRepositorio = perfilIdentidadeRepositorio;
        this.formaAcessoRepositorio = formaAcessoRepositorio;
        this.vinculoSocialRepositorio = vinculoSocialRepositorio;
        this.conviteOrganizacionalRepositorio = conviteOrganizacionalRepositorio;
        this.vinculoOrganizacionalRepositorio = vinculoOrganizacionalRepositorio;
        if (exigirProvisionadorPerfil) {
            this.provisionadorPerfilSistemaServico = Objects.requireNonNull(
                    provisionadorPerfilSistemaServico, "provisionadorPerfilSistemaServico é obrigatório");
            this.consultadorDisponibilidadeUsuarioSistemaServico = Objects.requireNonNull(
                    consultadorDisponibilidadeUsuarioSistemaServico,
                    "consultadorDisponibilidadeUsuarioSistemaServico é obrigatório"
            );
        } else {
            this.provisionadorPerfilSistemaServico = provisionadorPerfilSistemaServico;
            this.consultadorDisponibilidadeUsuarioSistemaServico = consultadorDisponibilidadeUsuarioSistemaServico;
        }
        this.canalEnvioCodigoCadastroEmail = Objects.requireNonNull(
                canalEnvioCodigoCadastroEmail, "canalEnvioCodigoCadastroEmail é obrigatório");
        this.canalEnvioCodigoCadastroTelefone = Objects.requireNonNull(
                canalEnvioCodigoCadastroTelefone, "canalEnvioCodigoCadastroTelefone é obrigatório");
        this.canalNotificacaoTentativaCadastroEmail = Objects.requireNonNull(
                canalNotificacaoTentativaCadastroEmail, "canalNotificacaoTentativaCadastroEmail é obrigatório");
        this.dispositivoProperties = Objects.requireNonNull(dispositivoProperties, "dispositivoProperties é obrigatório");
        this.clock = Objects.requireNonNull(clock, "clock é obrigatório");
        this.sincronizacaoModeloMultiappService = sincronizacaoModeloMultiappService;
        this.auditoriaService = auditoriaService;
        this.contextoSocialPendenteJdbc = contextoSocialPendenteJdbc;
        this.resolvedorContextoFluxoPublico = Objects.requireNonNull(
                resolvedorContextoFluxoPublico, "resolvedorContextoFluxoPublico e obrigatorio");
        this.resolvedorProjetoFluxoPublico = Objects.requireNonNull(
                resolvedorProjetoFluxoPublico, "resolvedorProjetoFluxoPublico e obrigatorio");
        this.tolerarFalhaDisponibilidadeCentralUsuario = tolerarFalhaDisponibilidadeCentralUsuario;
        this.provisionamentoIdentidadeServiceCompat = provisionamentoIdentidadeServiceCompat;
    }

    public CadastroInternoRealizado cadastrar(final String nomeCompleto,
                                              final String emailPrincipal,
                                              final String telefonePrincipal,
                                              final CanalValidacaoTelefoneCadastro tipoValidacaoTelefone,
                                              final String senhaPura,
                                              final String sistemaSolicitante,
                                              final String ipSolicitante,
                                              final String userAgentSolicitante) {
        return cadastrarCompleto(
                TipoPessoaCadastro.FISICA,
                nomeCompleto,
                null,
                "",
                null,
                null,
                null,
                emailPrincipal,
                telefonePrincipal,
                tipoValidacaoTelefone,
                senhaPura,
                null,
                null,
                null,
                sistemaSolicitante,
                ipSolicitante,
                userAgentSolicitante,
                null
        );
    }

    public CadastroInternoRealizado cadastrarPublico(final TipoPessoaCadastro tipoPessoa,
                                                     final String nomeCompleto,
                                                     final String nomeFantasia,
                                                     final String usuario,
                                                     final SexoPessoaCadastro sexo,
                                                     final String paisNascimento,
                                                     final LocalDate dataNascimento,
                                                     final String emailPrincipal,
                                                     final String telefonePrincipal,
                                                     final CanalValidacaoTelefoneCadastro tipoValidacaoTelefone,
                                                     final String senhaPura,
                                                     final String sistemaSolicitante,
                                                     final String ipSolicitante,
                                                     final String userAgentSolicitante) {
        return cadastrarPublico(
                tipoPessoa,
                nomeCompleto,
                nomeFantasia,
                usuario,
                sexo,
                paisNascimento,
                dataNascimento,
                emailPrincipal,
                telefonePrincipal,
                tipoValidacaoTelefone,
                senhaPura,
                null,
                sistemaSolicitante,
                sistemaSolicitante,
                ipSolicitante,
                userAgentSolicitante,
                null
        );
    }

    public CadastroInternoRealizado cadastrarPublico(final TipoPessoaCadastro tipoPessoa,
                                                     final String nomeCompleto,
                                                     final String nomeFantasia,
                                                     final String usuario,
                                                     final SexoPessoaCadastro sexo,
                                                     final String paisNascimento,
                                                     final LocalDate dataNascimento,
                                                     final String emailPrincipal,
                                                     final String telefonePrincipal,
                                                     final CanalValidacaoTelefoneCadastro tipoValidacaoTelefone,
                                                     final String senhaPura,
                                                     final VinculoSocialPendenteCadastro vinculoSocialPendente,
                                                     final String sistemaSolicitante,
                                                     final String ipSolicitante,
                                                     final String userAgentSolicitante) {
        return cadastrarPublico(
                tipoPessoa,
                nomeCompleto,
                nomeFantasia,
                usuario,
                sexo,
                paisNascimento,
                dataNascimento,
                emailPrincipal,
                telefonePrincipal,
                tipoValidacaoTelefone,
                senhaPura,
                vinculoSocialPendente,
                sistemaSolicitante,
                sistemaSolicitante,
                ipSolicitante,
                userAgentSolicitante,
                null
        );
    }

    public CadastroInternoRealizado cadastrarPublico(final TipoPessoaCadastro tipoPessoa,
                                                     final String nomeCompleto,
                                                     final String nomeFantasia,
                                                     final String usuario,
                                                     final SexoPessoaCadastro sexo,
                                                     final String paisNascimento,
                                                     final LocalDate dataNascimento,
                                                     final String emailPrincipal,
                                                     final String telefonePrincipal,
                                                     final CanalValidacaoTelefoneCadastro tipoValidacaoTelefone,
                                                     final String senhaPura,
                                                     final VinculoSocialPendenteCadastro vinculoSocialPendente,
                                                     final String sistemaSolicitante,
                                                     final String ipSolicitante,
                                                     final String userAgentSolicitante,
                                                     final ContextoSolicitacaoFluxoPublico contextoSolicitacao) {
        return cadastrarPublico(
                tipoPessoa,
                nomeCompleto,
                nomeFantasia,
                usuario,
                sexo,
                paisNascimento,
                dataNascimento,
                emailPrincipal,
                telefonePrincipal,
                tipoValidacaoTelefone,
                senhaPura,
                vinculoSocialPendente,
                sistemaSolicitante,
                sistemaSolicitante,
                ipSolicitante,
                userAgentSolicitante,
                contextoSolicitacao
        );
    }

    public CadastroInternoRealizado cadastrarPublico(final TipoPessoaCadastro tipoPessoa,
                                                     final String nomeCompleto,
                                                     final String nomeFantasia,
                                                     final String usuario,
                                                     final SexoPessoaCadastro sexo,
                                                     final String paisNascimento,
                                                     final LocalDate dataNascimento,
                                                     final String emailPrincipal,
                                                     final String telefonePrincipal,
                                                     final CanalValidacaoTelefoneCadastro tipoValidacaoTelefone,
                                                     final String senhaPura,
                                                     final VinculoSocialPendenteCadastro vinculoSocialPendente,
                                                     final String aplicacaoId,
                                                     final String sistemaSolicitante,
                                                     final String ipSolicitante,
                                                     final String userAgentSolicitante,
                                                     final ContextoSolicitacaoFluxoPublico contextoSolicitacao) {
        return cadastrarPublico(
                tipoPessoa,
                nomeCompleto,
                nomeFantasia,
                usuario,
                sexo,
                paisNascimento,
                dataNascimento,
                emailPrincipal,
                telefonePrincipal,
                tipoValidacaoTelefone,
                senhaPura,
                vinculoSocialPendente,
                null,
                aplicacaoId,
                sistemaSolicitante,
                ipSolicitante,
                userAgentSolicitante,
                contextoSolicitacao
        );
    }

    public CadastroInternoRealizado cadastrarPublico(final TipoPessoaCadastro tipoPessoa,
                                                     final String nomeCompleto,
                                                     final String nomeFantasia,
                                                     final String usuario,
                                                     final SexoPessoaCadastro sexo,
                                                     final String paisNascimento,
                                                     final LocalDate dataNascimento,
                                                     final String emailPrincipal,
                                                     final String telefonePrincipal,
                                                     final CanalValidacaoTelefoneCadastro tipoValidacaoTelefone,
                                                     final String senhaPura,
                                                     final VinculoSocialPendenteCadastro vinculoSocialPendente,
                                                     final String aplicacaoId,
                                                     final String sistemaSolicitante,
                                                     final String ipSolicitante,
                                                     final String userAgentSolicitante) {
        return cadastrarPublico(
                tipoPessoa,
                nomeCompleto,
                nomeFantasia,
                usuario,
                sexo,
                paisNascimento,
                dataNascimento,
                emailPrincipal,
                telefonePrincipal,
                tipoValidacaoTelefone,
                senhaPura,
                vinculoSocialPendente,
                null,
                aplicacaoId,
                sistemaSolicitante,
                ipSolicitante,
                userAgentSolicitante,
                null
        );
    }

    public CadastroInternoRealizado cadastrarPublico(final TipoPessoaCadastro tipoPessoa,
                                                     final String nomeCompleto,
                                                     final String nomeFantasia,
                                                     final String usuario,
                                                     final SexoPessoaCadastro sexo,
                                                     final String paisNascimento,
                                                     final LocalDate dataNascimento,
                                                     final String emailPrincipal,
                                                     final String telefonePrincipal,
                                                     final CanalValidacaoTelefoneCadastro tipoValidacaoTelefone,
                                                     final String senhaPura,
                                                     final VinculoSocialPendenteCadastro vinculoSocialPendente,
                                                     final ConviteOrganizacionalValidado conviteOrganizacional,
                                                     final String sistemaSolicitante,
                                                     final String ipSolicitante,
                                                     final String userAgentSolicitante) {
        return cadastrarPublico(
                tipoPessoa,
                nomeCompleto,
                nomeFantasia,
                usuario,
                sexo,
                paisNascimento,
                dataNascimento,
                emailPrincipal,
                telefonePrincipal,
                tipoValidacaoTelefone,
                senhaPura,
                vinculoSocialPendente,
                conviteOrganizacional,
                sistemaSolicitante,
                sistemaSolicitante,
                ipSolicitante,
                userAgentSolicitante,
                null
        );
    }

    public CadastroInternoRealizado cadastrarPublico(final TipoPessoaCadastro tipoPessoa,
                                                     final String nomeCompleto,
                                                     final String nomeFantasia,
                                                     final String usuario,
                                                     final SexoPessoaCadastro sexo,
                                                     final String paisNascimento,
                                                     final LocalDate dataNascimento,
                                                     final String emailPrincipal,
                                                     final String telefonePrincipal,
                                                     final CanalValidacaoTelefoneCadastro tipoValidacaoTelefone,
                                                     final String senhaPura,
                                                     final VinculoSocialPendenteCadastro vinculoSocialPendente,
                                                     final ConviteOrganizacionalValidado conviteOrganizacional,
                                                     final String sistemaSolicitante,
                                                     final String ipSolicitante,
                                                     final String userAgentSolicitante,
                                                     final ContextoSolicitacaoFluxoPublico contextoSolicitacao) {
        return cadastrarPublico(
                tipoPessoa,
                nomeCompleto,
                nomeFantasia,
                usuario,
                sexo,
                paisNascimento,
                dataNascimento,
                emailPrincipal,
                telefonePrincipal,
                tipoValidacaoTelefone,
                senhaPura,
                vinculoSocialPendente,
                conviteOrganizacional,
                sistemaSolicitante,
                sistemaSolicitante,
                ipSolicitante,
                userAgentSolicitante,
                contextoSolicitacao
        );
    }

    public CadastroInternoRealizado cadastrarPublico(final TipoPessoaCadastro tipoPessoa,
                                                     final String nomeCompleto,
                                                     final String nomeFantasia,
                                                     final String usuario,
                                                     final SexoPessoaCadastro sexo,
                                                     final String paisNascimento,
                                                     final LocalDate dataNascimento,
                                                     final String emailPrincipal,
                                                     final String telefonePrincipal,
                                                     final CanalValidacaoTelefoneCadastro tipoValidacaoTelefone,
                                                     final String senhaPura,
                                                     final VinculoSocialPendenteCadastro vinculoSocialPendente,
                                                     final ConviteOrganizacionalValidado conviteOrganizacional,
                                                     final String aplicacaoId,
                                                     final String sistemaSolicitante,
                                                     final String ipSolicitante,
                                                     final String userAgentSolicitante) {
        return cadastrarPublico(
                tipoPessoa,
                nomeCompleto,
                nomeFantasia,
                usuario,
                sexo,
                paisNascimento,
                dataNascimento,
                emailPrincipal,
                telefonePrincipal,
                tipoValidacaoTelefone,
                senhaPura,
                vinculoSocialPendente,
                conviteOrganizacional,
                aplicacaoId,
                sistemaSolicitante,
                ipSolicitante,
                userAgentSolicitante,
                null
        );
    }

    public CadastroInternoRealizado cadastrarPublico(final TipoPessoaCadastro tipoPessoa,
                                                     final String nomeCompleto,
                                                     final String nomeFantasia,
                                                     final String usuario,
                                                     final SexoPessoaCadastro sexo,
                                                     final String paisNascimento,
                                                     final LocalDate dataNascimento,
                                                     final String emailPrincipal,
                                                     final String telefonePrincipal,
                                                     final CanalValidacaoTelefoneCadastro tipoValidacaoTelefone,
                                                     final String senhaPura,
                                                     final VinculoSocialPendenteCadastro vinculoSocialPendente,
                                                     final ConviteOrganizacionalValidado conviteOrganizacional,
                                                     final String aplicacaoId,
                                                     final String sistemaSolicitante,
                                                     final String ipSolicitante,
                                                     final String userAgentSolicitante,
                                                     final ContextoSolicitacaoFluxoPublico contextoSolicitacao) {
        return cadastrarCompleto(
                Objects.requireNonNull(tipoPessoa, "tipoPessoa é obrigatório"),
                nomeCompleto,
                nomeFantasia,
                usuario,
                sexo,
                paisNascimento,
                dataNascimento,
                emailPrincipal,
                telefonePrincipal,
                tipoValidacaoTelefone,
                senhaPura,
                vinculoSocialPendente,
                conviteOrganizacional,
                aplicacaoId,
                sistemaSolicitante,
                ipSolicitante,
                userAgentSolicitante,
                contextoSolicitacao
        );
    }

    public ConfirmacaoEmailCadastroInternoRealizada confirmarEmail(final UUID cadastroId, final String codigo) {
        ConfirmacaoEmailCadastroPublicoRealizada confirmacao = confirmarEmailDetalhado(cadastroId, codigo, null);
        return new ConfirmacaoEmailCadastroInternoRealizada(
                confirmacao.cadastroId(),
                confirmacao.subjectRemoto(),
                confirmacao.emailPrincipal(),
                confirmacao.emailConfirmado(),
                confirmacao.podeAutenticar()
        );
    }

    public ConfirmacaoEmailCadastroPublicoRealizada confirmarEmailPublico(final UUID cadastroId,
                                                                          final String codigo,
                                                                          final String codigoTelefone) {
        return confirmarEmailDetalhado(cadastroId, codigo, codigoTelefone);
    }

    public ConfirmacaoEmailCadastroPublicoRealizada confirmarTelefonePublico(final UUID cadastroId,
                                                                             final String codigoTelefone) {
        if (cadastroId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CadastroId é obrigatório.");
        }
        CadastroConta cadastroConta = cadastroContaRepositorio.findByCadastroId(cadastroId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cadastro não encontrado."));
        if (!cadastroConta.possuiTelefoneParaValidacao()) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "O cadastro não exige confirmação de telefone."
            );
        }
        if (!cadastroConta.emailJaConfirmado()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "O e-mail ainda precisa ser confirmado antes do telefone."
            );
        }
        if (cadastroConta.etapaTelefoneConcluida()) {
            return montarRespostaConfirmacao(cadastroConta, cadastroConta.getStatus().name());
        }

        OffsetDateTime agora = OffsetDateTime.now(clock);
        if (cadastroConta.codigoTelefoneExpirado(agora)) {
            throw new ResponseStatusException(HttpStatus.GONE, "O código de confirmação do cadastro expirou.");
        }

        String codigoTelefoneNormalizado = obrigatorio(
                codigoTelefone,
                "codigoTelefone",
                "Código de confirmação do telefone é obrigatório."
        );
        if (!Objects.equals(
                cadastroConta.getCodigoTelefoneHash(),
                hashCodigoTelefone(
                        codigoTelefoneNormalizado,
                        cadastroConta.getTelefonePrincipal(),
                        cadastroConta.getSubjectRemoto()))) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "O código de confirmação do telefone informado é inválido."
            );
        }

        return finalizarCadastroPublico(cadastroConta, agora);
    }

    public boolean identificadorPublicoSistemaDisponivelPublico(final String identificadorPublicoSistema,
                                                                final String sistemaSolicitante) {
        String usuarioNormalizado = normalizarUsuarioOpcional(identificadorPublicoSistema);
        String sistemaNormalizado = normalizarUsuarioOpcional(sistemaSolicitante);
        if (usuarioNormalizado == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "usuario é obrigatório.");
        }
        if (sistemaNormalizado == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "aplicacaoId é obrigatório.");
        }
        return identificadorPublicoSistemaDisponivelNormalizado(usuarioNormalizado, sistemaNormalizado);
    }

    public boolean usuarioDisponivelPublico(final String usuario, final String sistemaSolicitante) {
        return identificadorPublicoSistemaDisponivelPublico(usuario, sistemaSolicitante);
    }

    public boolean possuiCadastroPendenteEmailPublico(final String emailPrincipal) {
        String emailNormalizado = obrigatorio(emailPrincipal, "emailPrincipal").toLowerCase(Locale.ROOT);
        return cadastroContaRepositorio.findByEmailPrincipal(emailNormalizado)
                .map(this::cadastroPendenteEmail)
                .orElse(false);
    }

    public Optional<UUID> buscarCadastroPendenteEmailPublico(final String emailPrincipal) {
        String emailNormalizado = obrigatorio(emailPrincipal, "emailPrincipal").toLowerCase(Locale.ROOT);
        return cadastroContaRepositorio.findByEmailPrincipal(emailNormalizado)
                .filter(this::cadastroPendenteEmail)
                .map(CadastroConta::getCadastroId);
    }

    public Optional<CadastroConta> buscarCadastroPendenteEmailPublicoPorProjeto(final String aplicacaoId,
                                                                                 final String emailPrincipal) {
        ProjetoFluxoPublicoResolvido projeto = resolvedorProjetoFluxoPublico.resolverAtivo(aplicacaoId);
        String emailNormalizado = obrigatorio(emailPrincipal, "emailPrincipal").toLowerCase(Locale.ROOT);
        return cadastroContaRepositorio.findAllByEmailPrincipal(emailNormalizado).stream()
                .filter(cadastro -> Objects.equals(cadastro.getClienteEcossistemaId(), projeto.clienteEcossistemaId()))
                .filter(this::cadastroPendenteEmail)
                .findFirst();
    }

    public void definirSenhaCadastroPendentePublico(final UUID cadastroId,
                                                    final String senhaPura,
                                                    final String confirmacaoSenha) {
        if (cadastroId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CadastroId é obrigatório.");
        }
        CadastroConta cadastroConta = cadastroContaRepositorio.findByCadastroId(cadastroId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cadastro não encontrado."));
        if (!cadastroConta.emailJaConfirmado() || !cadastroConta.etapaTelefoneConcluida()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A conta ainda precisa validar os contatos antes de definir uma nova senha."
            );
        }
        String senhaNormalizada = obrigatorio(senhaPura, "senhaPura");
        String confirmacaoNormalizada = obrigatorio(confirmacaoSenha, "confirmacaoSenha");
        if (!Objects.equals(senhaNormalizada, confirmacaoNormalizada)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "A confirmação de senha não confere.");
        }
        RecuperacaoSenhaService.validarPoliticaSenha(senhaNormalizada);
        clienteAdministracaoCadastroKeycloak.redefinirSenha(cadastroConta.getSubjectRemoto(), senhaNormalizada);
    }

    public StatusCadastroPublico consultarStatusCadastroPublico(final UUID cadastroId) {
        if (cadastroId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CadastroId é obrigatório.");
        }
        CadastroConta cadastroConta = cadastroContaRepositorio.findByCadastroId(cadastroId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cadastro não encontrado."));
        return montarStatusCadastroPublico(cadastroConta);
    }

    public void reenviarCodigoEmail(final UUID cadastroId) {
        if (cadastroId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CadastroId é obrigatório.");
        }
        CadastroConta cadastroConta = cadastroContaRepositorio.findByCadastroId(cadastroId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cadastro não encontrado."));
        if (cadastroConta.emailJaConfirmado()) {
            return;
        }
        if (cadastroConta.ultrapassouReenviosEmail(dispositivoProperties.getCodigo().getReenviosMaximos())) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "O limite de reenvios foi atingido.");
        }

        OffsetDateTime agora = OffsetDateTime.now(clock);
        String codigoClaro = gerarCodigoNumerico();
        cadastroConta.atualizarCodigoEmail(
                hashCodigoEmail(codigoClaro, cadastroConta.getEmailPrincipal(), cadastroConta.getSubjectRemoto()),
                agora,
                agora.plusHours(dispositivoProperties.getCodigo().getExpiracaoHoras()),
                agora
        );
        sincronizarCadastroSeConfigurado(cadastroConta);
        try {
            canalEnvioCodigoCadastroEmail.enviar(cadastroConta, codigoClaro);
        } catch (EntregaEmailException ex) {
            throw traduzirFalhaEnvioCodigoCadastro(cadastroConta, ex);
        }
    }

    public void reenviarCodigoTelefone(final UUID cadastroId) {
        if (cadastroId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CadastroId é obrigatório.");
        }
        CadastroConta cadastroConta = cadastroContaRepositorio.findByCadastroId(cadastroId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cadastro não encontrado."));
        if (!cadastroConta.possuiTelefoneParaValidacao()) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "O cadastro não exige confirmação de telefone."
            );
        }
        if (!cadastroConta.emailJaConfirmado()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "O e-mail ainda precisa ser confirmado antes do telefone."
            );
        }
        if (cadastroConta.etapaTelefoneConcluida()) {
            return;
        }

        OffsetDateTime agora = OffsetDateTime.now(clock);
        String codigoTelefoneClaro = gerarCodigoNumerico();
        cadastroConta.definirCodigoTelefone(
                hashCodigoTelefone(
                        codigoTelefoneClaro,
                        cadastroConta.getTelefonePrincipal(),
                        cadastroConta.getSubjectRemoto()
                ),
                agora,
                agora.plusHours(dispositivoProperties.getCodigo().getExpiracaoHoras()),
                agora
        );
        sincronizarCadastroSeConfigurado(cadastroConta);
        canalEnvioCodigoCadastroTelefone.enviar(cadastroConta, codigoTelefoneClaro);
    }

    public void cancelarCadastroPendentePublico(final UUID cadastroId) {
        if (cadastroId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CadastroId é obrigatório.");
        }
        cadastroContaRepositorio.findByCadastroId(cadastroId)
                .filter(this::cadastroPendenteEmail)
                .ifPresent(this::removerCadastroPendente);
    }

    public int expurgarCadastrosPendentesExpirados() {
        OffsetDateTime limite = OffsetDateTime.now(clock).minusHours(48);
        List<CadastroConta> expirados = cadastroContaRepositorio.findByCriadoEmBefore(limite).stream()
                .filter(this::cadastroPendenteEmail)
                .toList();
        int removidos = 0;
        for (CadastroConta cadastroConta : expirados) {
            try {
                removerCadastroPendente(cadastroConta);
                removidos += 1;
            } catch (RuntimeException ex) {
                LOGGER.warn(
                        "Falha ao expurgar cadastro pendente expirado cadastroId={} subjectRemoto={}",
                        cadastroConta.getCadastroId(),
                        cadastroConta.getSubjectRemoto(),
                        ex
                );
            }
        }
        return removidos;
    }

    private CadastroInternoRealizado cadastrarCompleto(final TipoPessoaCadastro tipoPessoa,
                                                       final String nomeCompleto,
                                                       final String nomeFantasia,
                                                       final String usuario,
                                                       final SexoPessoaCadastro sexo,
                                                       final String paisNascimento,
                                                       final LocalDate dataNascimento,
                                                       final String emailPrincipal,
                                                       final String telefonePrincipal,
                                                       final CanalValidacaoTelefoneCadastro tipoValidacaoTelefone,
                                                       final String senhaPura,
                                                       final VinculoSocialPendenteCadastro vinculoSocialPendente,
                                                       final ConviteOrganizacionalValidado conviteOrganizacional,
                                                       final String aplicacaoId,
                                                       final String sistemaSolicitante,
                                                       final String ipSolicitante,
                                                       final String userAgentSolicitante,
                                                       final ContextoSolicitacaoFluxoPublico contextoSolicitacao) {
        String nomeNormalizado = obrigatorio(nomeCompleto, "nomeCompleto");
        String emailNormalizado = obrigatorio(emailPrincipal, "emailPrincipal").toLowerCase(Locale.ROOT);
        String telefoneNormalizado = normalizarOpcional(telefonePrincipal);
        CanalValidacaoTelefoneCadastro tipoValidacaoTelefoneNormalizado =
                telefoneNormalizado == null
                        ? null
                        : Objects.requireNonNullElse(tipoValidacaoTelefone, CanalValidacaoTelefoneCadastro.SMS);
        String senhaNormalizada = obrigatorio(senhaPura, "senhaPura");
        RecuperacaoSenhaService.validarPoliticaSenha(senhaNormalizada);
        String sistemaNormalizado = obrigatorio(sistemaSolicitante, "sistemaSolicitante");
        String usuarioNormalizado = normalizarUsuarioOpcional(usuario);
        String nomeFantasiaNormalizado = normalizarOpcional(nomeFantasia);
        String paisNascimentoNormalizado = normalizarOpcional(paisNascimento);
        boolean fluxoCadastroPublico = usuarioNormalizado != null && !usuarioNormalizado.isBlank();
        ProjetoFluxoPublicoResolvido projeto = aplicacaoId == null || aplicacaoId.isBlank()
                ? null
                : resolvedorProjetoFluxoPublico.resolverAtivo(aplicacaoId);
        ContextoSolicitacaoFluxoPublico contextoResolvido = resolvedorContextoFluxoPublico.resolver(
                emailNormalizado,
                contextoSolicitacao
        );
        if (projeto != null) {
            contextoResolvido = contextoResolvido.mesclarFaltantes(projeto.comoContextoPadrao());
        }

        validarDuplicidadeUsuario(usuarioNormalizado, sistemaNormalizado);
        validarDuplicidadeEmail(emailNormalizado);

        CadastroKeycloakProvisionado cadastroKeycloak;
        try {
            cadastroKeycloak = clienteAdministracaoCadastroKeycloak.criarUsuarioPendente(
                    nomeNormalizado,
                    emailNormalizado,
                    senhaNormalizada
            );
        } catch (ResponseStatusException exception) {
            if (exception.getStatusCode() == HttpStatus.CONFLICT) {
                throw criarErroEmailIndisponivel();
            }
            throw exception;
        }

        OffsetDateTime agora = OffsetDateTime.now(clock);
        String codigoClaro = gerarCodigoNumerico();
        boolean exigeValidacaoTelefone = projeto != null
                ? projeto.exigeValidacaoTelefone()
                : !fluxoCadastroPublico && telefoneNormalizado != null;
        String codigoTelefoneClaro = exigeValidacaoTelefone && telefoneNormalizado != null
                ? gerarCodigoNumerico()
                : null;
        CadastroConta cadastroConta = new CadastroConta(
                UUID.randomUUID(),
                cadastroKeycloak.subjectRemoto(),
                Objects.requireNonNull(tipoPessoa, "tipoPessoa é obrigatório"),
                nomeNormalizado,
                nomeFantasiaNormalizado,
                Objects.requireNonNullElse(usuarioNormalizado, ""),
                sexo,
                paisNascimentoNormalizado,
                dataNascimento,
                emailNormalizado,
                telefoneNormalizado,
                tipoValidacaoTelefoneNormalizado,
                hashCodigoEmail(codigoClaro, emailNormalizado, cadastroKeycloak.subjectRemoto()),
                agora,
                agora.plusHours(dispositivoProperties.getCodigo().getExpiracaoHoras()),
                sistemaNormalizado,
                ipSolicitante,
                userAgentSolicitante,
                agora,
                agora,
                contextoResolvido
        );
        if (codigoTelefoneClaro != null) {
            cadastroConta.definirCodigoTelefone(
                    hashCodigoTelefone(codigoTelefoneClaro, telefoneNormalizado, cadastroKeycloak.subjectRemoto()),
                    agora,
                    agora.plusHours(dispositivoProperties.getCodigo().getExpiracaoHoras()),
                    agora
            );
        }
        if (vinculoSocialPendente != null) {
            cadastroConta.definirVinculoSocialPendente(
                    vinculoSocialPendente.provedor(),
                    vinculoSocialPendente.identificadorExterno(),
                    vinculoSocialPendente.nomeUsuarioExterno(),
                    agora
            );
        }
        if (conviteOrganizacional != null) {
            cadastroConta.definirConviteOrganizacionalPendente(
                    conviteOrganizacional.codigo(),
                    conviteOrganizacional.organizacaoId(),
                    conviteOrganizacional.nomeOrganizacao(),
                    conviteOrganizacional.emailConvidado(),
                    conviteOrganizacional.exigeContaSeparada(),
                    agora
            );
        }
        cadastroConta.registrarProjetoFluxoPublico(
                projeto == null ? null : projeto.clienteEcossistemaId(),
                exigeValidacaoTelefone,
                agora
        );
        cadastroConta = cadastroContaRepositorio.save(cadastroConta);

        if (provisionamentoIdentidadeServiceCompat != null) {
            provisionamentoIdentidadeServiceCompat.provisionarCadastroPendente(
                    cadastroKeycloak.subjectRemoto(),
                    emailNormalizado,
                    nomeNormalizado,
                    agora
            );
        }

        sincronizarCadastroSeConfigurado(cadastroConta);
        try {
            canalEnvioCodigoCadastroEmail.enviar(cadastroConta, codigoClaro);
        } catch (EntregaEmailException ex) {
            try {
                removerCadastroPendente(cadastroConta);
            } catch (RuntimeException cleanupException) {
                LOGGER.warn(
                        "Falha ao reverter cadastro pendente apos erro de envio cadastroId={} subjectRemoto={}",
                        cadastroConta.getCadastroId(),
                        cadastroConta.getSubjectRemoto(),
                        cleanupException
                );
            }
            throw traduzirFalhaEnvioCodigoCadastro(cadastroConta, ex);
        }
        if (exigeValidacaoTelefone && codigoTelefoneClaro != null) {
            canalEnvioCodigoCadastroTelefone.enviar(cadastroConta, codigoTelefoneClaro);
        }

        return new CadastroInternoRealizado(
                cadastroConta.getCadastroId(),
                cadastroKeycloak.subjectRemoto(),
                emailNormalizado,
                true
        );
    }

    private ConfirmacaoEmailCadastroPublicoRealizada confirmarEmailDetalhado(final UUID cadastroId,
                                                                             final String codigo,
                                                                             final String codigoTelefone) {
        if (cadastroId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CadastroId é obrigatório.");
        }
        String codigoNormalizado = obrigatorio(codigo, "codigo");
        CadastroConta cadastroConta = cadastroContaRepositorio.findByCadastroId(cadastroId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cadastro não encontrado."));

        if (cadastroConta.emailJaConfirmado()) {
            if (cadastroConta.possuiTelefoneParaValidacao()
                    && !cadastroConta.etapaTelefoneConcluida()
                    && codigoTelefone != null
                    && !codigoTelefone.isBlank()) {
                return confirmarTelefonePublico(cadastroId, codigoTelefone);
            }
            return montarRespostaConfirmacao(cadastroConta, cadastroConta.getStatus().name());
        }

        OffsetDateTime agora = OffsetDateTime.now(clock);
        if (cadastroConta.codigoEmailExpirado(agora)) {
            throw new ResponseStatusException(HttpStatus.GONE, "O código de confirmação do cadastro expirou.");
        }
        if (cadastroConta.getTentativasConfirmacaoEmail() >= dispositivoProperties.getCodigo().getTentativasMaximas()) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "O limite de tentativas de confirmação foi atingido."
            );
        }

        cadastroConta.registrarTentativaConfirmacao(agora);
        if (!Objects.equals(
                cadastroConta.getCodigoEmailHash(),
                hashCodigoEmail(codigoNormalizado, cadastroConta.getEmailPrincipal(), cadastroConta.getSubjectRemoto()))) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "O código de confirmação informado é inválido.");
        }
        cadastroConta.marcarEmailConfirmado(agora);
        sincronizarCadastroSeConfigurado(cadastroConta);

        if (ehFluxoCadastroPublico(cadastroConta)
                && cadastroConta.possuiTelefoneParaValidacao()
                && codigoTelefone != null
                && !codigoTelefone.isBlank()) {
            String codigoTelefoneNormalizado = obrigatorio(
                    codigoTelefone,
                    "codigoTelefone",
                    "Código de confirmação do telefone é obrigatório."
            );
            if (cadastroConta.codigoTelefoneExpirado(agora)) {
                throw new ResponseStatusException(HttpStatus.GONE, "O código de confirmação do cadastro expirou.");
            }
            if (!Objects.equals(
                    cadastroConta.getCodigoTelefoneHash(),
                    hashCodigoTelefone(
                            codigoTelefoneNormalizado,
                            cadastroConta.getTelefonePrincipal(),
                            cadastroConta.getSubjectRemoto()))) {
                throw new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "O código de confirmação do telefone informado é inválido."
                );
            }
            return finalizarCadastroPublico(cadastroConta, agora);
        }

        if (ehFluxoCadastroPublico(cadastroConta) && cadastroConta.possuiTelefoneParaValidacao()) {
            return montarRespostaConfirmacao(cadastroConta, "EMAIL_CONFIRMADO");
        }

        return finalizarCadastroPublico(cadastroConta, agora);
    }

    private ConfirmacaoEmailCadastroPublicoRealizada finalizarCadastroPublico(final CadastroConta cadastroConta,
                                                                              final OffsetDateTime agora) {
        String statusPerfilSistema = "EMAIL_CONFIRMADO";
        if (ehFluxoCadastroPublico(cadastroConta)) {
            Pessoa pessoa = confirmarPessoaCanonicaPublica(cadastroConta, agora);
            ProvisionamentoPerfilSistemaRealizado provisionamento =
                    provisionarPerfilSistemaPublico(cadastroConta, pessoa.getId());
            cadastroConta.definirProvisionamentoPerfil(
                    pessoa.getId(),
                    provisionamento.perfilSistemaId(),
                    agora
            );
            statusPerfilSistema = provisionamento.statusPerfilSistema();
        } else {
            clienteContextoPessoaPerfilSistema.buscarPorSub(cadastroConta.getSubjectRemoto())
                    .ifPresent(contexto -> {
                        cadastroConta.definirPessoaIdPerfil(contexto.pessoaId(), agora);
                        if (contexto.perfilSistemaId() != null && !contexto.perfilSistemaId().isBlank()) {
                            cadastroConta.definirProvisionamentoPerfil(
                                    contexto.pessoaId(),
                                    contexto.perfilSistemaId(),
                                    agora
                            );
                        }
                    });
            if (provisionamentoIdentidadeServiceCompat != null) {
                provisionamentoIdentidadeServiceCompat.confirmarEmailCadastro(
                        cadastroConta.getSubjectRemoto(),
                        cadastroConta.getEmailPrincipal(),
                        cadastroConta.getNomeCompleto(),
                        agora
                );
            }
            statusPerfilSistema = clienteContextoPessoaPerfilSistema.buscarPorSub(cadastroConta.getSubjectRemoto())
                    .map(ContextoPessoaPerfilSistema::statusPerfilSistema)
                    .filter(valor -> valor != null && !valor.isBlank())
                    .orElse(statusPerfilSistema);
        }

        vincularIdentidadesSociaisPendentesDoCadastro(cadastroConta, agora);
        materializarVinculoOrganizacionalSeNecessario(cadastroConta, agora);
        clienteAdministracaoCadastroKeycloak.confirmarEmailEAtivarUsuario(
                cadastroConta.getSubjectRemoto(),
                cadastroConta.getNomeCompleto(),
                cadastroConta.getDataNascimento()
        );
        if (cadastroConta.possuiVinculoSocialPendente()) {
            cadastroConta.limparVinculoSocialPendente(agora);
        }
        cadastroConta.marcarContatosConfirmados(agora);
        sincronizarCadastroSeConfigurado(cadastroConta);

        return montarRespostaConfirmacao(cadastroConta, statusPerfilSistema);
    }

    private Pessoa confirmarPessoaCanonicaPublica(final CadastroConta cadastroConta,
                                                  final OffsetDateTime agora) {
        ProvisionamentoIdentidadeService provisionamentoIdentidadeService = Objects.requireNonNull(
                provisionamentoIdentidadeServiceCompat,
                "provisionamentoIdentidadeServiceCompat é obrigatório para o fluxo público"
        );
        Pessoa pessoa = provisionamentoIdentidadeService.confirmarEmailCadastro(
                cadastroConta.getSubjectRemoto(),
                cadastroConta.getEmailPrincipal(),
                cadastroConta.getNomeCompleto(),
                agora
        );
        cadastroConta.definirPessoaIdPerfil(pessoa.getId(), agora);
        return pessoa;
    }

    private ProvisionamentoPerfilSistemaRealizado provisionarPerfilSistemaPublico(final CadastroConta cadastroConta,
                                                                                  final Long pessoaIdCentral) {
        ProvisionadorPerfilSistemaServico provisionador = Objects.requireNonNull(
                provisionadorPerfilSistemaServico,
                "provisionadorPerfilSistemaServico é obrigatório para o fluxo público"
        );
        return provisionador.provisionarCadastroConfirmado(cadastroConta, pessoaIdCentral);
    }

    private ConfirmacaoEmailCadastroPublicoRealizada montarRespostaConfirmacao(final CadastroConta cadastroConta,
                                                                               final String statusPerfilSistemaPadrao) {
        ContextoPessoaPerfilSistema contexto = clienteContextoPessoaPerfilSistema.buscarPorSub(cadastroConta.getSubjectRemoto()).orElse(null);
        String perfilSistemaId = cadastroConta.getPerfilSistemaId();
        if ((perfilSistemaId == null || perfilSistemaId.isBlank()) && contexto != null) {
            perfilSistemaId = contexto.perfilSistemaId();
        }
        String statusPerfilSistema = contexto == null
                || contexto.statusPerfilSistema() == null
                || contexto.statusPerfilSistema().isBlank()
                ? statusPerfilSistemaPadrao
                : contexto.statusPerfilSistema();
        boolean telefoneObrigatorio = cadastroConta.possuiTelefoneParaValidacao();
        boolean telefoneConfirmado = cadastroConta.telefoneJaConfirmado();
        boolean podeAutenticar = cadastroConta.emailJaConfirmado() && cadastroConta.etapaTelefoneConcluida();
        return new ConfirmacaoEmailCadastroPublicoRealizada(
                cadastroConta.getCadastroId(),
                cadastroConta.getSubjectRemoto(),
                cadastroConta.getEmailPrincipal(),
                Objects.requireNonNullElse(perfilSistemaId, ""),
                statusPerfilSistema,
                cadastroConta.emailJaConfirmado(),
                telefoneConfirmado,
                telefoneObrigatorio,
                podeAutenticar,
                resolverProximoPasso(cadastroConta)
        );
    }

    private StatusCadastroPublico montarStatusCadastroPublico(final CadastroConta cadastroConta) {
        return new StatusCadastroPublico(
                cadastroConta.getCadastroId(),
                cadastroConta.getEmailPrincipal(),
                Objects.requireNonNullElse(cadastroConta.getTelefonePrincipal(), ""),
                cadastroConta.emailJaConfirmado(),
                cadastroConta.telefoneJaConfirmado(),
                cadastroConta.possuiTelefoneParaValidacao(),
                cadastroConta.emailJaConfirmado() && cadastroConta.etapaTelefoneConcluida(),
                resolverProximoPasso(cadastroConta)
        );
    }

    private void validarDuplicidadeUsuario(final String usuarioNormalizado, final String sistemaSolicitante) {
        if (usuarioNormalizado == null || usuarioNormalizado.isBlank()) {
            return;
        }
        if (!identificadorPublicoSistemaDisponivelNormalizado(usuarioNormalizado, sistemaSolicitante)) {
            throw FluxoPublicoException.conflito("usuario_indisponivel", "Este usuário não está disponível.");
        }
    }

    private boolean identificadorPublicoSistemaDisponivelNormalizado(final String usuarioNormalizado,
                                                                     final String sistemaSolicitante) {
        if (cadastroContaRepositorio.findByUsuarioIgnoreCaseAndSistemaSolicitanteIgnoreCase(
                usuarioNormalizado,
                sistemaSolicitante
        ).isPresent()) {
            return false;
        }
        if (tolerarFalhaDisponibilidadeCentralUsuario) {
            LOGGER.warn(
                    "cadastro_publico_disponibilidade_central_tolerada usuario={} sistemaSolicitante={}",
                    usuarioNormalizado,
                    sistemaSolicitante
            );
            return true;
        }
        return consultadorDisponibilidadeUsuarioSistemaServico == null
                || consultadorDisponibilidadeUsuarioSistemaServico.usuarioDisponivel(
                        usuarioNormalizado,
                        sistemaSolicitante
                );
    }

    private void validarDuplicidadeEmail(final String emailNormalizado) {
        Optional<CadastroConta> cadastroExistente = cadastroContaRepositorio.findByEmailPrincipal(emailNormalizado);
        if (cadastroExistente.isPresent()) {
            throw criarErroEmailIndisponivel();
        }
        clienteContextoPessoaPerfilSistema.buscarPorEmail(emailNormalizado)
                .ifPresent(contexto -> {
                    notificarTentativaCadastroContaExistente(emailNormalizado);
                    throw criarErroEmailIndisponivel();
                });
    }

    private FluxoPublicoException criarErroEmailIndisponivel() {
        return FluxoPublicoException.conflito(
                "email_indisponivel",
                "Já existe uma conta com o e-mail informado. Entre ou recupere a senha."
        );
    }

    private ProvedorVinculoSocial resolverProvedorVinculoSocialPendente(final CadastroConta cadastroConta) {
        return ProvedorVinculoSocial.fromAlias(cadastroConta.getVinculoSocialPendenteProvedor())
                .orElseThrow(() -> new IllegalStateException("Provedor social pendente inválido para o cadastro."));
    }

    private void vincularIdentidadesSociaisPendentesDoCadastro(final CadastroConta cadastroConta,
                                                               final OffsetDateTime instante) {
        for (IdentidadeSocialPendenteFinalizacao identidadePendente :
                resolverIdentidadesSociaisPendentesDoCadastro(cadastroConta)) {
            clienteAdministracaoCadastroKeycloak.vincularIdentidadeFederada(
                    cadastroConta.getSubjectRemoto(),
                    identidadePendente.identidadeFederada()
            );
            materializarVinculoSocialLocalSeNecessario(
                    cadastroConta,
                    identidadePendente.identidadeFederada(),
                    instante
            );
            if (identidadePendente.contextoId() != null && contextoSocialPendenteJdbc != null) {
                contextoSocialPendenteJdbc.consumir(identidadePendente.contextoId());
            }
        }
    }

    private List<IdentidadeSocialPendenteFinalizacao> resolverIdentidadesSociaisPendentesDoCadastro(
            final CadastroConta cadastroConta) {
        java.util.LinkedHashMap<String, IdentidadeSocialPendenteFinalizacao> identidades = new java.util.LinkedHashMap<>();

        if (contextoSocialPendenteJdbc != null) {
            try {
                ProjetoFluxoPublicoResolvido projeto = resolvedorProjetoFluxoPublico
                        .resolverAtivo(cadastroConta.getSistemaSolicitante());
                if (projeto != null) {
                    List<ContextoSocialPendenteJdbc.ContextoSocialPendenteCadastro> contextos =
                            contextoSocialPendenteJdbc.listarPendentesAberturaCadastro(
                                    projeto.clienteEcossistemaId(),
                                    cadastroConta.getEmailPrincipal()
                            );
                    for (ContextoSocialPendenteJdbc.ContextoSocialPendenteCadastro contexto : contextos) {
                        Optional<ProvedorVinculoSocial> provedor = ProvedorVinculoSocial.fromAlias(contexto.provedor());
                        if (provedor.isEmpty()) {
                            LOGGER.warn(
                                    "contexto_social_pendente_ignorado_provedor_desconhecido cadastroId={} provedor={}",
                                    cadastroConta.getCadastroId(),
                                    contexto.provedor()
                            );
                            continue;
                        }
                        IdentidadeFederadaKeycloak identidadeFederada = new IdentidadeFederadaKeycloak(
                                provedor.orElseThrow(),
                                contexto.identificadorExterno(),
                                contexto.nomeUsuarioExterno(),
                                contexto.nomeExibicaoExterno(),
                                contexto.urlAvatarExterno()
                        );
                        adicionarIdentidadeSocialPendente(
                                identidades,
                                identidadeFederada,
                                contexto.id()
                        );
                    }
                }
            } catch (RuntimeException exception) {
                LOGGER.warn(
                        "falha_resolucao_contextos_sociais_pendentes_pos_confirmacao cadastroId={} sistema={} motivo={}",
                        cadastroConta.getCadastroId(),
                        cadastroConta.getSistemaSolicitante(),
                        exception.getMessage()
                );
            }
        }

        if (cadastroConta.possuiVinculoSocialPendente()) {
            adicionarIdentidadeSocialPendente(
                    identidades,
                    new IdentidadeFederadaKeycloak(
                            resolverProvedorVinculoSocialPendente(cadastroConta),
                            cadastroConta.getVinculoSocialPendenteIdentificadorExterno(),
                            cadastroConta.getVinculoSocialPendenteNomeUsuarioExterno()
                    ),
                    null
            );
        }

        return List.copyOf(identidades.values());
    }

    private void adicionarIdentidadeSocialPendente(
            final java.util.LinkedHashMap<String, IdentidadeSocialPendenteFinalizacao> identidades,
            final IdentidadeFederadaKeycloak identidadeFederada,
            final UUID contextoId) {
        String chave = identidadeFederada.provedor().getAliasApi()
                + "::"
                + identidadeFederada.identificadorCanonico();
        identidades.putIfAbsent(
                chave,
                new IdentidadeSocialPendenteFinalizacao(identidadeFederada, contextoId)
        );
    }

    private void materializarVinculoSocialLocalSeNecessario(final CadastroConta cadastroConta,
                                                            final IdentidadeFederadaKeycloak identidadeFederada,
                                                            final OffsetDateTime instante) {
        if (pessoaRepositorio == null
                || perfilIdentidadeRepositorio == null
                || formaAcessoRepositorio == null
                || vinculoSocialRepositorio == null) {
            return;
        }

        Pessoa pessoa = pessoaRepositorio.findById(cadastroConta.getPessoaIdPerfil())
                .orElseThrow(() -> new IllegalStateException("Pessoa não encontrada para materializar o vínculo social."));
        PerfilIdentidade perfil = perfilIdentidadeRepositorio.findBySub(cadastroConta.getSubjectRemoto())
                .orElseThrow(() -> new IllegalStateException("Perfil não encontrado para materializar o vínculo social."));

        reconciliarFormaAcessoSocialLocal(pessoa, identidadeFederada, instante);
        reconciliarVinculoSocialLocal(perfil, identidadeFederada, instante);
    }

    private void reconciliarFormaAcessoSocialLocal(final Pessoa pessoa,
                                                   final IdentidadeFederadaKeycloak identidadeFederada,
                                                   final OffsetDateTime instante) {
        Optional<FormaAcesso> conflito = formaAcessoRepositorio.findByTipoAndProvedorAndIdentificador(
                TipoFormaAcesso.SOCIAL,
                identidadeFederada.provedor().getAliasFormaAcesso(),
                identidadeFederada.identificadorCanonico()
        );
        if (conflito.isPresent() && !Objects.equals(conflito.orElseThrow().getPessoa().getId(), pessoa.getId())) {
            throw new IllegalStateException("Forma de acesso social já vinculada a outra pessoa");
        }

        Optional<FormaAcesso> existente = formaAcessoRepositorio.findByPessoa(pessoa).stream()
                .filter(formaAcesso -> formaAcesso.getTipo() == TipoFormaAcesso.SOCIAL)
                .filter(formaAcesso -> Objects.equals(
                        formaAcesso.getProvedor(),
                        identidadeFederada.provedor().getAliasFormaAcesso()))
                .findFirst();

        if (existente.isPresent()) {
            FormaAcesso formaAcesso = existente.orElseThrow();
            formaAcesso.atualizarIdentificador(identidadeFederada.identificadorCanonico(), false, instante);
            formaAcessoRepositorio.save(formaAcesso);
            return;
        }

        formaAcessoRepositorio.save(new FormaAcesso(
                pessoa,
                TipoFormaAcesso.SOCIAL,
                identidadeFederada.provedor().getAliasFormaAcesso(),
                identidadeFederada.identificadorCanonico(),
                false,
                instante,
                instante
        ));
    }

    private void reconciliarVinculoSocialLocal(final PerfilIdentidade perfil,
                                               final IdentidadeFederadaKeycloak identidadeFederada,
                                               final OffsetDateTime instante) {
        Optional<VinculoSocial> existente = vinculoSocialRepositorio.findByPerfil(perfil).stream()
                .filter(vinculoSocial -> Objects.equals(
                        vinculoSocial.getProvedor(),
                        identidadeFederada.provedor().getAliasApi()))
                .findFirst();

        if (existente.isPresent()) {
            VinculoSocial vinculoSocial = existente.orElseThrow();
            vinculoSocial.atualizarIdentificador(identidadeFederada.identificadorExibicao());
            vinculoSocialRepositorio.save(vinculoSocial);
            return;
        }

        vinculoSocialRepositorio.save(new VinculoSocial(
                perfil,
                identidadeFederada.provedor().getAliasApi(),
                identidadeFederada.identificadorExibicao(),
                instante
        ));
    }

    private void materializarVinculoOrganizacionalSeNecessario(final CadastroConta cadastroConta,
                                                               final OffsetDateTime instante) {
        if (vinculoOrganizacionalRepositorio == null) {
            return;
        }
        if (cadastroConta.getConviteOrganizacionalCodigo() == null
                || cadastroConta.getConviteOrganizacionalCodigo().isBlank()) {
            return;
        }
        if (cadastroConta.getPerfilSistemaId() == null || cadastroConta.getPerfilSistemaId().isBlank()) {
            throw new IllegalStateException("Usuario do perfil nao encontrado para materializar o vinculo organizacional.");
        }
        if (cadastroConta.getPessoaIdPerfil() == null) {
            throw new IllegalStateException("Pessoa do perfil nao encontrada para materializar o vinculo organizacional.");
        }

        boolean existe = vinculoOrganizacionalRepositorio.findByOrganizacaoIdAndPerfilSistemaId(
                cadastroConta.getConviteOrganizacionalOrganizacaoId(),
                cadastroConta.getPerfilSistemaId()
        ).isPresent();
        if (!existe) {
            vinculoOrganizacionalRepositorio.save(new VinculoOrganizacional(
                    cadastroConta.getCadastroId(),
                    cadastroConta.getPessoaIdPerfil(),
                    cadastroConta.getPerfilSistemaId(),
                    cadastroConta.getConviteOrganizacionalOrganizacaoId(),
                    cadastroConta.getConviteOrganizacionalNomeOrganizacao(),
                    cadastroConta.getConviteOrganizacionalCodigo(),
                    cadastroConta.getConviteOrganizacionalEmailConvidado(),
                    cadastroConta.isConviteOrganizacionalExigeContaSeparada(),
                    instante
            ));
        }

        if (conviteOrganizacionalRepositorio != null) {
            conviteOrganizacionalRepositorio.findByCodigoIgnoreCase(cadastroConta.getConviteOrganizacionalCodigo())
                    .filter(convite -> convite.getStatus() == StatusConviteOrganizacional.ATIVO)
                    .ifPresent(convite -> {
                        convite.marcarConsumido();
                        conviteOrganizacionalRepositorio.save(convite);
                    });
        }
    }

    private record IdentidadeSocialPendenteFinalizacao(
            IdentidadeFederadaKeycloak identidadeFederada,
            UUID contextoId
    ) {
    }

    private void notificarTentativaCadastroContaExistente(final String emailNormalizado) {
        try {
            canalNotificacaoTentativaCadastroEmail.notificar(emailNormalizado);
        } catch (RuntimeException ex) {
            LOGGER.warn("Falha ao enviar o aviso de tentativa de cadastro para {}", emailNormalizado, ex);
        }
    }

    private FluxoPublicoException traduzirFalhaEnvioCodigoCadastro(final CadastroConta cadastroConta,
                                                                   final EntregaEmailException excecao) {
        LOGGER.warn(
                "cadastro_publico_envio_email_falhou codigo={} cadastroId={} subjectRemoto={} sistema={}",
                excecao.getCodigo(),
                cadastroConta.getCadastroId(),
                cadastroConta.getSubjectRemoto(),
                cadastroConta.getSistemaSolicitante(),
                excecao
        );
        if (auditoriaService != null) {
            auditoriaService.registrarEvento(
                    "CADASTRO_EMAIL_FALHA",
                    cadastroConta.getSubjectRemoto(),
                    "codigo=" + excecao.getCodigo()
                            + ";cadastroId=" + cadastroConta.getCadastroId()
                            + ";sistema=" + Objects.requireNonNullElse(cadastroConta.getSistemaSolicitante(), "")
            );
        }
        return new FluxoPublicoException(
                HttpStatus.SERVICE_UNAVAILABLE,
                excecao.getCodigo(),
                excecao.getMensagemPublica()
        );
    }

    private boolean cadastroPendenteEmail(final CadastroConta cadastroConta) {
        return cadastroConta != null
                && (!cadastroConta.emailJaConfirmado() || !cadastroConta.etapaTelefoneConcluida());
    }

    private String resolverProximoPasso(final CadastroConta cadastroConta) {
        if (!cadastroConta.emailJaConfirmado()) {
            return "VALIDAR_EMAIL";
        }
        if (!cadastroConta.etapaTelefoneConcluida()) {
            return "VALIDAR_TELEFONE";
        }
        return "LOGIN";
    }

    private void removerCadastroPendente(final CadastroConta cadastroConta) {
        clienteAdministracaoCadastroKeycloak.removerUsuarioPendente(cadastroConta.getSubjectRemoto());
        cadastroContaRepositorio.delete(cadastroConta);
        removerCadastroSeConfigurado(cadastroConta.getCadastroId());
    }

    private boolean ehFluxoCadastroPublico(final CadastroConta cadastroConta) {
        return cadastroConta.getUsuario() != null && !cadastroConta.getUsuario().isBlank();
    }

    private String gerarCodigoNumerico() {
        int limite = (int) Math.pow(10, dispositivoProperties.getCodigo().getTamanho());
        String formato = "%0" + dispositivoProperties.getCodigo().getTamanho() + "d";
        return formato.formatted(SECURE_RANDOM.nextInt(limite));
    }

    private String hashCodigoEmail(final String codigoClaro,
                                   final String emailPrincipal,
                                   final String subjectRemoto) {
        String material = obrigatorio(codigoClaro, "codigoClaro")
                + "|" + obrigatorio(emailPrincipal, "emailPrincipal")
                + "|" + obrigatorio(subjectRemoto, "subjectRemoto")
                + "|CADASTRO_EMAIL";
        try {
            Mac mac = Mac.getInstance(HMAC_ALG);
            mac.init(new SecretKeySpec(
                    dispositivoProperties.getCodigo().getSegredoHmac().getBytes(StandardCharsets.UTF_8),
                    HMAC_ALG
            ));
            return hexFormat.formatHex(mac.doFinal(material.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Falha ao calcular o hash do código de confirmação do cadastro.", ex);
        }
    }

    private String hashCodigoTelefone(final String codigoClaro,
                                      final String telefonePrincipal,
                                      final String subjectRemoto) {
        String material = obrigatorio(codigoClaro, "codigoClaro")
                + "|" + obrigatorio(telefonePrincipal, "telefonePrincipal")
                + "|" + obrigatorio(subjectRemoto, "subjectRemoto")
                + "|CADASTRO_TELEFONE";
        try {
            Mac mac = Mac.getInstance(HMAC_ALG);
            mac.init(new SecretKeySpec(
                    dispositivoProperties.getCodigo().getSegredoHmac().getBytes(StandardCharsets.UTF_8),
                    HMAC_ALG
            ));
            return hexFormat.formatHex(mac.doFinal(material.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Falha ao calcular o hash do código de confirmação do telefone.", ex);
        }
    }

    private static String obrigatorio(final String valor, final String campo) {
        return obrigatorio(valor, campo, null);
    }

    private static String obrigatorio(final String valor, final String campo, final String mensagem) {
        if (valor == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    mensagem == null ? campo + " é obrigatório." : mensagem
            );
        }
        String normalizado = valor.trim();
        if (normalizado.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    mensagem == null ? campo + " é obrigatório." : mensagem
            );
        }
        return normalizado;
    }

    private static String normalizarOpcional(final String valor) {
        if (valor == null) {
            return null;
        }
        String normalizado = valor.trim();
        return normalizado.isBlank() ? null : normalizado;
    }

    private void sincronizarCadastroSeConfigurado(final CadastroConta cadastroConta) {
        if (sincronizacaoModeloMultiappService != null) {
            sincronizacaoModeloMultiappService.sincronizarCadastro(cadastroConta);
        }
    }

    private void removerCadastroSeConfigurado(final UUID cadastroId) {
        if (sincronizacaoModeloMultiappService != null) {
            sincronizacaoModeloMultiappService.removerCadastro(cadastroId);
        }
    }

    private static String normalizarUsuarioOpcional(final String valor) {
        String normalizado = normalizarOpcional(valor);
        return normalizado == null ? null : normalizado.toLowerCase(Locale.ROOT);
    }

    private static final class ClienteContextoPessoaPerfilSistemaLegado implements ClienteContextoPessoaPerfilSistema {

        private final FormaAcessoRepositorio formaAcessoRepositorio;
        private final ProvisionamentoIdentidadeService provisionamentoIdentidadeService;

        private ClienteContextoPessoaPerfilSistemaLegado(final FormaAcessoRepositorio formaAcessoRepositorio,
                                                  final ProvisionamentoIdentidadeService provisionamentoIdentidadeService) {
            this.formaAcessoRepositorio = formaAcessoRepositorio;
            this.provisionamentoIdentidadeService = provisionamentoIdentidadeService;
        }

        @Override
        public Optional<ContextoPessoaPerfilSistema> buscarPorPessoaId(final Long pessoaId) {
            return Optional.empty();
        }

        @Override
        public Optional<ContextoPessoaPerfilSistema> buscarPorSub(final String sub) {
            return provisionamentoIdentidadeService.localizarPessoaPorSub(sub)
                    .map(pessoa -> new ContextoPessoaPerfilSistema(
                            pessoa.getId(),
                            pessoa.getSub(),
                            pessoa.getEmail(),
                            pessoa.getNome(),
                            null,
                            null));
        }

        @Override
        public Optional<ContextoPessoaPerfilSistema> buscarPorEmail(final String email) {
            return formaAcessoRepositorio.findByTipoAndProvedorAndIdentificador(
                            TipoFormaAcesso.EMAIL_SENHA, "EMAIL", email)
                    .map(formaAcesso -> formaAcesso.getPessoa())
                    .map(pessoa -> new ContextoPessoaPerfilSistema(
                            pessoa.getId(),
                            pessoa.getSub(),
                            pessoa.getEmail(),
                            pessoa.getNome(),
                            null,
                            null));
        }

        @Override
        public Optional<ContextoPessoaPerfilSistema> buscarPorIdentificadorPublicoSistema(
                final String identificadorPublicoSistema) {
            return Optional.empty();
        }
    }
}
