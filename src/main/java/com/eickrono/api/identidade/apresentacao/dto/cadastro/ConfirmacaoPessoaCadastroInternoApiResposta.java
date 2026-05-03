package com.eickrono.api.identidade.apresentacao.dto.cadastro;

import com.eickrono.api.identidade.dominio.modelo.Pessoa;

public record ConfirmacaoPessoaCadastroInternoApiResposta(
        Long pessoaId,
        String sub,
        String emailPrincipal
) {

    public static ConfirmacaoPessoaCadastroInternoApiResposta de(final Pessoa pessoa) {
        return new ConfirmacaoPessoaCadastroInternoApiResposta(
                pessoa.getId(),
                pessoa.getSub(),
                pessoa.getEmail()
        );
    }
}
