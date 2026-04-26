package com.eickrono.api.identidade.aplicacao.servico;

import com.eickrono.api.identidade.dominio.modelo.CadastroConta;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CanalEnvioCodigoCadastroTelefoneLog implements CanalEnvioCodigoCadastroTelefone {

    private static final Logger LOGGER = LoggerFactory.getLogger(CanalEnvioCodigoCadastroTelefoneLog.class);

    @Override
    public void enviar(final CadastroConta cadastroConta, final String codigo) {
        CadastroConta cadastro = Objects.requireNonNull(cadastroConta, "cadastroConta é obrigatório");
        String codigoConfirmacao = Objects.requireNonNull(codigo, "codigo é obrigatório").trim();
        if (codigoConfirmacao.isBlank()) {
            throw new IllegalArgumentException("codigo é obrigatório");
        }

        LOGGER.info(
                "Enviando código de confirmação de cadastro por {} para {} (cadastroId={}, sistema={}) - código={}",
                cadastro.getCanalValidacaoTelefone(),
                cadastro.getTelefonePrincipal(),
                cadastro.getCadastroId(),
                cadastro.getSistemaSolicitante(),
                codigoConfirmacao
        );
    }
}
