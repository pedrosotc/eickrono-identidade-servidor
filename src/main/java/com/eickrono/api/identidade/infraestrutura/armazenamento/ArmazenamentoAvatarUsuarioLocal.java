package com.eickrono.api.identidade.infraestrutura.armazenamento;

import com.eickrono.api.identidade.aplicacao.modelo.ArquivoAvatarArmazenado;
import com.eickrono.api.identidade.aplicacao.servico.ArmazenamentoAvatarUsuario;
import com.eickrono.api.identidade.infraestrutura.configuracao.AvatarStorageProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@ConditionalOnProperty(prefix = "identidade.avatar.storage", name = "tipo", havingValue = "local", matchIfMissing = true)
public class ArmazenamentoAvatarUsuarioLocal implements ArmazenamentoAvatarUsuario {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArmazenamentoAvatarUsuarioLocal.class);

    private final AvatarStorageProperties properties;

    public ArmazenamentoAvatarUsuarioLocal(final AvatarStorageProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties é obrigatório");
    }

    @Override
    public ArquivoAvatarArmazenado armazenar(final String origem,
                                             final String nomeArquivo,
                                             final String contentType,
                                             final byte[] conteudo,
                                             final String hashConteudo) {
        String extensao = extensao(contentType);
        String origemPath = origem.toLowerCase(Locale.ROOT);
        String storageKey = "avatares/" + origemPath + "/" + hashConteudo + extensao;
        Path destino = resolverPath(storageKey);
        try {
            Files.createDirectories(destino.getParent());
            Files.write(destino, conteudo);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Nao foi possivel armazenar avatar.", ex);
        }
        LOGGER.info(
                "qa_avatar_storage_local_fim storageKey={} contentType={} tamanhoBytes={}",
                storageKey,
                contentType,
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
        Path arquivo = resolverPath(storageKey);
        if (!Files.isRegularFile(arquivo)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readAllBytes(arquivo));
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Nao foi possivel carregar avatar.", ex);
        }
    }

    private Path resolverPath(final String storageKey) {
        Path raiz = properties.getDiretorio().toAbsolutePath().normalize();
        Path arquivo = raiz.resolve(storageKey).normalize();
        if (!arquivo.startsWith(raiz)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Storage key de avatar invalida.");
        }
        return arquivo;
    }

    private String montarUrlPublica(final String storageKey) {
        String base = properties.getPublicUrlBase();
        String normalizada = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        return normalizada + "/" + storageKey;
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
