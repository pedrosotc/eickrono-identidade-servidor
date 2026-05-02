package com.eickrono.api.identidade.dominio.modelo;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Provedores sociais suportados pela API de identidade.
 */
public enum ProvedorVinculoSocial {
    GOOGLE(true, "google"),
    APPLE(true, "apple"),
    FACEBOOK(false, "facebook"),
    LINKEDIN(true, "linkedin"),
    INSTAGRAM(false, "instagram"),
    X(false, "x");

    private final String aliasApi;
    private final boolean trustEmail;
    private final String[] aliasesAceitos;

    ProvedorVinculoSocial(final boolean trustEmail, final String aliasApi, final String... aliasesAceitos) {
        this.aliasApi = aliasApi;
        this.trustEmail = trustEmail;
        this.aliasesAceitos = aliasesAceitos;
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
                .filter(provedor -> provedor.aliasApi.equals(aliasNormalizado)
                        || Arrays.stream(provedor.aliasesAceitos).anyMatch(aliasNormalizado::equals))
                .findFirst();
    }
}
