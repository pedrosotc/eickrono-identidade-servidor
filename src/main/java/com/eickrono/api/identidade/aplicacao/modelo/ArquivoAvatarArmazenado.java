package com.eickrono.api.identidade.aplicacao.modelo;

public record ArquivoAvatarArmazenado(
        String urlAvatar,
        String storageKey,
        String contentType,
        long tamanhoBytes,
        String hashConteudo,
        String versao
) {
}
