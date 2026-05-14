package com.eickrono.api.identidade.aplicacao.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.eickrono.api.identidade.aplicacao.excecao.ApiAutenticadaException;
import com.eickrono.api.identidade.aplicacao.modelo.IdentidadeFederadaKeycloak;
import com.eickrono.api.identidade.aplicacao.modelo.ProjetoFluxoPublicoResolvido;
import com.eickrono.api.identidade.aplicacao.modelo.SessaoInternaAutenticada;
import com.eickrono.api.identidade.apresentacao.dto.AtualizarAvatarPreferidoApiRequest;
import com.eickrono.api.identidade.apresentacao.dto.VinculoSocialDto;
import com.eickrono.api.identidade.apresentacao.dto.VinculosSociaisDto;
import com.eickrono.api.identidade.dominio.modelo.AuditoriaEventoIdentidade;
import com.eickrono.api.identidade.dominio.modelo.FormaAcesso;
import com.eickrono.api.identidade.dominio.modelo.PerfilIdentidade;
import com.eickrono.api.identidade.dominio.modelo.Pessoa;
import com.eickrono.api.identidade.dominio.modelo.ProvedorVinculoSocial;
import com.eickrono.api.identidade.dominio.modelo.TipoFormaAcesso;
import com.eickrono.api.identidade.dominio.modelo.VinculoSocial;
import com.eickrono.api.identidade.dominio.repositorio.AuditoriaEventoIdentidadeRepositorio;
import com.eickrono.api.identidade.dominio.repositorio.FormaAcessoRepositorio;
import com.eickrono.api.identidade.dominio.repositorio.PerfilIdentidadeRepositorio;
import com.eickrono.api.identidade.dominio.repositorio.VinculoSocialRepositorio;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpStatus;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class VinculoSocialServiceTest {

    @Mock
    private PerfilIdentidadeRepositorio perfilRepositorio;
    @Mock
    private ProvisionamentoIdentidadeService provisionamentoIdentidadeService;
    @Mock
    private AutenticacaoSessaoInternaServico autenticacaoSessaoInternaServico;
    @Mock
    private ClienteAdministracaoCadastroKeycloak clienteAdministracaoCadastroKeycloak;
    @Mock
    private JwtDecoder jwtDecoder;
    @Mock
    private ContextoSocialPendenteJdbc contextoSocialPendenteJdbc;
    @Mock
    private ResolvedorProjetoFluxoPublico resolvedorProjetoFluxoPublico;
    @Mock
    private AvatarSocialProjetoJdbc avatarSocialProjetoJdbc;

    private final List<VinculoSocial> vinculosPersistidos = new ArrayList<>();
    private final List<FormaAcesso> formasAcessoPersistidas = new ArrayList<>();
    private final List<AuditoriaEventoIdentidade> auditorias = new ArrayList<>();
    private final ClienteAdministracaoVinculosSociaisKeycloakFake clienteAdministracaoVinculosSociaisKeycloak =
            new ClienteAdministracaoVinculosSociaisKeycloakFake();

    private VinculoSocialService vinculoSocialService;
    private long proximoIdVinculo = 42L;
    private long proximoIdFormaAcesso = 100L;

    @Test
    @DisplayName("listar vínculos sociais: deve retornar os provedores suportados e ignorar vínculos legados não suportados")
    void deveListarProvedoresSuportados() throws Exception {
        inicializarServico();
        PerfilIdentidade perfil = criarPerfil();
        Pessoa pessoa = criarPessoa();
        Jwt jwt = jwt("sub-123");
        when(provisionamentoIdentidadeService.provisionarOuAtualizar(jwt)).thenReturn(pessoa);
        when(perfilRepositorio.findBySub("sub-123")).thenReturn(Optional.of(perfil));

        vinculosPersistidos.add(criarVinculo(perfil, 1L, "google", "teste@gmail.com"));
        vinculosPersistidos.add(criarVinculo(perfil, 2L, "GITHUB", "legacy"));

        VinculosSociaisDto resposta = vinculoSocialService.listar(jwt);

        assertThat(resposta.provedores()).hasSize(6);
        assertThat(resposta.provedores())
                .extracting(VinculoSocialDto::provedor, VinculoSocialDto::vinculado)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("google", true),
                        org.assertj.core.groups.Tuple.tuple("apple", false),
                        org.assertj.core.groups.Tuple.tuple("facebook", false),
                        org.assertj.core.groups.Tuple.tuple("linkedin", false),
                        org.assertj.core.groups.Tuple.tuple("instagram", false),
                        org.assertj.core.groups.Tuple.tuple("x", false));
        assertThat(resposta.provedores().getFirst().identificadorMascarado()).isEqualTo("t***@gmail.com");
    }

    @Test
    @DisplayName("sincronizar vínculos sociais: deve reconciliar Keycloak, projeção local e formas de acesso")
    void deveSincronizarVinculosComKeycloak() throws Exception {
        inicializarServico();
        PerfilIdentidade perfil = criarPerfil();
        Pessoa pessoa = criarPessoa();
        Jwt jwt = jwt("sub-123");
        when(provisionamentoIdentidadeService.provisionarOuAtualizar(jwt)).thenReturn(pessoa);
        when(perfilRepositorio.findBySub("sub-123")).thenReturn(Optional.of(perfil));

        vinculosPersistidos.add(criarVinculo(perfil, 1L, "google", "antigo"));
        vinculosPersistidos.add(criarVinculo(perfil, 2L, "apple", "sera-removido"));
        formasAcessoPersistidas.add(criarFormaAcesso(
                pessoa,
                10L,
                "GOOGLE",
                "legacy-google-id"));
        formasAcessoPersistidas.add(criarFormaAcesso(
                pessoa,
                11L,
                "APPLE",
                "legacy-apple-id"));

        clienteAdministracaoVinculosSociaisKeycloak.definir(
                "sub-123",
                List.of(
                        new IdentidadeFederadaKeycloak(
                                ProvedorVinculoSocial.GOOGLE,
                                "google-sub-1",
                                "teste@gmail.com"),
                        new IdentidadeFederadaKeycloak(
                                ProvedorVinculoSocial.LINKEDIN,
                                "linkedin-sub-55",
                                "thiago-linkedin")));

        VinculosSociaisDto resposta = vinculoSocialService.sincronizar(jwt, "google");

        assertThat(auditorias).hasSize(1);
        assertThat(auditorias.getFirst().getTipoEvento()).isEqualTo("VINCULO_SOCIAL_SINCRONIZADO");

        assertThat(vinculosPersistidos)
                .extracting(VinculoSocial::getProvedor, VinculoSocial::getIdentificador)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("google", "teste@gmail.com"),
                        org.assertj.core.groups.Tuple.tuple("linkedin", "thiago-linkedin"));

        assertThat(formasAcessoPersistidas)
                .extracting(FormaAcesso::getProvedor, FormaAcesso::getIdentificador)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("GOOGLE", "google-sub-1"),
                        org.assertj.core.groups.Tuple.tuple("LINKEDIN", "linkedin-sub-55"));

        assertThat(resposta.provedores())
                .extracting(VinculoSocialDto::provedor, VinculoSocialDto::vinculado)
                .contains(
                        org.assertj.core.groups.Tuple.tuple("google", true),
                        org.assertj.core.groups.Tuple.tuple("linkedin", true),
                        org.assertj.core.groups.Tuple.tuple("apple", false));
    }

    @Test
    @DisplayName("entrar e vincular: deve materializar vínculo social pendente sem novo token social")
    void deveVincularContextoPendenteAposLoginLocal() throws Exception {
        inicializarServico();
        PerfilIdentidade perfil = criarPerfil();
        Pessoa pessoa = criarPessoa();
        Jwt jwt = jwt("sub-123");
        UUID contextoId = UUID.fromString("aaaaaaaa-1111-2222-3333-bbbbbbbbbbbb");
        ContextoSocialPendenteJdbc.ContextoSocialPendenteAtivo contexto =
                new ContextoSocialPendenteJdbc.ContextoSocialPendenteAtivo(
                        contextoId,
                        1L,
                        "apple",
                        "apple-user-id",
                        "apple-user",
                        "Pessoa Apple",
                        null,
                        UUID.fromString("cccccccc-1111-2222-3333-dddddddddddd"),
                        "teste@eickrono.com",
                        "ENTRAR_E_VINCULAR",
                        0,
                        3
                );
        when(jwtDecoder.decode("access-login")).thenReturn(jwt);
        when(provisionamentoIdentidadeService.provisionarOuAtualizar(jwt)).thenReturn(pessoa);
        when(perfilRepositorio.findBySub("sub-123")).thenReturn(Optional.of(perfil));

        vinculoSocialService.vincularContextoPendenteAposLoginLocal(
                "access-login",
                contexto,
                "eickrono-thimisu-app");

        verify(clienteAdministracaoCadastroKeycloak).vincularIdentidadeFederada(
                eq("sub-123"),
                eq(new IdentidadeFederadaKeycloak(
                        ProvedorVinculoSocial.APPLE,
                        "apple-user-id",
                        "apple-user",
                        "Pessoa Apple",
                        null)));
        verify(contextoSocialPendenteJdbc).consumirSeCompativel(contextoId, "teste@eickrono.com");
        assertThat(vinculosPersistidos)
                .extracting(VinculoSocial::getProvedor, VinculoSocial::getIdentificador)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("apple", "apple-user"));
        assertThat(formasAcessoPersistidas)
                .extracting(FormaAcesso::getProvedor, FormaAcesso::getIdentificador)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("APPLE", "apple-user-id"));
        assertThat(auditorias).hasSize(1);
        assertThat(auditorias.getFirst().getTipoEvento()).isEqualTo("VINCULO_SOCIAL_VINCULADO");
    }

    @Test
    @DisplayName("remover vínculo social: deve remover no Keycloak e limpar a projeção local")
    void deveRemoverVinculoSocial() throws Exception {
        inicializarServico();
        PerfilIdentidade perfil = criarPerfil();
        Pessoa pessoa = criarPessoa();
        Jwt jwt = jwt("sub-123");
        when(provisionamentoIdentidadeService.provisionarOuAtualizar(jwt)).thenReturn(pessoa);
        when(perfilRepositorio.findBySub("sub-123")).thenReturn(Optional.of(perfil));

        vinculosPersistidos.add(criarVinculo(perfil, 1L, "google", "teste@gmail.com"));
        formasAcessoPersistidas.add(criarFormaAcesso(
                pessoa,
                10L,
                "GOOGLE",
                "google-sub-1"));
        formasAcessoPersistidas.add(criarFormaAcesso(
                pessoa,
                11L,
                TipoFormaAcesso.EMAIL_SENHA,
                "EMAIL_SENHA",
                "teste@eickrono.com"));
        clienteAdministracaoVinculosSociaisKeycloak.definir(
                "sub-123",
                List.of(new IdentidadeFederadaKeycloak(
                        ProvedorVinculoSocial.GOOGLE,
                        "google-sub-1",
                        "teste@gmail.com")));
        when(autenticacaoSessaoInternaServico.autenticar("teste@eickrono.com", "SenhaForte@123"))
                .thenReturn(new SessaoInternaAutenticada(true, "Bearer", "access", "refresh", 300L));

        VinculosSociaisDto resposta = vinculoSocialService.remover(jwt, "google", "SenhaForte@123");

        verify(perfilRepositorio).findBySub("sub-123");
        assertThat(clienteAdministracaoVinculosSociaisKeycloak.provedorRemovido("sub-123"))
                .contains(ProvedorVinculoSocial.GOOGLE);
        assertThat(vinculosPersistidos).isEmpty();
        assertThat(formasAcessoPersistidas)
                .singleElement()
                .extracting(FormaAcesso::getTipo)
                .isEqualTo(TipoFormaAcesso.EMAIL_SENHA);
        assertThat(auditorias.getFirst().getTipoEvento()).isEqualTo("VINCULO_SOCIAL_REMOVIDO");
        assertThat(resposta.provedores().stream()
                .filter(item -> item.provedor().equals("google"))
                .findFirst()
                .orElseThrow()
                .vinculado()).isFalse();
    }

    @Test
    @DisplayName("remover vínculo social: deve rejeitar quando a senha atual não confere")
    void deveRejeitarRemocaoQuandoSenhaAtualNaoConfere() throws Exception {
        inicializarServico();
        PerfilIdentidade perfil = criarPerfil();
        Pessoa pessoa = criarPessoa();
        Jwt jwt = jwt("sub-123");
        when(provisionamentoIdentidadeService.provisionarOuAtualizar(jwt)).thenReturn(pessoa);
        when(perfilRepositorio.findBySub("sub-123")).thenReturn(Optional.of(perfil));

        vinculosPersistidos.add(criarVinculo(perfil, 1L, "google", "teste@gmail.com"));
        formasAcessoPersistidas.add(criarFormaAcesso(
                pessoa,
                10L,
                "GOOGLE",
                "google-sub-1"));
        formasAcessoPersistidas.add(criarFormaAcesso(
                pessoa,
                11L,
                TipoFormaAcesso.EMAIL_SENHA,
                "EMAIL_SENHA",
                "teste@eickrono.com"));
        when(autenticacaoSessaoInternaServico.autenticar("teste@eickrono.com", "SenhaIncorreta"))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais invalidas."));

        assertThatThrownBy(() -> vinculoSocialService.remover(jwt, "google", "SenhaIncorreta"))
                .isInstanceOf(ApiAutenticadaException.class)
                .extracting("codigo")
                .isEqualTo("senha_confirmacao_invalida");
    }

    @Test
    @DisplayName("remover vínculo social: deve permitir remover vínculo secundário sem reautenticação por senha")
    void devePermitirRemoverVinculoSecundarioSemSenha() throws Exception {
        inicializarServico();
        PerfilIdentidade perfil = criarPerfil();
        Pessoa pessoa = criarPessoa();
        Jwt jwt = jwt("sub-123");
        when(provisionamentoIdentidadeService.provisionarOuAtualizar(jwt)).thenReturn(pessoa);
        when(perfilRepositorio.findBySub("sub-123")).thenReturn(Optional.of(perfil));

        vinculosPersistidos.add(criarVinculo(perfil, 1L, "google", "teste@gmail.com"));
        vinculosPersistidos.add(criarVinculo(perfil, 2L, "apple", "usuario@icloud.test"));
        formasAcessoPersistidas.add(criarFormaAcesso(
                pessoa,
                10L,
                "GOOGLE",
                "google-sub-1"));
        formasAcessoPersistidas.add(criarFormaAcesso(
                pessoa,
                11L,
                "APPLE",
                "apple-sub-1"));
        clienteAdministracaoVinculosSociaisKeycloak.definir(
                "sub-123",
                List.of(
                        new IdentidadeFederadaKeycloak(
                                ProvedorVinculoSocial.GOOGLE,
                                "google-sub-1",
                                "teste@gmail.com"),
                        new IdentidadeFederadaKeycloak(
                                ProvedorVinculoSocial.APPLE,
                                "apple-sub-1",
                                "usuario@icloud.test")));

        VinculosSociaisDto resposta = vinculoSocialService.remover(jwt, "google", "   ");

        verifyNoInteractions(autenticacaoSessaoInternaServico);
        assertThat(clienteAdministracaoVinculosSociaisKeycloak.provedorRemovido("sub-123"))
                .contains(ProvedorVinculoSocial.GOOGLE);
        assertThat(vinculosPersistidos)
                .extracting(VinculoSocial::getProvedor)
                .containsExactly("apple");
        assertThat(formasAcessoPersistidas)
                .extracting(FormaAcesso::getProvedor)
                .containsExactly("APPLE");
        assertThat(resposta.provedores().stream()
                .filter(item -> item.provedor().equals("google"))
                .findFirst()
                .orElseThrow()
                .vinculado()).isFalse();
    }

    @Test
    @DisplayName("remover vínculo social: deve exigir senha ao remover a última credencial social com senha disponível")
    void deveExigirSenhaAoRemoverUltimaCredencialSocial() throws Exception {
        inicializarServico();
        PerfilIdentidade perfil = criarPerfil();
        Pessoa pessoa = criarPessoa();
        Jwt jwt = jwt("sub-123");
        when(provisionamentoIdentidadeService.provisionarOuAtualizar(jwt)).thenReturn(pessoa);
        when(perfilRepositorio.findBySub("sub-123")).thenReturn(Optional.of(perfil));

        vinculosPersistidos.add(criarVinculo(perfil, 1L, "google", "teste@gmail.com"));
        formasAcessoPersistidas.add(criarFormaAcesso(
                pessoa,
                10L,
                "GOOGLE",
                "google-sub-1"));
        formasAcessoPersistidas.add(criarFormaAcesso(
                pessoa,
                11L,
                TipoFormaAcesso.EMAIL_SENHA,
                "EMAIL_SENHA",
                "teste@eickrono.com"));

        assertThatThrownBy(() -> vinculoSocialService.remover(jwt, "google", "   "))
                .isInstanceOf(ApiAutenticadaException.class)
                .extracting("codigo")
                .isEqualTo("senha_confirmacao_obrigatoria");
    }

    @Test
    @DisplayName("remover vínculo social: deve impedir a remoção da última credencial social sem alternativa de senha")
    void deveImpedirRemocaoDaUltimaCredencialSocialSemSenhaAlternativa() throws Exception {
        inicializarServico();
        PerfilIdentidade perfil = criarPerfil();
        Pessoa pessoa = criarPessoa();
        Jwt jwt = jwt("sub-123");
        when(provisionamentoIdentidadeService.provisionarOuAtualizar(jwt)).thenReturn(pessoa);
        when(perfilRepositorio.findBySub("sub-123")).thenReturn(Optional.of(perfil));

        vinculosPersistidos.add(criarVinculo(perfil, 1L, "google", "teste@gmail.com"));
        formasAcessoPersistidas.add(criarFormaAcesso(
                pessoa,
                10L,
                "GOOGLE",
                "google-sub-1"));

        assertThatThrownBy(() -> vinculoSocialService.remover(jwt, "google", ""))
                .isInstanceOf(ApiAutenticadaException.class)
                .extracting("codigo")
                .isEqualTo("ultima_credencial_social");
    }

    @Test
    @DisplayName("vincular vínculo social nativo: deve reconciliar vínculo e forma de acesso sem broker HTML")
    void deveVincularRedeSocialNativamente() throws Exception {
        inicializarServico();
        PerfilIdentidade perfil = criarPerfil();
        Pessoa pessoa = criarPessoa();
        Jwt jwt = jwt("sub-123");
        when(provisionamentoIdentidadeService.provisionarOuAtualizar(jwt)).thenReturn(pessoa);
        when(perfilRepositorio.findBySub("sub-123")).thenReturn(Optional.of(perfil));
        when(autenticacaoSessaoInternaServico.autenticarSocial("google", "google-access-token"))
                .thenReturn(new SessaoInternaAutenticada(
                        true,
                        "Bearer",
                        "access-social-google",
                        "refresh-social-google",
                        300L));
        when(jwtDecoder.decode("access-social-google"))
                .thenReturn(Jwt.withTokenValue("access-social-google")
                        .header("alg", "none")
                        .subject("sub-social-google")
                        .claim("email", "teste@gmail.com")
                        .build());
        clienteAdministracaoVinculosSociaisKeycloak.definir(
                "sub-social-google",
                List.of(new IdentidadeFederadaKeycloak(
                        ProvedorVinculoSocial.GOOGLE,
                        "google-sub-1",
                        "teste@gmail.com")));
        Mockito.doAnswer(invocation -> {
            String subjectRemoto = invocation.getArgument(0, String.class);
            IdentidadeFederadaKeycloak identidadeFederada =
                    invocation.getArgument(1, IdentidadeFederadaKeycloak.class);
            clienteAdministracaoVinculosSociaisKeycloak.definir(subjectRemoto, List.of(identidadeFederada));
            return null;
        }).when(clienteAdministracaoCadastroKeycloak)
                .vincularIdentidadeFederada(eq("sub-123"), any(IdentidadeFederadaKeycloak.class));

        VinculosSociaisDto resposta = vinculoSocialService.vincular(jwt, "google", "google-access-token");

        assertThat(auditorias).hasSize(1);
        assertThat(auditorias.getFirst().getTipoEvento()).isEqualTo("VINCULO_SOCIAL_VINCULADO");
        assertThat(vinculosPersistidos)
                .extracting(VinculoSocial::getProvedor, VinculoSocial::getIdentificador)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("google", "teste@gmail.com"));
        assertThat(formasAcessoPersistidas)
                .extracting(FormaAcesso::getProvedor, FormaAcesso::getIdentificador)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("GOOGLE", "google-sub-1"));
        assertThat(resposta.provedores().stream()
                .filter(item -> item.provedor().equals("google"))
                .findFirst()
                .orElseThrow()
                .vinculado()).isTrue();
    }

    @Test
    @DisplayName("vincular vínculo social nativo: deve consumir contexto social pendente compatível")
    void deveConsumirContextoSocialPendenteAoVincularRedeSocialNativamente() throws Exception {
        inicializarServico();
        PerfilIdentidade perfil = criarPerfil();
        Pessoa pessoa = criarPessoa();
        Jwt jwt = jwt("sub-123");
        when(provisionamentoIdentidadeService.provisionarOuAtualizar(jwt)).thenReturn(pessoa);
        when(perfilRepositorio.findBySub("sub-123")).thenReturn(Optional.of(perfil));
        when(autenticacaoSessaoInternaServico.autenticarSocial("google", "google-access-token"))
                .thenReturn(new SessaoInternaAutenticada(
                        true,
                        "Bearer",
                        "access-social-google",
                        "refresh-social-google",
                        300L));
        when(jwtDecoder.decode("access-social-google"))
                .thenReturn(Jwt.withTokenValue("access-social-google")
                        .header("alg", "none")
                        .subject("sub-social-google")
                        .claim("email", "teste@gmail.com")
                        .build());
        clienteAdministracaoVinculosSociaisKeycloak.definir(
                "sub-social-google",
                List.of(new IdentidadeFederadaKeycloak(
                        ProvedorVinculoSocial.GOOGLE,
                        "google-sub-1",
                        "teste@gmail.com")));
        Mockito.doAnswer(invocation -> {
            String subjectRemoto = invocation.getArgument(0, String.class);
            IdentidadeFederadaKeycloak identidadeFederada =
                    invocation.getArgument(1, IdentidadeFederadaKeycloak.class);
            clienteAdministracaoVinculosSociaisKeycloak.definir(subjectRemoto, List.of(identidadeFederada));
            return null;
        }).when(clienteAdministracaoCadastroKeycloak)
                .vincularIdentidadeFederada(eq("sub-123"), any(IdentidadeFederadaKeycloak.class));

        UUID contextoId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        vinculoSocialService.vincular(jwt, "google", "google-access-token", contextoId);

        verify(contextoSocialPendenteJdbc).consumirSeCompativel(contextoId, "teste@eickrono.com");
    }

    @Test
    @DisplayName("vincular vínculo social nativo: deve rejeitar quando a identidade já pertence a outra conta")
    void deveRejeitarVinculoSocialQuePertenceOutraConta() throws Exception {
        inicializarServico();
        PerfilIdentidade perfil = criarPerfil();
        Pessoa pessoa = criarPessoa();
        definirId(Pessoa.class, pessoa, 1L);
        Pessoa outraPessoa = new Pessoa(
                "sub-999",
                "outra@eickrono.com",
                "Outra Pessoa",
                Set.of("CLIENTE"),
                Set.of("ROLE_cliente"),
                OffsetDateTime.parse("2024-05-01T12:00:00Z"));
        definirId(Pessoa.class, outraPessoa, 2L);
        Jwt jwt = jwt("sub-123");
        when(provisionamentoIdentidadeService.provisionarOuAtualizar(jwt)).thenReturn(pessoa);
        when(perfilRepositorio.findBySub("sub-123")).thenReturn(Optional.of(perfil));
        when(autenticacaoSessaoInternaServico.autenticarSocial("google", "google-access-token"))
                .thenReturn(new SessaoInternaAutenticada(
                        true,
                        "Bearer",
                        "access-social-google",
                        "refresh-social-google",
                        300L));
        when(jwtDecoder.decode("access-social-google"))
                .thenReturn(Jwt.withTokenValue("access-social-google")
                        .header("alg", "none")
                        .subject("sub-social-google")
                        .claim("email", "teste@gmail.com")
                        .build());
        clienteAdministracaoVinculosSociaisKeycloak.definir(
                "sub-social-google",
                List.of(new IdentidadeFederadaKeycloak(
                        ProvedorVinculoSocial.GOOGLE,
                        "google-sub-1",
                        "teste@gmail.com")));
        formasAcessoPersistidas.add(criarFormaAcesso(
                outraPessoa,
                77L,
                "GOOGLE",
                "google-sub-1"));

        assertThatThrownBy(() -> vinculoSocialService.vincular(jwt, "google", "google-access-token"))
                .isInstanceOf(ApiAutenticadaException.class)
                .extracting("codigo")
                .isEqualTo("vinculo_social_pertence_a_outra_conta");
        verifyNoInteractions(clienteAdministracaoCadastroKeycloak);
    }

    @Test
    @DisplayName("sincronizar vínculos sociais: deve rejeitar provedor não suportado")
    void deveRejeitarProvedorNaoSuportado() {
        inicializarServico();

        assertThatThrownBy(() -> vinculoSocialService.sincronizar(jwt("sub-123"), "github"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Provedor social não suportado");
    }

    @Test
    @DisplayName("atualizar avatar preferido: deve refletir o avatar social principal do projeto")
    void deveAtualizarAvatarPreferidoSocialDoProjeto() throws Exception {
        inicializarServico();
        PerfilIdentidade perfil = criarPerfil();
        Pessoa pessoa = criarPessoa();
        Jwt jwt = jwt("sub-123");
        when(provisionamentoIdentidadeService.provisionarOuAtualizar(jwt)).thenReturn(pessoa);
        when(perfilRepositorio.findBySub("sub-123")).thenReturn(Optional.of(perfil));

        VinculoSocial vinculo = criarVinculo(perfil, 1L, "google", "teste@gmail.com");
        vinculosPersistidos.add(vinculo);
        FormaAcesso formaAcesso = criarFormaAcesso(pessoa, 10L, "GOOGLE", "google-sub-1");
        formaAcesso.atualizarDadosExternos(
                "Pessoa Google",
                "https://cdn.eickrono.test/google.png",
                OffsetDateTime.parse("2024-05-03T10:00:00Z"));
        formasAcessoPersistidas.add(formaAcesso);
        when(avatarSocialProjetoJdbc.buscarPreferencia("sub-123", 1L))
                .thenReturn(new AvatarSocialProjetoJdbc.PreferenciaAvatarProjeto(
                        "SOCIAL",
                        "https://cdn.eickrono.test/google.png",
                        "GOOGLE"));

        VinculosSociaisDto resposta = vinculoSocialService.atualizarAvatarPreferido(
                jwt,
                new AtualizarAvatarPreferidoApiRequest("eickrono-thimisu-app", "SOCIAL", "google", null));

        verify(avatarSocialProjetoJdbc).definirAvatarSocial(
                eq("sub-123"),
                eq(1L),
                eq(ProvedorVinculoSocial.GOOGLE),
                any());
        assertThat(resposta.avatarPreferidoOrigem()).isEqualTo("SOCIAL");
        assertThat(resposta.avatarPreferidoUrl()).isEqualTo("https://cdn.eickrono.test/google.png");
        VinculoSocialDto google = resposta.provedores().stream()
                .filter(item -> item.provedor().equals("google"))
                .findFirst()
                .orElseThrow();
        assertThat(google.avatarPrincipalNoProjeto()).isTrue();
        assertThat(google.urlAvatarExterno()).isEqualTo("https://cdn.eickrono.test/google.png");
    }

    @Test
    @DisplayName("sincronizar vínculos sociais: deve manter o vínculo mesmo quando o provedor não informar foto")
    void deveSincronizarVinculoSocialSemFotoDisponivel() throws Exception {
        inicializarServico();
        PerfilIdentidade perfil = criarPerfil();
        Pessoa pessoa = criarPessoa();
        Jwt jwt = jwt("sub-123");
        when(provisionamentoIdentidadeService.provisionarOuAtualizar(jwt)).thenReturn(pessoa);
        when(perfilRepositorio.findBySub("sub-123")).thenReturn(Optional.of(perfil));

        clienteAdministracaoVinculosSociaisKeycloak.definir(
                "sub-123",
                List.of(new IdentidadeFederadaKeycloak(
                        ProvedorVinculoSocial.GOOGLE,
                        "google-sub-1",
                        "teste@gmail.com",
                        "Pessoa Google",
                        null)));

        VinculosSociaisDto resposta = vinculoSocialService.sincronizar(
                jwt,
                "google",
                null,
                "eickrono-thimisu-app");

        VinculoSocialDto google = resposta.provedores().stream()
                .filter(item -> item.provedor().equals("google"))
                .findFirst()
                .orElseThrow();
        assertThat(google.vinculado()).isTrue();
        assertThat(google.urlAvatarExterno()).isNull();
        assertThat(google.avatarPrincipalNoProjeto()).isFalse();
        assertThat(google.statusAvatarSocial()).isEqualTo("FOTO_NAO_DISPONIVEL");
        assertThat(google.mensagemAvatarSocial())
                .isEqualTo("Esta conta esta vinculada, mas nao ha foto disponivel para usar no perfil neste momento.");
        assertThat(vinculosPersistidos.getFirst().getUrlAvatarExterno()).isNull();
        assertThat(formasAcessoPersistidas.getFirst().getUrlAvatarExterno()).isNull();
    }

    @Test
    @DisplayName("sincronizar vínculos sociais: deve manter o vínculo da Apple mesmo quando o provedor não informar foto")
    void deveSincronizarVinculoAppleSemFotoDisponivel() throws Exception {
        inicializarServico();
        PerfilIdentidade perfil = criarPerfil();
        Pessoa pessoa = criarPessoa();
        Jwt jwt = jwt("sub-123");
        when(provisionamentoIdentidadeService.provisionarOuAtualizar(jwt)).thenReturn(pessoa);
        when(perfilRepositorio.findBySub("sub-123")).thenReturn(Optional.of(perfil));

        clienteAdministracaoVinculosSociaisKeycloak.definir(
                "sub-123",
                List.of(new IdentidadeFederadaKeycloak(
                        ProvedorVinculoSocial.APPLE,
                        "apple-sub-1",
                        "usuario@icloud.test",
                        "Pessoa Apple",
                        "   ")));

        VinculosSociaisDto resposta = vinculoSocialService.sincronizar(
                jwt,
                "apple",
                null,
                "eickrono-thimisu-app");

        VinculoSocialDto apple = resposta.provedores().stream()
                .filter(item -> item.provedor().equals("apple"))
                .findFirst()
                .orElseThrow();
        assertThat(apple.vinculado()).isTrue();
        assertThat(apple.urlAvatarExterno()).isNull();
        assertThat(apple.avatarPrincipalNoProjeto()).isFalse();
        assertThat(apple.statusAvatarSocial()).isEqualTo("PROVEDOR_SEM_SUPORTE_DE_FOTO");
        assertThat(apple.mensagemAvatarSocial())
                .isEqualTo("Esta conta esta vinculada, mas este provedor nao disponibiliza foto para uso no perfil neste aplicativo.");
        assertThat(vinculosPersistidos.getFirst().getProvedor()).isEqualTo("apple");
        assertThat(vinculosPersistidos.getFirst().getUrlAvatarExterno()).isNull();
        assertThat(formasAcessoPersistidas.getFirst().getProvedor()).isEqualTo("APPLE");
        assertThat(formasAcessoPersistidas.getFirst().getUrlAvatarExterno()).isNull();
    }

    @Test
    @DisplayName("atualizar avatar preferido: deve rejeitar quando a rede social não possui foto disponível")
    void deveRejeitarAvatarPreferidoSocialSemFotoDisponivel() throws Exception {
        inicializarServico();
        Jwt jwt = jwt("sub-123");
        doThrow(ApiAutenticadaException.conflito(
                "avatar_social_indisponivel",
                "A rede social informada ainda nao possui foto disponivel para este projeto.",
                Map.of("provedor", "google")))
                .when(avatarSocialProjetoJdbc)
                .definirAvatarSocial(eq("sub-123"), eq(1L), eq(ProvedorVinculoSocial.GOOGLE), any());

        assertThatThrownBy(() -> vinculoSocialService.atualizarAvatarPreferido(
                jwt,
                new AtualizarAvatarPreferidoApiRequest("eickrono-thimisu-app", "SOCIAL", "google", null)))
                .isInstanceOf(ApiAutenticadaException.class)
                .extracting("codigo")
                .isEqualTo("avatar_social_indisponivel");
    }

    @Test
    @DisplayName("atualizar avatar preferido: deve rejeitar Apple quando a rede social não possui foto disponível")
    void deveRejeitarAvatarPreferidoAppleSemFotoDisponivel() throws Exception {
        inicializarServico();
        Jwt jwt = jwt("sub-123");
        doThrow(ApiAutenticadaException.conflito(
                "avatar_social_indisponivel",
                "A rede social informada ainda nao possui foto disponivel para este projeto.",
                Map.of("provedor", "apple")))
                .when(avatarSocialProjetoJdbc)
                .definirAvatarSocial(eq("sub-123"), eq(1L), eq(ProvedorVinculoSocial.APPLE), any());

        assertThatThrownBy(() -> vinculoSocialService.atualizarAvatarPreferido(
                jwt,
                new AtualizarAvatarPreferidoApiRequest("eickrono-thimisu-app", "SOCIAL", "apple", null)))
                .isInstanceOf(ApiAutenticadaException.class)
                .extracting("codigo")
                .isEqualTo("avatar_social_indisponivel");
    }

    private void inicializarServico() {
        vinculosPersistidos.clear();
        formasAcessoPersistidas.clear();
        auditorias.clear();
        clienteAdministracaoVinculosSociaisKeycloak.limpar();
        proximoIdVinculo = 42L;
        proximoIdFormaAcesso = 100L;
        Mockito.lenient().when(avatarSocialProjetoJdbc.buscarPreferencia(any(), any()))
                .thenReturn(AvatarSocialProjetoJdbc.PreferenciaAvatarProjeto.vazia());
        Mockito.lenient().when(resolvedorProjetoFluxoPublico.resolverAtivo(any()))
                .thenReturn(new ProjetoFluxoPublicoResolvido(
                        1L,
                        "thimisu",
                        "Thimisu",
                        "Aplicacao",
                        "Thimisu",
                        "Mobile",
                        false));

        AuditoriaService auditoriaService = new AuditoriaService(auditoriaRepositorio());
        vinculoSocialService = new VinculoSocialService(
                Objects.requireNonNull(perfilRepositorio),
                vinculoRepositorio(),
                formaAcessoRepositorio(),
                auditoriaService,
                Objects.requireNonNull(provisionamentoIdentidadeService),
                clienteAdministracaoVinculosSociaisKeycloak,
                Objects.requireNonNull(clienteAdministracaoCadastroKeycloak),
                Objects.requireNonNull(autenticacaoSessaoInternaServico),
                Objects.requireNonNull(jwtDecoder),
                Objects.requireNonNull(contextoSocialPendenteJdbc),
                Objects.requireNonNull(resolvedorProjetoFluxoPublico),
                Objects.requireNonNull(avatarSocialProjetoJdbc));
    }

    private VinculoSocialRepositorio vinculoRepositorio() {
        return (VinculoSocialRepositorio) Proxy.newProxyInstance(
                VinculoSocialRepositorio.class.getClassLoader(),
                new Class<?>[] {VinculoSocialRepositorio.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findByPerfil" -> vinculosPersistidos.stream()
                            .filter(vinculo -> vinculo.getPerfil().equals(Objects.requireNonNull(args)[0]))
                            .toList();
                    case "save" -> salvarVinculo((VinculoSocial) Objects.requireNonNull(args)[0]);
                    case "deleteAll" -> {
                        for (Object item : (Iterable<?>) Objects.requireNonNull(args)[0]) {
                            vinculosPersistidos.remove(item);
                        }
                        yield null;
                    }
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "VinculoSocialRepositorioFake";
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private FormaAcessoRepositorio formaAcessoRepositorio() {
        return (FormaAcessoRepositorio) Proxy.newProxyInstance(
                FormaAcessoRepositorio.class.getClassLoader(),
                new Class<?>[] {FormaAcessoRepositorio.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findByTipoAndProvedorAndIdentificador" -> localizarFormaPorChave(args);
                    case "findByPessoa" -> localizarFormasDaPessoa((Pessoa) Objects.requireNonNull(args)[0]);
                    case "save" -> salvarFormaAcesso((FormaAcesso) Objects.requireNonNull(args)[0]);
                    case "deleteAll" -> {
                        for (Object item : (Iterable<?>) Objects.requireNonNull(args)[0]) {
                            formasAcessoPersistidas.remove(item);
                        }
                        yield null;
                    }
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "FormaAcessoRepositorioFake";
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private AuditoriaEventoIdentidadeRepositorio auditoriaRepositorio() {
        return (AuditoriaEventoIdentidadeRepositorio) Proxy.newProxyInstance(
                AuditoriaEventoIdentidadeRepositorio.class.getClassLoader(),
                new Class<?>[] {AuditoriaEventoIdentidadeRepositorio.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "save" -> {
                        AuditoriaEventoIdentidade auditoria = (AuditoriaEventoIdentidade) Objects.requireNonNull(args)[0];
                        auditorias.add(auditoria);
                        yield auditoria;
                    }
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "AuditoriaEventoIdentidadeRepositorioFake";
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private Optional<FormaAcesso> localizarFormaPorChave(final Object[] args) {
        TipoFormaAcesso tipo = (TipoFormaAcesso) Objects.requireNonNull(args)[0];
        String provedor = (String) args[1];
        String identificador = (String) args[2];
        return formasAcessoPersistidas.stream()
                .filter(forma -> forma.getTipo() == tipo)
                .filter(forma -> Objects.equals(forma.getProvedor(), provedor))
                .filter(forma -> Objects.equals(forma.getIdentificador(), identificador))
                .findFirst();
    }

    private List<FormaAcesso> localizarFormasDaPessoa(final Pessoa pessoa) {
        return formasAcessoPersistidas.stream()
                .filter(forma -> Objects.equals(forma.getPessoa().getId(), pessoa.getId()))
                .toList();
    }

    private PerfilIdentidade criarPerfil() {
        return new PerfilIdentidade(
                "sub-123",
                "teste@eickrono.com",
                "Pessoa Teste",
                Set.of("CLIENTE"),
                Set.of("ROLE_cliente"),
                OffsetDateTime.parse("2024-05-01T12:00:00Z"));
    }

    private Pessoa criarPessoa() {
        return new Pessoa(
                "sub-123",
                "teste@eickrono.com",
                "Pessoa Teste",
                Set.of("CLIENTE"),
                Set.of("ROLE_cliente"),
                OffsetDateTime.parse("2024-05-01T12:00:00Z"));
    }

    private Jwt jwt(final String sub) {
        return Jwt.withTokenValue("token")
                .subject(sub)
                .claim("email", "teste@eickrono.com")
                .claim("name", "Pessoa Teste")
                .header("alg", "none")
                .build();
    }

    private VinculoSocial criarVinculo(final PerfilIdentidade perfil,
                                       final Long id,
                                       final String provedor,
                                       final String identificador) throws Exception {
        VinculoSocial vinculo = new VinculoSocial(
                perfil,
                provedor,
                identificador,
                OffsetDateTime.parse("2024-05-02T15:00:00Z"));
        definirId(VinculoSocial.class, vinculo, id);
        return vinculo;
    }

    private FormaAcesso criarFormaAcesso(final Pessoa pessoa,
                                         final Long id,
                                         final String provedor,
                                         final String identificador) throws Exception {
        return criarFormaAcesso(pessoa, id, TipoFormaAcesso.SOCIAL, provedor, identificador);
    }

    private FormaAcesso criarFormaAcesso(final Pessoa pessoa,
                                         final Long id,
                                         final TipoFormaAcesso tipo,
                                         final String provedor,
                                         final String identificador) throws Exception {
        FormaAcesso formaAcesso = new FormaAcesso(
                pessoa,
                tipo,
                provedor,
                identificador,
                false,
                OffsetDateTime.parse("2024-05-02T15:00:00Z"),
                OffsetDateTime.parse("2024-05-02T15:00:00Z"));
        definirId(FormaAcesso.class, formaAcesso, id);
        return formaAcesso;
    }

    private VinculoSocial salvarVinculo(final VinculoSocial vinculo) throws Exception {
        VinculoSocial salvo = Objects.requireNonNull(vinculo);
        if (salvo.getId() == null) {
            definirId(VinculoSocial.class, salvo, proximoIdVinculo++);
        }
        vinculosPersistidos.removeIf(existente -> Objects.equals(existente.getId(), salvo.getId()));
        vinculosPersistidos.add(salvo);
        return salvo;
    }

    private FormaAcesso salvarFormaAcesso(final FormaAcesso formaAcesso) throws Exception {
        FormaAcesso salvo = Objects.requireNonNull(formaAcesso);
        if (salvo.getId() == null) {
            definirId(FormaAcesso.class, salvo, proximoIdFormaAcesso++);
        }
        formasAcessoPersistidas.removeIf(existente -> Objects.equals(existente.getId(), salvo.getId()));
        formasAcessoPersistidas.add(salvo);
        return salvo;
    }

    private void definirId(final Class<?> tipo, final Object alvo, final Long id) throws Exception {
        Field field = tipo.getDeclaredField("id");
        field.setAccessible(true);
        field.set(alvo, id);
    }

    private static final class ClienteAdministracaoVinculosSociaisKeycloakFake
            implements ClienteAdministracaoVinculosSociaisKeycloak {

        private final Map<String, Map<ProvedorVinculoSocial, IdentidadeFederadaKeycloak>> identidadesPorUsuario =
                new java.util.LinkedHashMap<>();
        private final Map<String, ProvedorVinculoSocial> remocoes = new java.util.LinkedHashMap<>();

        @Override
        public List<IdentidadeFederadaKeycloak> listarIdentidadesFederadas(final String subjectRemoto) {
            return new ArrayList<>(identidadesPorUsuario.getOrDefault(subjectRemoto, Map.of()).values());
        }

        @Override
        public void removerIdentidadeFederada(final String subjectRemoto, final ProvedorVinculoSocial provedor) {
            identidadesPorUsuario.computeIfAbsent(subjectRemoto, ignored -> new java.util.LinkedHashMap<>())
                    .remove(provedor);
            remocoes.put(subjectRemoto, provedor);
        }

        void definir(final String subjectRemoto, final List<IdentidadeFederadaKeycloak> identidadesFederadas) {
            Map<ProvedorVinculoSocial, IdentidadeFederadaKeycloak> porProvedor = new java.util.LinkedHashMap<>();
            for (IdentidadeFederadaKeycloak identidadeFederada : identidadesFederadas) {
                porProvedor.put(identidadeFederada.provedor(), identidadeFederada);
            }
            identidadesPorUsuario.put(subjectRemoto, porProvedor);
        }

        Optional<ProvedorVinculoSocial> provedorRemovido(final String subjectRemoto) {
            return Optional.ofNullable(remocoes.get(subjectRemoto));
        }

        void limpar() {
            identidadesPorUsuario.clear();
            remocoes.clear();
        }
    }
}
