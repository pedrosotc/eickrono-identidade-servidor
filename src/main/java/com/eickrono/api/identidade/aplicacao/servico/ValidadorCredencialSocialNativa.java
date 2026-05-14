package com.eickrono.api.identidade.aplicacao.servico;

import com.eickrono.api.identidade.aplicacao.modelo.CredencialSocialNativaValidada;

public interface ValidadorCredencialSocialNativa {

    CredencialSocialNativaValidada validar(String provedor, String tokenExterno);
}
