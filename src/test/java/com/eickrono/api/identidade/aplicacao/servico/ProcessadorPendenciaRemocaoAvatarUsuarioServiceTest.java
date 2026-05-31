package com.eickrono.api.identidade.aplicacao.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eickrono.api.identidade.infraestrutura.configuracao.AvatarRemocaoProperties;
import java.util.List;
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
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

@ExtendWith(MockitoExtension.class)
class ProcessadorPendenciaRemocaoAvatarUsuarioServiceTest {

    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Mock
    private S3Client s3Client;

    @Test
    @DisplayName("deve apagar objeto S3 e marcar pendencia como removida")
    void deveApagarObjetoS3EMarcarPendenciaComoRemovida() {
        AvatarRemocaoProperties properties = new AvatarRemocaoProperties();
        properties.setLoteMaximo(10);
        properties.setTentativasMaximas(3);
        ProcessadorPendenciaRemocaoAvatarUsuarioService service =
                new ProcessadorPendenciaRemocaoAvatarUsuarioService(jdbcTemplate, s3Client, properties);
        UUID pendenciaId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        when(jdbcTemplate.queryForList(anyString(), anyMap())).thenReturn(List.of(java.util.Map.of(
                "id", pendenciaId,
                "bucket", "eickrono-avatares-hml",
                "storage_key", "avatares/thimisu/avatar.png",
                "tentativas", 0
        )));

        int removidas = service.processarPendencias();

        assertThat(removidas).isEqualTo(1);
        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(captor.capture());
        assertThat(captor.getValue().bucket()).isEqualTo("eickrono-avatares-hml");
        assertThat(captor.getValue().key()).isEqualTo("avatares/thimisu/avatar.png");
    }

    @Test
    @DisplayName("deve tratar objeto ausente como remocao idempotente")
    void deveTratarObjetoAusenteComoRemocaoIdempotente() {
        AvatarRemocaoProperties properties = new AvatarRemocaoProperties();
        ProcessadorPendenciaRemocaoAvatarUsuarioService service =
                new ProcessadorPendenciaRemocaoAvatarUsuarioService(jdbcTemplate, s3Client, properties);
        when(jdbcTemplate.queryForList(anyString(), anyMap())).thenReturn(List.of(java.util.Map.of(
                "id", UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "bucket", "eickrono-avatares-hml",
                "storage_key", "avatares/thimisu/avatar.png",
                "tentativas", 0
        )));
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().message("ausente").build());

        int removidas = service.processarPendencias();

        assertThat(removidas).isEqualTo(1);
    }

    @Test
    @DisplayName("deve rejeitar pendencia com storage key fora do namespace de avatar")
    void deveRejeitarPendenciaComStorageKeyForaDoNamespaceDeAvatar() {
        AvatarRemocaoProperties properties = new AvatarRemocaoProperties();
        ProcessadorPendenciaRemocaoAvatarUsuarioService service =
                new ProcessadorPendenciaRemocaoAvatarUsuarioService(jdbcTemplate, s3Client, properties);
        when(jdbcTemplate.queryForList(anyString(), anyMap())).thenReturn(List.of(java.util.Map.of(
                "id", UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "bucket", "eickrono-avatares-hml",
                "storage_key", "../segredos.txt",
                "tentativas", 0
        )));

        assertThatThrownBy(service::processarPendencias)
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("storageKey de avatar invalida");
    }
}
