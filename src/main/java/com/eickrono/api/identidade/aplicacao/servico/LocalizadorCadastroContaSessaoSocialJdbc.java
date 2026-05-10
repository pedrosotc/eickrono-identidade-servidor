package com.eickrono.api.identidade.aplicacao.servico;

import com.eickrono.api.identidade.aplicacao.modelo.CadastroContaSessaoSocialResolvido;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class LocalizadorCadastroContaSessaoSocialJdbc {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final HexFormat hexFormat = HexFormat.of();

    public LocalizadorCadastroContaSessaoSocialJdbc(final NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate é obrigatório");
    }

    public Optional<CadastroContaSessaoSocialResolvido> localizarPorSub(final String subRemoto) {
        if (!StringUtils.hasText(subRemoto)) {
            return Optional.empty();
        }
        String subNormalizado = subRemoto.trim();
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("subRemoto", subNormalizado)
                .addValue("pessoaId", gerarPessoaId(subNormalizado));
        return jdbcTemplate.query("""
                SELECT cadastro.id AS cadastro_id,
                       (cadastro.email_confirmado_em IS NOT NULL) AS email_confirmado
                FROM autenticacao.cadastros_conta cadastro
                LEFT JOIN autenticacao.usuarios usuario
                  ON usuario.id = cadastro.usuario_id
                WHERE usuario.sub_remoto = :subRemoto
                   OR cadastro.pessoa_id = :pessoaId
                ORDER BY cadastro.atualizado_em DESC
                LIMIT 1
                """, params, this::mapear).stream().findFirst();
    }

    private CadastroContaSessaoSocialResolvido mapear(final ResultSet rs, final int rowNum) throws SQLException {
        return new CadastroContaSessaoSocialResolvido(
                rs.getObject("cadastro_id", UUID.class),
                rs.getBoolean("email_confirmado")
        );
    }

    private UUID gerarPessoaId(final String subRemoto) {
        return gerarUuidDeterministico("identidade.pessoa:" + subRemoto);
    }

    private UUID gerarUuidDeterministico(final String valorBase) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(valorBase.getBytes(StandardCharsets.UTF_8));
            String hex = hexFormat.formatHex(hash);
            return UUID.fromString(
                    hex.substring(0, 8) + "-" +
                            hex.substring(8, 12) + "-" +
                            hex.substring(12, 16) + "-" +
                            hex.substring(16, 20) + "-" +
                            hex.substring(20, 32)
            );
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo MD5 indisponível para geração determinística", e);
        }
    }
}
