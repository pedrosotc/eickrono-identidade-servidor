package com.eickrono.api.identidade.aplicacao.modelo;

import java.util.Objects;
import java.util.UUID;

public record ContaLocalProjetoPorEmailResolvida(
        UUID usuarioId,
        String emailNormalizado,
        String loginSugerido) {

    public ContaLocalProjetoPorEmailResolvida {
        Objects.requireNonNull(usuarioId, "usuarioId é obrigatório");
        Objects.requireNonNull(emailNormalizado, "emailNormalizado é obrigatório");
        Objects.requireNonNull(loginSugerido, "loginSugerido é obrigatório");
    }
}
