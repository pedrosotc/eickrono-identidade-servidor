package com.eickrono.api.identidade.apresentacao.dto;

import java.time.OffsetDateTime;

/**
 * Payload devolvido quando o backend emite silenciosamente um token de dispositivo para a sessão autenticada.
 */
public record RegistroDispositivoSessaoResponse(
        String tokenDispositivo,
        OffsetDateTime tokenDispositivoExpiraEm
) {
}
