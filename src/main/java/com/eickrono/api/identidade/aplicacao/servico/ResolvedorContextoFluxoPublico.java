package com.eickrono.api.identidade.aplicacao.servico;

import com.eickrono.api.identidade.aplicacao.modelo.ContextoSolicitacaoFluxoPublico;
import com.eickrono.api.identidade.dominio.modelo.CadastroConta;
import com.eickrono.api.identidade.dominio.modelo.RecuperacaoSenha;
import com.eickrono.api.identidade.dominio.repositorio.CadastroContaRepositorio;
import com.eickrono.api.identidade.dominio.repositorio.RecuperacaoSenhaRepositorio;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class ResolvedorContextoFluxoPublico {

    private final CadastroContaRepositorio cadastroContaRepositorio;
    private final RecuperacaoSenhaRepositorio recuperacaoSenhaRepositorio;

    public ResolvedorContextoFluxoPublico(final CadastroContaRepositorio cadastroContaRepositorio,
                                          final RecuperacaoSenhaRepositorio recuperacaoSenhaRepositorio) {
        this.cadastroContaRepositorio = Objects.requireNonNull(
                cadastroContaRepositorio, "cadastroContaRepositorio e obrigatorio");
        this.recuperacaoSenhaRepositorio = Objects.requireNonNull(
                recuperacaoSenhaRepositorio, "recuperacaoSenhaRepositorio e obrigatorio");
    }

    public ContextoSolicitacaoFluxoPublico resolver(final String emailPrincipal,
                                                    final ContextoSolicitacaoFluxoPublico contextoAtual) {
        ContextoSolicitacaoFluxoPublico resolvido = contextoAtual == null
                ? new ContextoSolicitacaoFluxoPublico(null, null, null, null, null, null, null)
                : contextoAtual.sanitizado();
        String emailNormalizado = normalizarEmail(emailPrincipal);
        if (emailNormalizado == null) {
            return resolvido;
        }

        List<ContextoPersistido> historicos = new ArrayList<>();
        cadastroContaRepositorio.findTopByEmailPrincipalOrderByAtualizadoEmDesc(emailNormalizado)
                .map(this::deCadastro)
                .ifPresent(historicos::add);
        recuperacaoSenhaRepositorio.findTopByEmailPrincipalOrderByAtualizadoEmDesc(emailNormalizado)
                .map(this::deRecuperacao)
                .ifPresent(historicos::add);
        historicos.sort(Comparator.comparing(ContextoPersistido::atualizadoEm).reversed());

        for (final ContextoPersistido contextoPersistido : historicos) {
            resolvido = resolvido.mesclarFaltantes(contextoPersistido.contexto());
        }
        return resolvido;
    }

    private ContextoPersistido deCadastro(final CadastroConta cadastroConta) {
        return new ContextoPersistido(
                cadastroConta.getAtualizadoEm(),
                new ContextoSolicitacaoFluxoPublico(
                        cadastroConta.getLocaleSolicitante(),
                        cadastroConta.getTimeZoneSolicitante(),
                        cadastroConta.getTipoProdutoExibicao(),
                        cadastroConta.getProdutoExibicao(),
                        cadastroConta.getCanalExibicao(),
                        cadastroConta.getEmpresaExibicao(),
                        cadastroConta.getAmbienteExibicao()
                )
        );
    }

    private ContextoPersistido deRecuperacao(final RecuperacaoSenha recuperacaoSenha) {
        return new ContextoPersistido(
                recuperacaoSenha.getAtualizadoEm(),
                new ContextoSolicitacaoFluxoPublico(
                        recuperacaoSenha.getLocaleSolicitante(),
                        recuperacaoSenha.getTimeZoneSolicitante(),
                        recuperacaoSenha.getTipoProdutoExibicao(),
                        recuperacaoSenha.getProdutoExibicao(),
                        recuperacaoSenha.getCanalExibicao(),
                        recuperacaoSenha.getEmpresaExibicao(),
                        recuperacaoSenha.getAmbienteExibicao()
                )
        );
    }

    private String normalizarEmail(final String emailPrincipal) {
        if (emailPrincipal == null) {
            return null;
        }
        String valor = emailPrincipal.trim().toLowerCase();
        return valor.isEmpty() ? null : valor;
    }

    private record ContextoPersistido(
            OffsetDateTime atualizadoEm,
            ContextoSolicitacaoFluxoPublico contexto
    ) {
    }
}
