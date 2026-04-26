package com.eickrono.api.identidade.apresentacao.dto.fluxo;

import java.time.OffsetDateTime;

public record ConviteOrganizacionalApiResposta(
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
