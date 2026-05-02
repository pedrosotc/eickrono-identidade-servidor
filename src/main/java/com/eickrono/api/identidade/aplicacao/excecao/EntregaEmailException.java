package com.eickrono.api.identidade.aplicacao.excecao;

import java.util.Objects;

public class EntregaEmailException extends RuntimeException {

    private final String codigo;
    private final String mensagemPublica;

    public EntregaEmailException(final String codigo,
                                 final String mensagemPublica,
                                 final String mensagemTecnica,
                                 final Throwable causa) {
        super(Objects.requireNonNull(mensagemTecnica, "mensagemTecnica é obrigatória"), causa);
        this.codigo = Objects.requireNonNull(codigo, "codigo é obrigatório");
        this.mensagemPublica = Objects.requireNonNull(mensagemPublica, "mensagemPublica é obrigatória");
    }

    public String getCodigo() {
        return codigo;
    }

    public String getMensagemPublica() {
        return mensagemPublica;
    }
}
