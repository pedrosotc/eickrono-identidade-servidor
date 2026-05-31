package com.eickrono.api.identidade.aplicacao.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eickrono.api.identidade.AplicacaoApiIdentidade;
import com.eickrono.api.identidade.support.InfraestruturaTesteIdentidade;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest(classes = AplicacaoApiIdentidade.class)
@ActiveProfiles("test")
@ContextConfiguration(initializers = InfraestruturaTesteIdentidade.Initializer.class)
class AvatarUsuarioModeloCanonicoMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void deveCriarTabelasCanonicasDePessoaContatoEAvatar() {
        assertThat(regclass("identidade.pessoas")).isEqualTo("identidade.pessoas");
        assertThat(regclass("identidade.contatos_email")).isEqualTo("identidade.contatos_email");
        assertThat(regclass("identidade.contatos_telefone")).isEqualTo("identidade.contatos_telefone");
        assertThat(regclass("identidade.avatar_origens")).isEqualTo("identidade.avatar_origens");
        assertThat(regclass("identidade.avatar_usuario")).isEqualTo("identidade.avatar_usuario");
        assertThat(regclass("autenticacao.cadastros_conta_avatares"))
                .isEqualTo("autenticacao.cadastros_conta_avatares");
    }

    @Test
    void deveCriarCatalogoInicialDeOrigensDeAvatar() {
        List<String> codigos = jdbcTemplate.queryForList("""
                SELECT codigo
                FROM identidade.avatar_origens
                ORDER BY codigo
                """, String.class);

        assertThat(codigos).contains("APPLE", "FACEBOOK", "GOOGLE", "INSTAGRAM", "LINKEDIN", "THIMISU", "X");
    }

    @Test
    void deveCriarIndiceUnicoParaUmAvatarPreferidoAtivoPorUsuarioCliente() {
        Integer quantidade = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM pg_indexes
                WHERE schemaname = 'identidade'
                  AND tablename = 'avatar_usuario'
                  AND indexname = 'uk_avatar_usuario_preferido_ativo'
                  AND indexdef ILIKE '%WHERE ((preferido IS TRUE) AND (removido_em IS NULL))%'
                """, Integer.class);

        assertThat(quantidade).isEqualTo(1);
    }

    @Test
    void deveCriarIndiceUnicoParaUmAvatarPreferidoPorCadastro() {
        Integer quantidade = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM pg_indexes
                WHERE schemaname = 'autenticacao'
                  AND tablename = 'cadastros_conta_avatares'
                  AND indexname = 'uk_cadastros_conta_avatares_preferido'
                  AND indexdef ILIKE '%WHERE (preferido IS TRUE)%'
                """, Integer.class);

        assertThat(quantidade).isEqualTo(1);
    }

    @Test
    void deveImpedirDoisAvataresPreferidosAtivosParaMesmoUsuarioCliente() {
        UUID usuarioClienteId = criarUsuarioCliente();
        Long origemThimisuId = origemAvatarId("THIMISU");
        UUID primeiroAvatarId = UUID.randomUUID();

        inserirAvatarUsuario(
                primeiroAvatarId,
                usuarioClienteId,
                origemThimisuId,
                "https://cdn.eickrono.test/avatar-1.png",
                true);

        assertThatThrownBy(() -> inserirAvatarUsuario(
                UUID.randomUUID(),
                usuarioClienteId,
                origemThimisuId,
                "https://cdn.eickrono.test/avatar-2.png",
                true))
                .isInstanceOf(DataIntegrityViolationException.class);

        jdbcTemplate.update("""
                UPDATE identidade.avatar_usuario
                SET removido_em = CURRENT_TIMESTAMP
                WHERE id = ?
                """, primeiroAvatarId);

        inserirAvatarUsuario(
                UUID.randomUUID(),
                usuarioClienteId,
                origemThimisuId,
                "https://cdn.eickrono.test/avatar-2.png",
                true);

        Integer preferidosAtivos = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM identidade.avatar_usuario
                WHERE usuario_cliente_id = ?
                  AND preferido IS TRUE
                  AND removido_em IS NULL
                """, Integer.class, usuarioClienteId);

        assertThat(preferidosAtivos).isEqualTo(1);
    }

    @Test
    void deveImpedirDoisAvataresPreferidosParaMesmoCadastro() {
        UUID cadastroId = criarCadastroConta();
        Long origemThimisuId = origemAvatarId("THIMISU");

        inserirAvatarCadastro(
                cadastroId,
                origemThimisuId,
                "https://cdn.eickrono.test/cadastro-avatar-1.png",
                true);

        assertThatThrownBy(() -> inserirAvatarCadastro(
                cadastroId,
                origemThimisuId,
                "https://cdn.eickrono.test/cadastro-avatar-2.png",
                true))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deveConfigurarOrigemThimisuParaUploadEOrigensSociaisParaVinculoSocial() {
        Boolean thimisuPermiteUpload = jdbcTemplate.queryForObject("""
                SELECT permite_upload_usuario
                FROM identidade.avatar_origens
                WHERE codigo = 'THIMISU'
                """, Boolean.class);
        Boolean thimisuPermiteVinculoSocial = jdbcTemplate.queryForObject("""
                SELECT permite_vinculo_social
                FROM identidade.avatar_origens
                WHERE codigo = 'THIMISU'
                """, Boolean.class);
        Integer origensSociais = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM identidade.avatar_origens
                WHERE codigo IN ('APPLE', 'FACEBOOK', 'GOOGLE', 'INSTAGRAM', 'LINKEDIN', 'X')
                  AND permite_vinculo_social IS TRUE
                  AND permite_upload_usuario IS FALSE
                """, Integer.class);

        assertThat(thimisuPermiteUpload).isTrue();
        assertThat(thimisuPermiteVinculoSocial).isFalse();
        assertThat(origensSociais).isEqualTo(6);
    }

    private String regclass(final String nomeTabela) {
        return jdbcTemplate.queryForObject(
                "SELECT to_regclass(?)::TEXT",
                String.class,
                nomeTabela);
    }

    private UUID criarUsuarioCliente() {
        UUID usuarioId = UUID.randomUUID();
        UUID pessoaId = UUID.randomUUID();
        UUID usuarioClienteId = UUID.randomUUID();
        Long clienteEcossistemaId = clienteThimisuId();

        jdbcTemplate.update("""
                INSERT INTO autenticacao.usuarios (
                    id,
                    pessoa_id,
                    sub_remoto,
                    status_global,
                    credencial_local_habilitada,
                    criado_em,
                    atualizado_em
                )
                VALUES (?, ?, ?, 'ATIVO', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, usuarioId, pessoaId, "sub-avatar-test-" + usuarioId);
        jdbcTemplate.update("""
                INSERT INTO autenticacao.usuarios_clientes_ecossistema (
                    id,
                    usuario_id,
                    cliente_ecossistema_id,
                    status_vinculo,
                    vinculado_em,
                    atualizado_em
                )
                VALUES (?, ?, ?, 'ATIVO', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, usuarioClienteId, usuarioId, clienteEcossistemaId);
        return usuarioClienteId;
    }

    private UUID criarCadastroConta() {
        UUID cadastroId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO autenticacao.cadastros_conta (
                    id,
                    pessoa_id,
                    cliente_ecossistema_id,
                    email_id,
                    status_processo,
                    codigo_email_hash,
                    codigo_email_gerado_em,
                    codigo_email_expira_em,
                    criado_em,
                    atualizado_em
                )
                VALUES (
                    ?,
                    ?,
                    ?,
                    ?,
                    'PENDENTE',
                    'hash-avatar-test',
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP + INTERVAL '15 minutes',
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP
                )
                """,
                cadastroId,
                UUID.randomUUID(),
                clienteThimisuId(),
                UUID.randomUUID());
        return cadastroId;
    }

    private void inserirAvatarUsuario(final UUID avatarId,
                                      final UUID usuarioClienteId,
                                      final Long origemId,
                                      final String urlAvatar,
                                      final boolean preferido) {
        jdbcTemplate.update("""
                INSERT INTO identidade.avatar_usuario (
                    id,
                    usuario_cliente_id,
                    origem_id,
                    url_avatar,
                    preferido,
                    criado_em,
                    atualizado_em
                )
                VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                avatarId,
                usuarioClienteId,
                origemId,
                urlAvatar,
                preferido);
    }

    private void inserirAvatarCadastro(final UUID cadastroId,
                                       final Long origemId,
                                       final String urlAvatar,
                                       final boolean preferido) {
        jdbcTemplate.update("""
                INSERT INTO autenticacao.cadastros_conta_avatares (
                    id,
                    cadastro_id,
                    origem_id,
                    url_avatar,
                    preferido,
                    criado_em,
                    atualizado_em
                )
                VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                UUID.randomUUID(),
                cadastroId,
                origemId,
                urlAvatar,
                preferido);
    }

    private Long clienteThimisuId() {
        return jdbcTemplate.queryForObject("""
                SELECT id
                FROM catalogo.clientes_ecossistema
                WHERE codigo = 'eickrono-thimisu-app'
                """, Long.class);
    }

    private Long origemAvatarId(final String codigo) {
        return jdbcTemplate.queryForObject("""
                SELECT id
                FROM identidade.avatar_origens
                WHERE codigo = ?
                """, Long.class, codigo);
    }
}
