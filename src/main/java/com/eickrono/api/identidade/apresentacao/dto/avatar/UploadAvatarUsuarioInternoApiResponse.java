package com.eickrono.api.identidade.apresentacao.dto.avatar;

import com.eickrono.api.identidade.aplicacao.modelo.ArquivoAvatarArmazenado;

public record UploadAvatarUsuarioInternoApiResponse(
        String urlAvatar,
        String storageKey,
        String contentType,
        Long tamanhoBytes,
        String hashConteudo,
        String versao
) {

    public static UploadAvatarUsuarioInternoApiResponse de(final ArquivoAvatarArmazenado arquivo) {
        return new UploadAvatarUsuarioInternoApiResponse(
                arquivo.urlAvatar(),
                arquivo.storageKey(),
                arquivo.contentType(),
                arquivo.tamanhoBytes(),
                arquivo.hashConteudo(),
                arquivo.versao()
        );
    }
}
