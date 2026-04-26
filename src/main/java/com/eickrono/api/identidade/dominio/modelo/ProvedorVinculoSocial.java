package com.eickrono.api.identidade.dominio.modelo;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Provedores sociais suportados pela API de identidade.
 */
public enum ProvedorVinculoSocial {
    GOOGLE("google", true),
    APPLE("apple", true),
    FACEBOOK("facebook", false),
    LINKEDIN("linkedin", true),
    INSTAGRAM("instagram", false);

    private final String aliasApi;
    private final boolean trustEmail;

    ProvedorVinculoSocial(final String aliasApi, final boolean trustEmail) {
        this.aliasApi = aliasApi;
        this.trustEmail = trustEmail;
    }

    public String getAliasApi() {
        return aliasApi;
    }

    public String getAliasFormaAcesso() {
        return aliasApi.toUpperCase(Locale.ROOT);
    }

    public boolean confiaEmailDoProvedor() {
        return trustEmail;
    }

    public static Optional<ProvedorVinculoSocial> fromAlias(final String alias) {
        if (alias == null || alias.isBlank()) {
            return Optional.empty();
        }
        String aliasNormalizado = alias.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(provedor -> provedor.aliasApi.equals(aliasNormalizado))
                .findFirst();
    }
}
