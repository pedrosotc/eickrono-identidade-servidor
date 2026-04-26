package com.eickrono.api.identidade.apresentacao.api;

import com.eickrono.api.identidade.aplicacao.excecao.FluxoPublicoException;
import com.eickrono.api.identidade.aplicacao.modelo.CadastroInternoRealizado;
import com.eickrono.api.identidade.aplicacao.modelo.ConfirmacaoCodigoRecuperacaoSenhaRealizada;
import com.eickrono.api.identidade.aplicacao.modelo.ConfirmacaoEmailCadastroPublicoRealizada;
import com.eickrono.api.identidade.aplicacao.modelo.ConviteOrganizacionalValidado;
import com.eickrono.api.identidade.aplicacao.modelo.ContextoPessoaPerfil;
import com.eickrono.api.identidade.aplicacao.modelo.DispositivoSessaoRegistrado;
import com.eickrono.api.identidade.aplicacao.modelo.RecuperacaoSenhaIniciada;
import com.eickrono.api.identidade.aplicacao.modelo.SessaoInternaAutenticada;
import com.eickrono.api.identidade.aplicacao.servico.AtestacaoAppServico;
import com.eickrono.api.identidade.aplicacao.servico.AvaliacaoSegurancaAplicativoService;
import com.eickrono.api.identidade.aplicacao.servico.AutenticacaoSessaoInternaServico;
import com.eickrono.api.identidade.aplicacao.servico.CadastroContaInternaServico;
import com.eickrono.api.identidade.aplicacao.servico.ClienteContextoPessoaPerfil;
import com.eickrono.api.identidade.aplicacao.servico.ConviteOrganizacionalService;
import com.eickrono.api.identidade.aplicacao.servico.RecuperacaoSenhaService;
import com.eickrono.api.identidade.aplicacao.servico.RegistroDispositivoLoginSilenciosoService;
import com.eickrono.api.identidade.apresentacao.dto.fluxo.CadastroApiRequest;
import com.eickrono.api.identidade.apresentacao.dto.fluxo.CadastroApiResposta;
import com.eickrono.api.identidade.apresentacao.dto.fluxo.ConfirmacaoCodigoRecuperacaoSenhaApiResposta;
import com.eickrono.api.identidade.apresentacao.dto.fluxo.ConfirmacaoEmailCadastroApiResposta;
import com.eickrono.api.identidade.apresentacao.dto.fluxo.ConfirmarCodigoRecuperacaoSenhaApiRequest;
import com.eickrono.api.identidade.apresentacao.dto.fluxo.ConfirmarEmailCadastroApiRequest;
import com.eickrono.api.identidade.apresentacao.dto.fluxo.CriarSessaoApiRequest;
import com.eickrono.api.identidade.apresentacao.dto.fluxo.DisponibilidadeUsuarioCadastroApiResposta;
import com.eickrono.api.identidade.apresentacao.dto.fluxo.IniciarRecuperacaoSenhaApiRequest;
import com.eickrono.api.identidade.apresentacao.dto.fluxo.RecuperacaoSenhaApiResposta;
import com.eickrono.api.identidade.apresentacao.dto.fluxo.RenovarSessaoApiRequest;
import com.eickrono.api.identidade.apresentacao.dto.fluxo.RedefinirSenhaRecuperacaoApiRequest;
import com.eickrono.api.identidade.apresentacao.dto.fluxo.SessaoApiResposta;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.Map;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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
    private final ClienteContextoPessoaPerfil clienteContextoPessoaPerfil;
    private final ConviteOrganizacionalService conviteOrganizacionalService;
    private final RecuperacaoSenhaService recuperacaoSenhaService;
    private final RegistroDispositivoLoginSilenciosoService registroDispositivoLoginSilenciosoService;

    public FluxoPublicoController(final CadastroContaInternaServico cadastroContaInternaServico,
                                  final AtestacaoAppServico atestacaoAppServico,
                                  final AvaliacaoSegurancaAplicativoService avaliacaoSegurancaAplicativoService,
                                  final AutenticacaoSessaoInternaServico autenticacaoSessaoInternaServico,
                                  final ClienteContextoPessoaPerfil clienteContextoPessoaPerfil,
                                  final ConviteOrganizacionalService conviteOrganizacionalService,
                                  final RecuperacaoSenhaService recuperacaoSenhaService,
                                  final RegistroDispositivoLoginSilenciosoService registroDispositivoLoginSilenciosoService) {
        this.cadastroContaInternaServico = Objects.requireNonNull(
                cadastroContaInternaServico, "cadastroContaInternaServico é obrigatório");
        this.atestacaoAppServico = Objects.requireNonNull(atestacaoAppServico, "atestacaoAppServico é obrigatório");
        this.avaliacaoSegurancaAplicativoService = Objects.requireNonNull(
                avaliacaoSegurancaAplicativoService, "avaliacaoSegurancaAplicativoService é obrigatório");
        this.autenticacaoSessaoInternaServico = Objects.requireNonNull(
                autenticacaoSessaoInternaServico, "autenticacaoSessaoInternaServico é obrigatório");
        this.clienteContextoPessoaPerfil = Objects.requireNonNull(
                clienteContextoPessoaPerfil, "clienteContextoPessoaPerfil é obrigatório");
        this.conviteOrganizacionalService = Objects.requireNonNull(
                conviteOrganizacionalService, "conviteOrganizacionalService é obrigatório");
        this.recuperacaoSenhaService = Objects.requireNonNull(
                recuperacaoSenhaService, "recuperacaoSenhaService é obrigatório");
        this.registroDispositivoLoginSilenciosoService = Objects.requireNonNull(
                registroDispositivoLoginSilenciosoService, "registroDispositivoLoginSilenciosoService é obrigatório");
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
                    requisicao.vinculoSocialPendente() == null
                            ? null
                            : requisicao.vinculoSocialPendente().paraModelo(),
                    conviteOrganizacional,
                    "app-flutter-publico",
                    extrairIp(servletRequest),
                    servletRequest.getHeader("User-Agent")
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
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "cadastro_publico_falhou usuario={} email={} telefone={} aplicacaoId={}",
                    usuarioMascarado,
                    emailMascarado,
                    telefoneMascarado,
                    requisicao.aplicacaoId(),
                    exception
            );
            throw exception;
        }
    }

    @GetMapping("/cadastros/usuarios/disponibilidade")
    public DisponibilidadeUsuarioCadastroApiResposta consultarDisponibilidadeUsuario(
            @RequestParam final String usuario) {
        String usuarioNormalizado = Objects.requireNonNull(usuario, "usuario é obrigatório")
                .trim()
                .toLowerCase(Locale.ROOT);
        if (usuarioNormalizado.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "usuario é obrigatório.");
        }
        return new DisponibilidadeUsuarioCadastroApiResposta(
                usuarioNormalizado,
                cadastroContaInternaServico.usuarioDisponivelPublico(usuarioNormalizado)
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
                confirmacao.usuarioId(),
                confirmacao.statusUsuario(),
                confirmacao.emailPrincipal(),
                confirmacao.emailConfirmado(),
                confirmacao.podeAutenticar(),
                "LOGIN"
        );
    }

    @PostMapping("/cadastros/{cadastroId}/confirmacoes/email/reenvio")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void reenviarConfirmacaoEmailCadastro(@PathVariable final String cadastroId) {
        cadastroContaInternaServico.reenviarCodigoEmail(parseCadastroId(cadastroId));
    }

    @DeleteMapping("/cadastros/{cadastroId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelarCadastro(@PathVariable final String cadastroId) {
        cadastroContaInternaServico.cancelarCadastroPendentePublico(parseCadastroId(cadastroId));
    }

    @PostMapping("/sessoes")
    public SessaoApiResposta criarSessao(@Valid @RequestBody final CriarSessaoApiRequest requisicao,
                                         final HttpServletRequest servletRequest) {
        String loginNormalizado = requisicao.login().trim().toLowerCase(Locale.ROOT);
        LoginPublicoResolvido loginResolvido = resolverLoginPublico(loginNormalizado);
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
        } catch (RuntimeException exception) {
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
        } catch (RuntimeException exception) {
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
            FluxoPublicoException erroMapeado = mapearErroLoginPublico(loginResolvido.loginCanonico(), exception);
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
        ContextoPessoaPerfil contexto = clienteContextoPessoaPerfil.buscarPorEmail(loginResolvido.loginCanonico())
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
        String statusUsuario = Objects.requireNonNullElse(contexto.statusUsuario(), STATUS_LIBERADO);
        if (!STATUS_LIBERADO.equalsIgnoreCase(statusUsuario)) {
            LOGGER.warn(
                    "login_publico_contexto_bloqueado login={} statusUsuario={}",
                    loginMascarado,
                    statusUsuario
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
                "login_publico_sucesso login={} usuarioId={} statusUsuario={} tokenDispositivoEmitido={} tokenDispositivoExpiraEm={}",
                loginMascarado,
                contexto.usuarioId(),
                statusUsuario,
                dispositivoRegistrado.tokenDispositivo() != null && !dispositivoRegistrado.tokenDispositivo().isBlank(),
                dispositivoRegistrado.tokenDispositivoExpiraEm()
        );
        return new SessaoApiResposta(
                sessao.autenticado(),
                sessao.tipoToken(),
                sessao.accessToken(),
                sessao.refreshToken(),
                sessao.expiresIn(),
                dispositivoRegistrado.tokenDispositivo(),
                dispositivoRegistrado.tokenDispositivoExpiraEm(),
                statusUsuario,
                contexto.emailPrincipal(),
                false,
                true,
                true
        );
    }

    private LoginPublicoResolvido resolverLoginPublico(final String loginNormalizado) {
        if (loginNormalizado.contains("@")) {
            return new LoginPublicoResolvido(loginNormalizado, loginNormalizado, Optional.empty());
        }
        Optional<ContextoPessoaPerfil> contextoPorUsuario = clienteContextoPessoaPerfil.buscarPorUsuario(loginNormalizado);
        String loginCanonico = contextoPorUsuario
                .map(ContextoPessoaPerfil::emailPrincipal)
                .map(email -> email == null ? null : email.trim().toLowerCase(Locale.ROOT))
                .filter(email -> !email.isBlank())
                .orElse(loginNormalizado);
        return new LoginPublicoResolvido(loginNormalizado, loginCanonico, contextoPorUsuario);
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
                false,
                true,
                true
        );
    }

    @PostMapping("/recuperacoes-senha")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public RecuperacaoSenhaApiResposta iniciarRecuperacaoSenha(
            @Valid @RequestBody final IniciarRecuperacaoSenhaApiRequest requisicao) {
        RecuperacaoSenhaIniciada recuperacao = recuperacaoSenhaService.iniciar(requisicao.emailPrincipal());
        return new RecuperacaoSenhaApiResposta(
                recuperacao.fluxoId().toString(),
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

    private FluxoPublicoException mapearErroLoginPublico(final String loginNormalizado,
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
            Optional<ContextoPessoaPerfil> contextoResolvido) {
    }
}
