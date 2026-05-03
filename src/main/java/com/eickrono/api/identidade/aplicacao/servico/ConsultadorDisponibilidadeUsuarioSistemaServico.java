package com.eickrono.api.identidade.aplicacao.servico;

public interface ConsultadorDisponibilidadeUsuarioSistemaServico {

    boolean usuarioDisponivel(String usuario, String sistemaSolicitante);
}
