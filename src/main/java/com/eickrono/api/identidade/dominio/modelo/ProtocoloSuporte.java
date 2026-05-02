package com.eickrono.api.identidade.dominio.modelo;

import java.util.Locale;
import java.util.UUID;

public final class ProtocoloSuporte {

    private static final String PREFIXO_CADASTRO = "CAD";
    private static final String PREFIXO_RECUPERACAO = "REC";

    private ProtocoloSuporte() {
        // utilitario
    }

    public static String gerarCadastro() {
        return gerar(PREFIXO_CADASTRO);
    }

    public static String gerarRecuperacaoSenha() {
        return gerar(PREFIXO_RECUPERACAO);
    }

    private static String gerar(final String prefixo) {
        return prefixo + "-" + UUID.randomUUID().toString().toUpperCase(Locale.ROOT);
    }
}
