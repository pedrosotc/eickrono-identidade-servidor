package com.eickrono.api.identidade.aplicacao.modelo;

public record ContextoSolicitacaoFluxoPublico(
        String locale,
        String timeZone,
        String tipoProdutoExibicao,
        String produtoExibicao,
        String canalExibicao,
        String empresaExibicao,
        String ambienteExibicao
) {

    public ContextoSolicitacaoFluxoPublico sanitizado() {
        return new ContextoSolicitacaoFluxoPublico(
                normalizar(locale, 16),
                normalizar(timeZone, 64),
                normalizar(tipoProdutoExibicao, 32),
                normalizar(produtoExibicao, 128),
                normalizar(canalExibicao, 64),
                normalizar(empresaExibicao, 128),
                normalizar(ambienteExibicao, 32)
        );
    }

    public ContextoSolicitacaoFluxoPublico mesclarFaltantes(final ContextoSolicitacaoFluxoPublico fallback) {
        if (fallback == null) {
            return sanitizado();
        }
        ContextoSolicitacaoFluxoPublico atual = sanitizado();
        ContextoSolicitacaoFluxoPublico reserva = fallback.sanitizado();
        return new ContextoSolicitacaoFluxoPublico(
                escolher(atual.locale(), reserva.locale()),
                escolher(atual.timeZone(), reserva.timeZone()),
                escolher(atual.tipoProdutoExibicao(), reserva.tipoProdutoExibicao()),
                escolher(atual.produtoExibicao(), reserva.produtoExibicao()),
                escolher(atual.canalExibicao(), reserva.canalExibicao()),
                escolher(atual.empresaExibicao(), reserva.empresaExibicao()),
                escolher(atual.ambienteExibicao(), reserva.ambienteExibicao())
        );
    }

    public boolean vazio() {
        ContextoSolicitacaoFluxoPublico contexto = sanitizado();
        return contexto.locale() == null
                && contexto.timeZone() == null
                && contexto.tipoProdutoExibicao() == null
                && contexto.produtoExibicao() == null
                && contexto.canalExibicao() == null
                && contexto.empresaExibicao() == null
                && contexto.ambienteExibicao() == null;
    }

    private static String escolher(final String prioritario, final String fallback) {
        return prioritario != null ? prioritario : fallback;
    }

    private static String normalizar(final String valor, final int tamanhoMaximo) {
        if (valor == null) {
            return null;
        }
        String texto = valor.trim();
        if (texto.isEmpty()) {
            return null;
        }
        if (texto.length() <= tamanhoMaximo) {
            return texto;
        }
        return texto.substring(0, tamanhoMaximo);
    }
}
