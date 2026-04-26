package com.eickrono.api.identidade.aplicacao.servico;

import com.eickrono.api.identidade.aplicacao.excecao.FluxoPublicoException;
import com.eickrono.api.identidade.aplicacao.modelo.ConviteOrganizacionalValidado;
import com.eickrono.api.identidade.dominio.modelo.ConviteOrganizacional;
import com.eickrono.api.identidade.dominio.modelo.StatusConviteOrganizacional;
import com.eickrono.api.identidade.dominio.repositorio.ConviteOrganizacionalRepositorio;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ConviteOrganizacionalService {

    private final ConviteOrganizacionalRepositorio conviteOrganizacionalRepositorio;
    private final ClienteContextoPessoaPerfil clienteContextoPessoaPerfil;
    private final Clock clock;

    public ConviteOrganizacionalService(final ConviteOrganizacionalRepositorio conviteOrganizacionalRepositorio,
                                        final ClienteContextoPessoaPerfil clienteContextoPessoaPerfil,
                                        final Clock clock) {
        this.conviteOrganizacionalRepositorio = Objects.requireNonNull(
                conviteOrganizacionalRepositorio, "conviteOrganizacionalRepositorio e obrigatorio");
        this.clienteContextoPessoaPerfil = Objects.requireNonNull(
                clienteContextoPessoaPerfil, "clienteContextoPessoaPerfil e obrigatorio");
        this.clock = Objects.requireNonNull(clock, "clock e obrigatorio");
    }

    public ConviteOrganizacionalValidado consultarPublico(final String codigo) {
        String codigoNormalizado = normalizarCodigo(codigo);
        ConviteOrganizacional convite = conviteOrganizacionalRepositorio.findByCodigoIgnoreCase(codigoNormalizado)
                .orElseThrow(() -> new FluxoPublicoException(
                        HttpStatus.NOT_FOUND,
                        "convite_invalido",
                        "O convite informado nao foi encontrado.",
                        Map.of("motivo", "nao_encontrado", "codigo", codigoNormalizado)
                ));

        OffsetDateTime agora = OffsetDateTime.now(clock);
        if (convite.estaExpirado(agora)) {
            throw new FluxoPublicoException(
                    HttpStatus.GONE,
                    "convite_invalido",
                    "O convite informado expirou.",
                    Map.of("motivo", "expirado", "codigo", convite.getCodigo())
            );
        }
        if (convite.getStatus() == StatusConviteOrganizacional.REVOGADO) {
            throw new FluxoPublicoException(
                    HttpStatus.GONE,
                    "convite_invalido",
                    "O convite informado foi revogado.",
                    Map.of("motivo", "revogado", "codigo", convite.getCodigo())
            );
        }
        if (convite.getStatus() == StatusConviteOrganizacional.CONSUMIDO) {
            throw new FluxoPublicoException(
                    HttpStatus.GONE,
                    "convite_invalido",
                    "O convite informado ja foi utilizado.",
                    Map.of("motivo", "consumido", "codigo", convite.getCodigo())
            );
        }

        boolean contaExistenteDetectada = convite.isExigeContaSeparada()
                && possuiContaExistenteConvite(convite.getEmailConvidado());

        return new ConviteOrganizacionalValidado(
                convite.getCodigo(),
                convite.getOrganizacaoId(),
                convite.getNomeOrganizacao(),
                convite.getEmailConvidado(),
                convite.getNomeConvidado(),
                convite.isExigeContaSeparada(),
                contaExistenteDetectada,
                convite.getExpiraEm()
        );
    }

    private boolean possuiContaExistenteConvite(final String emailConvidado) {
        if (emailConvidado == null || emailConvidado.isBlank()) {
            return false;
        }
        return clienteContextoPessoaPerfil.buscarPorEmail(emailConvidado.trim().toLowerCase(Locale.ROOT)).isPresent();
    }

    private String normalizarCodigo(final String codigo) {
        String valor = Objects.requireNonNull(codigo, "codigo e obrigatorio").trim();
        if (valor.isEmpty()) {
            throw new FluxoPublicoException(
                    HttpStatus.BAD_REQUEST,
                    "convite_invalido",
                    "O codigo do convite e obrigatorio."
            );
        }
        return valor.toUpperCase(Locale.ROOT);
    }
}
