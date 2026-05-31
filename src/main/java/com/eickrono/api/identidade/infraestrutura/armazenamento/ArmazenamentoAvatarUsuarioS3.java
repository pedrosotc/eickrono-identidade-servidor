package com.eickrono.api.identidade.infraestrutura.armazenamento;

import com.eickrono.api.identidade.aplicacao.modelo.ArquivoAvatarArmazenado;
import com.eickrono.api.identidade.aplicacao.servico.ArmazenamentoAvatarUsuario;
import com.eickrono.api.identidade.infraestrutura.configuracao.AvatarStorageProperties;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Component
@ConditionalOnProperty(prefix = "identidade.avatar.storage", name = "tipo", havingValue = "s3")
public class ArmazenamentoAvatarUsuarioS3 implements ArmazenamentoAvatarUsuario {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArmazenamentoAvatarUsuarioS3.class);

    private final S3Client s3Client;
    private final AvatarStorageProperties properties;

    public ArmazenamentoAvatarUsuarioS3(final S3Client s3Client,
                                        final AvatarStorageProperties properties) {
        this.s3Client = Objects.requireNonNull(s3Client, "s3Client é obrigatório");
        this.properties = Objects.requireNonNull(properties, "properties é obrigatório");
    }

    @Override
    public ArquivoAvatarArmazenado armazenar(final String origem,
                                             final String nomeArquivo,
                                             final String contentType,
                                             final byte[] conteudo,
                                             final String hashConteudo) {
        String storageKey = montarStorageKey(origem, contentType, hashConteudo);
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket())
                .key(storageKey)
                .contentType(contentType)
                .contentLength((long) conteudo.length)
                .metadata(Map.of(
                        "origem", origem,
                        "nome-arquivo", nomeArquivo,
                        "hash-conteudo", hashConteudo
                ))
                .build();
        try {
            LOGGER.info(
                    "qa_avatar_storage_s3_inicio bucket={} storageKey={} contentType={} tamanhoBytes={}",
                    bucket(),
                    storageKey,
                    contentType,
                    conteudo.length
            );
            s3Client.putObject(request, RequestBody.fromBytes(conteudo));
        } catch (S3Exception | SdkClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Nao foi possivel armazenar avatar.", ex);
        }
        LOGGER.info(
                "qa_avatar_storage_s3_fim bucket={} storageKey={} tamanhoBytes={}",
                bucket(),
                storageKey,
                conteudo.length
        );
        return new ArquivoAvatarArmazenado(
                montarUrlPublica(storageKey),
                storageKey,
                contentType,
                conteudo.length,
                hashConteudo,
                hashConteudo
        );
    }

    @Override
    public Optional<byte[]> carregar(final String storageKey) {
        validarStorageKey(storageKey);
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket())
                .key(storageKey)
                .build();
        try {
            ResponseBytes<GetObjectResponse> response = s3Client.getObject(
                    request,
                    ResponseTransformer.toBytes()
            );
            return Optional.of(response.asByteArray());
        } catch (NoSuchKeyException ex) {
            return Optional.empty();
        } catch (S3Exception ex) {
            if (ex.statusCode() == HttpStatus.NOT_FOUND.value()) {
                return Optional.empty();
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Nao foi possivel carregar avatar.", ex);
        } catch (SdkClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Nao foi possivel carregar avatar.", ex);
        }
    }

    private String montarStorageKey(final String origem,
                                    final String contentType,
                                    final String hashConteudo) {
        String origemPath = origem.toLowerCase(Locale.ROOT);
        String storageKey = "avatares/" + origemPath + "/" + hashConteudo + extensao(contentType);
        validarStorageKey(storageKey);
        return storageKey;
    }

    private String bucket() {
        if (!StringUtils.hasText(properties.getBucket())) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Bucket de avatar nao configurado.");
        }
        return properties.getBucket().trim();
    }

    private String montarUrlPublica(final String storageKey) {
        String base = properties.getPublicUrlBase();
        String normalizada = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        return normalizada + "/" + storageKey;
    }

    private static void validarStorageKey(final String storageKey) {
        if (!StringUtils.hasText(storageKey)
                || storageKey.startsWith("/")
                || storageKey.contains("..")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Storage key de avatar invalida.");
        }
    }

    private static String extensao(final String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Tipo de avatar invalido.");
        };
    }
}
