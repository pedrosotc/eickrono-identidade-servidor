package com.eickrono.api.identidade.aplicacao.servico;

import com.eickrono.api.identidade.aplicacao.modelo.ArquivoAvatarArmazenado;
import com.eickrono.api.identidade.infraestrutura.configuracao.AvatarStorageProperties;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AvatarUsuarioUploadService {

    private static final Map<String, String> CONTENT_TYPES_SUPORTADOS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );
    private static final Logger LOGGER = LoggerFactory.getLogger(AvatarUsuarioUploadService.class);

    private final ArmazenamentoAvatarUsuario armazenamentoAvatarUsuario;
    private final AvatarStorageProperties properties;

    public AvatarUsuarioUploadService(final ArmazenamentoAvatarUsuario armazenamentoAvatarUsuario,
                                      final AvatarStorageProperties properties) {
        this.armazenamentoAvatarUsuario = Objects.requireNonNull(
                armazenamentoAvatarUsuario,
                "armazenamentoAvatarUsuario é obrigatório");
        this.properties = Objects.requireNonNull(properties, "properties é obrigatório");
    }

    public ArquivoAvatarArmazenado armazenar(final String origem,
                                             final String nomeArquivo,
                                             final String contentType,
                                             final Long tamanhoBytesDeclarado,
                                             final String conteudoBase64) {
        String origemNormalizada = normalizarObrigatorio(origem, "origem").toUpperCase(Locale.ROOT);
        String contentTypeNormalizado = normalizarContentType(contentType);
        String nomeArquivoNormalizado = normalizarNomeArquivo(nomeArquivo, contentTypeNormalizado);
        byte[] conteudo = decodificarBase64(conteudoBase64);
        validarTamanho(conteudo, tamanhoBytesDeclarado);
        String hashConteudo = hashSha256(conteudo);
        LOGGER.info(
                "qa_avatar_upload_validado origem={} contentType={} tamanhoBytes={} hashPresente={} nomeArquivo={}",
                origemNormalizada,
                contentTypeNormalizado,
                conteudo.length,
                StringUtils.hasText(hashConteudo),
                nomeArquivoNormalizado
        );
        return armazenamentoAvatarUsuario.armazenar(
                origemNormalizada,
                nomeArquivoNormalizado,
                contentTypeNormalizado,
                conteudo,
                hashConteudo
        );
    }

    private void validarTamanho(final byte[] conteudo, final Long tamanhoBytesDeclarado) {
        if (conteudo.length == 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Avatar vazio.");
        }
        if (conteudo.length > properties.getMaxBytes()) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Avatar excede o tamanho permitido.");
        }
        if (tamanhoBytesDeclarado != null && tamanhoBytesDeclarado.longValue() != conteudo.length) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Tamanho declarado do avatar nao confere com o conteudo enviado."
            );
        }
    }

    private static byte[] decodificarBase64(final String conteudoBase64) {
        String normalizado = normalizarObrigatorio(conteudoBase64, "conteudoBase64");
        try {
            return Base64.getDecoder().decode(normalizado);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Avatar em base64 invalido.", ex);
        }
    }

    private static String normalizarContentType(final String contentType) {
        String normalizado = normalizarObrigatorio(contentType, "contentType").toLowerCase(Locale.ROOT);
        if (!CONTENT_TYPES_SUPORTADOS.containsKey(normalizado)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Tipo de imagem de avatar nao suportado.");
        }
        return normalizado;
    }

    private static String normalizarNomeArquivo(final String nomeArquivo, final String contentType) {
        String normalizado = StringUtils.hasText(nomeArquivo) ? nomeArquivo.trim() : "avatar";
        String extensao = "." + CONTENT_TYPES_SUPORTADOS.get(contentType);
        if (!normalizado.toLowerCase(Locale.ROOT).endsWith(extensao)) {
            return normalizado + extensao;
        }
        return normalizado;
    }

    private static String normalizarObrigatorio(final String valor, final String campo) {
        if (!StringUtils.hasText(valor)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, campo + " é obrigatório.");
        }
        return valor.trim();
    }

    private static String hashSha256(final byte[] conteudo) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(conteudo));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponivel no runtime Java.", ex);
        }
    }
}
