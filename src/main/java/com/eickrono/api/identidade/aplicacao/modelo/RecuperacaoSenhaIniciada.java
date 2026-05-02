package com.eickrono.api.identidade.aplicacao.modelo;

import java.util.UUID;

public record RecuperacaoSenhaIniciada(
        UUID fluxoId,
        UUID cadastroId,
        String proximoPasso,
        boolean requerNovaSenha
) {

    public static RecuperacaoSenhaIniciada validarCodigoRecuperacao(final UUID fluxoId) {
        return new RecuperacaoSenhaIniciada(fluxoId, null, "VALIDAR_CODIGO_RECUPERACAO", true);
    }

    public static RecuperacaoSenhaIniciada validarContatosCadastro(final UUID cadastroId,
                                                                   final boolean requerNovaSenha) {
        return new RecuperacaoSenhaIniciada(null, cadastroId, "VALIDAR_CONTATOS", requerNovaSenha);
    }
}
