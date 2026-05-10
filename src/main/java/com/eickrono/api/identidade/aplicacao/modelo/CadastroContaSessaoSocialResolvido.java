package com.eickrono.api.identidade.aplicacao.modelo;

import java.util.Objects;
import java.util.UUID;

public record CadastroContaSessaoSocialResolvido(
        UUID cadastroId,
        boolean emailConfirmado
) {
    public CadastroContaSessaoSocialResolvido {
        Objects.requireNonNull(cadastroId, "cadastroId é obrigatório");
    }
}
