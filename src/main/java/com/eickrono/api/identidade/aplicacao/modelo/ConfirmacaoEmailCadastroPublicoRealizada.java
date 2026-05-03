package com.eickrono.api.identidade.aplicacao.modelo;

import java.util.UUID;

public record ConfirmacaoEmailCadastroPublicoRealizada(
        UUID cadastroId,
        String subjectRemoto,
        String emailPrincipal,
        String perfilSistemaId,
        String statusPerfilSistema,
        boolean emailConfirmado,
        boolean telefoneConfirmado,
        boolean telefoneObrigatorio,
        boolean podeAutenticar,
        String proximoPasso
) {
}
