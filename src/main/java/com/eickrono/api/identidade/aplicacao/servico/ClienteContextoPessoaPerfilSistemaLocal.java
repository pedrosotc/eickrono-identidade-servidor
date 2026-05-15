package com.eickrono.api.identidade.aplicacao.servico;

import com.eickrono.api.identidade.aplicacao.modelo.ContextoPessoaPerfilSistema;
import com.eickrono.api.identidade.dominio.modelo.CadastroConta;
import com.eickrono.api.identidade.dominio.repositorio.CadastroContaRepositorio;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ClienteContextoPessoaPerfilSistemaLocal implements ClienteContextoPessoaPerfilSistema {

    private static final String STATUS_LIBERADO = "LIBERADO";

    private final CadastroContaRepositorio cadastroContaRepositorio;

    public ClienteContextoPessoaPerfilSistemaLocal(final CadastroContaRepositorio cadastroContaRepositorio) {
        this.cadastroContaRepositorio = Objects.requireNonNull(
                cadastroContaRepositorio, "cadastroContaRepositorio e obrigatorio");
    }

    @Override
    public Optional<ContextoPessoaPerfilSistema> buscarPorPessoaId(final Long pessoaId) {
        if (pessoaId == null) {
            return Optional.empty();
        }
        return cadastroContaRepositorio.findTopByPessoaIdPerfilOrderByAtualizadoEmDesc(pessoaId)
                .filter(this::cadastroLocalCompleto)
                .map(this::mapearContexto);
    }

    @Override
    public Optional<ContextoPessoaPerfilSistema> buscarPorSub(final String sub) {
        return normalizarObrigatorioOpcional(sub)
                .flatMap(cadastroContaRepositorio::findBySubjectRemoto)
                .filter(this::cadastroLocalCompleto)
                .map(this::mapearContexto);
    }

    @Override
    public Optional<ContextoPessoaPerfilSistema> buscarPorEmail(final String email) {
        return normalizarObrigatorioOpcional(email)
                .map(valor -> valor.toLowerCase(Locale.ROOT))
                .flatMap(cadastroContaRepositorio::findTopByEmailPrincipalOrderByAtualizadoEmDesc)
                .filter(this::cadastroLocalCompleto)
                .map(this::mapearContexto);
    }

    @Override
    public Optional<ContextoPessoaPerfilSistema> buscarPorIdentificadorPublicoSistema(
            final String identificadorPublicoSistema) {
        return normalizarObrigatorioOpcional(identificadorPublicoSistema)
                .map(valor -> valor.toLowerCase(Locale.ROOT))
                .flatMap(cadastroContaRepositorio::findTopByUsuarioIgnoreCaseOrderByAtualizadoEmDesc)
                .filter(this::cadastroLocalCompleto)
                .map(this::mapearContexto);
    }

    private boolean cadastroLocalCompleto(final CadastroConta cadastroConta) {
        return cadastroConta.emailJaConfirmado()
                && cadastroConta.getPessoaIdPerfil() != null
                && textoPresente(cadastroConta.getUsuario())
                && textoPresente(cadastroConta.getEmailPrincipal());
    }

    private ContextoPessoaPerfilSistema mapearContexto(final CadastroConta cadastroConta) {
        return new ContextoPessoaPerfilSistema(
                cadastroConta.getPessoaIdPerfil(),
                cadastroConta.getSubjectRemoto(),
                cadastroConta.getEmailPrincipal(),
                cadastroConta.getNomeCompleto(),
                cadastroConta.getUsuario(),
                cadastroConta.getPerfilSistemaId(),
                STATUS_LIBERADO
        );
    }

    private static Optional<String> normalizarObrigatorioOpcional(final String valor) {
        if (valor == null) {
            return Optional.empty();
        }
        String normalizado = valor.trim();
        return normalizado.isBlank() ? Optional.empty() : Optional.of(normalizado);
    }

    private static boolean textoPresente(final String valor) {
        return valor != null && !valor.isBlank();
    }
}
