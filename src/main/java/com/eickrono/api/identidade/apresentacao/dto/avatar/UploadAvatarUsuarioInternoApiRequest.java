package com.eickrono.api.identidade.apresentacao.dto.avatar;

import jakarta.validation.constraints.NotBlank;

public record UploadAvatarUsuarioInternoApiRequest(
        @NotBlank String origem,
        String nomeArquivo,
        @NotBlank String contentType,
        Long tamanhoBytes,
        @NotBlank String conteudoBase64
) {
}
