package com.eickrono.api.identidade.apresentacao.dto;

import java.util.List;

/**
 * Resposta da listagem de vínculos sociais suportados.
 */
public record VinculosSociaisDto(
        List<VinculoSocialDto> provedores,
        String avatarPreferidoOrigem,
        String avatarPreferidoUrl
) {
    public VinculosSociaisDto {
        provedores = provedores == null ? List.of() : List.copyOf(provedores);
    }

    @Override
    public List<VinculoSocialDto> provedores() {
        return List.copyOf(provedores);
    }
}
