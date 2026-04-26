package com.eickrono.api.identidade.aplicacao.modelo;

import java.time.OffsetDateTime;

public record ConviteOrganizacionalValidado(
        String codigo,
        String organizacaoId,
        String nomeOrganizacao,
        String emailConvidado,
        String nomeConvidado,
        boolean exigeContaSeparada,
        boolean contaExistenteDetectada,
        OffsetDateTime expiraEm
) {
}
