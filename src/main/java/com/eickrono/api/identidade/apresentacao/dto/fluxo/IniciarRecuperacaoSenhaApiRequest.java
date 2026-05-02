package com.eickrono.api.identidade.apresentacao.dto.fluxo;

import jakarta.validation.constraints.NotBlank;

public record IniciarRecuperacaoSenhaApiRequest(
        @NotBlank String aplicacaoId,
        @NotBlank String emailPrincipal,
        String locale,
        String timeZone,
        String tipoProdutoExibicao,
        String produtoExibicao,
        String canalExibicao,
        String empresaExibicao,
        String ambienteExibicao
) {
}
