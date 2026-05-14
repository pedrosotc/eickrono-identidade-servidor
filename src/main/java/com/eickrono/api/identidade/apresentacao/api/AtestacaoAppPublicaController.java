package com.eickrono.api.identidade.apresentacao.api;

import com.eickrono.api.identidade.aplicacao.excecao.AtestacaoAppInvalidaException;
import com.eickrono.api.identidade.aplicacao.modelo.DesafioAtestacaoGerado;
import com.eickrono.api.identidade.aplicacao.modelo.ValidacaoAtestacaoAppConcluida;
import com.eickrono.api.identidade.aplicacao.servico.AtestacaoAppServico;
import com.eickrono.api.identidade.apresentacao.dto.atestacao.CriarDesafioAtestacaoApiRequest;
import com.eickrono.api.identidade.apresentacao.dto.atestacao.DesafioAtestacaoApiResposta;
import com.eickrono.api.identidade.apresentacao.dto.atestacao.ValidacaoAtestacaoApiResposta;
import com.eickrono.api.identidade.apresentacao.dto.atestacao.ValidarAtestacaoApiRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/publica/atestacoes")
public class AtestacaoAppPublicaController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AtestacaoAppPublicaController.class);

    private final AtestacaoAppServico servico;

    public AtestacaoAppPublicaController(final AtestacaoAppServico servico) {
        this.servico = Objects.requireNonNull(servico, "servico é obrigatório");
    }

    @PostMapping("/desafios")
    @ResponseStatus(HttpStatus.CREATED)
    public DesafioAtestacaoApiResposta criarDesafio(@Valid @RequestBody final CriarDesafioAtestacaoApiRequest request,
                                                    final HttpServletRequest servletRequest) {
        final String enderecoIp = extrairEnderecoIp(servletRequest);
        final String userAgent = servletRequest.getHeader("User-Agent");
        LOGGER.info(
                "atestacao_publica_desafio_recebido aplicacaoId={} operacao={} plataforma={} cadastroId={} usuarioSub={} pessoaIdPerfil={} registroDispositivoId={} ip={}",
                request.aplicacaoId(),
                request.operacao(),
                request.plataforma(),
                request.cadastroId(),
                request.usuarioSub(),
                request.pessoaIdPerfil(),
                request.registroDispositivoId(),
                enderecoIp
        );
        try {
            DesafioAtestacaoGerado desafio = servico.gerarDesafio(
                    request.operacao(),
                    request.plataforma(),
                    request.usuarioSub(),
                    request.pessoaIdPerfil(),
                    request.cadastroId(),
                    request.registroDispositivoId(),
                    enderecoIp,
                    userAgent
            );
            LOGGER.info(
                    "atestacao_publica_desafio_concluido identificadorDesafio={} operacao={} plataforma={} provedorEsperado={}",
                    desafio.identificadorDesafio(),
                    desafio.operacao(),
                    desafio.plataforma(),
                    desafio.provedorEsperado()
            );
            return DesafioAtestacaoApiResposta.de(desafio);
        } catch (ResponseStatusException erro) {
            LOGGER.warn(
                    "atestacao_publica_desafio_invalido operacao={} plataforma={} status={} motivo={}",
                    request.operacao(),
                    request.plataforma(),
                    erro.getStatusCode().value(),
                    erro.getReason()
            );
            throw erro;
        } catch (AtestacaoAppInvalidaException erro) {
            LOGGER.error(
                    "atestacao_publica_desafio_falhou aplicacaoId={} operacao={} plataforma={} cadastroId={} usuarioSub={} pessoaIdPerfil={} registroDispositivoId={}",
                    request.aplicacaoId(),
                    request.operacao(),
                    request.plataforma(),
                    request.cadastroId(),
                    request.usuarioSub(),
                    request.pessoaIdPerfil(),
                    request.registroDispositivoId(),
                    erro
            );
            throw erro;
        }
    }

    @PostMapping("/validacoes")
    public ValidacaoAtestacaoApiResposta validarAtestacao(@Valid @RequestBody final ValidarAtestacaoApiRequest request) {
        ValidacaoAtestacaoAppConcluida validacao = servico.validarComprovante(request.paraEntradaAplicacao());
        return ValidacaoAtestacaoApiResposta.de(validacao);
    }

    private static String extrairEnderecoIp(final HttpServletRequest servletRequest) {
        String forwardedFor = servletRequest.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            int indiceVirgula = forwardedFor.indexOf(',');
            return indiceVirgula >= 0 ? forwardedFor.substring(0, indiceVirgula).trim() : forwardedFor.trim();
        }
        return servletRequest.getRemoteAddr();
    }
}
