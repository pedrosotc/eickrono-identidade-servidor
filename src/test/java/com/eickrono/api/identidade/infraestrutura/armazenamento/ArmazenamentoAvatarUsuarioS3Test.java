package com.eickrono.api.identidade.infraestrutura.armazenamento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eickrono.api.identidade.aplicacao.modelo.ArquivoAvatarArmazenado;
import com.eickrono.api.identidade.infraestrutura.configuracao.AvatarStorageProperties;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

@ExtendWith(MockitoExtension.class)
class ArmazenamentoAvatarUsuarioS3Test {

    @Mock
    private S3Client s3Client;

    @Test
    void deveArmazenarAvatarNoBucketConfiguradoERetornarUrlPublica() {
        ArmazenamentoAvatarUsuarioS3 armazenamento = new ArmazenamentoAvatarUsuarioS3(s3Client, properties());
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        ArquivoAvatarArmazenado arquivo = armazenamento.armazenar(
                "THIMISU",
                "avatar.png",
                "image/png",
                "conteudo-avatar".getBytes(StandardCharsets.UTF_8),
                "hash-avatar"
        );

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));
        PutObjectRequest request = requestCaptor.getValue();
        assertThat(request.bucket()).isEqualTo("eickrono-avatares-hml");
        assertThat(request.key()).isEqualTo("avatares/thimisu/hash-avatar.png");
        assertThat(request.contentType()).isEqualTo("image/png");
        assertThat(request.contentLength()).isEqualTo(15L);
        assertThat(request.metadata()).containsEntry("origem", "THIMISU");
        assertThat(request.metadata()).containsEntry("nome-arquivo", "avatar.png");
        assertThat(request.metadata()).containsEntry("hash-conteudo", "hash-avatar");
        assertThat(arquivo.urlAvatar())
                .isEqualTo("https://id-hml.eickrono.store/identidade/avatares/publicos/avatares/thimisu/hash-avatar.png");
        assertThat(arquivo.storageKey()).isEqualTo("avatares/thimisu/hash-avatar.png");
        assertThat(arquivo.contentType()).isEqualTo("image/png");
        assertThat(arquivo.tamanhoBytes()).isEqualTo(15L);
        assertThat(arquivo.hashConteudo()).isEqualTo("hash-avatar");
        assertThat(arquivo.versao()).isEqualTo("hash-avatar");
    }

    @Test
    void deveCarregarAvatarDoBucketConfigurado() {
        ArmazenamentoAvatarUsuarioS3 armazenamento = new ArmazenamentoAvatarUsuarioS3(s3Client, properties());
        byte[] conteudo = "avatar".getBytes(StandardCharsets.UTF_8);
        ResponseBytes<GetObjectResponse> response = ResponseBytes.fromByteArray(
                GetObjectResponse.builder().contentType("image/png").build(),
                conteudo
        );
        when(s3Client.getObject(
                any(GetObjectRequest.class),
                org.mockito.ArgumentMatchers.<ResponseTransformer<GetObjectResponse,
                        ResponseBytes<GetObjectResponse>>>any()
        ))
                .thenReturn(response);

        Optional<byte[]> carregado = armazenamento.carregar("avatares/thimisu/hash-avatar.png");

        ArgumentCaptor<GetObjectRequest> requestCaptor = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(s3Client).getObject(
                requestCaptor.capture(),
                org.mockito.ArgumentMatchers.<ResponseTransformer<GetObjectResponse,
                        ResponseBytes<GetObjectResponse>>>any()
        );
        assertThat(requestCaptor.getValue().bucket()).isEqualTo("eickrono-avatares-hml");
        assertThat(requestCaptor.getValue().key()).isEqualTo("avatares/thimisu/hash-avatar.png");
        assertThat(carregado).contains(conteudo);
    }

    @Test
    void deveRetornarVazioQuandoObjetoNaoExisteNoBucket() {
        ArmazenamentoAvatarUsuarioS3 armazenamento = new ArmazenamentoAvatarUsuarioS3(s3Client, properties());
        when(s3Client.getObject(
                any(GetObjectRequest.class),
                org.mockito.ArgumentMatchers.<ResponseTransformer<GetObjectResponse,
                        ResponseBytes<GetObjectResponse>>>any()
        ))
                .thenThrow(NoSuchKeyException.builder().build());

        Optional<byte[]> carregado = armazenamento.carregar("avatares/thimisu/inexistente.png");

        assertThat(carregado).isEmpty();
    }

    private AvatarStorageProperties properties() {
        AvatarStorageProperties properties = new AvatarStorageProperties();
        properties.setBucket("eickrono-avatares-hml");
        properties.setRegion("sa-east-1");
        properties.setPublicUrlBase("https://id-hml.eickrono.store/identidade/avatares/publicos");
        return properties;
    }
}
