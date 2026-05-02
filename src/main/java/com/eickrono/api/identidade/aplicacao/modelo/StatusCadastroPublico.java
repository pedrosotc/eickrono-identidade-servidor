package com.eickrono.api.identidade.aplicacao.modelo;

import java.util.UUID;

public record StatusCadastroPublico(
        UUID cadastroId,
        String emailPrincipal,
        String telefonePrincipal,
        boolean emailConfirmado,
        boolean telefoneConfirmado,
        boolean telefoneObrigatorio,
        boolean liberadoParaLogin,
        String proximoPasso
) {
}
