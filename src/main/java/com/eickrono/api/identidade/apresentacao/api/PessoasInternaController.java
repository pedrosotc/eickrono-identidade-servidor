package com.eickrono.api.identidade.apresentacao.api;

import com.eickrono.api.identidade.aplicacao.servico.ProvisionamentoIdentidadeService;
import com.eickrono.api.identidade.apresentacao.dto.cadastro.ConfirmacaoPessoaCadastroInternoApiRequest;
import com.eickrono.api.identidade.apresentacao.dto.cadastro.ConfirmacaoPessoaCadastroInternoApiResposta;
import com.eickrono.api.identidade.dominio.modelo.Pessoa;
import com.eickrono.api.identidade.infraestrutura.configuracao.ValidadorChamadaInterna;
import jakarta.validation.Valid;
import java.util.Objects;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/identidade/pessoas/interna")
public class PessoasInternaController {

    private static final String HEADER_SEGREDO_INTERNO = "X-Eickrono-Internal-Secret";

    private final ProvisionamentoIdentidadeService provisionamentoIdentidadeService;
    private final ValidadorChamadaInterna validadorChamadaInterna;

    public PessoasInternaController(final ProvisionamentoIdentidadeService provisionamentoIdentidadeService,
                                    final ValidadorChamadaInterna validadorChamadaInterna) {
        this.provisionamentoIdentidadeService = Objects.requireNonNull(
                provisionamentoIdentidadeService,
                "provisionamentoIdentidadeService é obrigatório"
        );
        this.validadorChamadaInterna = Objects.requireNonNull(
                validadorChamadaInterna,
                "validadorChamadaInterna é obrigatório"
        );
    }

    @PostMapping("/confirmacoes-email")
    public ConfirmacaoPessoaCadastroInternoApiResposta confirmarPessoaCadastro(
            @RequestHeader(HEADER_SEGREDO_INTERNO) final String segredoInterno,
            @AuthenticationPrincipal final Jwt jwt,
            @Valid @RequestBody final ConfirmacaoPessoaCadastroInternoApiRequest request) {
        validadorChamadaInterna.validar(segredoInterno, jwt, "PessoasInternaController");
        Pessoa pessoa = provisionamentoIdentidadeService.confirmarEmailCadastro(
                request.sub(),
                request.email(),
                request.nomeCompleto(),
                request.confirmadoEm()
        );
        return ConfirmacaoPessoaCadastroInternoApiResposta.de(pessoa);
    }
}
