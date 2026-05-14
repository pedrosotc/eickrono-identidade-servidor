package com.eickrono.api.identidade.aplicacao.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@ExtendWith(MockitoExtension.class)
class ContextoSocialPendenteJdbcTest {

    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Test
    void consumirSeCompativelUsaAliasIdAoLocalizarUsuarioPorEmail() throws Exception {
        UUID contextoId = UUID.fromString("aaaaaaaa-1111-2222-3333-bbbbbbbbbbbb");
        UUID usuarioId = UUID.fromString("cccccccc-1111-2222-3333-dddddddddddd");
        List<String> sqlsConsultadas = new ArrayList<>();
        AtomicInteger indiceConsulta = new AtomicInteger();
        ContextoSocialPendenteJdbc contextoSocialPendenteJdbc = new ContextoSocialPendenteJdbc(
                jdbcTemplate,
                Clock.fixed(Instant.parse("2026-05-13T04:44:00Z"), ZoneOffset.UTC)
        );
        when(jdbcTemplate.query(
                anyString(),
                any(MapSqlParameterSource.class),
                org.mockito.ArgumentMatchers.<RowMapper<Object>>any()))
                .thenAnswer(invocacao -> {
                    String sql = invocacao.getArgument(0);
                    RowMapper<?> rowMapper = invocacao.getArgument(2);
                    sqlsConsultadas.add(sql);
                    ResultSet resultSet = indiceConsulta.getAndIncrement() == 0
                            ? contextoPendenteResultSet(contextoId, usuarioId)
                            : usuarioResultSet(usuarioId);
                    return List.of(rowMapper.mapRow(resultSet, 0));
                });
        when(jdbcTemplate.update(anyString(), any(MapSqlParameterSource.class))).thenReturn(1);

        boolean consumido = contextoSocialPendenteJdbc.consumirSeCompativel(contextoId, "pedroso_tc@hotmail.com");

        assertThat(consumido).isTrue();
        assertThat(sqlsConsultadas).hasSize(2);
        assertThat(sqlsConsultadas.get(1)).contains("SELECT u.id AS id");
    }

    private ResultSet contextoPendenteResultSet(final UUID contextoId, final UUID usuarioId) throws Exception {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getObject("id", UUID.class)).thenReturn(contextoId);
        when(resultSet.getLong("cliente_ecossistema_id")).thenReturn(7L);
        when(resultSet.getString("provedor")).thenReturn("apple");
        when(resultSet.getString("identificador_externo")).thenReturn("apple-user-id");
        when(resultSet.getString("nome_usuario_externo")).thenReturn("pedroso_tc");
        when(resultSet.getString("nome_exibicao_externo")).thenReturn("Pedroso");
        when(resultSet.getString("url_avatar_externo")).thenReturn(null);
        when(resultSet.getObject("usuario_id_sugerido", UUID.class)).thenReturn(usuarioId);
        when(resultSet.getString("login_sugerido")).thenReturn("pedroso_tc@hotmail.com");
        when(resultSet.getString("modo_pendente")).thenReturn("ENTRAR_E_VINCULAR");
        when(resultSet.getInt("tentativas_falhas")).thenReturn(0);
        when(resultSet.getInt("tentativas_maximas")).thenReturn(3);
        return resultSet;
    }

    private ResultSet usuarioResultSet(final UUID usuarioId) throws Exception {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getObject("id", UUID.class)).thenReturn(usuarioId);
        return resultSet;
    }
}
