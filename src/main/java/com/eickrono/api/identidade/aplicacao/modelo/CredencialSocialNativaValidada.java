package com.eickrono.api.identidade.aplicacao.modelo;

import com.eickrono.api.identidade.dominio.modelo.ProvedorVinculoSocial;

public record CredencialSocialNativaValidada(
        ProvedorVinculoSocial provedor,
        String identificadorExterno,
        String email,
        String nomeUsuarioExterno,
        String nomeExibicaoExterno,
        String urlAvatarExterno
) {
}
