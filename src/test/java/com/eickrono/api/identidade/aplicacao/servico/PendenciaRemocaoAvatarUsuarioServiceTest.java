package com.eickrono.api.identidade.aplicacao.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eickrono.api.identidade.apresentacao.dto.avatar.MaterializarPendenciasRemocaoAvatarInternoApiRequest;
import com.eickrono.api.identidade.apresentacao.dto.avatar.MaterializarPendenciasRemocaoAvatarInternoApiResponse;
import com.eickrono.api.identidade.infraestrutura.configuracao.AvatarRemocaoProperties;
import com.eickrono.api.identidade.infraestrutura.configuracao.AvatarStorageProperties;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class PendenciaRemocaoAvatarUsuarioServiceTest {

    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("deve materializar pendencias para avatares controlados por storage")
    void deveMaterializarPendenciasParaAvataresControladosPorStorage() {
        AvatarStorageProperties storageProperties = new AvatarStorageProperties();
        storageProperties.setBucket("eickrono-avatares-hml");
        AvatarRemocaoProperties remocaoProperties = new AvatarRemocaoProperties();
        remocaoProperties.setPendenciaRetencaoDias(15);
        PendenciaRemocaoAvatarUsuarioService service = new PendenciaRemocaoAvatarUsuarioService(
                jdbcTemplate,
                storageProperties,
                remocaoProperties
        );
        UUID correlacaoId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID usuarioClienteId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID avatarId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        when(jdbcTemplate.queryForList(anyString(), eq(Map.of("usuarioClienteIds", List.of(usuarioClienteId)))))
                .thenReturn(List.of(Map.of(
                        "avatar_id", avatarId,
                        "usuario_cliente_id", usuarioClienteId,
                        "origem", "THIMISU",
                        "storage_key", "avatares/thimisu/avatar.png"
                )));
        when(jdbcTemplate.update(anyString(), any(MapSqlParameterSource.class))).thenReturn(1);

        MaterializarPendenciasRemocaoAvatarInternoApiResponse response = service.materializar(
                new MaterializarPendenciasRemocaoAvatarInternoApiRequest(
                        correlacaoId.toString(),
                        "thimisu",
                        List.of(usuarioClienteId.toString())
                )
        );

        assertThat(response.correlacaoId()).isEqualTo(correlacaoId.toString());
        assertThat(response.produto()).isEqualTo("THIMISU");
        assertThat(response.pendenciasMaterializadas()).isEqualTo(1);
        assertThat(response.avatarIds()).containsExactly(avatarId.toString());
        assertThat(response.storageKeys()).containsExactly("avatares/thimisu/avatar.png");
        ArgumentCaptor<MapSqlParameterSource> captor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbcTemplate).update(anyString(), captor.capture());
        MapSqlParameterSource parametros = captor.getValue();
        assertThat(parametros.getValue("bucket")).isEqualTo("eickrono-avatares-hml");
        assertThat(parametros.getValue("status")).isEqualTo("PENDENTE");
        assertThat(parametros.getValue("produto")).isEqualTo("THIMISU");
        assertThat(parametros.getValue("avatarId")).isEqualTo(avatarId);
        assertThat(parametros.getValue("usuarioClienteId")).isEqualTo(usuarioClienteId);
    }

    @Test
    @DisplayName("deve rejeitar materializacao sem bucket configurado")
    void deveRejeitarMaterializacaoSemBucketConfigurado() {
        AvatarStorageProperties storageProperties = new AvatarStorageProperties();
        AvatarRemocaoProperties remocaoProperties = new AvatarRemocaoProperties();
        PendenciaRemocaoAvatarUsuarioService service = new PendenciaRemocaoAvatarUsuarioService(
                jdbcTemplate,
                storageProperties,
                remocaoProperties
        );

        assertThatThrownBy(() -> service.materializar(
                new MaterializarPendenciasRemocaoAvatarInternoApiRequest(
                        "11111111-1111-1111-1111-111111111111",
                        "THIMISU",
                        List.of("22222222-2222-2222-2222-222222222222")
                )
        )).isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("identidade.avatar.storage.bucket");
    }

    @Test
    @DisplayName("deve rejeitar storage key fora do namespace de avatares")
    void deveRejeitarStorageKeyForaDoNamespaceDeAvatares() {
        AvatarStorageProperties storageProperties = new AvatarStorageProperties();
        storageProperties.setBucket("eickrono-avatares-hml");
        AvatarRemocaoProperties remocaoProperties = new AvatarRemocaoProperties();
        PendenciaRemocaoAvatarUsuarioService service = new PendenciaRemocaoAvatarUsuarioService(
                jdbcTemplate,
                storageProperties,
                remocaoProperties
        );
        UUID usuarioClienteId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        when(jdbcTemplate.queryForList(anyString(), eq(Map.of("usuarioClienteIds", List.of(usuarioClienteId)))))
                .thenReturn(List.of(Map.of(
                        "avatar_id", UUID.fromString("33333333-3333-3333-3333-333333333333"),
                        "usuario_cliente_id", usuarioClienteId,
                        "origem", "THIMISU",
                        "storage_key", "../segredos.txt"
                )));

        assertThatThrownBy(() -> service.materializar(
                new MaterializarPendenciasRemocaoAvatarInternoApiRequest(
                        "11111111-1111-1111-1111-111111111111",
                        "THIMISU",
                        List.of(usuarioClienteId.toString())
                )
        )).isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("storageKey de avatar invalida");
    }
}
