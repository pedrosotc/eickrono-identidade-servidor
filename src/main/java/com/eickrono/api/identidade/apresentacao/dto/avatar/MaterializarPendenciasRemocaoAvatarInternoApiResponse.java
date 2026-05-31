package com.eickrono.api.identidade.apresentacao.dto.avatar;

import java.util.List;

public record MaterializarPendenciasRemocaoAvatarInternoApiResponse(
        String correlacaoId,
        String produto,
        int pendenciasMaterializadas,
        List<String> avatarIds,
        List<String> storageKeys
) {
}
