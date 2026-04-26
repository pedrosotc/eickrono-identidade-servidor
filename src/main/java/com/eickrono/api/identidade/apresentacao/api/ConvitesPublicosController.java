package com.eickrono.api.identidade.apresentacao.api;

import com.eickrono.api.identidade.aplicacao.modelo.ConviteOrganizacionalValidado;
import com.eickrono.api.identidade.aplicacao.servico.ConviteOrganizacionalService;
import com.eickrono.api.identidade.apresentacao.dto.fluxo.ConviteOrganizacionalApiResposta;
import java.util.Objects;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/publica/convites")
public class ConvitesPublicosController {

    private final ConviteOrganizacionalService conviteOrganizacionalService;

    public ConvitesPublicosController(final ConviteOrganizacionalService conviteOrganizacionalService) {
        this.conviteOrganizacionalService = Objects.requireNonNull(
                conviteOrganizacionalService, "conviteOrganizacionalService e obrigatorio");
    }

    @GetMapping("/{codigo}")
    public ConviteOrganizacionalApiResposta consultar(@PathVariable final String codigo) {
        ConviteOrganizacionalValidado convite = conviteOrganizacionalService.consultarPublico(codigo);
        return new ConviteOrganizacionalApiResposta(
                convite.codigo(),
                convite.organizacaoId(),
                convite.nomeOrganizacao(),
                convite.emailConvidado(),
                convite.nomeConvidado(),
                convite.exigeContaSeparada(),
                convite.contaExistenteDetectada(),
                convite.expiraEm()
        );
    }
}
