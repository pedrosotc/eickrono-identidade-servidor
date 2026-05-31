package com.eickrono.api.identidade.apresentacao.dto.avatar;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record MaterializarPendenciasRemocaoAvatarInternoApiRequest(
        @NotBlank String correlacaoId,
        @NotBlank String produto,
        @NotEmpty List<@NotBlank String> usuarioClienteIds
) {
}
