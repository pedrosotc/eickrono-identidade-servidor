package com.eickrono.api.identidade.apresentacao.api;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.eickrono.api.identidade.aplicacao.excecao.AtestacaoAppInvalidaException;
import com.eickrono.api.identidade.aplicacao.excecao.FluxoPublicoException;
import com.eickrono.api.identidade.aplicacao.modelo.CadastroInternoRealizado;
import com.eickrono.api.identidade.aplicacao.modelo.AvatarCadastroConfirmado;
import com.eickrono.api.identidade.aplicacao.modelo.ConfirmacaoCodigoRecuperacaoSenhaRealizada;
import com.eickrono.api.identidade.aplicacao.modelo.ConfirmacaoEmailCadastroPublicoRealizada;
import com.eickrono.api.identidade.aplicacao.modelo.ContextoPessoaPerfilSistema;
import com.eickrono.api.identidade.aplicacao.modelo.ContextoSolicitacaoFluxoPublico;
import com.eickrono.api.identidade.aplicacao.modelo.ConviteOrganizacionalValidado;
import com.eickrono.api.identidade.aplicacao.modelo.CredencialSocialNativaValidada;
import com.eickrono.api.identidade.aplicacao.modelo.DispositivoSessaoRegistrado;
import com.eickrono.api.identidade.aplicacao.modelo.PerfilSistemaProjetoPorEmailResolvido;
import com.eickrono.api.identidade.aplicacao.modelo.ProjetoFluxoPublicoResolvido;
import com.eickrono.api.identidade.aplicacao.modelo.RecuperacaoSenhaIniciada;
import com.eickrono.api.identidade.aplicacao.modelo.SessaoInternaAutenticada;
import com.eickrono.api.identidade.aplicacao.modelo.StatusCadastroPublico;
import com.eickrono.api.identidade.aplicacao.modelo.VinculoSocialConfirmadoCadastro;
import com.eickrono.api.identidade.aplicacao.servico.AtestacaoAppServico;
import com.eickrono.api.identidade.aplicacao.servico.AutenticacaoSessaoInternaServico;
import com.eickrono.api.identidade.aplicacao.servico.AvaliacaoSegurancaAplicativoService;
import com.eickrono.api.identidade.aplicacao.servico.AvatarSocialProjetoJdbc;
import com.eickrono.api.identidade.aplicacao.servico.CadastroContaInternaServico;
import com.eickrono.api.identidade.aplicacao.servico.ClienteContextoPessoaPerfilSistema;
import com.eickrono.api.identidade.aplicacao.servico.ConviteOrganizacionalService;
import com.eickrono.api.identidade.aplicacao.servico.LocalizadorPerfilSistemaProjetoPorEmailJdbc;
import com.eickrono.api.identidade.aplicacao.servico.RecuperacaoSenhaService;
import com.eickrono.api.identidade.aplicacao.servico.RegistroDispositivoLoginSilenciosoService;
import com.eickrono.api.identidade.aplicacao.servico.ResolvedorProjetoFluxoPublicoJdbc;
import com.eickrono.api.identidade.aplicacao.servico.ValidadorCredencialSocialNativa;
import com.eickrono.api.identidade.apresentacao.dto.fluxo.CadastroApiRequest;
import com.eickrono.api.identidade.apresentacao.dto.fluxo.CadastroApiResposta;
import com.eickrono.api.identidade.apresentacao.dto.fluxo.AvatarCadastroConfirmadoApiRequest;
import com.eickrono.api.identidade.apresentacao.dto.fluxo.ConfirmacaoCodigoRecuperacaoSenhaApiResposta;
import com.eickrono.api.identidade.apresentacao.dto.fluxo.ConfirmacaoEmailCadastroApiResposta;
import com.eickrono.api.identidade.apresentacao.dto.fluxo.ConfirmarCodigoRecuperacaoSenhaApiRequest;
import com.eickrono.api.identidade.apresentacao.dto.fluxo.ConfirmarEmailCadastroApiRequest;
import com.eickrono.api.identidade.apresentacao.dto.fluxo.ConfirmarTelefoneCadastroApiRequest;
import com.eickrono.api.identidade.apresentacao.dto.fluxo.CriarSessaoApiRequest;
import com.eickrono.api.identidade.apresentacao.dto.fluxo.CriarSessaoSocialApiRequest;
import com.eickrono.api.identidade.apresentacao.dto.fluxo.DisponibilidadeUsuarioCadastroApiResposta;
import com.eickrono.api.identidade.apresentacao.dto.fluxo.IniciarRecuperacaoSenhaApiRequest;
import com.eickrono.api.identidade.apresentacao.dto.fluxo.RecuperacaoSenhaApiResposta;
import com.eickrono.api.identidade.apresentacao.dto.fluxo.RedefinirSenhaRecuperacaoApiRequest;
import com.eickrono.api.identidade.apresentacao.dto.fluxo.RenovarSessaoApiRequest;
import com.eickrono.api.identidade.apresentacao.dto.fluxo.SessaoApiResposta;
import com.eickrono.api.identidade.apresentacao.dto.fluxo.StatusCadastroPublicoApiResposta;
import com.eickrono.api.identidade.apresentacao.dto.fluxo.VinculoSocialConfirmadoApiRequest;
import com.eickrono.api.identidade.dominio.modelo.CadastroConta;
import com.eickrono.api.identidade.dominio.modelo.FormaAcesso;
import com.eickrono.api.identidade.dominio.modelo.TipoFormaAcesso;
import com.eickrono.api.identidade.dominio.repositorio.FormaAcessoRepositorio;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/publica")
public class FluxoPublicoController {

    private static final Logger LOGGER = LoggerFactory.getLogger(FluxoPublicoController.class);

    private static final String ERRO_KEYCLOAK_CONTA_DESABILITADA = "Account disabled";
    private static final String ERRO_KEYCLOAK_CONTA_INCOMPLETA = "Account is not fully set up";
    private static final String ERRO_KEYCLOAK_CREDENCIAIS_INVALIDAS = "Invalid user credentials";
    private static final String STATUS_PENDENTE_EMAIL = "PENDENTE_EMAIL";
    private static final String STATUS_LIBERADO = "LIBERADO";

    private final CadastroContaInternaServico cadastroContaInternaServico;
    private final AtestacaoAppServico atestacaoAppServico;
    private final AvaliacaoSegurancaAplicativoService avaliacaoSegurancaAplicativoService;
    private final AutenticacaoSessaoInternaServico autenticacaoSessaoInternaServico;
    private final ClienteContextoPessoaPerfilSistema clienteContextoPessoaPerfilSistema;
    private final ConviteOrganizacionalService conviteOrganizacionalService;
    private final RecuperacaoSenhaService recuperacaoSenhaService;
    private final RegistroDispositivoLoginSilenciosoService registroDispositivoLoginSilenciosoService;
    private final ResolvedorProjetoFluxoPublicoJdbc resolvedorProjetoFluxoPublico;
    private final LocalizadorPerfilSistemaProjetoPorEmailJdbc localizadorPerfilSistemaProjetoPorEmail;
    private final FormaAcessoRepositorio formaAcessoRepositorio;
    private final ValidadorCredencialSocialNativa validadorCredencialSocialNativa;
    private final AvatarSocialProjetoJdbc avatarSocialProjetoJdbc;

    public FluxoPublicoController(final CadastroContaInternaServico cadastroContaInternaServico,
                                  final AtestacaoAppServico atestacaoAppServico,
                                  final AvaliacaoSegurancaAplicativoService avaliacaoSegurancaAplicativoService,
                                  final AutenticacaoSessaoInternaServico autenticacaoSessaoInternaServico,
                                  final ClienteContextoPessoaPerfilSistema clienteContextoPessoaPerfilSistema,
                                  final ConviteOrganizacionalService conviteOrganizacionalService,
                                  final RecuperacaoSenhaService recuperacaoSenhaService,
                                  final RegistroDispositivoLoginSilenciosoService registroDispositivoLoginSilenciosoService,
                                  final ResolvedorProjetoFluxoPublicoJdbc resolvedorProjetoFluxoPublico,
                                  final LocalizadorPerfilSistemaProjetoPorEmailJdbc localizadorPerfilSistemaProjetoPorEmail,
                                  final FormaAcessoRepositorio formaAcessoRepositorio,
                                  final ValidadorCredencialSocialNativa validadorCredencialSocialNativa,
                                  final AvatarSocialProjetoJdbc avatarSocialProjetoJdbc) {
        this.cadastroContaInternaServico = Objects.requireNonNull(
                cadastroContaInternaServico, "cadastroContaInternaServico é obrigatório");
        this.atestacaoAppServico = Objects.requireNonNull(atestacaoAppServico, "atestacaoAppServico é obrigatório");
        this.avaliacaoSegurancaAplicativoService = Objects.requireNonNull(
                avaliacaoSegurancaAplicativoService, "avaliacaoSegurancaAplicativoService é obrigatório");
        this.autenticacaoSessaoInternaServico = Objects.requireNonNull(
                autenticacaoSessaoInternaServico, "autenticacaoSessaoInternaServico é obrigatório");
        this.clienteContextoPessoaPerfilSistema = Objects.requireNonNull(
                clienteContextoPessoaPerfilSistema, "clienteContextoPessoaPerfilSistema é obrigatório");
        this.conviteOrganizacionalService = Objects.requireNonNull(
                conviteOrganizacionalService, "conviteOrganizacionalService é obrigatório");
        this.recuperacaoSenhaService = Objects.requireNonNull(
                recuperacaoSenhaService, "recuperacaoSenhaService é obrigatório");
        this.registroDispositivoLoginSilenciosoService = Objects.requireNonNull(
                registroDispositivoLoginSilenciosoService, "registroDispositivoLoginSilenciosoService é obrigatório");
        this.resolvedorProjetoFluxoPublico = Objects.requireNonNull(
                resolvedorProjetoFluxoPublico, "resolvedorProjetoFluxoPublico é obrigatório");
        this.localizadorPerfilSistemaProjetoPorEmail = Objects.requireNonNull(
                localizadorPerfilSistemaProjetoPorEmail,
                "localizadorPerfilSistemaProjetoPorEmail é obrigatório"
        );
        this.formaAcessoRepositorio = Objects.requireNonNull(
                formaAcessoRepositorio,
                "formaAcessoRepositorio é obrigatório"
        );
        this.validadorCredencialSocialNativa = Objects.requireNonNull(
                validadorCredencialSocialNativa,
                "validadorCredencialSocialNativa é obrigatório"
        );
        this.avatarSocialProjetoJdbc = Objects.requireNonNull(
                avatarSocialProjetoJdbc,
                "avatarSocialProjetoJdbc é obrigatório"
        );
    }

    @PostMapping("/cadastros")
    @ResponseStatus(HttpStatus.CREATED)
    public CadastroApiResposta criarCadastro(@Valid @RequestBody final CadastroApiRequest requisicao,
                                             final HttpServletRequest servletRequest) {
        return processarCriacaoCadastro(requisicao, servletRequest, null);
    }

    @PostMapping("/convites/{codigo}/cadastros")
    @ResponseStatus(HttpStatus.CREATED)
    public CadastroApiResposta criarCadastroPorConvite(@PathVariable final String codigo,
                                                       @Valid @RequestBody final CadastroApiRequest requisicao,
                                                       final HttpServletRequest servletRequest) {
        ConviteOrganizacionalValidado convite = conviteOrganizacionalService.consultarPublico(codigo);
        validarEmailCompativelComConvite(requisicao, convite);
        return processarCriacaoCadastro(requisicao, servletRequest, convite);
    }

    private CadastroApiResposta processarCriacaoCadastro(final CadastroApiRequest requisicao,
                                                         final HttpServletRequest servletRequest,
                                                         final ConviteOrganizacionalValidado conviteOrganizacional) {
        final String emailMascarado = mascararIdentificador(requisicao.emailPrincipal());
        final String usuarioMascarado = mascararIdentificador(requisicao.usuario());
        final String telefoneMascarado = mascararIdentificador(Objects.requireNonNullElse(requisicao.telefone(), ""));
        LOGGER.info(
                "cadastro_publico_recebido usuario={} email={} telefone={} aplicacaoId={} plataforma={} tipoPessoa={} ip={}",
                usuarioMascarado,
                emailMascarado,
                telefoneMascarado,
                requisicao.aplicacaoId(),
                requisicao.plataformaApp(),
                requisicao.tipoPessoa(),
                extrairIp(servletRequest)
        );
        try {
            validarRegrasCadastro(requisicao);
            List<VinculoSocialConfirmadoCadastro> vinculosSociaisConfirmados =
                    resolverVinculosSociaisConfirmadosDoCadastro(requisicao);
            List<AvatarCadastroConfirmado> avataresConfirmados =
                    resolverAvataresConfirmadosDoCadastro(requisicao);
            LOGGER.info(
                    "qa_cadastro_publico_payload_social_avatar_recebido usuario={} email={} vinculosSociaisConfirmados={} vinculosAvatarPreferidos={} avataresCadastroConfirmados={} avataresPreferidos={}",
                    usuarioMascarado,
                    emailMascarado,
                    vinculosSociaisConfirmados.size(),
                    vinculosSociaisConfirmados.stream()
                            .filter(VinculoSocialConfirmadoCadastro::avatarPreferido)
                            .count(),
                    avataresConfirmados.size(),
                    avataresConfirmados.stream().filter(AvatarCadastroConfirmado::preferido).count()
            );
            atestacaoAppServico.validarComprovante(requisicao.atestacao().paraEntrada());
            avaliacaoSegurancaAplicativoService.avaliar(
                    "CADASTRO",
                    requisicao.aplicacaoId(),
                    requisicao.plataformaApp().name(),
                    requisicao.segurancaAplicativo(),
                    requisicao.emailPrincipal()
            );
            CadastroInternoRealizado cadastro = cadastroContaInternaServico.cadastrarPublico(
                    requisicao.tipoPessoa(),
                    requisicao.nomeCompleto(),
                    requisicao.nomeFantasia(),
                    requisicao.usuario(),
                    requisicao.sexo(),
                    requisicao.paisNascimento(),
                    requisicao.dataNascimento(),
                    requisicao.emailPrincipal(),
                    requisicao.telefone(),
                    requisicao.tipoValidacaoTelefone(),
                    requisicao.senha(),
                    conviteOrganizacional,
                    requisicao.aplicacaoId(),
                    requisicao.aplicacaoId(),
                    extrairIp(servletRequest),
                    servletRequest.getHeader("User-Agent"),
                    construirContextoSolicitacao(requisicao),
                    vinculosSociaisConfirmados,
                    avataresConfirmados
            );
            LOGGER.info(
                    "cadastro_publico_concluido cadastroId={} usuario={} email={} status={} proximoPasso={}",
                    cadastro.cadastroId(),
                    usuarioMascarado,
                    emailMascarado,
                    STATUS_PENDENTE_EMAIL,
                    "VALIDAR_CONTATOS"
            );
            return new CadastroApiResposta(
                    cadastro.cadastroId().toString(),
                    "",
                    STATUS_PENDENTE_EMAIL,
                    cadastro.emailPrincipal(),
                    Objects.requireNonNullElse(requisicao.telefone(), ""),
                    cadastro.verificacaoEmailObrigatoria(),
                    "VALIDAR_CONTATOS"
            );
        } catch (FluxoPublicoException exception) {
            LOGGER.warn(
                    "cadastro_publico_rejeitado codigo={} status={} usuario={} email={} detalhes={}",
                    exception.getCodigo(),
                    exception.getStatus().value(),
                    usuarioMascarado,
                    emailMascarado,
                    exception.getDetalhes()
            );
            throw exception;
        } catch (ResponseStatusException exception) {
            LOGGER.warn(
                    "cadastro_publico_invalido status={} motivo={} usuario={} email={}",
                    exception.getStatusCode().value(),
                    Objects.requireNonNullElse(exception.getReason(), ""),
                    usuarioMascarado,
                    emailMascarado
            );
            throw exception;
        }
    }

    @GetMapping("/cadastros/usuarios/disponibilidade")
    public DisponibilidadeUsuarioCadastroApiResposta consultarDisponibilidadeUsuario(
            @RequestParam final String usuario,
            @RequestParam final String aplicacaoId) {
        String usuarioNormalizado = Objects.requireNonNull(usuario, "usuario é obrigatório")
                .trim()
                .toLowerCase(Locale.ROOT);
        String aplicacaoIdNormalizado = Objects.requireNonNull(aplicacaoId, "aplicacaoId é obrigatório")
                .trim()
                .toLowerCase(Locale.ROOT);
        if (usuarioNormalizado.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "usuario é obrigatório.");
        }
        if (aplicacaoIdNormalizado.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "aplicacaoId é obrigatório.");
        }
        return new DisponibilidadeUsuarioCadastroApiResposta(
                usuarioNormalizado,
                cadastroContaInternaServico.identificadorPublicoSistemaDisponivelPublico(
                        usuarioNormalizado,
                        aplicacaoIdNormalizado
                )
        );
    }

    @PostMapping("/cadastros/{cadastroId}/confirmacoes/email")
    public ConfirmacaoEmailCadastroApiResposta confirmarEmailCadastro(@PathVariable final String cadastroId,
                                                                     @Valid @RequestBody
                                                                     final ConfirmarEmailCadastroApiRequest requisicao) {
        ConfirmacaoEmailCadastroPublicoRealizada confirmacao = cadastroContaInternaServico.confirmarEmailPublico(
                parseCadastroId(cadastroId),
                requisicao.codigo(),
                requisicao.codigoTelefone()
        );
        return new ConfirmacaoEmailCadastroApiResposta(
                confirmacao.cadastroId().toString(),
                confirmacao.perfilSistemaId(),
                confirmacao.statusPerfilSistema(),
                confirmacao.emailPrincipal(),
                confirmacao.emailConfirmado(),
                confirmacao.telefoneConfirmado(),
                confirmacao.telefoneObrigatorio(),
                confirmacao.podeAutenticar(),
                confirmacao.proximoPasso()
        );
    }

    @PostMapping("/cadastros/{cadastroId}/confirmacoes/telefone")
    public ConfirmacaoEmailCadastroApiResposta confirmarTelefoneCadastro(@PathVariable final String cadastroId,
                                                                        @Valid @RequestBody
                                                                        final ConfirmarTelefoneCadastroApiRequest requisicao) {
        ConfirmacaoEmailCadastroPublicoRealizada confirmacao = cadastroContaInternaServico.confirmarTelefonePublico(
                parseCadastroId(cadastroId),
                requisicao.codigo()
        );
        return new ConfirmacaoEmailCadastroApiResposta(
                confirmacao.cadastroId().toString(),
                confirmacao.perfilSistemaId(),
                confirmacao.statusPerfilSistema(),
                confirmacao.emailPrincipal(),
                confirmacao.emailConfirmado(),
                confirmacao.telefoneConfirmado(),
                confirmacao.telefoneObrigatorio(),
                confirmacao.podeAutenticar(),
                confirmacao.proximoPasso()
        );
    }

    @PostMapping("/cadastros/{cadastroId}/senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void definirSenhaCadastroPendente(@PathVariable final String cadastroId,
                                             @Valid @RequestBody final RedefinirSenhaRecuperacaoApiRequest requisicao) {
        cadastroContaInternaServico.definirSenhaCadastroPendentePublico(
                parseCadastroId(cadastroId),
                requisicao.senha(),
                requisicao.confirmacaoSenha()
        );
    }

    @PostMapping("/cadastros/{cadastroId}/confirmacoes/email/reenvio")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void reenviarConfirmacaoEmailCadastro(@PathVariable final String cadastroId) {
        cadastroContaInternaServico.reenviarCodigoEmail(parseCadastroId(cadastroId));
    }

    @PostMapping("/cadastros/{cadastroId}/confirmacoes/telefone/reenvio")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void reenviarConfirmacaoTelefoneCadastro(@PathVariable final String cadastroId) {
        cadastroContaInternaServico.reenviarCodigoTelefone(parseCadastroId(cadastroId));
    }

    @GetMapping("/cadastros/{cadastroId}/status")
    public StatusCadastroPublicoApiResposta consultarStatusCadastro(@PathVariable final String cadastroId) {
        StatusCadastroPublico statusCadastro = cadastroContaInternaServico.consultarStatusCadastroPublico(
                parseCadastroId(cadastroId)
        );
        return new StatusCadastroPublicoApiResposta(
                statusCadastro.cadastroId().toString(),
                statusCadastro.emailPrincipal(),
                statusCadastro.telefonePrincipal(),
                statusCadastro.emailConfirmado(),
                statusCadastro.telefoneConfirmado(),
                statusCadastro.telefoneObrigatorio(),
                statusCadastro.liberadoParaLogin(),
                statusCadastro.proximoPasso()
        );
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/cadastros/{cadastroId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelarCadastro(@PathVariable final String cadastroId) {
        cadastroContaInternaServico.cancelarCadastroPendentePublico(parseCadastroId(cadastroId));
    }

    @PostMapping("/sessoes")
    public SessaoApiResposta criarSessao(@Valid @RequestBody final CriarSessaoApiRequest requisicao,
                                         final HttpServletRequest servletRequest) {
        String loginNormalizado = requisicao.login().trim().toLowerCase(Locale.ROOT);
        LoginPublicoResolvido loginResolvido = resolverLoginPublico(loginNormalizado);
        ProjetoFluxoPublicoResolvido projeto = resolvedorProjetoFluxoPublico.resolverAtivo(requisicao.aplicacaoId());
        String loginMascarado = mascararIdentificador(loginNormalizado);
        String instalacaoMascarada = mascararIdentificador(requisicao.dispositivo().identificadorInstalacao());
        String identificadorAplicativo = resolverIdentificadorAplicativo(requisicao);
        LOGGER.info(
                "login_publico_recebido login={} aplicacaoId={} plataforma={} instalacao={} identificadorAplicativo={} ip={}",
                loginMascarado,
                requisicao.aplicacaoId(),
                requisicao.dispositivo().plataforma(),
                instalacaoMascarada,
                identificadorAplicativo,
                extrairIp(servletRequest)
        );

        try {
            atestacaoAppServico.validarComprovante(requisicao.atestacao().paraEntrada());
            LOGGER.info(
                    "login_publico_atestacao_validada login={} provedor={} tipoComprovante={}",
                    loginMascarado,
                    requisicao.atestacao().provedor(),
                    requisicao.atestacao().tipoComprovante()
            );
        } catch (AtestacaoAppInvalidaException exception) {
            LOGGER.warn(
                    "login_publico_atestacao_rejeitada login={} provedor={} tipoComprovante={} motivo={}",
                    loginMascarado,
                    requisicao.atestacao().provedor(),
                    requisicao.atestacao().tipoComprovante(),
                    exception.getMessage()
            );
            throw exception;
        }
        try {
            avaliacaoSegurancaAplicativoService.avaliar(
                    "LOGIN",
                    requisicao.aplicacaoId(),
                    requisicao.dispositivo().plataforma(),
                    requisicao.segurancaAplicativo(),
                    loginResolvido.loginCanonico()
            );
            LOGGER.info(
                    "login_publico_seguranca_aprovada login={} provedorAtestacao={} scoreRiscoLocal={} assinaturaValida={} identidadeAplicativoValida={} sinaisRisco={}",
                    loginMascarado,
                    requisicao.segurancaAplicativo().provedorAtestacao(),
                    requisicao.segurancaAplicativo().scoreRiscoLocal(),
                    requisicao.segurancaAplicativo().assinaturaValida(),
                    requisicao.segurancaAplicativo().identidadeAplicativoValida(),
                    requisicao.segurancaAplicativo().sinaisRisco() == null ? 0 : requisicao.segurancaAplicativo().sinaisRisco().size()
            );
        } catch (FluxoPublicoException exception) {
            LOGGER.warn(
                    "login_publico_seguranca_rejeitada login={} provedorAtestacao={} scoreRiscoLocal={} motivo={}",
                    loginMascarado,
                    requisicao.segurancaAplicativo().provedorAtestacao(),
                    requisicao.segurancaAplicativo().scoreRiscoLocal(),
                    exception.getMessage()
            );
            throw exception;
        }
        SessaoInternaAutenticada sessao;
        try {
            sessao = autenticacaoSessaoInternaServico.autenticar(
                    loginResolvido.loginCanonico(),
                    requisicao.senha()
            );
        } catch (ResponseStatusException exception) {
            FluxoPublicoException erroMapeado = mapearErroLoginPublico(
                    requisicao.aplicacaoId(),
                    loginResolvido.loginCanonico(),
                    exception
            );
            LOGGER.warn(
                    "login_publico_autenticacao_rejeitada login={} codigo={} status={} motivo={}",
                    loginMascarado,
                    erroMapeado.getCodigo(),
                    erroMapeado.getStatus().value(),
                    Objects.requireNonNullElse(exception.getReason(), "")
            );
            throw erroMapeado;
        }
        LOGGER.info(
                "login_publico_credenciais_validadas login={} autenticado={} expiresIn={}",
                loginMascarado,
                sessao.autenticado(),
                sessao.expiresIn()
        );
        ContextoPessoaPerfilSistema contexto = clienteContextoPessoaPerfilSistema.buscarPorEmail(loginResolvido.loginCanonico())
                .or(loginResolvido::contextoResolvido)
                .orElseThrow(() -> {
                    LOGGER.warn(
                            "login_publico_contexto_ausente login={} motivo=conta_nao_liberada",
                            loginMascarado
                    );
                    return new FluxoPublicoException(
                            HttpStatus.FORBIDDEN,
                            "conta_nao_liberada",
                            "A conta ainda não está liberada para utilizar o aplicativo."
                    );
                });
        String statusPerfilSistema = Objects.requireNonNullElse(contexto.statusPerfilSistema(), STATUS_LIBERADO);
        if (!STATUS_LIBERADO.equalsIgnoreCase(statusPerfilSistema)) {
            LOGGER.warn(
                    "login_publico_contexto_bloqueado login={} statusPerfilSistema={}",
                    loginMascarado,
                    statusPerfilSistema
            );
            throw new FluxoPublicoException(
                    HttpStatus.FORBIDDEN,
                    "conta_nao_liberada",
                    "A conta ainda não está liberada para utilizar o aplicativo."
            );
        }
        DispositivoSessaoRegistrado dispositivoRegistrado = registroDispositivoLoginSilenciosoService.registrar(
                contexto,
                requisicao.dispositivo()
        );
        LOGGER.info(
                "login_publico_sucesso login={} perfilSistemaId={} statusPerfilSistema={} tokenDispositivoEmitido={} tokenDispositivoExpiraEm={}",
                loginMascarado,
                contexto.perfilSistemaId(),
                statusPerfilSistema,
                dispositivoRegistrado.tokenDispositivo() != null && !dispositivoRegistrado.tokenDispositivo().isBlank(),
                dispositivoRegistrado.tokenDispositivoExpiraEm()
        );
        AvatarSocialProjetoJdbc.PreferenciaAvatarProjeto avatarPreferido = buscarAvatarPreferido(contexto, projeto);
        return new SessaoApiResposta(
                sessao.autenticado(),
                sessao.tipoToken(),
                sessao.accessToken(),
                sessao.refreshToken(),
                sessao.expiresIn(),
                dispositivoRegistrado.tokenDispositivo(),
                dispositivoRegistrado.tokenDispositivoExpiraEm(),
                statusPerfilSistema,
                contexto.emailPrincipal(),
                contexto.usuario(),
                avatarPreferido.url(),
                resolverOrigemAvatar(avatarPreferido),
                avatarPreferido.versao(),
                avatarPreferido.atualizadoEm(),
                false,
                true,
                true
        );
    }

    @PostMapping("/sessoes/sociais")
    public SessaoApiResposta criarSessaoSocial(@Valid @RequestBody final CriarSessaoSocialApiRequest requisicao,
                                               final HttpServletRequest servletRequest) {
        String provedorNormalizado = requisicao.provedor().trim().toLowerCase(Locale.ROOT);
        String instalacaoMascarada = mascararIdentificador(requisicao.dispositivo().identificadorInstalacao());
        String identificadorAplicativo = resolverIdentificadorAplicativo(requisicao);
        LOGGER.info(
                "login_social_publico_recebido provedor={} aplicacaoId={} plataforma={} instalacao={} identificadorAplicativo={} ip={}",
                provedorNormalizado,
                requisicao.aplicacaoId(),
                requisicao.dispositivo().plataforma(),
                instalacaoMascarada,
                identificadorAplicativo,
                extrairIp(servletRequest)
        );

        try {
            atestacaoAppServico.validarComprovante(requisicao.atestacao().paraEntrada());
            LOGGER.info(
                    "login_social_publico_atestacao_validada provedor={} tipoComprovante={}",
                    provedorNormalizado,
                    requisicao.atestacao().tipoComprovante()
            );
        } catch (AtestacaoAppInvalidaException exception) {
            LOGGER.warn(
                    "login_social_publico_atestacao_rejeitada provedor={} tipoComprovante={} motivo={}",
                    provedorNormalizado,
                    requisicao.atestacao().tipoComprovante(),
                    exception.getMessage()
            );
            throw exception;
        }

        try {
            avaliacaoSegurancaAplicativoService.avaliar(
                    "LOGIN_SOCIAL",
                    requisicao.aplicacaoId(),
                    requisicao.dispositivo().plataforma(),
                    requisicao.segurancaAplicativo(),
                    provedorNormalizado
            );
            LOGGER.info(
                    "login_social_publico_seguranca_aprovada provedor={} provedorAtestacao={} scoreRiscoLocal={} assinaturaValida={} identidadeAplicativoValida={} sinaisRisco={}",
                    provedorNormalizado,
                    requisicao.segurancaAplicativo().provedorAtestacao(),
                    requisicao.segurancaAplicativo().scoreRiscoLocal(),
                    requisicao.segurancaAplicativo().assinaturaValida(),
                    requisicao.segurancaAplicativo().identidadeAplicativoValida(),
                    requisicao.segurancaAplicativo().sinaisRisco() == null ? 0 : requisicao.segurancaAplicativo().sinaisRisco().size()
            );
        } catch (FluxoPublicoException exception) {
            LOGGER.warn(
                    "login_social_publico_seguranca_rejeitada provedor={} provedorAtestacao={} scoreRiscoLocal={} motivo={}",
                    provedorNormalizado,
                    requisicao.segurancaAplicativo().provedorAtestacao(),
                    requisicao.segurancaAplicativo().scoreRiscoLocal(),
                    exception.getMessage()
            );
            throw exception;
        }

        CredencialSocialNativaValidada credencialSocial = validarCredencialSocialNativa(
                provedorNormalizado,
                requisicao
        );
        Optional<FormaAcesso> formaAcessoSocial = formaAcessoSocialVinculadaLocalmente(credencialSocial);
        if (formaAcessoSocial.isEmpty()) {
            throw resolverSocialSemContaLocal(requisicao, credencialSocial);
        }

        SessaoInternaAutenticada sessao;
        try {
            sessao = autenticacaoSessaoInternaServico.autenticarSocial(
                    provedorNormalizado,
                    requisicao.tokenExterno()
            );
        } catch (ResponseStatusException exception) {
            if (exception.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                LOGGER.warn(
                        "login_social_publico_rejeitado provedor={} status={} motivo={}",
                        provedorNormalizado,
                        exception.getStatusCode().value(),
                        exception.getReason()
                );
                throw erroAutenticacaoSocialInvalida();
            }
            throw exception;
        }
        LOGGER.info(
                "login_social_publico_sessao_emitida provedor={} autenticado={} expiresIn={}",
                provedorNormalizado,
                sessao.autenticado(),
                sessao.expiresIn()
        );
        ContextoPessoaPerfilSistema contexto = resolverContextoSessaoSocial(
                formaAcessoSocial.orElseThrow(),
                credencialSocial
        ).orElseThrow(() -> {
            LOGGER.warn(
                    "login_social_publico_contexto_ausente provedor={} motivo=conta_nao_liberada",
                    provedorNormalizado
            );
            return new FluxoPublicoException(
                    HttpStatus.FORBIDDEN,
                    "conta_nao_liberada",
                    "A conta ainda não está liberada para utilizar o aplicativo."
            );
        });
        String statusPerfilSistema = Objects.requireNonNullElse(contexto.statusPerfilSistema(), STATUS_LIBERADO);
        if (!STATUS_LIBERADO.equalsIgnoreCase(statusPerfilSistema)) {
            LOGGER.warn(
                    "login_social_publico_contexto_bloqueado provedor={} statusPerfilSistema={}",
                    provedorNormalizado,
                    statusPerfilSistema
            );
            throw new FluxoPublicoException(
                    HttpStatus.FORBIDDEN,
                    "conta_nao_liberada",
                    "A conta ainda não está liberada para utilizar o aplicativo."
            );
        }
        ProjetoFluxoPublicoResolvido projeto = resolvedorProjetoFluxoPublico.resolverAtivo(requisicao.aplicacaoId());
        AvatarSocialProjetoJdbc.PreferenciaAvatarProjeto avatarPreferido = buscarAvatarPreferido(contexto, projeto);
        return new SessaoApiResposta(
                sessao.autenticado(),
                sessao.tipoToken(),
                sessao.accessToken(),
                sessao.refreshToken(),
                sessao.expiresIn(),
                null,
                null,
                statusPerfilSistema,
                contexto.emailPrincipal(),
                contexto.usuario(),
                avatarPreferido.url(),
                resolverOrigemAvatar(avatarPreferido),
                avatarPreferido.versao(),
                avatarPreferido.atualizadoEm(),
                false,
                false,
                true
        );
    }

    private CredencialSocialNativaValidada validarCredencialSocialNativa(
            final String provedorNormalizado,
            final CriarSessaoSocialApiRequest requisicao) {
        try {
            CredencialSocialNativaValidada credencial =
                    validadorCredencialSocialNativa.validar(provedorNormalizado, requisicao.tokenExterno());
            return new CredencialSocialNativaValidada(
                    credencial.provedor(),
                    credencial.identificadorExterno(),
                    credencial.email(),
                    normalizarTexto(credencial.nomeUsuarioExterno(), requisicao.nomeUsuarioExterno()),
                    normalizarTexto(credencial.nomeExibicaoExterno(), requisicao.nomeCompleto()),
                    normalizarTexto(credencial.urlAvatarExterno(), requisicao.urlAvatarExterno())
            );
        } catch (ResponseStatusException exception) {
            if (exception.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                LOGGER.warn(
                        "login_social_publico_rejeitado provedor={} status={} motivo={}",
                        provedorNormalizado,
                        exception.getStatusCode().value(),
                        exception.getReason()
                );
                throw erroAutenticacaoSocialInvalida();
            }
            throw exception;
        }
    }

    private FluxoPublicoException erroAutenticacaoSocialInvalida() {
        return new FluxoPublicoException(
                HttpStatus.UNAUTHORIZED,
                "autenticacao_social_invalida",
                "Não foi possível concluir a autenticação com a rede social informada."
        );
    }

    private Optional<FormaAcesso> formaAcessoSocialVinculadaLocalmente(
            final CredencialSocialNativaValidada credencialSocial) {
        return formaAcessoRepositorio.findByTipoAndProvedorAndIdentificador(
                TipoFormaAcesso.SOCIAL,
                credencialSocial.provedor().getAliasFormaAcesso(),
                credencialSocial.identificadorExterno()
        );
    }

    private Optional<ContextoPessoaPerfilSistema> resolverContextoSessaoSocial(
            final FormaAcesso formaAcesso,
            final CredencialSocialNativaValidada credencialSocial) {
        Optional<ContextoPessoaPerfilSistema> contextoPorPessoa = Optional.ofNullable(formaAcesso.getPessoa())
                .map(pessoa -> pessoa.getId())
                .flatMap(clienteContextoPessoaPerfilSistema::buscarPorPessoaId);
        if (contextoPorPessoa.isPresent()) {
            return contextoPorPessoa;
        }
        return Optional.ofNullable(normalizarEmail(credencialSocial.email()))
                .flatMap(clienteContextoPessoaPerfilSistema::buscarPorEmail);
    }

    private FluxoPublicoException resolverSocialSemContaLocal(
            final CriarSessaoSocialApiRequest requisicao,
            final CredencialSocialNativaValidada credencialSocial) {
        String emailSocial = normalizarEmail(credencialSocial.email());
        String provedor = credencialSocial.provedor().getAliasApi();
        String identificadorExterno = credencialSocial.identificadorExterno();
        if (!StringUtils.hasText(emailSocial)) {
            return new FluxoPublicoException(
                    HttpStatus.CONFLICT,
                    "conflito_vinculo_social_ambiguo",
                    "Não foi possível determinar automaticamente se esta rede social deve abrir cadastro ou ser vinculada. Tente novamente ou procure suporte.",
                    montarDetalhesConflitoVinculoSocial(
                            provedor,
                            identificadorExterno,
                            null,
                            "SUPORTE"
                    )
            );
        }

        ProjetoFluxoPublicoResolvido projeto = resolvedorProjetoFluxoPublico.resolverAtivo(requisicao.aplicacaoId());
        Optional<PerfilSistemaProjetoPorEmailResolvido> contaExistente =
                localizadorPerfilSistemaProjetoPorEmail.localizar(
                        projeto.clienteEcossistemaId(),
                        emailSocial
                );
        Map<String, Object> detalhes = new java.util.LinkedHashMap<>();
        detalhes.put("provedor", provedor);
        if (StringUtils.hasText(identificadorExterno)) {
            detalhes.put("identificadorExterno", identificadorExterno);
        }
        detalhes.put("email", emailSocial);
        if (StringUtils.hasText(credencialSocial.nomeUsuarioExterno())) {
            detalhes.put("nomeUsuarioExterno", credencialSocial.nomeUsuarioExterno().trim());
        }
        if (StringUtils.hasText(credencialSocial.nomeExibicaoExterno())) {
            detalhes.put("nomeExibicaoExterno", credencialSocial.nomeExibicaoExterno().trim());
        }
        if (StringUtils.hasText(credencialSocial.urlAvatarExterno())) {
            detalhes.put("urlAvatarExterno", credencialSocial.urlAvatarExterno().trim());
        }
        if (contaExistente.isPresent()) {
            detalhes.put("acaoSugerida", "ENTRAR_E_VINCULAR");
            detalhes.put("emailContaExistente", contaExistente.get().emailNormalizado());
            detalhes.put("loginSugerido", contaExistente.get().identificadorPublicoSistemaSugerido());
            return new FluxoPublicoException(
                    HttpStatus.CONFLICT,
                    "social_sem_conta_local",
                    "Ja existe uma conta neste projeto com o mesmo e-mail desta rede social. Deseja entrar e vincular agora?",
                    detalhes
            );
        }
        detalhes.put("acaoSugerida", "ABRIR_CADASTRO");
        return new FluxoPublicoException(
                HttpStatus.CONFLICT,
                "social_sem_conta_local",
                "Esta rede social foi autenticada com sucesso, mas ainda nao esta ligada a uma conta local. Deseja abrir o cadastro com os dados recebidos?",
                detalhes
        );
    }

    private Map<String, Object> montarDetalhesConflitoVinculoSocial(final String provedor,
                                                                    final String identificadorExterno,
                                                                    final String email,
                                                                    final String acaoSugerida) {
        Map<String, Object> detalhes = new java.util.LinkedHashMap<>();
        if (StringUtils.hasText(provedor)) {
            detalhes.put("provedor", provedor);
        }
        if (StringUtils.hasText(identificadorExterno)) {
            detalhes.put("identificadorExterno", identificadorExterno);
        }
        if (StringUtils.hasText(email)) {
            detalhes.put("email", email);
        }
        if (StringUtils.hasText(acaoSugerida)) {
            detalhes.put("acaoSugerida", acaoSugerida);
        }
        return detalhes;
    }

    private static String normalizarTexto(final String... valores) {
        if (valores == null) {
            return null;
        }
        for (String valor : valores) {
            if (StringUtils.hasText(valor)) {
                return valor.trim();
            }
        }
        return null;
    }

    private String normalizarEmail(final String valor) {
        if (!StringUtils.hasText(valor)) {
            return null;
        }
        return valor.trim().toLowerCase(Locale.ROOT);
    }

    private AvatarSocialProjetoJdbc.PreferenciaAvatarProjeto buscarAvatarPreferido(
            final ContextoPessoaPerfilSistema contexto,
            final ProjetoFluxoPublicoResolvido projeto) {
        if (contexto == null || projeto == null) {
            return AvatarSocialProjetoJdbc.PreferenciaAvatarProjeto.vazia();
        }
        return avatarSocialProjetoJdbc.buscarPreferencia(
                contexto.sub(),
                projeto.clienteEcossistemaId()
        );
    }

    private String resolverOrigemAvatar(final AvatarSocialProjetoJdbc.PreferenciaAvatarProjeto preferencia) {
        if (preferencia == null) {
            return null;
        }
        if ("SOCIAL".equalsIgnoreCase(preferencia.origem()) && StringUtils.hasText(preferencia.provedorSocial())) {
            return preferencia.provedorSocial().trim().toUpperCase(Locale.ROOT);
        }
        if ("URL_EXTERNA".equalsIgnoreCase(preferencia.origem())) {
            return "THIMISU";
        }
        return StringUtils.hasText(preferencia.origem())
                ? preferencia.origem().trim().toUpperCase(Locale.ROOT)
                : null;
    }

    private static String resolverIdentificadorAplicativo(final CriarSessaoSocialApiRequest requisicao) {
        return Optional.ofNullable(requisicao.segurancaAplicativo().bundleIdentifier())
                .map(String::trim)
                .filter(valor -> !valor.isEmpty())
                .or(() -> Optional.ofNullable(requisicao.segurancaAplicativo().packageName())
                        .map(String::trim)
                        .filter(valor -> !valor.isEmpty()))
                .orElse(requisicao.aplicacaoId());
    }

    private LoginPublicoResolvido resolverLoginPublico(final String loginNormalizado) {
        if (loginNormalizado.contains("@")) {
            return new LoginPublicoResolvido(loginNormalizado, loginNormalizado, Optional.empty());
        }
        Optional<ContextoPessoaPerfilSistema> contextoPorIdentificadorPublicoSistema =
                clienteContextoPessoaPerfilSistema.buscarPorIdentificadorPublicoSistema(loginNormalizado);
        String loginCanonico = contextoPorIdentificadorPublicoSistema
                .map(ContextoPessoaPerfilSistema::emailPrincipal)
                .map(email -> email == null ? null : email.trim().toLowerCase(Locale.ROOT))
                .filter(email -> !email.isBlank())
                .orElse(loginNormalizado);
        return new LoginPublicoResolvido(loginNormalizado, loginCanonico, contextoPorIdentificadorPublicoSistema);
    }

    @PostMapping("/sessoes/refresh")
    public SessaoApiResposta renovarSessao(@Valid @RequestBody final RenovarSessaoApiRequest requisicao) {
        SessaoInternaAutenticada sessao = autenticacaoSessaoInternaServico.renovar(
                requisicao.refreshToken(),
                requisicao.tokenDispositivo()
        );
        return new SessaoApiResposta(
                sessao.autenticado(),
                sessao.tipoToken(),
                sessao.accessToken(),
                sessao.refreshToken(),
                sessao.expiresIn(),
                null,
                null,
                STATUS_LIBERADO,
                null,
                null,
                false,
                true,
                true
        );
    }

    @PostMapping("/recuperacoes-senha")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public RecuperacaoSenhaApiResposta iniciarRecuperacaoSenha(
            @Valid @RequestBody final IniciarRecuperacaoSenhaApiRequest requisicao) {
        RecuperacaoSenhaIniciada recuperacao = recuperacaoSenhaService.iniciar(
                requisicao.aplicacaoId(),
                requisicao.emailPrincipal(),
                construirContextoSolicitacao(requisicao)
        );
        return new RecuperacaoSenhaApiResposta(
                recuperacao.fluxoId() == null ? null : recuperacao.fluxoId().toString(),
                recuperacao.cadastroId() == null ? null : recuperacao.cadastroId().toString(),
                recuperacao.proximoPasso(),
                recuperacao.requerNovaSenha(),
                "Se este e-mail estiver cadastrado, enviaremos um código de verificação."
        );
    }

    @PostMapping("/recuperacoes-senha/{fluxoId}/confirmacoes/email")
    public ConfirmacaoCodigoRecuperacaoSenhaApiResposta confirmarCodigoRecuperacaoSenha(
            @PathVariable final String fluxoId,
            @Valid @RequestBody final ConfirmarCodigoRecuperacaoSenhaApiRequest requisicao) {
        ConfirmacaoCodigoRecuperacaoSenhaRealizada confirmacao =
                recuperacaoSenhaService.confirmarCodigo(parseCadastroId(fluxoId), requisicao.codigo());
        return new ConfirmacaoCodigoRecuperacaoSenhaApiResposta(
                confirmacao.fluxoId().toString(),
                confirmacao.codigoConfirmado(),
                confirmacao.podeDefinirSenha()
        );
    }

    @PostMapping("/recuperacoes-senha/{fluxoId}/confirmacoes/email/reenvio")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void reenviarCodigoRecuperacaoSenha(@PathVariable final String fluxoId) {
        recuperacaoSenhaService.reenviarCodigo(parseCadastroId(fluxoId));
    }

    @PostMapping("/recuperacoes-senha/{fluxoId}/senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void redefinirSenhaRecuperacaoSenha(
            @PathVariable final String fluxoId,
            @Valid @RequestBody final RedefinirSenhaRecuperacaoApiRequest requisicao) {
        recuperacaoSenhaService.redefinirSenha(
                parseCadastroId(fluxoId),
                requisicao.senha(),
                requisicao.confirmacaoSenha()
        );
    }

    private static void validarRegrasCadastro(final CadastroApiRequest requisicao) {
        if (!Objects.equals(requisicao.senha(), requisicao.confirmacaoSenha())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "A confirmação de senha não confere.");
        }
        if (requisicao.tipoPessoa() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tipoPessoa é obrigatório.");
        }
        if (requisicao.tipoPessoa().name().equals("FISICA")) {
            validarFisica(requisicao.sexo(), requisicao.paisNascimento(), requisicao.dataNascimento());
            return;
        }
        if (requisicao.dataNascimento() != null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Pessoa jurídica não deve informar data de nascimento.");
        }
    }

    private static List<AvatarCadastroConfirmado> resolverAvataresConfirmadosDoCadastro(
            final CadastroApiRequest requisicao) {
        Map<String, AvatarCadastroConfirmado> avatares = new LinkedHashMap<>();
        if (requisicao.avataresCadastroConfirmados() != null) {
            requisicao.avataresCadastroConfirmados()
                    .forEach(avatar -> adicionarAvatarConfirmado(avatares, avatar));
        }
        return List.copyOf(avatares.values());
    }

    private static List<VinculoSocialConfirmadoCadastro> resolverVinculosSociaisConfirmadosDoCadastro(
            final CadastroApiRequest requisicao) {
        Map<String, VinculoSocialConfirmadoCadastro> vinculos = new LinkedHashMap<>();
        if (requisicao.vinculosSociaisConfirmados() != null) {
            requisicao.vinculosSociaisConfirmados()
                    .forEach(vinculo -> adicionarVinculoSocialConfirmado(vinculos, vinculo));
        }
        return List.copyOf(vinculos.values());
    }

    private static void adicionarVinculoSocialConfirmado(
            final Map<String, VinculoSocialConfirmadoCadastro> vinculos,
            final VinculoSocialConfirmadoApiRequest vinculo) {
        if (vinculo == null) {
            return;
        }
        String provedor = normalizarTexto(vinculo.provedor());
        String identificadorExterno = normalizarTexto(vinculo.identificadorExterno());
        if (!StringUtils.hasText(provedor) || !StringUtils.hasText(identificadorExterno)) {
            return;
        }
        vinculos.put(
                (provedor + "::" + identificadorExterno).toLowerCase(Locale.ROOT),
                new VinculoSocialConfirmadoCadastro(
                        provedor,
                        identificadorExterno,
                        normalizarTexto(vinculo.nomeUsuarioExterno()),
                        normalizarTexto(vinculo.email()),
                        normalizarTexto(vinculo.nomeCompleto()),
                        normalizarTexto(vinculo.urlAvatarExterno()),
                        Boolean.TRUE.equals(vinculo.avatarPreferido())
                )
        );
    }

    private static void adicionarAvatarConfirmado(final Map<String, AvatarCadastroConfirmado> avatares,
                                                  final AvatarCadastroConfirmadoApiRequest avatar) {
        if (avatar == null) {
            return;
        }
        String origem = normalizarTexto(avatar.origem());
        String urlAvatar = normalizarTexto(avatar.urlAvatar());
        String conteudoBase64 = normalizarTexto(avatar.conteudoBase64());
        if (!StringUtils.hasText(origem)
                || (!StringUtils.hasText(urlAvatar) && !StringUtils.hasText(conteudoBase64))) {
            return;
        }
        String chaveAvatar = StringUtils.hasText(urlAvatar)
                ? urlAvatar
                : "conteudo:" + hashTexto(conteudoBase64);
        avatares.put(
                (origem + "::" + chaveAvatar).toLowerCase(Locale.ROOT),
                new AvatarCadastroConfirmado(
                        origem,
                        urlAvatar,
                        normalizarTexto(avatar.storageKey()),
                        normalizarTexto(avatar.nomeArquivo()),
                        normalizarTexto(avatar.contentType()),
                        avatar.tamanhoBytes(),
                        normalizarTexto(avatar.hashConteudo()),
                        normalizarTexto(avatar.versao()),
                        conteudoBase64,
                        Boolean.TRUE.equals(avatar.preferido())
                )
        );
    }

    private static String hashTexto(final String valor) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponivel no runtime Java.", ex);
        }
    }

    private static void validarEmailCompativelComConvite(final CadastroApiRequest requisicao,
                                                         final ConviteOrganizacionalValidado convite) {
        if (convite.emailConvidado() == null || convite.emailConvidado().isBlank()) {
            return;
        }
        String emailConvite = convite.emailConvidado().trim().toLowerCase(Locale.ROOT);
        String emailRequisicao = requisicao.emailPrincipal().trim().toLowerCase(Locale.ROOT);
        if (convite.exigeContaSeparada() && convite.contaExistenteDetectada()) {
            if (Objects.equals(emailConvite, emailRequisicao)) {
                throw new FluxoPublicoException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "conta_separada_obrigatoria",
                        "Este convite exige a criacao de uma conta organizacional separada com outro e-mail.",
                        Map.of(
                                "codigoConvite", convite.codigo(),
                                "emailConvidado", convite.emailConvidado()
                        )
                );
            }
            return;
        }
        if (!Objects.equals(emailConvite, emailRequisicao)) {
            throw new FluxoPublicoException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "email_convite_invalido",
                    "O e-mail informado nao corresponde ao convite organizacional.",
                    Map.of(
                            "codigoConvite", convite.codigo(),
                            "emailConvidado", convite.emailConvidado()
                    )
            );
        }
    }

    private static void validarFisica(final Object sexo, final String paisNascimento, final LocalDate dataNascimento) {
        if (sexo == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "sexo é obrigatório para pessoa física.");
        }
        if (paisNascimento == null || paisNascimento.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "paisNascimento é obrigatório para pessoa física."
            );
        }
        if (dataNascimento == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "dataNascimento é obrigatória para pessoa física."
            );
        }
    }

    private static UUID parseCadastroId(final String cadastroId) {
        try {
            return UUID.fromString(Objects.requireNonNull(cadastroId, "cadastroId é obrigatório"));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cadastroId inválido.");
        }
    }

    private static ContextoSolicitacaoFluxoPublico construirContextoSolicitacao(final CadastroApiRequest requisicao) {
        ContextoSolicitacaoFluxoPublico contexto = new ContextoSolicitacaoFluxoPublico(
                requisicao.locale(),
                requisicao.timeZone(),
                requisicao.tipoProdutoExibicao(),
                requisicao.produtoExibicao(),
                requisicao.canalExibicao(),
                requisicao.empresaExibicao(),
                requisicao.ambienteExibicao()
        ).sanitizado();
        return contexto.vazio() ? null : contexto;
    }

    private static ContextoSolicitacaoFluxoPublico construirContextoSolicitacao(
            final IniciarRecuperacaoSenhaApiRequest requisicao) {
        ContextoSolicitacaoFluxoPublico contexto = new ContextoSolicitacaoFluxoPublico(
                requisicao.locale(),
                requisicao.timeZone(),
                requisicao.tipoProdutoExibicao(),
                requisicao.produtoExibicao(),
                requisicao.canalExibicao(),
                requisicao.empresaExibicao(),
                requisicao.ambienteExibicao()
        ).sanitizado();
        return contexto.vazio() ? null : contexto;
    }

    private FluxoPublicoException mapearErroLoginPublico(final String aplicacaoId,
                                                         final String loginNormalizado,
                                                         final ResponseStatusException exception) {
        String motivo = Objects.requireNonNullElse(exception.getReason(), "").trim();
        if (ERRO_KEYCLOAK_CONTA_DESABILITADA.equalsIgnoreCase(motivo)
                || ERRO_KEYCLOAK_CONTA_INCOMPLETA.equalsIgnoreCase(motivo)) {
            UUID cadastroPendenteId = cadastroContaInternaServico
                    .buscarCadastroPendenteEmailPublico(loginNormalizado)
                    .orElse(null);
            if (cadastroPendenteId != null) {
                return new FluxoPublicoException(
                        HttpStatus.FORBIDDEN,
                        "conta_nao_liberada",
                        "A conta ainda não está liberada para utilizar o aplicativo.",
                        Map.of("cadastroId", cadastroPendenteId.toString())
                );
            }
            if (ERRO_KEYCLOAK_CONTA_INCOMPLETA.equalsIgnoreCase(motivo)) {
                return new FluxoPublicoException(
                        HttpStatus.FORBIDDEN,
                        "conta_incompleta",
                        "A conta nao esta completamente configurada para autenticacao."
                );
            }
            return new FluxoPublicoException(
                    HttpStatus.FORBIDDEN,
                    "conta_desabilitada",
                    "A conta está desabilitada para autenticação."
            );
        }
        if (ERRO_KEYCLOAK_CREDENCIAIS_INVALIDAS.equalsIgnoreCase(motivo)
                || "Credenciais invalidas.".equalsIgnoreCase(motivo)) {
            Optional<CadastroConta> cadastroPendenteMesmoProjeto = cadastroContaInternaServico
                    .buscarCadastroPendenteEmailPublicoPorProjeto(aplicacaoId, loginNormalizado);
            if (cadastroPendenteMesmoProjeto.isPresent()) {
                CadastroConta cadastro = cadastroPendenteMesmoProjeto.get();
                return new FluxoPublicoException(
                        HttpStatus.FORBIDDEN,
                        "conta_pendente_redefinir_senha",
                        "Sua conta ainda precisa ser validada. Deseja continuar a validacao e definir uma nova senha?",
                        Map.of(
                                "cadastroId", cadastro.getCadastroId().toString(),
                                "email", cadastro.getEmailPrincipal(),
                                "requerNovaSenha", true
                        )
                );
            }
            return new FluxoPublicoException(
                    HttpStatus.UNAUTHORIZED,
                    "credenciais_invalidas",
                    "Credenciais inválidas."
            );
        }
        return new FluxoPublicoException(
                HttpStatus.BAD_GATEWAY,
                "falha_autenticacao",
                "Não foi possível autenticar a sessão agora."
        );
    }

    private static String extrairIp(final HttpServletRequest servletRequest) {
        String forwardedFor = servletRequest.getHeader("X-Forwarded-For");
        if (forwardedFor == null || forwardedFor.isBlank()) {
            return servletRequest.getRemoteAddr();
        }
        return forwardedFor.split(",")[0].trim();
    }

    private static String mascararIdentificador(final String valor) {
        if (valor == null || valor.isBlank()) {
            return "vazio";
        }
        String normalizado = valor.trim().toLowerCase(Locale.ROOT);
        int indiceArroba = normalizado.indexOf('@');
        if (indiceArroba > 0) {
            return mascararTrecho(normalizado.substring(0, indiceArroba))
                    + "@"
                    + mascararTrecho(normalizado.substring(indiceArroba + 1));
        }
        return mascararTrecho(normalizado);
    }

    private static String mascararTrecho(final String valor) {
        if (valor == null || valor.isBlank()) {
            return "vazio";
        }
        if (valor.length() <= 2) {
            return "*".repeat(valor.length());
        }
        return valor.charAt(0) + "***" + valor.charAt(valor.length() - 1);
    }

    private static String resolverIdentificadorAplicativo(final CriarSessaoApiRequest requisicao) {
        if (requisicao.segurancaAplicativo().bundleIdentifier() != null
                && !requisicao.segurancaAplicativo().bundleIdentifier().isBlank()) {
            return requisicao.segurancaAplicativo().bundleIdentifier();
        }
        if (requisicao.segurancaAplicativo().packageName() != null
                && !requisicao.segurancaAplicativo().packageName().isBlank()) {
            return requisicao.segurancaAplicativo().packageName();
        }
        return requisicao.aplicacaoId();
    }

    private record LoginPublicoResolvido(
            String loginOriginal,
            String loginCanonico,
            Optional<ContextoPessoaPerfilSistema> contextoResolvido) {
    }
}
