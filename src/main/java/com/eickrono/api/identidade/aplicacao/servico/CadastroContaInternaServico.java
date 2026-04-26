package com.eickrono.api.identidade.aplicacao.servico;

import com.eickrono.api.identidade.aplicacao.excecao.FluxoPublicoException;
import com.eickrono.api.identidade.aplicacao.modelo.CadastroInternoRealizado;
import com.eickrono.api.identidade.aplicacao.modelo.CadastroKeycloakProvisionado;
import com.eickrono.api.identidade.aplicacao.modelo.ConfirmacaoEmailCadastroInternoRealizada;
import com.eickrono.api.identidade.aplicacao.modelo.ConfirmacaoEmailCadastroPublicoRealizada;
import com.eickrono.api.identidade.aplicacao.modelo.ConviteOrganizacionalValidado;
import com.eickrono.api.identidade.aplicacao.modelo.ContextoPessoaPerfil;
import com.eickrono.api.identidade.aplicacao.modelo.IdentidadeFederadaKeycloak;
import com.eickrono.api.identidade.aplicacao.modelo.ProvisionamentoPerfilRealizado;
import com.eickrono.api.identidade.aplicacao.modelo.VinculoSocialPendenteCadastro;
import com.eickrono.api.identidade.dominio.modelo.CadastroConta;
import com.eickrono.api.identidade.dominio.modelo.CanalValidacaoTelefoneCadastro;
import com.eickrono.api.identidade.dominio.modelo.FormaAcesso;
import com.eickrono.api.identidade.dominio.modelo.PerfilIdentidade;
import com.eickrono.api.identidade.dominio.modelo.Pessoa;
import com.eickrono.api.identidade.dominio.modelo.ProvedorVinculoSocial;
import com.eickrono.api.identidade.dominio.modelo.SexoPessoaCadastro;
import com.eickrono.api.identidade.dominio.modelo.StatusCadastroConta;
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
    private final ClienteContextoPessoaPerfil clienteContextoPessoaPerfil;
    private final ClienteAdministracaoCadastroKeycloak clienteAdministracaoCadastroKeycloak;
    private final PessoaRepositorio pessoaRepositorio;
    private final PerfilIdentidadeRepositorio perfilIdentidadeRepositorio;
    private final FormaAcessoRepositorio formaAcessoRepositorio;
    private final VinculoSocialRepositorio vinculoSocialRepositorio;
    private final ConviteOrganizacionalRepositorio conviteOrganizacionalRepositorio;
    private final VinculoOrganizacionalRepositorio vinculoOrganizacionalRepositorio;
    private final ProvisionadorPerfilDominioServico provisionadorPerfilDominioServico;
    private final CanalEnvioCodigoCadastroEmail canalEnvioCodigoCadastroEmail;
    private final CanalEnvioCodigoCadastroTelefone canalEnvioCodigoCadastroTelefone;
    private final CanalNotificacaoTentativaCadastroEmail canalNotificacaoTentativaCadastroEmail;
    private final DispositivoProperties dispositivoProperties;
    private final Clock clock;
    private final SincronizacaoModeloMultiappService sincronizacaoModeloMultiappService;
    private ProvisionamentoIdentidadeService provisionamentoIdentidadeServiceCompat;
    private final HexFormat hexFormat = HexFormat.of();

    @Autowired
    public CadastroContaInternaServico(final CadastroContaRepositorio cadastroContaRepositorio,
                                       final ClienteContextoPessoaPerfil clienteContextoPessoaPerfil,
                                       final ClienteAdministracaoCadastroKeycloak clienteAdministracaoCadastroKeycloak,
                                       final PessoaRepositorio pessoaRepositorio,
                                       final PerfilIdentidadeRepositorio perfilIdentidadeRepositorio,
                                       final FormaAcessoRepositorio formaAcessoRepositorio,
                                       final VinculoSocialRepositorio vinculoSocialRepositorio,
                                       final ConviteOrganizacionalRepositorio conviteOrganizacionalRepositorio,
                                       final VinculoOrganizacionalRepositorio vinculoOrganizacionalRepositorio,
                                       final ProvisionadorPerfilDominioServico provisionadorPerfilDominioServico,
                                       final CanalEnvioCodigoCadastroEmail canalEnvioCodigoCadastroEmail,
                                       final CanalEnvioCodigoCadastroTelefone canalEnvioCodigoCadastroTelefone,
                                       final CanalNotificacaoTentativaCadastroEmail canalNotificacaoTentativaCadastroEmail,
                                       final DispositivoProperties dispositivoProperties,
                                       final Clock clock,
                                       final SincronizacaoModeloMultiappService sincronizacaoModeloMultiappService) {
        this(
                cadastroContaRepositorio,
                clienteContextoPessoaPerfil,
                clienteAdministracaoCadastroKeycloak,
                pessoaRepositorio,
                perfilIdentidadeRepositorio,
                formaAcessoRepositorio,
                vinculoSocialRepositorio,
                conviteOrganizacionalRepositorio,
                vinculoOrganizacionalRepositorio,
                provisionadorPerfilDominioServico,
                canalEnvioCodigoCadastroEmail,
                canalEnvioCodigoCadastroTelefone,
                canalNotificacaoTentativaCadastroEmail,
                dispositivoProperties,
                clock,
                sincronizacaoModeloMultiappService,
                true
        );
    }

    public CadastroContaInternaServico(final CadastroContaRepositorio cadastroContaRepositorio,
                                       final FormaAcessoRepositorio formaAcessoRepositorio,
                                       final ClienteAdministracaoCadastroKeycloak clienteAdministracaoCadastroKeycloak,
                                       final ProvisionamentoIdentidadeService provisionamentoIdentidadeService,
                                       final CanalEnvioCodigoCadastroEmail canalEnvioCodigoCadastroEmail,
                                       final CanalEnvioCodigoCadastroTelefone canalEnvioCodigoCadastroTelefone,
                                       final DispositivoProperties dispositivoProperties,
                                       final Clock clock) {
        this(
                cadastroContaRepositorio,
                new ClienteContextoPessoaPerfilLegado(
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
                canalEnvioCodigoCadastroEmail,
                canalEnvioCodigoCadastroTelefone,
                email -> {
                },
                dispositivoProperties,
                clock,
                null,
                false
        );
        this.provisionamentoIdentidadeServiceCompat = provisionamentoIdentidadeService;
    }

    public CadastroContaInternaServico(final CadastroContaRepositorio cadastroContaRepositorio,
                                       final ClienteContextoPessoaPerfil clienteContextoPessoaPerfil,
                                       final ClienteAdministracaoCadastroKeycloak clienteAdministracaoCadastroKeycloak,
                                       final PessoaRepositorio pessoaRepositorio,
                                       final PerfilIdentidadeRepositorio perfilIdentidadeRepositorio,
                                       final FormaAcessoRepositorio formaAcessoRepositorio,
                                       final VinculoSocialRepositorio vinculoSocialRepositorio,
                                       final ConviteOrganizacionalRepositorio conviteOrganizacionalRepositorio,
                                       final VinculoOrganizacionalRepositorio vinculoOrganizacionalRepositorio,
                                       final ProvisionadorPerfilDominioServico provisionadorPerfilDominioServico,
                                       final CanalEnvioCodigoCadastroEmail canalEnvioCodigoCadastroEmail,
                                       final CanalEnvioCodigoCadastroTelefone canalEnvioCodigoCadastroTelefone,
                                       final CanalNotificacaoTentativaCadastroEmail canalNotificacaoTentativaCadastroEmail,
                                       final DispositivoProperties dispositivoProperties,
                                       final Clock clock) {
        this(
                cadastroContaRepositorio,
                clienteContextoPessoaPerfil,
                clienteAdministracaoCadastroKeycloak,
                pessoaRepositorio,
                perfilIdentidadeRepositorio,
                formaAcessoRepositorio,
                vinculoSocialRepositorio,
                conviteOrganizacionalRepositorio,
                vinculoOrganizacionalRepositorio,
                provisionadorPerfilDominioServico,
                canalEnvioCodigoCadastroEmail,
                canalEnvioCodigoCadastroTelefone,
                canalNotificacaoTentativaCadastroEmail,
                dispositivoProperties,
                clock,
                null,
                true
        );
    }

    private CadastroContaInternaServico(final CadastroContaRepositorio cadastroContaRepositorio,
                                        final ClienteContextoPessoaPerfil clienteContextoPessoaPerfil,
                                        final ClienteAdministracaoCadastroKeycloak clienteAdministracaoCadastroKeycloak,
                                        final PessoaRepositorio pessoaRepositorio,
                                        final PerfilIdentidadeRepositorio perfilIdentidadeRepositorio,
                                        final FormaAcessoRepositorio formaAcessoRepositorio,
                                        final VinculoSocialRepositorio vinculoSocialRepositorio,
                                        final ConviteOrganizacionalRepositorio conviteOrganizacionalRepositorio,
                                        final VinculoOrganizacionalRepositorio vinculoOrganizacionalRepositorio,
                                        final ProvisionadorPerfilDominioServico provisionadorPerfilDominioServico,
                                        final CanalEnvioCodigoCadastroEmail canalEnvioCodigoCadastroEmail,
                                        final CanalEnvioCodigoCadastroTelefone canalEnvioCodigoCadastroTelefone,
                                        final CanalNotificacaoTentativaCadastroEmail canalNotificacaoTentativaCadastroEmail,
                                        final DispositivoProperties dispositivoProperties,
                                        final Clock clock,
                                        final SincronizacaoModeloMultiappService sincronizacaoModeloMultiappService,
                                        final boolean exigirProvisionadorPerfil) {
        this.cadastroContaRepositorio = Objects.requireNonNull(cadastroContaRepositorio, "cadastroContaRepositorio é obrigatório");
        this.clienteContextoPessoaPerfil = Objects.requireNonNull(
                clienteContextoPessoaPerfil, "clienteContextoPessoaPerfil é obrigatório");
        this.clienteAdministracaoCadastroKeycloak = Objects.requireNonNull(
                clienteAdministracaoCadastroKeycloak, "clienteAdministracaoCadastroKeycloak é obrigatório");
        this.pessoaRepositorio = pessoaRepositorio;
        this.perfilIdentidadeRepositorio = perfilIdentidadeRepositorio;
        this.formaAcessoRepositorio = formaAcessoRepositorio;
        this.vinculoSocialRepositorio = vinculoSocialRepositorio;
        this.conviteOrganizacionalRepositorio = conviteOrganizacionalRepositorio;
        this.vinculoOrganizacionalRepositorio = vinculoOrganizacionalRepositorio;
        if (exigirProvisionadorPerfil) {
            this.provisionadorPerfilDominioServico = Objects.requireNonNull(
                    provisionadorPerfilDominioServico, "provisionadorPerfilDominioServico é obrigatório");
        } else {
            this.provisionadorPerfilDominioServico = provisionadorPerfilDominioServico;
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
        this.provisionamentoIdentidadeServiceCompat = null;
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
                sistemaSolicitante,
                ipSolicitante,
                userAgentSolicitante
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
                null,
                sistemaSolicitante,
                ipSolicitante,
                userAgentSolicitante
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
                null,
                sistemaSolicitante,
                ipSolicitante,
                userAgentSolicitante
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
                sistemaSolicitante,
                ipSolicitante,
                userAgentSolicitante
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

    public boolean usuarioDisponivelPublico(final String usuario) {
        String usuarioNormalizado = normalizarUsuarioOpcional(usuario);
        if (usuarioNormalizado == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "usuario é obrigatório.");
        }
        return usuarioDisponivelNormalizado(usuarioNormalizado);
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
        String codigoTelefoneClaro = cadastroConta.possuiTelefoneParaValidacao() ? gerarCodigoNumerico() : null;
        cadastroConta.atualizarCodigoEmail(
                hashCodigoEmail(codigoClaro, cadastroConta.getEmailPrincipal(), cadastroConta.getSubjectRemoto()),
                agora,
                agora.plusHours(dispositivoProperties.getCodigo().getExpiracaoHoras()),
                agora
        );
        if (codigoTelefoneClaro != null) {
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
        }
        sincronizarCadastroSeConfigurado(cadastroConta);
        canalEnvioCodigoCadastroEmail.enviar(cadastroConta, codigoClaro);
        if (codigoTelefoneClaro != null) {
            canalEnvioCodigoCadastroTelefone.enviar(cadastroConta, codigoTelefoneClaro);
        }
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
        List<CadastroConta> expirados = cadastroContaRepositorio.findByStatusAndCriadoEmBefore(
                StatusCadastroConta.PENDENTE_EMAIL,
                limite
        );
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
                                                       final String sistemaSolicitante,
                                                       final String ipSolicitante,
                                                       final String userAgentSolicitante) {
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

        validarDuplicidadeUsuario(usuarioNormalizado);
        validarDuplicidadeEmail(emailNormalizado);

        CadastroKeycloakProvisionado cadastroKeycloak = clienteAdministracaoCadastroKeycloak.criarUsuarioPendente(
                nomeNormalizado,
                emailNormalizado,
                senhaNormalizada
        );

        OffsetDateTime agora = OffsetDateTime.now(clock);
        String codigoClaro = gerarCodigoNumerico();
        String codigoTelefoneClaro = telefoneNormalizado == null ? null : gerarCodigoNumerico();
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
                agora
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
        canalEnvioCodigoCadastroEmail.enviar(cadastroConta, codigoClaro);
        if (codigoTelefoneClaro != null) {
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
            return montarRespostaConfirmacao(cadastroConta, cadastroConta.getStatus().name());
        }

        OffsetDateTime agora = OffsetDateTime.now(clock);
        if (cadastroConta.codigoEmailExpirado(agora) || cadastroConta.codigoTelefoneExpirado(agora)) {
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
        if (ehFluxoCadastroPublico(cadastroConta) && cadastroConta.possuiTelefoneParaValidacao()) {
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
        }

        String statusUsuario = "EMAIL_CONFIRMADO";
        if (ehFluxoCadastroPublico(cadastroConta)) {
            ProvisionadorPerfilDominioServico provisionador = Objects.requireNonNull(
                    provisionadorPerfilDominioServico,
                    "provisionadorPerfilDominioServico é obrigatório para o fluxo público"
            );
            ProvisionamentoPerfilRealizado provisionamento = provisionador.provisionarCadastroConfirmado(cadastroConta);
            cadastroConta.definirProvisionamentoPerfil(
                    provisionamento.pessoaId(),
                    provisionamento.usuarioId(),
                    agora
            );
            statusUsuario = provisionamento.statusUsuario();
        } else {
            clienteContextoPessoaPerfil.buscarPorSub(cadastroConta.getSubjectRemoto())
                    .ifPresent(contexto -> {
                        cadastroConta.definirPessoaIdPerfil(contexto.pessoaId(), agora);
                        if (contexto.usuarioId() != null && !contexto.usuarioId().isBlank()) {
                            cadastroConta.definirProvisionamentoPerfil(
                                    contexto.pessoaId(),
                                    contexto.usuarioId(),
                                    agora
                            );
                        }
                    });
            if (provisionamentoIdentidadeServiceCompat != null) {
                provisionamentoIdentidadeServiceCompat.confirmarEmailCadastro(
                        cadastroConta.getSubjectRemoto(),
                        cadastroConta.getEmailPrincipal(),
                        agora
                );
            }
            statusUsuario = clienteContextoPessoaPerfil.buscarPorSub(cadastroConta.getSubjectRemoto())
                    .map(ContextoPessoaPerfil::statusUsuario)
                    .filter(valor -> valor != null && !valor.isBlank())
                    .orElse(statusUsuario);
        }

        if (cadastroConta.possuiVinculoSocialPendente()) {
            clienteAdministracaoCadastroKeycloak.vincularIdentidadeFederada(
                    cadastroConta.getSubjectRemoto(),
                    new IdentidadeFederadaKeycloak(
                            resolverProvedorVinculoSocialPendente(cadastroConta),
                            cadastroConta.getVinculoSocialPendenteIdentificadorExterno(),
                            cadastroConta.getVinculoSocialPendenteNomeUsuarioExterno()
                    )
            );
            materializarVinculoSocialLocalSeNecessario(cadastroConta, agora);
        }
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

        return montarRespostaConfirmacao(cadastroConta, statusUsuario);
    }

    private ConfirmacaoEmailCadastroPublicoRealizada montarRespostaConfirmacao(final CadastroConta cadastroConta,
                                                                               final String statusUsuarioPadrao) {
        ContextoPessoaPerfil contexto = clienteContextoPessoaPerfil.buscarPorSub(cadastroConta.getSubjectRemoto()).orElse(null);
        String usuarioId = cadastroConta.getUsuarioIdPerfil();
        if ((usuarioId == null || usuarioId.isBlank()) && contexto != null) {
            usuarioId = contexto.usuarioId();
        }
        String statusUsuario = contexto == null || contexto.statusUsuario() == null || contexto.statusUsuario().isBlank()
                ? statusUsuarioPadrao
                : contexto.statusUsuario();
        return new ConfirmacaoEmailCadastroPublicoRealizada(
                cadastroConta.getCadastroId(),
                cadastroConta.getSubjectRemoto(),
                cadastroConta.getEmailPrincipal(),
                Objects.requireNonNullElse(usuarioId, ""),
                statusUsuario,
                true,
                true
        );
    }

    private void validarDuplicidadeUsuario(final String usuarioNormalizado) {
        if (usuarioNormalizado == null || usuarioNormalizado.isBlank()) {
            return;
        }
        if (!usuarioDisponivelNormalizado(usuarioNormalizado)) {
            throw FluxoPublicoException.conflito("usuario_indisponivel", "Este usuário não está disponível.");
        }
    }

    private boolean usuarioDisponivelNormalizado(final String usuarioNormalizado) {
        if (cadastroContaRepositorio.findByUsuarioIgnoreCase(usuarioNormalizado).isPresent()) {
            return false;
        }
        return provisionadorPerfilDominioServico == null
                || provisionadorPerfilDominioServico.usuarioDisponivel(usuarioNormalizado);
    }

    private void validarDuplicidadeEmail(final String emailNormalizado) {
        Optional<CadastroConta> cadastroExistente = cadastroContaRepositorio.findByEmailPrincipal(emailNormalizado);
        if (cadastroExistente.isPresent()) {
            throw FluxoPublicoException.conflito(
                    "cadastro_nao_disponivel",
                    "Não foi possível concluir o cadastro com os dados informados."
            );
        }
        clienteContextoPessoaPerfil.buscarPorEmail(emailNormalizado)
                .ifPresent(contexto -> {
                    notificarTentativaCadastroContaExistente(emailNormalizado);
                    throw FluxoPublicoException.conflito(
                            "cadastro_nao_disponivel",
                            "Não foi possível concluir o cadastro com os dados informados."
                    );
                });
    }

    private ProvedorVinculoSocial resolverProvedorVinculoSocialPendente(final CadastroConta cadastroConta) {
        return ProvedorVinculoSocial.fromAlias(cadastroConta.getVinculoSocialPendenteProvedor())
                .orElseThrow(() -> new IllegalStateException("Provedor social pendente inválido para o cadastro."));
    }

    private void materializarVinculoSocialLocalSeNecessario(final CadastroConta cadastroConta,
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
        IdentidadeFederadaKeycloak identidadeFederada = new IdentidadeFederadaKeycloak(
                resolverProvedorVinculoSocialPendente(cadastroConta),
                cadastroConta.getVinculoSocialPendenteIdentificadorExterno(),
                cadastroConta.getVinculoSocialPendenteNomeUsuarioExterno()
        );

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
        if (cadastroConta.getUsuarioIdPerfil() == null || cadastroConta.getUsuarioIdPerfil().isBlank()) {
            throw new IllegalStateException("Usuario do perfil nao encontrado para materializar o vinculo organizacional.");
        }
        if (cadastroConta.getPessoaIdPerfil() == null) {
            throw new IllegalStateException("Pessoa do perfil nao encontrada para materializar o vinculo organizacional.");
        }

        boolean existe = vinculoOrganizacionalRepositorio.findByOrganizacaoIdAndUsuarioIdPerfil(
                cadastroConta.getConviteOrganizacionalOrganizacaoId(),
                cadastroConta.getUsuarioIdPerfil()
        ).isPresent();
        if (!existe) {
            vinculoOrganizacionalRepositorio.save(new VinculoOrganizacional(
                    cadastroConta.getCadastroId(),
                    cadastroConta.getPessoaIdPerfil(),
                    cadastroConta.getUsuarioIdPerfil(),
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

    private void notificarTentativaCadastroContaExistente(final String emailNormalizado) {
        try {
            canalNotificacaoTentativaCadastroEmail.notificar(emailNormalizado);
        } catch (RuntimeException ex) {
            LOGGER.warn("Falha ao enviar o aviso de tentativa de cadastro para {}", emailNormalizado, ex);
        }
    }

    private boolean cadastroPendenteEmail(final CadastroConta cadastroConta) {
        return cadastroConta.getStatus() == StatusCadastroConta.PENDENTE_EMAIL;
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

    private static final class ClienteContextoPessoaPerfilLegado implements ClienteContextoPessoaPerfil {

        private final FormaAcessoRepositorio formaAcessoRepositorio;
        private final ProvisionamentoIdentidadeService provisionamentoIdentidadeService;

        private ClienteContextoPessoaPerfilLegado(final FormaAcessoRepositorio formaAcessoRepositorio,
                                                  final ProvisionamentoIdentidadeService provisionamentoIdentidadeService) {
            this.formaAcessoRepositorio = formaAcessoRepositorio;
            this.provisionamentoIdentidadeService = provisionamentoIdentidadeService;
        }

        @Override
        public Optional<ContextoPessoaPerfil> buscarPorPessoaId(final Long pessoaId) {
            return Optional.empty();
        }

        @Override
        public Optional<ContextoPessoaPerfil> buscarPorSub(final String sub) {
            return provisionamentoIdentidadeService.localizarPessoaPorSub(sub)
                    .map(pessoa -> new ContextoPessoaPerfil(
                            pessoa.getId(),
                            pessoa.getSub(),
                            pessoa.getEmail(),
                            pessoa.getNome(),
                            null,
                            null));
        }

        @Override
        public Optional<ContextoPessoaPerfil> buscarPorEmail(final String email) {
            return formaAcessoRepositorio.findByTipoAndProvedorAndIdentificador(
                            TipoFormaAcesso.EMAIL_SENHA, "EMAIL", email)
                    .map(formaAcesso -> formaAcesso.getPessoa())
                    .map(pessoa -> new ContextoPessoaPerfil(
                            pessoa.getId(),
                            pessoa.getSub(),
                            pessoa.getEmail(),
                            pessoa.getNome(),
                            null,
                            null));
        }

        @Override
        public Optional<ContextoPessoaPerfil> buscarPorUsuario(final String usuario) {
            return Optional.empty();
        }
    }
}
