package com.eickrono.api.identidade.aplicacao.servico;

import com.eickrono.api.identidade.aplicacao.modelo.ContextoSolicitacaoFluxoPublico;
import com.eickrono.api.identidade.infraestrutura.configuracao.CadastroEmailProperties;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Objects;

final class FormatadorContextoEmailFluxoPublico {

    private static final DateTimeFormatter FORMATADOR_DATA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FORMATADOR_DATA_HORA_UTC =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm 'UTC'");

    private FormatadorContextoEmailFluxoPublico() {
    }

    static String descreverOrigem(final CadastroEmailProperties properties,
                                  final ContextoSolicitacaoFluxoPublico contextoSolicitacao) {
        ContextoSolicitacaoFluxoPublico contexto = contextoSolicitacao == null
                ? new ContextoSolicitacaoFluxoPublico(null, null, null, null, null, null, null)
                : contextoSolicitacao.sanitizado();
        String base = montarBaseExibicao(contexto);
        if (base == null) {
            return Objects.requireNonNullElse(properties.getNomeAplicacao(), "Eickrono");
        }
        if (ambienteVisivel(contexto.ambienteExibicao())) {
            return base + " [" + contexto.ambienteExibicao() + "]";
        }
        return base;
    }

    static String formatarValidadeLocal(final OffsetDateTime validadeUtc,
                                        final ContextoSolicitacaoFluxoPublico contextoSolicitacao) {
        ZoneId zoneId = resolverZoneId(contextoSolicitacao == null ? null : contextoSolicitacao.timeZone());
        if (zoneId == null) {
            return FORMATADOR_DATA_HORA_UTC.format(validadeUtc);
        }
        Locale locale = resolverLocale(contextoSolicitacao == null ? null : contextoSolicitacao.locale());
        ZonedDateTime instanteLocal = validadeUtc.atZoneSameInstant(zoneId);
        return "%s (%s)".formatted(
                FORMATADOR_DATA_HORA.withLocale(locale).format(instanteLocal),
                descreverFuso(zoneId, locale)
        );
    }

    static String formatarReferenciaUtc(final OffsetDateTime validadeUtc) {
        return FORMATADOR_DATA_HORA_UTC.format(validadeUtc.withOffsetSameInstant(ZoneOffset.UTC));
    }

    private static String montarBaseExibicao(final ContextoSolicitacaoFluxoPublico contexto) {
        String tipo = contexto.tipoProdutoExibicao();
        String produto = contexto.produtoExibicao();
        String empresa = contexto.empresaExibicao();
        if (tipo == null && produto == null && empresa == null) {
            return null;
        }

        StringBuilder descricao = new StringBuilder();
        if (tipo != null) {
            descricao.append(tipo);
        }
        if (produto != null) {
            if (descricao.length() > 0) {
                descricao.append(' ');
            }
            descricao.append(produto);
        }
        if (empresa != null) {
            if (descricao.length() > 0) {
                descricao.append(" da ");
            }
            descricao.append(empresa);
        }
        return descricao.toString();
    }

    private static boolean ambienteVisivel(final String ambienteExibicao) {
        if (ambienteExibicao == null || ambienteExibicao.isBlank()) {
            return false;
        }
        String valor = ambienteExibicao.trim().toLowerCase(Locale.ROOT);
        return !valor.equals("prod")
                && !valor.equals("producao")
                && !valor.equals("production");
    }

    private static Locale resolverLocale(final String localeTexto) {
        if (localeTexto == null || localeTexto.isBlank()) {
            return Locale.forLanguageTag("pt-BR");
        }
        return Locale.forLanguageTag(localeTexto.replace('_', '-'));
    }

    private static ZoneId resolverZoneId(final String timeZone) {
        if (timeZone == null || timeZone.isBlank()) {
            return null;
        }
        String valor = timeZone.trim();
        try {
            return ZoneId.of(valor);
        } catch (Exception ignored) {
        }
        try {
            return ZoneId.of(valor, ZoneId.SHORT_IDS);
        } catch (Exception ignored) {
        }
        try {
            return ZoneOffset.of(valor);
        } catch (Exception ignored) {
        }
        if (valor.startsWith("UTC") && valor.length() > 3) {
            try {
                return ZoneOffset.of(valor.substring(3));
            } catch (Exception ignored) {
            }
        }
        if (valor.startsWith("GMT") && valor.length() > 3) {
            try {
                return ZoneOffset.of(valor.substring(3));
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static String descreverFuso(final ZoneId zoneId, final Locale locale) {
        if (zoneId instanceof ZoneOffset) {
            return zoneId.getId();
        }
        String identificador = zoneId.getId();
        if (identificador.contains("/")) {
            String cidade = identificador.substring(identificador.lastIndexOf('/') + 1)
                    .replace('_', ' ');
            return "horario de " + cidade;
        }
        return zoneId.getDisplayName(TextStyle.SHORT, locale);
    }
}
