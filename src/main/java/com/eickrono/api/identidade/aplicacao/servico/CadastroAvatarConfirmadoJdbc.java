package com.eickrono.api.identidade.aplicacao.servico;

import com.eickrono.api.identidade.aplicacao.modelo.ArquivoAvatarArmazenado;
import com.eickrono.api.identidade.aplicacao.modelo.AvatarCadastroConfirmado;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Component
public class CadastroAvatarConfirmadoJdbc {

    private static final Logger LOGGER = LoggerFactory.getLogger(CadastroAvatarConfirmadoJdbc.class);

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final Clock clock;
    private final AvatarUsuarioUploadService avatarUsuarioUploadService;

    public CadastroAvatarConfirmadoJdbc(final NamedParameterJdbcTemplate jdbcTemplate,
                                        final Clock clock,
                                        final AvatarUsuarioUploadService avatarUsuarioUploadService) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate é obrigatório");
        this.clock = Objects.requireNonNull(clock, "clock é obrigatório");
        this.avatarUsuarioUploadService = Objects.requireNonNull(
                avatarUsuarioUploadService,
                "avatarUsuarioUploadService é obrigatório");
    }

    @Transactional
    public void registrar(final UUID cadastroId,
                          final List<AvatarCadastroConfirmado> avatares) {
        if (cadastroId == null || avatares == null || avatares.isEmpty()) {
            return;
        }
        OffsetDateTime agora = OffsetDateTime.now(clock);
        jdbcTemplate.update("""
                DELETE FROM autenticacao.cadastros_conta_avatares
                 WHERE cadastro_id = :cadastroId
                """, new MapSqlParameterSource("cadastroId", cadastroId));
        int gravados = 0;
        for (AvatarCadastroConfirmado avatar : avatares) {
            if (avatar == null) {
                continue;
            }
            AvatarCadastroConfirmado materializado = materializar(avatar);
            int linhas = registrarAvatar(cadastroId, materializado, agora);
            if (linhas == 0) {
                throw new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "Origem de avatar de cadastro nao configurada."
                );
            }
            gravados += linhas;
        }
        LOGGER.info(
                "qa_avatar_cadastro_confirmado_registrado cadastroId={} avatares={}",
                cadastroId,
                gravados
        );
    }

    @Transactional
    public void consumirParaUsuario(final UUID cadastroId,
                                    final String subjectRemoto,
                                    final Long clienteEcossistemaId,
                                    final OffsetDateTime atualizadoEm) {
        if (cadastroId == null || !StringUtils.hasText(subjectRemoto)
                || clienteEcossistemaId == null || atualizadoEm == null) {
            return;
        }
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("cadastroId", cadastroId)
                .addValue("subjectRemoto", subjectRemoto.trim())
                .addValue("clienteEcossistemaId", clienteEcossistemaId)
                .addValue("atualizadoEm", atualizadoEm);

        jdbcTemplate.update("""
                UPDATE identidade.avatar_usuario avatar
                   SET preferido = FALSE,
                       atualizado_em = :atualizadoEm
                  FROM autenticacao.usuarios usuario
                  JOIN autenticacao.usuarios_clientes_ecossistema usuario_cliente
                    ON usuario_cliente.usuario_id = usuario.id
                   AND usuario_cliente.cliente_ecossistema_id = :clienteEcossistemaId
                 WHERE usuario.sub_remoto = :subjectRemoto
                   AND avatar.usuario_cliente_id = usuario_cliente.id
                   AND avatar.preferido IS TRUE
                   AND avatar.removido_em IS NULL
                   AND EXISTS (
                       SELECT 1
                         FROM autenticacao.cadastros_conta_avatares cadastro_avatar
                        WHERE cadastro_avatar.cadastro_id = :cadastroId
                          AND cadastro_avatar.preferido IS TRUE
                   )
                """, params);

        int consumidos = jdbcTemplate.update("""
                INSERT INTO identidade.avatar_usuario (
                    id,
                    usuario_cliente_id,
                    origem_id,
                    forma_acesso_id,
                    nome_exibicao_externo,
                    url_avatar,
                    storage_key,
                    content_type,
                    tamanho_bytes,
                    hash_conteudo,
                    versao,
                    preferido,
                    criado_em,
                    atualizado_em,
                    removido_em
                )
                SELECT cadastro_avatar.id,
                       usuario_cliente.id,
                       cadastro_avatar.origem_id,
                       NULL,
                       NULL,
                       cadastro_avatar.url_avatar,
                       cadastro_avatar.storage_key,
                       cadastro_avatar.content_type,
                       cadastro_avatar.tamanho_bytes,
                       cadastro_avatar.hash_conteudo,
                       cadastro_avatar.versao,
                       cadastro_avatar.preferido,
                       cadastro_avatar.criado_em,
                       :atualizadoEm,
                       NULL
                  FROM autenticacao.cadastros_conta_avatares cadastro_avatar
                  JOIN autenticacao.usuarios usuario
                    ON usuario.sub_remoto = :subjectRemoto
                  JOIN autenticacao.usuarios_clientes_ecossistema usuario_cliente
                    ON usuario_cliente.usuario_id = usuario.id
                   AND usuario_cliente.cliente_ecossistema_id = :clienteEcossistemaId
                 WHERE cadastro_avatar.cadastro_id = :cadastroId
                ON CONFLICT (id) DO UPDATE
                SET usuario_cliente_id = EXCLUDED.usuario_cliente_id,
                    origem_id = EXCLUDED.origem_id,
                    url_avatar = EXCLUDED.url_avatar,
                    storage_key = EXCLUDED.storage_key,
                    content_type = EXCLUDED.content_type,
                    tamanho_bytes = EXCLUDED.tamanho_bytes,
                    hash_conteudo = EXCLUDED.hash_conteudo,
                    versao = EXCLUDED.versao,
                    preferido = EXCLUDED.preferido,
                    atualizado_em = EXCLUDED.atualizado_em,
                    removido_em = NULL
                """, params);
        LOGGER.info(
                "qa_avatar_cadastro_confirmado_consumido cadastroId={} subjectRemoto={} clienteEcossistemaId={} avatares={}",
                cadastroId,
                mascararSubject(subjectRemoto),
                clienteEcossistemaId,
                consumidos
        );
    }

    private int registrarAvatar(final UUID cadastroId,
                                final AvatarCadastroConfirmado avatar,
                                final OffsetDateTime agora) {
        String origem = normalizarOrigem(avatar.origem());
        String urlAvatar = normalizarObrigatorio(avatar.urlAvatar(), "urlAvatar");
        return jdbcTemplate.update("""
                INSERT INTO autenticacao.cadastros_conta_avatares (
                    id,
                    cadastro_id,
                    origem_id,
                    url_avatar,
                    storage_key,
                    content_type,
                    tamanho_bytes,
                    hash_conteudo,
                    versao,
                    preferido,
                    criado_em,
                    atualizado_em
                )
                SELECT :id,
                       :cadastroId,
                       origem.id,
                       :urlAvatar,
                       :storageKey,
                       :contentType,
                       :tamanhoBytes,
                       :hashConteudo,
                       :versao,
                       :preferido,
                       :criadoEm,
                       :atualizadoEm
                  FROM identidade.avatar_origens origem
                 WHERE origem.codigo = :origem
                   AND origem.ativo IS TRUE
                """,
                new MapSqlParameterSource()
                        .addValue("id", UUID.randomUUID())
                        .addValue("cadastroId", cadastroId)
                        .addValue("origem", origem)
                        .addValue("urlAvatar", urlAvatar)
                        .addValue("storageKey", normalizarOpcional(avatar.storageKey()))
                        .addValue("contentType", normalizarOpcional(avatar.contentType()))
                        .addValue("tamanhoBytes", avatar.tamanhoBytes())
                        .addValue("hashConteudo", normalizarOpcional(avatar.hashConteudo()))
                        .addValue("versao", normalizarOpcional(avatar.versao()))
                        .addValue("preferido", avatar.preferido())
                        .addValue("criadoEm", agora)
                        .addValue("atualizadoEm", agora));
    }

    private AvatarCadastroConfirmado materializar(final AvatarCadastroConfirmado avatar) {
        if (!StringUtils.hasText(avatar.conteudoBase64())) {
            return avatar;
        }
        ArquivoAvatarArmazenado arquivo = avatarUsuarioUploadService.armazenar(
                avatar.origem(),
                avatar.nomeArquivo(),
                avatar.contentType(),
                avatar.tamanhoBytes(),
                avatar.conteudoBase64()
        );
        return new AvatarCadastroConfirmado(
                avatar.origem(),
                arquivo.urlAvatar(),
                arquivo.storageKey(),
                avatar.nomeArquivo(),
                arquivo.contentType(),
                arquivo.tamanhoBytes(),
                arquivo.hashConteudo(),
                arquivo.versao(),
                null,
                avatar.preferido()
        );
    }

    private static String normalizarObrigatorio(final String valor, final String campo) {
        String normalizado = normalizarOpcional(valor);
        if (normalizado == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, campo + " é obrigatório.");
        }
        return normalizado;
    }

    private static String normalizarOrigem(final String valor) {
        return normalizarObrigatorio(valor, "origem").toUpperCase(Locale.ROOT);
    }

    private static String normalizarOpcional(final String valor) {
        if (valor == null) {
            return null;
        }
        String normalizado = valor.trim();
        return normalizado.isBlank() ? null : normalizado;
    }

    private static String mascararSubject(final String subject) {
        if (subject == null || subject.length() < 8) {
            return "***";
        }
        return subject.substring(0, 4) + "***" + subject.substring(subject.length() - 4);
    }
}
