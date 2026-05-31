package com.eickrono.api.identidade.aplicacao.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eickrono.api.identidade.aplicacao.modelo.ArquivoAvatarArmazenado;
import com.eickrono.api.identidade.infraestrutura.armazenamento.ArmazenamentoAvatarUsuarioLocal;
import com.eickrono.api.identidade.infraestrutura.configuracao.AvatarStorageProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.server.ResponseStatusException;

class AvatarUsuarioUploadServiceTest {

    @TempDir
    private Path diretorioTemporario;

    @Test
    void deveArmazenarAvatarLocalComUrlPublicaHashEVersao() throws Exception {
        AvatarStorageProperties properties = properties();
        AvatarUsuarioUploadService service = new AvatarUsuarioUploadService(
                new ArmazenamentoAvatarUsuarioLocal(properties),
                properties
        );
        byte[] conteudo = new byte[]{1, 2, 3};

        ArquivoAvatarArmazenado arquivo = service.armazenar(
                "thimisu",
                "avatar-processado",
                "image/png",
                3L,
                Base64.getEncoder().encodeToString(conteudo)
        );

        assertThat(arquivo.storageKey()).startsWith("avatares/thimisu/");
        assertThat(arquivo.storageKey()).endsWith(".png");
        assertThat(arquivo.urlAvatar()).isEqualTo("https://cdn.eickrono.test/" + arquivo.storageKey());
        assertThat(arquivo.contentType()).isEqualTo("image/png");
        assertThat(arquivo.tamanhoBytes()).isEqualTo(3L);
        assertThat(arquivo.hashConteudo()).isEqualTo(arquivo.versao());
        assertThat(Files.readAllBytes(diretorioTemporario.resolve(arquivo.storageKey()))).containsExactly(conteudo);
    }

    @Test
    void deveRejeitarContentTypeNaoSuportado() {
        AvatarStorageProperties properties = properties();
        AvatarUsuarioUploadService service = new AvatarUsuarioUploadService(
                new ArmazenamentoAvatarUsuarioLocal(properties),
                properties
        );

        assertThatThrownBy(() -> service.armazenar("THIMISU", "avatar.gif", "image/gif", 1L, "AA=="))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Tipo de imagem de avatar nao suportado");
    }

    private AvatarStorageProperties properties() {
        AvatarStorageProperties properties = new AvatarStorageProperties();
        properties.setDiretorio(diretorioTemporario);
        properties.setPublicUrlBase("https://cdn.eickrono.test");
        properties.setMaxBytes(1024L);
        return properties;
    }
}
