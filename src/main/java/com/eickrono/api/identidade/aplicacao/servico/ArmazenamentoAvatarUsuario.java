package com.eickrono.api.identidade.aplicacao.servico;

import com.eickrono.api.identidade.aplicacao.modelo.ArquivoAvatarArmazenado;
import java.util.Optional;

public interface ArmazenamentoAvatarUsuario {

    ArquivoAvatarArmazenado armazenar(String origem,
                                      String nomeArquivo,
                                      String contentType,
                                      byte[] conteudo,
                                      String hashConteudo);

    Optional<byte[]> carregar(String storageKey);
}
