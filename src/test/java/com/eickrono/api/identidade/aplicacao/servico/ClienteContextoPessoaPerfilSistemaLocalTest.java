package com.eickrono.api.identidade.aplicacao.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eickrono.api.identidade.aplicacao.modelo.ContextoPessoaPerfilSistema;
import com.eickrono.api.identidade.dominio.modelo.CadastroConta;
import com.eickrono.api.identidade.dominio.repositorio.CadastroContaRepositorio;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClienteContextoPessoaPerfilSistemaLocalTest {

    @Mock
    private CadastroContaRepositorio cadastroContaRepositorio;

    private ClienteContextoPessoaPerfilSistemaLocal cliente;

    @BeforeEach
    void setUp() {
        cliente = new ClienteContextoPessoaPerfilSistemaLocal(cadastroContaRepositorio);
    }

    @Test
    @DisplayName("deve resolver contexto por email usando cadastro local completo")
    void deveResolverContextoPorEmailLocal() {
        CadastroConta cadastro = cadastroCompletoConfirmado();
        when(cadastroContaRepositorio.findTopByEmailPrincipalOrderByAtualizadoEmDesc("ana@eickrono.com"))
                .thenReturn(Optional.of(cadastro));

        Optional<ContextoPessoaPerfilSistema> resultado = cliente.buscarPorEmail(" ANA@EICKRONO.COM ");

        assertThat(resultado).isPresent();
        assertThat(resultado.orElseThrow().usuario()).isEqualTo("ana.souza");
        assertThat(resultado.orElseThrow().statusPerfilSistema()).isEqualTo("LIBERADO");
    }

    @Test
    @DisplayName("deve resolver contexto por pessoa sem exigir perfil do produto")
    void deveResolverContextoPorPessoaSemPerfilProduto() {
        CadastroConta cadastro = cadastroCompletoConfirmado();
        when(cadastroContaRepositorio.findTopByPessoaIdPerfilOrderByAtualizadoEmDesc(10L))
                .thenReturn(Optional.of(cadastro));

        Optional<ContextoPessoaPerfilSistema> resultado = cliente.buscarPorPessoaId(10L);

        assertThat(resultado).isPresent();
        assertThat(resultado.orElseThrow().perfilSistemaId()).isNull();
        assertThat(resultado.orElseThrow().statusPerfilSistema()).isEqualTo("LIBERADO");
    }

    @Test
    @DisplayName("deve resolver identificador publico pelo usuario local")
    void deveResolverContextoPorUsuarioLocal() {
        CadastroConta cadastro = cadastroCompletoConfirmado();
        when(cadastroContaRepositorio.findTopByUsuarioIgnoreCaseOrderByAtualizadoEmDesc("ana.souza"))
                .thenReturn(Optional.of(cadastro));

        Optional<ContextoPessoaPerfilSistema> resultado = cliente.buscarPorIdentificadorPublicoSistema(" Ana.Souza ");

        assertThat(resultado).isPresent();
        assertThat(resultado.orElseThrow().emailPrincipal()).isEqualTo("ana@eickrono.com");
    }

    @Test
    @DisplayName("nao deve expor cadastro sem usuario como contexto valido")
    void naoDeveResolverCadastroSemUsuario() {
        CadastroConta cadastro = cadastroCompletoConfirmado();
        when(cadastro.getUsuario()).thenReturn(null);
        when(cadastroContaRepositorio.findBySubjectRemoto("sub-ana")).thenReturn(Optional.of(cadastro));

        Optional<ContextoPessoaPerfilSistema> resultado = cliente.buscarPorSub("sub-ana");

        assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("nao deve expor cadastro com email pendente como contexto valido")
    void naoDeveResolverCadastroPendenteEmail() {
        CadastroConta cadastro = cadastroCompletoConfirmado();
        when(cadastro.emailJaConfirmado()).thenReturn(false);
        when(cadastroContaRepositorio.findBySubjectRemoto("sub-ana")).thenReturn(Optional.of(cadastro));

        Optional<ContextoPessoaPerfilSistema> resultado = cliente.buscarPorSub("sub-ana");

        assertThat(resultado).isEmpty();
    }

    private CadastroConta cadastroCompletoConfirmado() {
        CadastroConta cadastro = mock(CadastroConta.class);
        lenient().when(cadastro.emailJaConfirmado()).thenReturn(true);
        lenient().when(cadastro.getPessoaIdPerfil()).thenReturn(10L);
        lenient().when(cadastro.getSubjectRemoto()).thenReturn("sub-ana");
        lenient().when(cadastro.getEmailPrincipal()).thenReturn("ana@eickrono.com");
        lenient().when(cadastro.getNomeCompleto()).thenReturn("Ana Souza");
        lenient().when(cadastro.getUsuario()).thenReturn("ana.souza");
        lenient().when(cadastro.getPerfilSistemaId()).thenReturn(null);
        return cadastro;
    }
}
