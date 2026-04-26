package com.eickrono.api.identidade.aplicacao.servico;

import com.eickrono.api.identidade.dominio.modelo.CadastroConta;

public interface CanalEnvioCodigoCadastroTelefone {

    void enviar(CadastroConta cadastroConta, String codigo);
}
