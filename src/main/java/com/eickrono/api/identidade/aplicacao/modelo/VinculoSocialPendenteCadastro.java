package com.eickrono.api.identidade.aplicacao.modelo;

import com.eickrono.api.identidade.dominio.modelo.ProvedorVinculoSocial;
import java.util.Objects;

/**
 * Representa o vínculo social autenticado que ainda precisa ser anexado
 * definitivamente a um cadastro público pendente.
 */
public record VinculoSocialPendenteCadastro(
        ProvedorVinculoSocial provedor,
        String identificadorExterno,
        String nomeUsuarioExterno) {

    public VinculoSocialPendenteCadastro {
        Objects.requireNonNull(provedor, "provedor é obrigatório");
        identificadorExterno = normalizarObrigatorio(identificadorExterno, "identificadorExterno");
        nomeUsuarioExterno = normalizarOpcional(nomeUsuarioExterno);
    }

    private static String normalizarObrigatorio(final String valor, final String campo) {
        String texto = normalizarOpcional(valor);
        if (texto == null) {
            throw new IllegalArgumentException(campo + " é obrigatório");
        }
        return texto;
    }

    private static String normalizarOpcional(final String valor) {
        if (valor == null) {
            return null;
        }
        String texto = valor.trim();
        return texto.isEmpty() ? null : texto;
    }
}
