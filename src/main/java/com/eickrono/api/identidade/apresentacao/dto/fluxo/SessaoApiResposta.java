package com.eickrono.api.identidade.apresentacao.dto.fluxo;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

// Mantém statusUsuario apenas como compatibilidade de JSON; internamente o contrato já trata statusPerfilSistema.
public record SessaoApiResposta(
        boolean autenticado,
        String tipoToken,
        String accessToken,
        String refreshToken,
        long expiresIn,
        String tokenDispositivo,
        java.time.OffsetDateTime tokenDispositivoExpiraEm,
        @JsonProperty("statusUsuario") String statusPerfilSistema,
        String emailPrincipal,
        String usuario,
        String avatarPreferidoUrl,
        String avatarPreferidoOrigem,
        String avatarPreferidoVersao,
        OffsetDateTime avatarPreferidoAtualizadoEm,
        boolean primeiraSessao,
        boolean podeOferecerBiometria,
        boolean podeOferecerVinculacaoSocial
) {
    public SessaoApiResposta(final boolean autenticado,
                             final String tipoToken,
                             final String accessToken,
                             final String refreshToken,
                             final long expiresIn,
                             final String tokenDispositivo,
                             final OffsetDateTime tokenDispositivoExpiraEm,
                             final String statusPerfilSistema,
                             final String emailPrincipal,
                             final String usuario,
                             final boolean primeiraSessao,
                             final boolean podeOferecerBiometria,
                             final boolean podeOferecerVinculacaoSocial) {
        this(
                autenticado,
                tipoToken,
                accessToken,
                refreshToken,
                expiresIn,
                tokenDispositivo,
                tokenDispositivoExpiraEm,
                statusPerfilSistema,
                emailPrincipal,
                usuario,
                null,
                null,
                null,
                null,
                primeiraSessao,
                podeOferecerBiometria,
                podeOferecerVinculacaoSocial
        );
    }
}
