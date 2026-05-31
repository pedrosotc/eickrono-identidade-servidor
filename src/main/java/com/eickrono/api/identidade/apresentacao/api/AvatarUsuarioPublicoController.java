package com.eickrono.api.identidade.apresentacao.api;

import com.eickrono.api.identidade.aplicacao.servico.ArmazenamentoAvatarUsuario;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/identidade/avatares/publicos")
public class AvatarUsuarioPublicoController {

    private static final String PREFIXO_PUBLICO = "/identidade/avatares/publicos/";

    private final ArmazenamentoAvatarUsuario armazenamentoAvatarUsuario;

    public AvatarUsuarioPublicoController(final ArmazenamentoAvatarUsuario armazenamentoAvatarUsuario) {
        this.armazenamentoAvatarUsuario = Objects.requireNonNull(
                armazenamentoAvatarUsuario,
                "armazenamentoAvatarUsuario é obrigatório");
    }

    @GetMapping("/**")
    public ResponseEntity<byte[]> carregar(final HttpServletRequest request) {
        String path = request.getRequestURI();
        int inicioStorageKey = path.indexOf(PREFIXO_PUBLICO);
        if (inicioStorageKey < 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        String storageKey = path.substring(inicioStorageKey + PREFIXO_PUBLICO.length());
        byte[] conteudo = armazenamentoAvatarUsuario.carregar(storageKey)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .contentType(MediaType.parseMediaType(contentType(storageKey)))
                .body(conteudo);
    }

    private static String contentType(final String storageKey) {
        String normalizado = storageKey.toLowerCase();
        if (normalizado.endsWith(".jpg") || normalizado.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (normalizado.endsWith(".png")) {
            return "image/png";
        }
        if (normalizado.endsWith(".webp")) {
            return "image/webp";
        }
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }
}
