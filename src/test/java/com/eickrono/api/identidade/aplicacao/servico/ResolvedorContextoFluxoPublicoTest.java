package com.eickrono.api.identidade.aplicacao.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eickrono.api.identidade.aplicacao.modelo.ContextoSolicitacaoFluxoPublico;
import java.sql.ResultSet;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class ResolvedorContextoFluxoPublicoTest {

    private NamedParameterJdbcTemplate jdbcTemplate;
    private ResolvedorContextoFluxoPublico resolvedor;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        resolvedor = new ResolvedorContextoFluxoPublico(jdbcTemplate);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void deveResolverContextoPeloModeloMultiappCanonico() throws Exception {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getObject("atualizado_em", OffsetDateTime.class))
                .thenReturn(OffsetDateTime.parse("2026-05-29T12:00:00Z"));
        when(resultSet.getString("locale_solicitante")).thenReturn("pt-BR");
        when(resultSet.getString("time_zone_solicitante")).thenReturn("America/Sao_Paulo");
        when(resultSet.getString("tipo_produto_exibicao")).thenReturn("app");
        when(resultSet.getString("produto_exibicao")).thenReturn("Thimisu");
        when(resultSet.getString("canal_exibicao")).thenReturn("mobile");
        when(resultSet.getString("empresa_exibicao")).thenReturn("Eickrono");
        when(resultSet.getString("ambiente_exibicao")).thenReturn("hml");
        when(jdbcTemplate.query(
                anyString(),
                any(MapSqlParameterSource.class),
                any(RowMapper.class)
        )).thenAnswer(invocation -> {
            RowMapper<Object> rowMapper = invocation.getArgument(2);
            return List.of(rowMapper.mapRow(resultSet, 0));
        });

        ContextoSolicitacaoFluxoPublico contexto = resolvedor.resolver(
                " USUARIO@EXEMPLO.COM ",
                new ContextoSolicitacaoFluxoPublico(null, null, null, null, null, null, null)
        );

        assertThat(contexto.locale()).isEqualTo("pt-BR");
        assertThat(contexto.timeZone()).isEqualTo("America/Sao_Paulo");
        assertThat(contexto.tipoProdutoExibicao()).isEqualTo("app");
        assertThat(contexto.produtoExibicao()).isEqualTo("Thimisu");
        assertThat(contexto.canalExibicao()).isEqualTo("mobile");
        assertThat(contexto.empresaExibicao()).isEqualTo("Eickrono");
        assertThat(contexto.ambienteExibicao()).isEqualTo("hml");

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MapSqlParameterSource> paramsCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), paramsCaptor.capture(), any(RowMapper.class));
        assertThat(sqlCaptor.getValue()).contains("autenticacao.cadastros_conta");
        assertThat(sqlCaptor.getValue()).contains("identidade.contatos_email");
        assertThat(paramsCaptor.getValue().getValue("email")).isEqualTo("usuario@exemplo.com");
    }

    @Test
    void devePreservarContextoAtualQuandoEmailForInvalido() {
        ContextoSolicitacaoFluxoPublico contextoAtual = new ContextoSolicitacaoFluxoPublico(
                "pt-BR",
                null,
                null,
                "Thimisu",
                null,
                null,
                null
        );

        ContextoSolicitacaoFluxoPublico contexto = resolvedor.resolver("   ", contextoAtual);

        assertThat(contexto.locale()).isEqualTo("pt-BR");
        assertThat(contexto.produtoExibicao()).isEqualTo("Thimisu");
    }
}
