package com.eickrono.api.identidade.apresentacao.api;

import com.eickrono.api.identidade.aplicacao.excecao.ApiAutenticadaException;
import com.eickrono.api.identidade.aplicacao.modelo.ContaLocalProjetoPorEmailResolvida;
import com.eickrono.api.identidade.aplicacao.modelo.ContextoPessoaPerfil;
import com.eickrono.api.identidade.aplicacao.modelo.IdentidadeFederadaKeycloak;
import com.eickrono.api.identidade.aplicacao.modelo.DispositivoSessaoRegistrado;
import com.eickrono.api.identidade.aplicacao.modelo.ProjetoFluxoPublicoResolvido;
import com.eickrono.api.identidade.aplicacao.servico.ClienteContextoPessoaPerfil;
import com.eickrono.api.identidade.aplicacao.servico.ClienteAdministracaoVinculosSociaisKeycloak;
import com.eickrono.api.identidade.aplicacao.servico.ContextoSocialPendenteJdbc;
import com.eickrono.api.identidade.aplicacao.servico.LocalizadorContaLocalProjetoPorEmailJdbc;
import com.eickrono.api.identidade.apresentacao.dto.ConfirmacaoRegistroRequest;
import com.eickrono.api.identidade.apresentacao.dto.ConfirmacaoRegistroResponse;
import com.eickrono.api.identidade.apresentacao.dto.PoliticaOfflineDispositivoResponse;
import com.eickrono.api.identidade.apresentacao.dto.ReenvioCodigoRequest;
import com.eickrono.api.identidade.apresentacao.dto.RegistrarEventosOfflineRequest;
import com.eickrono.api.identidade.apresentacao.dto.RegistroDispositivoRequest;
import com.eickrono.api.identidade.apresentacao.dto.RegistroDispositivoResponse;
import com.eickrono.api.identidade.apresentacao.dto.RegistroDispositivoSessaoResponse;
import com.eickrono.api.identidade.apresentacao.dto.RevogarTokenRequest;
import com.eickrono.api.identidade.dominio.modelo.CadastroConta;
import com.eickrono.api.identidade.dominio.modelo.FormaAcesso;
import com.eickrono.api.identidade.dominio.modelo.MotivoRevogacaoToken;
import com.eickrono.api.identidade.dominio.modelo.Pessoa;
import com.eickrono.api.identidade.dominio.modelo.TipoFormaAcesso;
import com.eickrono.api.identidade.dominio.repositorio.CadastroContaRepositorio;
import com.eickrono.api.identidade.dominio.repositorio.FormaAcessoRepositorio;
import com.eickrono.api.identidade.dominio.repositorio.PessoaRepositorio;
import com.eickrono.api.identidade.aplicacao.servico.OfflineDispositivoService;
import com.eickrono.api.identidade.aplicacao.servico.ResolvedorProjetoFluxoPublico;
import com.eickrono.api.identidade.aplicacao.servico.RegistroDispositivoLoginSilenciosoService;
import com.eickrono.api.identidade.aplicacao.servico.RegistroDispositivoService;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints responsáveis pelo registro e revogação de dispositivos móveis.
 */
@RestController
@RequestMapping("/identidade/dispositivos")
public class RegistroDispositivoController {
    private static final String STATUS_LIBERADO = "LIBERADO";
    private static final String STATUS_ATIVO = "ATIVO";
    private final RegistroDispositivoService registroDispositivoService;
    private final OfflineDispositivoService offlineDispositivoService;
    private final RegistroDispositivoLoginSilenciosoService registroDispositivoLoginSilenciosoService;
    private final ClienteContextoPessoaPerfil clienteContextoPessoaPerfil;
    private final ClienteAdministracaoVinculosSociaisKeycloak clienteAdministracaoVinculosSociaisKeycloak;
    private final ContextoSocialPendenteJdbc contextoSocialPendenteJdbc;
    private final ResolvedorProjetoFluxoPublico resolvedorProjetoFluxoPublico;
    private final LocalizadorContaLocalProjetoPorEmailJdbc localizadorContaLocalProjetoPorEmail;
    private final FormaAcessoRepositorio formaAcessoRepositorio;
    private final CadastroContaRepositorio cadastroContaRepositorio;
    private final PessoaRepositorio pessoaRepositorio;

    public RegistroDispositivoController(RegistroDispositivoService registroDispositivoService,
                                         OfflineDispositivoService offlineDispositivoService,
                                         RegistroDispositivoLoginSilenciosoService registroDispositivoLoginSilenciosoService,
                                         ClienteContextoPessoaPerfil clienteContextoPessoaPerfil,
                                         ClienteAdministracaoVinculosSociaisKeycloak clienteAdministracaoVinculosSociaisKeycloak,
                                         ContextoSocialPendenteJdbc contextoSocialPendenteJdbc,
                                         ResolvedorProjetoFluxoPublico resolvedorProjetoFluxoPublico,
                                         LocalizadorContaLocalProjetoPorEmailJdbc localizadorContaLocalProjetoPorEmail,
                                         FormaAcessoRepositorio formaAcessoRepositorio,
                                         CadastroContaRepositorio cadastroContaRepositorio,
                                         PessoaRepositorio pessoaRepositorio) {
        this.registroDispositivoService = registroDispositivoService;
        this.offlineDispositivoService = offlineDispositivoService;
        this.registroDispositivoLoginSilenciosoService = registroDispositivoLoginSilenciosoService;
        this.clienteContextoPessoaPerfil = clienteContextoPessoaPerfil;
        this.clienteAdministracaoVinculosSociaisKeycloak = clienteAdministracaoVinculosSociaisKeycloak;
        this.contextoSocialPendenteJdbc = contextoSocialPendenteJdbc;
        this.resolvedorProjetoFluxoPublico = resolvedorProjetoFluxoPublico;
        this.localizadorContaLocalProjetoPorEmail = localizadorContaLocalProjetoPorEmail;
        this.formaAcessoRepositorio = formaAcessoRepositorio;
        this.cadastroContaRepositorio = cadastroContaRepositorio;
        this.pessoaRepositorio = pessoaRepositorio;
    }

    @PostMapping("/registro")
    public ResponseEntity<RegistroDispositivoResponse> solicitarRegistro(@Valid @RequestBody RegistroDispositivoRequest request,
                                                                         @AuthenticationPrincipal Jwt jwt) {
        RegistroDispositivoResponse resposta = registroDispositivoService.solicitarRegistro(request, Optional.ofNullable(jwt));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(resposta);
    }

    @PostMapping("/registro/silencioso")
    public ResponseEntity<RegistroDispositivoSessaoResponse> registrarSessaoSilenciosa(
            @Valid @RequestBody com.eickrono.api.identidade.apresentacao.dto.fluxo.DispositivoSessaoApiRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String usuarioSub = extrairSub(jwt)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Usuario autenticado ausente"
                ));
        ContextoPessoaPerfil contexto = resolverContextoSessaoSocial(usuarioSub)
                .orElseThrow(() -> ApiAutenticadaException.conflito(
                        "social_sem_conta_local",
                        resolverMensagemSocialSemContaLocal(jwt, request),
                        montarDetalhesSocialSemContaLocal(usuarioSub, jwt, request)
                ));
        validarContaLiberadaParaSessaoSocial(contexto, usuarioSub);
        DispositivoSessaoRegistrado resposta = registroDispositivoLoginSilenciosoService.registrar(contexto, request);
        return ResponseEntity.ok(new RegistroDispositivoSessaoResponse(
                resposta.tokenDispositivo(),
                resposta.tokenDispositivoExpiraEm()
        ));
    }

    @PostMapping("/registro/{id}/confirmacao")
    public ResponseEntity<ConfirmacaoRegistroResponse> confirmarRegistro(@PathVariable("id") UUID id,
                                                                         @Valid @RequestBody ConfirmacaoRegistroRequest request,
                                                                         @AuthenticationPrincipal Jwt jwt) {
        ConfirmacaoRegistroResponse resposta = registroDispositivoService.confirmarRegistro(id, request, Optional.ofNullable(jwt));
        return ResponseEntity.ok(resposta);
    }

    @PostMapping("/offline/eventos")
    public ResponseEntity<Void> registrarEventosOffline(@AuthenticationPrincipal Jwt jwt,
                                                        @RequestHeader("X-Device-Token") String tokenDispositivo,
                                                        @RequestBody RegistrarEventosOfflineRequest request) {
        offlineDispositivoService.registrarEventosOffline(
                extrairSub(jwt).orElseThrow(),
                tokenDispositivo,
                request);
        return ResponseEntity.accepted().build();
    }

    @org.springframework.web.bind.annotation.GetMapping("/offline/politica")
    public ResponseEntity<PoliticaOfflineDispositivoResponse> obterPoliticaOffline() {
        return ResponseEntity.ok(offlineDispositivoService.obterPolitica());
    }

    @PostMapping("/registro/{id}/reenviar")
    public ResponseEntity<Void> reenviarCodigos(@PathVariable("id") UUID id,
                                                @RequestBody(required = false) ReenvioCodigoRequest request) {
        registroDispositivoService.reenviarCodigos(id, request == null ? new ReenvioCodigoRequest() : request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @PostMapping("/revogar")
    public ResponseEntity<Void> revogarToken(@AuthenticationPrincipal Jwt jwt,
                                             @RequestHeader("X-Device-Token") String tokenDispositivo,
                                             @RequestBody(required = false) RevogarTokenRequest request) {
        MotivoRevogacaoToken motivo = Optional.ofNullable(request)
                .map(RevogarTokenRequest::getMotivo)
                .flatMap(this::mapearMotivo)
                .orElse(MotivoRevogacaoToken.SOLICITACAO_CLIENTE);
        registroDispositivoService.revogarToken(
                extrairSub(jwt).orElseThrow(),
                tokenDispositivo,
                motivo);
        return ResponseEntity.noContent().build();
    }

    private Optional<String> extrairSub(Jwt jwt) {
        return Optional.ofNullable(jwt).map(Jwt::getSubject);
    }

    private Optional<MotivoRevogacaoToken> mapearMotivo(String valor) {
        if (!StringUtils.hasText(valor)) {
            return Optional.empty();
        }
        try {
            return Optional.of(MotivoRevogacaoToken.valueOf(valor.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private Map<String, Object> montarDetalhesSocialSemContaLocal(final String usuarioSub,
                                                                  final Jwt jwt,
                                                                  final com.eickrono.api.identidade.apresentacao.dto.fluxo.DispositivoSessaoApiRequest request) {
        ProjetoFluxoPublicoResolvido projeto = resolvedorProjetoFluxoPublico.resolverAtivo(request.aplicacaoId());
        Optional<ContaLocalProjetoPorEmailResolvida> contaLocalProjetoAtual =
                localizarContaLocalNoProjetoAtual(projeto, extrairEmail(jwt));
        Map<String, Object> detalhes = new LinkedHashMap<>();
        detalhes.put("sub", usuarioSub);
        detalhes.put("acaoSugerida", "ABRIR_CADASTRO");
        Optional<String> emailSocial = extrairEmail(jwt);
        emailSocial.ifPresent(email -> detalhes.put("email", email));
        contaLocalProjetoAtual.ifPresent(contaLocal -> {
            detalhes.put("acaoSugerida", "ENTRAR_E_VINCULAR");
            detalhes.put("loginSugerido", contaLocal.loginSugerido());
            detalhes.put("emailContaExistente", contaLocal.emailNormalizado());
        });

        final String[] provedorSocial = new String[1];
        final String[] identificadorExterno = new String[1];
        final String[] nomeUsuarioExterno = new String[1];
        final String[] nomeExibicaoExterno = new String[1];
        final String[] urlAvatarExterno = new String[1];
        try {
            listarIdentidadesFederadasSeguras(usuarioSub).stream()
                    .findFirst()
                    .ifPresent(identidade -> {
                        provedorSocial[0] = identidade.provedor().getAliasApi();
                        identificadorExterno[0] = identidade.identificadorExterno();
                        nomeUsuarioExterno[0] = identidade.nomeUsuarioExterno();
                        nomeExibicaoExterno[0] = normalizarTexto(
                                identidade.nomeExibicaoExterno(),
                                jwt.getClaimAsString("name")
                        );
                        urlAvatarExterno[0] = normalizarTexto(
                                identidade.urlAvatarExterno(),
                                jwt.getClaimAsString("picture"),
                                jwt.getClaimAsString("avatar_url"),
                                jwt.getClaimAsString("avatar")
                        );
                        detalhes.put("provedor", identidade.provedor().getAliasApi());
                        detalhes.put("identificadorExterno", identidade.identificadorExterno());
                        if (StringUtils.hasText(identidade.nomeUsuarioExterno())) {
                            detalhes.put("nomeUsuarioExterno", identidade.nomeUsuarioExterno());
                        }
                        if (StringUtils.hasText(nomeExibicaoExterno[0])) {
                            detalhes.put("nomeExibicaoExterno", nomeExibicaoExterno[0]);
                        }
                        if (StringUtils.hasText(urlAvatarExterno[0])) {
                            detalhes.put("urlAvatarExterno", urlAvatarExterno[0]);
                        }
                    });
        } catch (RuntimeException ignored) {
            // Mantém o contrato mínimo do erro mesmo quando a consulta administrativa falhar.
        }
        if (StringUtils.hasText(provedorSocial[0]) && StringUtils.hasText(identificadorExterno[0])) {
            UUID contextoSocialPendenteId = contextoSocialPendenteJdbc.registrarOuAtualizar(
                    projeto,
                    provedorSocial[0],
                    identificadorExterno[0],
                    emailSocial.orElse(null),
                    nomeUsuarioExterno[0],
                    nomeExibicaoExterno[0],
                    urlAvatarExterno[0],
                    contaLocalProjetoAtual.map(ContaLocalProjetoPorEmailResolvida::usuarioId).orElse(null),
                    contaLocalProjetoAtual.map(ContaLocalProjetoPorEmailResolvida::loginSugerido).orElse(null)
            );
            detalhes.put("contextoSocialPendenteId", contextoSocialPendenteId);
        }

        return detalhes;
    }

    private String resolverMensagemSocialSemContaLocal(final Jwt jwt,
                                                       final com.eickrono.api.identidade.apresentacao.dto.fluxo.DispositivoSessaoApiRequest request) {
        return resolverContaLocalNoProjetoAtual(request.aplicacaoId(), extrairEmail(jwt))
                .map(conta -> "Ja existe uma conta neste projeto com o mesmo e-mail desta rede social. Deseja entrar e vincular agora?")
                .orElse("Esta rede social foi autenticada com sucesso, mas ainda nao esta ligada a uma conta local. Deseja abrir o cadastro com os dados recebidos?");
    }

    private Optional<String> extrairEmail(final Jwt jwt) {
        return Optional.ofNullable(jwt)
                .map(token -> token.getClaimAsString("email"))
                .map(String::trim)
                .filter(valor -> !valor.isEmpty())
                .map(valor -> valor.toLowerCase(Locale.ROOT));
    }

    private String normalizarTexto(final String... valores) {
        for (String valor : valores) {
            if (StringUtils.hasText(valor)) {
                return valor.trim();
            }
        }
        return null;
    }

    private Optional<ContaLocalProjetoPorEmailResolvida> resolverContaLocalNoProjetoAtual(final String aplicacaoId,
                                                                                           final Optional<String> emailSocial) {
        if (!StringUtils.hasText(aplicacaoId) || emailSocial.isEmpty()) {
            return Optional.empty();
        }
        try {
            ProjetoFluxoPublicoResolvido projeto = resolvedorProjetoFluxoPublico.resolverAtivo(aplicacaoId);
            return localizarContaLocalNoProjetoAtual(projeto, emailSocial);
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private Optional<ContaLocalProjetoPorEmailResolvida> localizarContaLocalNoProjetoAtual(
            final ProjetoFluxoPublicoResolvido projeto,
            final Optional<String> emailSocial) {
        if (projeto == null || emailSocial.isEmpty()) {
            return Optional.empty();
        }
        return localizadorContaLocalProjetoPorEmail.localizar(projeto.clienteEcossistemaId(), emailSocial.get());
    }

    private Optional<ContextoPessoaPerfil> resolverContextoSessaoSocial(final String usuarioSub) {
        Optional<ContextoPessoaPerfil> contextoDireto = clienteContextoPessoaPerfil.buscarPorSub(usuarioSub);
        if (contextoDireto.isPresent()) {
            return contextoDireto;
        }

        for (IdentidadeFederadaKeycloak identidadeFederada : listarIdentidadesFederadasSeguras(usuarioSub)) {
            Optional<ContextoPessoaPerfil> contextoPorFormaAcesso = formaAcessoRepositorio
                    .findByTipoAndProvedorAndIdentificador(
                            TipoFormaAcesso.SOCIAL,
                            identidadeFederada.provedor().getAliasFormaAcesso(),
                            identidadeFederada.identificadorCanonico()
                    )
                    .flatMap(formaAcesso -> clienteContextoPessoaPerfil.buscarPorPessoaId(formaAcesso.getPessoa().getId()));
            if (contextoPorFormaAcesso.isPresent()) {
                return contextoPorFormaAcesso;
            }
        }

        return Optional.empty();
    }

    private void validarContaLiberadaParaSessaoSocial(final ContextoPessoaPerfil contexto, final String usuarioSub) {
        String statusUsuario = Optional.ofNullable(contexto.statusUsuario())
                .map(String::trim)
                .filter(valor -> !valor.isEmpty())
                .orElse(STATUS_LIBERADO);
        if (STATUS_LIBERADO.equalsIgnoreCase(statusUsuario) || STATUS_ATIVO.equalsIgnoreCase(statusUsuario)) {
            Optional<CadastroConta> cadastroConta = resolverCadastroContaSessaoSocial(contexto, usuarioSub);
            if (cadastroConta.map(CadastroConta::emailJaConfirmado).orElse(false)
                    || emailPrincipalLocalVerificado(contexto)) {
                return;
            }
            throw new ApiAutenticadaException(
                    HttpStatus.FORBIDDEN,
                    "conta_nao_liberada",
                    "A conta ainda não está liberada para utilizar o aplicativo.",
                    montarDetalhesContaNaoLiberadaSocial(
                            contexto,
                            usuarioSub,
                            statusUsuario,
                            cadastroConta,
                            false,
                            "EMAIL_NAO_CONFIRMADO",
                            null)
            );
        }
        if (statusIndicaContaDesabilitada(statusUsuario)) {
            throw new ApiAutenticadaException(
                    HttpStatus.FORBIDDEN,
                    "conta_desabilitada",
                    "A conta está desabilitada para autenticação.",
                    montarDetalhesContaNaoLiberadaSocial(
                            contexto,
                            usuarioSub,
                            statusUsuario,
                            resolverCadastroContaSessaoSocial(contexto, usuarioSub),
                            null,
                            "STATUS_DESABILITADO",
                            "SUPORTE")
            );
        }
        throw new ApiAutenticadaException(
                HttpStatus.FORBIDDEN,
                "conta_nao_liberada",
                "A conta ainda não está liberada para utilizar o aplicativo.",
                montarDetalhesContaNaoLiberadaSocial(
                        contexto,
                        usuarioSub,
                        statusUsuario,
                        resolverCadastroContaSessaoSocial(contexto, usuarioSub),
                        null,
                        null,
                        null)
        );
    }

    private Map<String, Object> montarDetalhesContaNaoLiberadaSocial(final ContextoPessoaPerfil contexto,
                                                                     final String usuarioSub,
                                                                     final String statusUsuario,
                                                                     final Optional<CadastroConta> cadastroConta,
                                                                     final Boolean emailVerificado,
                                                                     final String motivoBloqueio,
                                                                     final String acaoSugerida) {
        Map<String, Object> detalhes = new LinkedHashMap<>();
        detalhes.put("sub", usuarioSub);
        detalhes.put("statusUsuario", statusUsuario);
        if (StringUtils.hasText(contexto.emailPrincipal())) {
            detalhes.put("email", contexto.emailPrincipal());
        }
        if (emailVerificado != null) {
            detalhes.put("emailVerificado", emailVerificado);
        }
        if (StringUtils.hasText(motivoBloqueio)) {
            detalhes.put("motivoBloqueio", motivoBloqueio);
        }
        if (StringUtils.hasText(acaoSugerida)) {
            detalhes.put("acaoSugerida", acaoSugerida);
        }
        cadastroConta
                .map(cadastro -> cadastro.getCadastroId().toString())
                .ifPresent(cadastroId -> detalhes.put("cadastroId", cadastroId));
        return detalhes;
    }

    private boolean statusIndicaContaDesabilitada(final String statusUsuario) {
        String normalizado = statusUsuario.trim().toUpperCase(Locale.ROOT);
        return normalizado.equals("DESABILITADO")
                || normalizado.equals("DESATIVADO")
                || normalizado.equals("INATIVO")
                || normalizado.equals("BLOQUEADO")
                || normalizado.equals("SUSPENSO");
    }

    private Optional<CadastroConta> resolverCadastroContaSessaoSocial(final ContextoPessoaPerfil contexto,
                                                                      final String usuarioSub) {
        Optional<CadastroConta> cadastroPorContexto = Optional.ofNullable(contexto.sub())
                .filter(StringUtils::hasText)
                .flatMap(cadastroContaRepositorio::findBySubjectRemoto);
        if (cadastroPorContexto.isPresent()) {
            return cadastroPorContexto;
        }
        return Optional.ofNullable(usuarioSub)
                .filter(StringUtils::hasText)
                .flatMap(cadastroContaRepositorio::findBySubjectRemoto);
    }

    private boolean emailPrincipalLocalVerificado(final ContextoPessoaPerfil contexto) {
        Optional<Pessoa> pessoa = Optional.ofNullable(contexto.pessoaId())
                .flatMap(pessoaRepositorio::findById);
        if (pessoa.isEmpty()) {
            pessoa = Optional.ofNullable(contexto.sub())
                    .filter(StringUtils::hasText)
                    .flatMap(pessoaRepositorio::findBySub);
        }
        if (pessoa.isEmpty()) {
            return true;
        }
        Optional<FormaAcesso> formaAcessoEmail = formaAcessoRepositorio.findByPessoaAndTipoAndPrincipalTrue(
                pessoa.orElseThrow(),
                TipoFormaAcesso.EMAIL_SENHA
        );
        return formaAcessoEmail
                .map(FormaAcesso::getVerificadoEm)
                .isPresent();
    }

    private java.util.List<IdentidadeFederadaKeycloak> listarIdentidadesFederadasSeguras(final String usuarioSub) {
        try {
            return clienteAdministracaoVinculosSociaisKeycloak.listarIdentidadesFederadas(usuarioSub);
        } catch (RuntimeException ignored) {
            return java.util.List.of();
        }
    }
}
