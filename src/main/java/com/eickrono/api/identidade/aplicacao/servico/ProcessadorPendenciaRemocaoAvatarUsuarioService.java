package com.eickrono.api.identidade.aplicacao.servico;

import com.eickrono.api.identidade.infraestrutura.configuracao.AvatarRemocaoProperties;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
@ConditionalOnBean(S3Client.class)
public class ProcessadorPendenciaRemocaoAvatarUsuarioService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessadorPendenciaRemocaoAvatarUsuarioService.class);
    private static final String STATUS_PENDENTE = "PENDENTE";
    private static final String STATUS_PROCESSANDO = "PROCESSANDO";
    private static final String STATUS_REMOVIDA = "REMOVIDA";
    private static final String STATUS_FALHOU = "FALHOU";

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final S3Client s3Client;
    private final AvatarRemocaoProperties properties;

    public ProcessadorPendenciaRemocaoAvatarUsuarioService(final NamedParameterJdbcTemplate jdbcTemplate,
                                                          final S3Client s3Client,
                                                          final AvatarRemocaoProperties properties) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate é obrigatório");
        this.s3Client = Objects.requireNonNull(s3Client, "s3Client é obrigatório");
        this.properties = Objects.requireNonNull(properties, "properties é obrigatório");
    }

    @Scheduled(fixedDelayString = "${identidade.avatar.remocao.worker-intervalo-ms:60000}")
    public void processarAgendado() {
        if (!properties.isWorkerHabilitado()) {
            return;
        }
        processarPendencias();
    }

    @Transactional
    public int processarPendencias() {
        List<PendenciaRemocaoAvatar> pendencias = buscarPendencias();
        int removidas = 0;
        for (PendenciaRemocaoAvatar pendencia : pendencias) {
            if (processarPendencia(pendencia)) {
                removidas++;
            }
        }
        return removidas;
    }

    private List<PendenciaRemocaoAvatar> buscarPendencias() {
        return jdbcTemplate.queryForList(
                """
                SELECT id,
                       bucket,
                       storage_key,
                       tentativas
                  FROM identidade.pendencias_remocao_avatar_usuario
                 WHERE status IN (:statuses)
                   AND tentativas < :tentativasMaximas
                 ORDER BY solicitada_em, id
                 LIMIT :limite
                """,
                Map.of(
                        "statuses", List.of(STATUS_PENDENTE, STATUS_FALHOU),
                        "tentativasMaximas", Math.max(1, properties.getTentativasMaximas()),
                        "limite", Math.max(1, properties.getLoteMaximo())
                )
        ).stream().map(ProcessadorPendenciaRemocaoAvatarUsuarioService::mapearPendencia).toList();
    }

    private boolean processarPendencia(final PendenciaRemocaoAvatar pendencia) {
        validarStorageKey(pendencia.storageKey());
        marcarProcessando(pendencia.id());
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(pendencia.bucket())
                    .key(pendencia.storageKey())
                    .build());
            marcarRemovida(pendencia.id());
            LOGGER.info(
                    "qa_avatar_remocao_storage_removida pendenciaId={} bucket={} storageKey={}",
                    pendencia.id(),
                    pendencia.bucket(),
                    pendencia.storageKey()
            );
            return true;
        } catch (NoSuchKeyException ex) {
            marcarRemovida(pendencia.id());
            LOGGER.info(
                    "qa_avatar_remocao_storage_ja_ausente pendenciaId={} bucket={} storageKey={}",
                    pendencia.id(),
                    pendencia.bucket(),
                    pendencia.storageKey()
            );
            return true;
        } catch (S3Exception | SdkClientException ex) {
            marcarFalha(pendencia.id(), ex.getClass().getSimpleName(), ex.getMessage());
            LOGGER.warn(
                    "qa_avatar_remocao_storage_falhou pendenciaId={} bucket={} storageKey={} erro={}",
                    pendencia.id(),
                    pendencia.bucket(),
                    pendencia.storageKey(),
                    ex.getClass().getSimpleName()
            );
            return false;
        }
    }

    private void marcarProcessando(final UUID pendenciaId) {
        jdbcTemplate.update(
                """
                UPDATE identidade.pendencias_remocao_avatar_usuario
                   SET status = :status,
                       tentativas = tentativas + 1,
                       atualizado_em = :agora
                 WHERE id = :id
                """,
                new MapSqlParameterSource()
                        .addValue("status", STATUS_PROCESSANDO)
                        .addValue("agora", OffsetDateTime.now())
                        .addValue("id", pendenciaId)
        );
    }

    private void marcarRemovida(final UUID pendenciaId) {
        jdbcTemplate.update(
                """
                UPDATE identidade.pendencias_remocao_avatar_usuario
                   SET status = :status,
                       processada_em = :agora,
                       atualizado_em = :agora,
                       erro_codigo = NULL,
                       erro_mensagem = NULL
                 WHERE id = :id
                """,
                new MapSqlParameterSource()
                        .addValue("status", STATUS_REMOVIDA)
                        .addValue("agora", OffsetDateTime.now())
                        .addValue("id", pendenciaId)
        );
    }

    private void marcarFalha(final UUID pendenciaId, final String erroCodigo, final String erroMensagem) {
        jdbcTemplate.update(
                """
                UPDATE identidade.pendencias_remocao_avatar_usuario
                   SET status = :status,
                       erro_codigo = :erroCodigo,
                       erro_mensagem = :erroMensagem,
                       atualizado_em = :agora
                 WHERE id = :id
                """,
                new MapSqlParameterSource()
                        .addValue("status", STATUS_FALHOU)
                        .addValue("erroCodigo", erroCodigo)
                        .addValue("erroMensagem", erroMensagem)
                        .addValue("agora", OffsetDateTime.now())
                        .addValue("id", pendenciaId)
        );
    }

    private static PendenciaRemocaoAvatar mapearPendencia(final Map<String, Object> linha) {
        return new PendenciaRemocaoAvatar(
                (UUID) linha.get("id"),
                (String) linha.get("bucket"),
                (String) linha.get("storage_key")
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

    private record PendenciaRemocaoAvatar(UUID id, String bucket, String storageKey) {
    }
}
