package com.eickrono.api.identidade.aplicacao.servico;

import com.eickrono.api.identidade.aplicacao.modelo.ContextoSolicitacaoFluxoPublico;
import com.eickrono.api.identidade.dominio.modelo.CadastroConta;
import com.eickrono.api.identidade.dominio.modelo.RecuperacaoSenha;
import com.eickrono.api.identidade.dominio.repositorio.CadastroContaRepositorio;
import com.eickrono.api.identidade.dominio.repositorio.RecuperacaoSenhaRepositorio;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ResolvedorContextoFluxoPublico {

    private static final String SQL_CONTEXTOS_MULTIAPP = """
            SELECT atualizado_em,
                   locale_solicitante,
                   time_zone_solicitante,
                   tipo_produto_exibicao,
                   produto_exibicao,
                   canal_exibicao,
                   empresa_exibicao,
                   ambiente_exibicao
              FROM autenticacao.cadastros_conta
             WHERE email_id = (
                    SELECT email.id
                      FROM identidade.contatos_email email
                     WHERE LOWER(email.email) = :email
                     ORDER BY email.atualizado_em DESC
                     LIMIT 1
                   )
            UNION ALL
            SELECT atualizado_em,
                   locale_solicitante,
                   time_zone_solicitante,
                   tipo_produto_exibicao,
                   produto_exibicao,
                   canal_exibicao,
                   empresa_exibicao,
                   ambiente_exibicao
              FROM autenticacao.recuperacoes_senha
             WHERE email_id = (
                    SELECT email.id
                      FROM identidade.contatos_email email
                     WHERE LOWER(email.email) = :email
                     ORDER BY email.atualizado_em DESC
                     LIMIT 1
                   )
             ORDER BY atualizado_em DESC
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final CadastroContaRepositorio cadastroContaRepositorio;
    private final RecuperacaoSenhaRepositorio recuperacaoSenhaRepositorio;

    @Autowired
    public ResolvedorContextoFluxoPublico(final NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate e obrigatorio");
        this.cadastroContaRepositorio = null;
        this.recuperacaoSenhaRepositorio = null;
    }

    /**
     * Compatibilidade para testes/construtores legados enquanto os serviços que
     * ainda expõem IDs antigos não forem migrados para o modelo canônico.
     */
    public ResolvedorContextoFluxoPublico(final CadastroContaRepositorio cadastroContaRepositorio,
                                          final RecuperacaoSenhaRepositorio recuperacaoSenhaRepositorio) {
        this.jdbcTemplate = null;
        this.cadastroContaRepositorio = Objects.requireNonNull(
                cadastroContaRepositorio, "cadastroContaRepositorio e obrigatorio");
        this.recuperacaoSenhaRepositorio = Objects.requireNonNull(
                recuperacaoSenhaRepositorio, "recuperacaoSenhaRepositorio e obrigatorio");
    }

    public ContextoSolicitacaoFluxoPublico resolver(final String emailPrincipal,
                                                    final ContextoSolicitacaoFluxoPublico contextoAtual) {
        ContextoSolicitacaoFluxoPublico resolvido = contextoAtual == null
                ? new ContextoSolicitacaoFluxoPublico(null, null, null, null, null, null, null)
                : contextoAtual.sanitizado();
        String emailNormalizado = normalizarEmail(emailPrincipal);
        if (emailNormalizado == null) {
            return resolvido;
        }

        List<ContextoPersistido> historicos = new ArrayList<>();
        if (jdbcTemplate != null) {
            historicos.addAll(buscarContextosMultiapp(emailNormalizado));
        } else {
            historicos.addAll(buscarContextosLegados(emailNormalizado));
        }
        historicos.sort(Comparator.comparing(ContextoPersistido::atualizadoEm).reversed());

        for (final ContextoPersistido contextoPersistido : historicos) {
            resolvido = resolvido.mesclarFaltantes(contextoPersistido.contexto());
        }
        return resolvido;
    }

    private List<ContextoPersistido> buscarContextosMultiapp(final String emailNormalizado) {
        return jdbcTemplate.query(
                SQL_CONTEXTOS_MULTIAPP,
                new MapSqlParameterSource("email", emailNormalizado),
                (rs, rowNum) -> deLinhaContexto(rs)
        );
    }

    private List<ContextoPersistido> buscarContextosLegados(final String emailNormalizado) {
        List<ContextoPersistido> historicos = new ArrayList<>();
        cadastroContaRepositorio.findTopByEmailPrincipalOrderByAtualizadoEmDesc(emailNormalizado)
                .map(this::deCadastro)
                .ifPresent(historicos::add);
        recuperacaoSenhaRepositorio.findTopByEmailPrincipalOrderByAtualizadoEmDesc(emailNormalizado)
                .map(this::deRecuperacao)
                .ifPresent(historicos::add);
        return historicos;
    }

    private ContextoPersistido deLinhaContexto(final ResultSet rs) throws SQLException {
        return new ContextoPersistido(
                rs.getObject("atualizado_em", OffsetDateTime.class),
                new ContextoSolicitacaoFluxoPublico(
                        rs.getString("locale_solicitante"),
                        rs.getString("time_zone_solicitante"),
                        rs.getString("tipo_produto_exibicao"),
                        rs.getString("produto_exibicao"),
                        rs.getString("canal_exibicao"),
                        rs.getString("empresa_exibicao"),
                        rs.getString("ambiente_exibicao")
                )
        );
    }

    private ContextoPersistido deCadastro(final CadastroConta cadastroConta) {
        return new ContextoPersistido(
                cadastroConta.getAtualizadoEm(),
                new ContextoSolicitacaoFluxoPublico(
                        cadastroConta.getLocaleSolicitante(),
                        cadastroConta.getTimeZoneSolicitante(),
                        cadastroConta.getTipoProdutoExibicao(),
                        cadastroConta.getProdutoExibicao(),
                        cadastroConta.getCanalExibicao(),
                        cadastroConta.getEmpresaExibicao(),
                        cadastroConta.getAmbienteExibicao()
                )
        );
    }

    private ContextoPersistido deRecuperacao(final RecuperacaoSenha recuperacaoSenha) {
        return new ContextoPersistido(
                recuperacaoSenha.getAtualizadoEm(),
                new ContextoSolicitacaoFluxoPublico(
                        recuperacaoSenha.getLocaleSolicitante(),
                        recuperacaoSenha.getTimeZoneSolicitante(),
                        recuperacaoSenha.getTipoProdutoExibicao(),
                        recuperacaoSenha.getProdutoExibicao(),
                        recuperacaoSenha.getCanalExibicao(),
                        recuperacaoSenha.getEmpresaExibicao(),
                        recuperacaoSenha.getAmbienteExibicao()
                )
        );
    }

    private String normalizarEmail(final String emailPrincipal) {
        if (emailPrincipal == null) {
            return null;
        }
        String valor = emailPrincipal.trim().toLowerCase(Locale.ROOT);
        return valor.isEmpty() ? null : valor;
    }

    private record ContextoPersistido(
            OffsetDateTime atualizadoEm,
            ContextoSolicitacaoFluxoPublico contexto
    ) {
    }
}
