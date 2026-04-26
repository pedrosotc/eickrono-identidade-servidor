package com.eickrono.api.identidade.apresentacao.dto.fluxo;

import com.eickrono.api.identidade.aplicacao.modelo.VinculoSocialPendenteCadastro;
import com.eickrono.api.identidade.dominio.modelo.ProvedorVinculoSocial;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

public record VinculoSocialPendenteApiRequest(
        @NotBlank String provedor,
        @NotBlank String identificadorExterno,
        String nomeUsuarioExterno
) {

    public VinculoSocialPendenteCadastro paraModelo() {
        ProvedorVinculoSocial provedorResolvido = ProvedorVinculoSocial.fromAlias(provedor)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "provedor do vínculo social pendente é inválido."
                ));
        return new VinculoSocialPendenteCadastro(
                provedorResolvido,
                identificadorExterno,
                nomeUsuarioExterno
        );
    }
}
