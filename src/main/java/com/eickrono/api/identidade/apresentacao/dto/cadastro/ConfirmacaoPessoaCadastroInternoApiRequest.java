package com.eickrono.api.identidade.apresentacao.dto.cadastro;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record ConfirmacaoPessoaCadastroInternoApiRequest(
        @NotBlank String sub,
        @NotBlank String email,
        @NotBlank String nomeCompleto,
        @NotNull OffsetDateTime confirmadoEm
) {
}
