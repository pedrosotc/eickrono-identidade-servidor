package com.eickrono.api.identidade.apresentacao.api;

import com.eickrono.api.identidade.aplicacao.excecao.FluxoPublicoException;
import com.eickrono.api.identidade.apresentacao.dto.fluxo.ErroFluxoPublicoApiResposta;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(assignableTypes = {FluxoPublicoController.class, ConvitesPublicosController.class})
public class FluxoPublicoExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(FluxoPublicoExceptionHandler.class);

    @ExceptionHandler(FluxoPublicoException.class)
    public ResponseEntity<ErroFluxoPublicoApiResposta> tratarFluxoPublico(final FluxoPublicoException exception,
                                                                          final HttpServletRequest request) {
        LOGGER.warn(
                "fluxo_publico_exception path={} method={} codigo={} status={} detalhes={}",
                request.getRequestURI(),
                request.getMethod(),
                exception.getCodigo(),
                exception.getStatus().value(),
                exception.getDetalhes()
        );
        return ResponseEntity.status(exception.getStatus())
                .body(new ErroFluxoPublicoApiResposta(
                        exception.getCodigo(),
                        exception.getMessage(),
                        exception.getDetalhes()
                ));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErroFluxoPublicoApiResposta> tratarResponseStatus(final ResponseStatusException exception,
                                                                           final HttpServletRequest request) {
        String mensagem = Objects.requireNonNullElse(exception.getReason(), "Não foi possível concluir a solicitação.");
        LOGGER.warn(
                "fluxo_publico_response_status path={} method={} status={} motivo={}",
                request.getRequestURI(),
                request.getMethod(),
                exception.getStatusCode().value(),
                mensagem
        );
        return ResponseEntity.status(exception.getStatusCode())
                .body(new ErroFluxoPublicoApiResposta("fluxo_publico_erro", mensagem, null));
    }
}
