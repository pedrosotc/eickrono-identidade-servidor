package com.eickrono.api.identidade.aplicacao.modelo;

import java.util.List;

public record AvaliacaoSegurancaAplicativoRealizada(
        boolean bloqueada,
        boolean modoObservacao,
        int scoreRisco,
        List<String> sinaisCalculados
) {
    public AvaliacaoSegurancaAplicativoRealizada {
        sinaisCalculados = sinaisCalculados == null ? List.of() : List.copyOf(sinaisCalculados);
    }

    @Override
    public List<String> sinaisCalculados() {
        return List.copyOf(sinaisCalculados);
    }
}
