package com.eickrono.api.identidade.apresentacao.dto.fluxo;

public record RecuperacaoSenhaApiResposta(
        String fluxoId,
        String cadastroId,
        String proximoPasso,
        boolean requerNovaSenha,
        String mensagem
) {
}
