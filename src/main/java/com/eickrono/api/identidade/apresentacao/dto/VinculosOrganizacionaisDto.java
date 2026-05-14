package com.eickrono.api.identidade.apresentacao.dto;

import java.util.List;

/**
 * Resposta da listagem dos vínculos organizacionais do usuário autenticado.
 */
public record VinculosOrganizacionaisDto(List<VinculoOrganizacionalDto> vinculos) {
    public VinculosOrganizacionaisDto {
        vinculos = vinculos == null ? List.of() : List.copyOf(vinculos);
    }

    @Override
    public List<VinculoOrganizacionalDto> vinculos() {
        return List.copyOf(vinculos);
    }
}
