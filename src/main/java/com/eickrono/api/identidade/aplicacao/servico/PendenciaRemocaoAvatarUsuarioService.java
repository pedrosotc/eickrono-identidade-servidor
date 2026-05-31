package com.eickrono.api.identidade.aplicacao.servico;

import com.eickrono.api.identidade.apresentacao.dto.avatar.MaterializarPendenciasRemocaoAvatarInternoApiRequest;
import com.eickrono.api.identidade.apresentacao.dto.avatar.MaterializarPendenciasRemocaoAvatarInternoApiResponse;
import com.eickrono.api.identidade.infraestrutura.configuracao.AvatarRemocaoProperties;
import com.eickrono.api.identidade.infraestrutura.configuracao.AvatarStorageProperties;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PendenciaRemocaoAvatarUsuarioService {

    private static final String STATUS_PENDENTE = "PENDENTE";

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final AvatarStorageProperties storageProperties;
    private final AvatarRemocaoProperties remocaoProperties;

    public PendenciaRemocaoAvatarUsuarioService(final NamedParameterJdbcTemplate jdbcTemplate,
                                               final AvatarStorageProperties storageProperties,
                                               final AvatarRemocaoProperties remocaoProperties) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate é obrigatório");
        this.storageProperties = Objects.requireNonNull(storageProperties, "storageProperties é obrigatório");
        this.remocaoProperties = Objects.requireNonNull(remocaoProperties, "remocaoProperties é obrigatório");
    }

    @Transactional
    public MaterializarPendenciasRemocaoAvatarInternoApiResponse materializar(
            final MaterializarPendenciasRemocaoAvatarInternoApiRequest request) {
        UUID correlacaoId = uuidObrigatorio(request.correlacaoId(), "correlacaoId");
        String produto = textoObrigatorio(request.produto(), "produto").toUpperCase(Locale.ROOT);
        List<UUID> usuarioClienteIds = uuidsObrigatorios(request.usuarioClienteIds(), "usuarioClienteIds");
        String bucket = textoObrigatorio(storageProperties.getBucket(), "identidade.avatar.storage.bucket");
        OffsetDateTime retencaoExpiraEm = OffsetDateTime.now()
                .plusDays(Math.max(1, remocaoProperties.getPendenciaRetencaoDias()));

        List<AvatarRemovivel> avatares = buscarAvataresRemoviveis(usuarioClienteIds);
        List<String> avatarIds = new ArrayList<>();
        List<String> storageKeys = new ArrayList<>();
        int materializadas = 0;
        for (AvatarRemovivel avatar : avatares) {
            int linhas = inserirPendencia(correlacaoId, produto, bucket, retencaoExpiraEm, avatar);
            if (linhas > 0) {
                materializadas++;
            }
            avatarIds.add(avatar.avatarId().toString());
            storageKeys.add(avatar.storageKey());
        }

        return new MaterializarPendenciasRemocaoAvatarInternoApiResponse(
                correlacaoId.toString(),
                produto,
                materializadas,
                List.copyOf(avatarIds),
                List.copyOf(storageKeys)
        );
    }

    private List<AvatarRemovivel> buscarAvataresRemoviveis(final List<UUID> usuarioClienteIds) {
        List<Map<String, Object>> linhas = jdbcTemplate.queryForList(
                """
                SELECT avatar.id AS avatar_id,
                       avatar.usuario_cliente_id,
                       origem.codigo AS origem,
                       avatar.storage_key
                  FROM identidade.avatar_usuario avatar
                  JOIN identidade.avatar_origens origem
                    ON origem.id = avatar.origem_id
                 WHERE avatar.usuario_cliente_id IN (:usuarioClienteIds)
                   AND avatar.storage_key IS NOT NULL
                   AND BTRIM(avatar.storage_key) <> ''
                   AND avatar.removido_em IS NULL
                 ORDER BY avatar.criado_em, avatar.id
                """,
                Map.of("usuarioClienteIds", usuarioClienteIds)
        );
        return linhas.stream()
                .map(PendenciaRemocaoAvatarUsuarioService::mapearAvatarRemovivel)
                .toList();
    }

    private int inserirPendencia(final UUID correlacaoId,
                                 final String produto,
                                 final String bucket,
                                 final OffsetDateTime retencaoExpiraEm,
                                 final AvatarRemovivel avatar) {
        MapSqlParameterSource parametros = new MapSqlParameterSource()
                .addValue("id", UUID.randomUUID())
                .addValue("correlacaoId", correlacaoId)
                .addValue("avatarId", avatar.avatarId())
                .addValue("usuarioClienteId", avatar.usuarioClienteId())
                .addValue("produto", produto)
                .addValue("origem", avatar.origem())
                .addValue("bucket", bucket)
                .addValue("storageKey", avatar.storageKey())
                .addValue("status", STATUS_PENDENTE)
                .addValue("retencaoExpiraEm", retencaoExpiraEm);
        return jdbcTemplate.update(
                """
                INSERT INTO identidade.pendencias_remocao_avatar_usuario (
                    id,
                    correlacao_id,
                    avatar_id,
                    usuario_cliente_id,
                    produto,
                    origem,
                    bucket,
                    storage_key,
                    status,
                    retencao_expira_em
                ) VALUES (
                    :id,
                    :correlacaoId,
                    :avatarId,
                    :usuarioClienteId,
                    :produto,
                    :origem,
                    :bucket,
                    :storageKey,
                    :status,
                    :retencaoExpiraEm
                )
                ON CONFLICT (avatar_id, storage_key) DO NOTHING
                """,
                parametros
        );
    }

    private static AvatarRemovivel mapearAvatarRemovivel(final Map<String, Object> linha) {
        String storageKey = (String) linha.get("storage_key");
        validarStorageKey(storageKey);
        return new AvatarRemovivel(
                (UUID) linha.get("avatar_id"),
                (UUID) linha.get("usuario_cliente_id"),
                (String) linha.get("origem"),
                storageKey
        );
    }

    private static void validarStorageKey(final String storageKey) {
        if (!StringUtils.hasText(storageKey)
                || storageKey.startsWith("/")
                || storageKey.contains("..")
                || !storageKey.startsWith("avatares/")) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "storageKey de avatar invalida.");
        }
    }

    private static UUID uuidObrigatorio(final String valor, final String campo) {
        String normalizado = textoObrigatorio(valor, campo);
        try {
            return UUID.fromString(normalizado);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, campo + " invalido.", ex);
        }
    }

    private static List<UUID> uuidsObrigatorios(final List<String> valores, final String campo) {
        if (valores == null || valores.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, campo + " é obrigatório.");
        }
        return valores.stream()
                .map(valor -> uuidObrigatorio(valor, campo))
                .toList();
    }

    private static String textoObrigatorio(final String valor, final String campo) {
        if (!StringUtils.hasText(valor)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, campo + " é obrigatório.");
        }
        return valor.trim();
    }

    private record AvatarRemovivel(UUID avatarId, UUID usuarioClienteId, String origem, String storageKey) {
    }
}
