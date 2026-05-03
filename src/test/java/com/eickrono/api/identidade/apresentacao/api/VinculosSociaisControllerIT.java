package com.eickrono.api.identidade.apresentacao.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eickrono.api.identidade.AplicacaoApiIdentidade;
import com.eickrono.api.identidade.aplicacao.modelo.IdentidadeFederadaKeycloak;
import com.eickrono.api.identidade.aplicacao.modelo.SessaoInternaAutenticada;
import com.eickrono.api.identidade.aplicacao.servico.AutenticacaoSessaoInternaServico;
import com.eickrono.api.identidade.dominio.modelo.FormaAcesso;
import com.eickrono.api.identidade.dominio.modelo.ProvedorVinculoSocial;
import com.eickrono.api.identidade.dominio.modelo.TipoFormaAcesso;
import com.eickrono.api.identidade.dominio.modelo.VinculoSocial;
import com.eickrono.api.identidade.dominio.repositorio.FormaAcessoRepositorio;
import com.eickrono.api.identidade.dominio.repositorio.VinculoSocialRepositorio;
import com.eickrono.api.identidade.support.ClienteAdministracaoCadastroKeycloakStubConfiguration;
import com.eickrono.api.identidade.support.InfraestruturaTesteIdentidade;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest(classes = AplicacaoApiIdentidade.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ClienteAdministracaoCadastroKeycloakStubConfiguration.class)
@ContextConfiguration(initializers = InfraestruturaTesteIdentidade.Initializer.class)
class VinculosSociaisControllerIT {

    private static final String ENDPOINT = "/identidade/vinculos-sociais";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClienteAdministracaoCadastroKeycloakStubConfiguration keycloakStub;

    @Autowired
    private VinculoSocialRepositorio vinculoSocialRepositorio;

    @Autowired
    private FormaAcessoRepositorio formaAcessoRepositorio;

    @MockBean
    private AutenticacaoSessaoInternaServico autenticacaoSessaoInternaServico;

    @MockBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void limparEstado() {
        keycloakStub.limparIdentidadesFederadas();
        vinculoSocialRepositorio.deleteAll();
        formaAcessoRepositorio.deleteAll();
    }

    @Test
    void deveSincronizarListarERemoverVinculosSociais() throws Exception {
        when(autenticacaoSessaoInternaServico.autenticar(
                eq("teste@eickrono.com"),
                eq("SenhaForte@123")))
                .thenReturn(new SessaoInternaAutenticada(true, "Bearer", "token", "refresh", 300));
        keycloakStub.definirIdentidadesFederadas(
                "sub-123",
                List.of(new IdentidadeFederadaKeycloak(
                        ProvedorVinculoSocial.GOOGLE,
                        "google-sub-1",
                        "teste@gmail.com")));

        mockMvc.perform(post(ENDPOINT + "/google/sincronizacao")
                        .with(jwtEscopo("vinculos:escrever")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provedores[0].provedor").value("google"))
                .andExpect(jsonPath("$.provedores[0].vinculado").value(true))
                .andExpect(jsonPath("$.provedores[0].identificadorMascarado").value("t***@gmail.com"));

        assertThat(vinculoSocialRepositorio.findAll())
                .extracting(VinculoSocial::getProvedor, VinculoSocial::getIdentificador)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("google", "teste@gmail.com"));
        assertThat(formaAcessoRepositorio.findAll().stream()
                .filter(forma -> forma.getTipo() == TipoFormaAcesso.SOCIAL)
                .toList())
                .extracting(FormaAcesso::getProvedor, FormaAcesso::getIdentificador)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("GOOGLE", "google-sub-1"));

        mockMvc.perform(get(ENDPOINT)
                        .with(jwtEscopo("vinculos:ler")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provedores[0].provedor").value("google"))
                .andExpect(jsonPath("$.provedores[0].vinculado").value(true));

        mockMvc.perform(delete(ENDPOINT + "/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "senhaConfirmacao": "SenhaForte@123"
                                }
                                """)
                        .with(jwtEscopo("vinculos:escrever")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provedores[0].provedor").value("google"))
                .andExpect(jsonPath("$.provedores[0].vinculado").value(false));

        assertThat(vinculoSocialRepositorio.findAll()).isEmpty();
        assertThat(formaAcessoRepositorio.findAll().stream()
                .filter(forma -> forma.getTipo() == TipoFormaAcesso.SOCIAL)
                .toList()).isEmpty();
    }

    @Test
    void deveExigirSenhaAtualParaRemoverVinculoSocial() throws Exception {
        keycloakStub.definirIdentidadesFederadas(
                "sub-123",
                List.of(new IdentidadeFederadaKeycloak(
                        ProvedorVinculoSocial.GOOGLE,
                        "google-sub-1",
                        "teste@gmail.com")));

        mockMvc.perform(post(ENDPOINT + "/google/sincronizacao")
                        .with(jwtEscopo("vinculos:escrever")))
                .andExpect(status().isOk());

        mockMvc.perform(delete(ENDPOINT + "/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "senhaConfirmacao": "   "
                                }
                                """)
                        .with(jwtEscopo("vinculos:escrever")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("senha_confirmacao_obrigatoria"))
                .andExpect(jsonPath("$.detalhes.provedor").value("google"))
                .andExpect(jsonPath("$.detalhes.exigeReautenticacao").value(true));

        assertThat(vinculoSocialRepositorio.findAll()).hasSize(1);
    }

    @Test
    void devePermitirRemoverVinculoSecundarioSemSenhaQuandoAindaExistirOutraCredencialSocial() throws Exception {
        keycloakStub.definirIdentidadesFederadas(
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

        mockMvc.perform(post(ENDPOINT + "/google/sincronizacao")
                        .with(jwtEscopo("vinculos:escrever")))
                .andExpect(status().isOk());

        mockMvc.perform(delete(ENDPOINT + "/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "senhaConfirmacao": "   "
                                }
                                """)
                        .with(jwtEscopo("vinculos:escrever")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provedores[0].provedor").value("google"))
                .andExpect(jsonPath("$.provedores[0].vinculado").value(false));

        assertThat(vinculoSocialRepositorio.findAll())
                .extracting(VinculoSocial::getProvedor)
                .containsExactly("apple");
        assertThat(formaAcessoRepositorio.findAll().stream()
                .filter(forma -> forma.getTipo() == TipoFormaAcesso.SOCIAL)
                .toList())
                .extracting(FormaAcesso::getProvedor)
                .containsExactly("APPLE");
    }

    @Test
    void deveRejeitarRemocaoQuandoSenhaAtualNaoConfere() throws Exception {
        when(autenticacaoSessaoInternaServico.autenticar(
                eq("teste@eickrono.com"),
                eq("SenhaErrada@123")))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais invalidas."));
        keycloakStub.definirIdentidadesFederadas(
                "sub-123",
                List.of(new IdentidadeFederadaKeycloak(
                        ProvedorVinculoSocial.GOOGLE,
                        "google-sub-1",
                        "teste@gmail.com")));

        mockMvc.perform(post(ENDPOINT + "/google/sincronizacao")
                        .with(jwtEscopo("vinculos:escrever")))
                .andExpect(status().isOk());

        mockMvc.perform(delete(ENDPOINT + "/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "senhaConfirmacao": "SenhaErrada@123"
                                }
                                """)
                        .with(jwtEscopo("vinculos:escrever")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("senha_confirmacao_invalida"))
                .andExpect(jsonPath("$.detalhes.exigeReautenticacao").value(true));

        assertThat(vinculoSocialRepositorio.findAll()).hasSize(1);
        assertThat(formaAcessoRepositorio.findAll().stream()
                .filter(forma -> forma.getTipo() == TipoFormaAcesso.SOCIAL)
                .toList()).hasSize(1);
    }

    @Test
    void deveNegarListagemSemEscopo() throws Exception {
        mockMvc.perform(get(ENDPOINT)
                        .with(jwtSemEscopo()))
                .andExpect(status().isForbidden());
    }

    @Test
    void deveVincularRedeSocialNativamenteSemHtml() throws Exception {
        when(autenticacaoSessaoInternaServico.autenticarSocial(
                eq("google"),
                eq("google-access-token")))
                .thenReturn(new SessaoInternaAutenticada(
                        true,
                        "Bearer",
                        "access-social-google",
                        "refresh-social-google",
                        300));
        when(jwtDecoder.decode("access-social-google"))
                .thenReturn(Jwt.withTokenValue("access-social-google")
                        .header("alg", "none")
                        .subject("sub-social-google")
                        .claim("email", "teste@gmail.com")
                        .build());
        keycloakStub.definirIdentidadesFederadas(
                "sub-social-google",
                List.of(new IdentidadeFederadaKeycloak(
                        ProvedorVinculoSocial.GOOGLE,
                        "google-sub-1",
                        "teste@gmail.com")));

        mockMvc.perform(post(ENDPOINT + "/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tokenExterno": "google-access-token"
                                }
                                """)
                        .with(jwtEscopo("vinculos:escrever")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provedores[0].provedor").value("google"))
                .andExpect(jsonPath("$.provedores[0].vinculado").value(true))
                .andExpect(jsonPath("$.provedores[0].identificadorMascarado").value("t***@gmail.com"));

        assertThat(vinculoSocialRepositorio.findAll())
                .extracting(VinculoSocial::getProvedor, VinculoSocial::getIdentificador)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("google", "teste@gmail.com"));
        assertThat(formaAcessoRepositorio.findAll().stream()
                .filter(forma -> forma.getTipo() == TipoFormaAcesso.SOCIAL)
                .toList())
                .extracting(FormaAcesso::getProvedor, FormaAcesso::getIdentificador)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("GOOGLE", "google-sub-1"));
    }

    @Test
    void deveAtualizarAvatarPreferidoSocialPorProjeto() throws Exception {
        keycloakStub.definirIdentidadesFederadas(
                "sub-123",
                List.of(new IdentidadeFederadaKeycloak(
                        ProvedorVinculoSocial.GOOGLE,
                        "google-sub-1",
                        "teste@gmail.com",
                        "Pessoa Google",
                        "https://cdn.eickrono.test/google.png")));

        mockMvc.perform(post(ENDPOINT + "/google/sincronizacao")
                        .param("aplicacaoId", "eickrono-thimisu-app")
                        .with(jwtEscopo("vinculos:escrever")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provedores[0].urlAvatarExterno")
                        .value("https://cdn.eickrono.test/google.png"));

        mockMvc.perform(put(ENDPOINT + "/avatar-preferido")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "aplicacaoId": "eickrono-thimisu-app",
                                  "origem": "SOCIAL",
                                  "provedor": "google"
                                }
                                """)
                        .with(jwtEscopo("vinculos:escrever")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarPreferidoOrigem").value("SOCIAL"))
                .andExpect(jsonPath("$.avatarPreferidoUrl").value("https://cdn.eickrono.test/google.png"))
                .andExpect(jsonPath("$.provedores[0].provedor").value("google"))
                .andExpect(jsonPath("$.provedores[0].avatarPrincipalNoProjeto").value(true))
                .andExpect(jsonPath("$.provedores[0].urlAvatarExterno")
                        .value("https://cdn.eickrono.test/google.png"));
    }

    @Test
    void deveSincronizarVinculoSocialSemFotoDisponivel() throws Exception {
        keycloakStub.definirIdentidadesFederadas(
                "sub-123",
                List.of(new IdentidadeFederadaKeycloak(
                        ProvedorVinculoSocial.GOOGLE,
                        "google-sub-1",
                        "teste@gmail.com",
                        "Pessoa Google",
                        null)));

        mockMvc.perform(post(ENDPOINT + "/google/sincronizacao")
                        .param("aplicacaoId", "eickrono-thimisu-app")
                        .with(jwtEscopo("vinculos:escrever")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provedores[0].provedor").value("google"))
                .andExpect(jsonPath("$.provedores[0].avatarPrincipalNoProjeto").value(false))
                .andExpect(jsonPath("$.provedores[0].statusAvatarSocial").value("FOTO_NAO_DISPONIVEL"))
                .andExpect(jsonPath("$.provedores[0].mensagemAvatarSocial")
                        .value("Esta conta esta vinculada, mas nao ha foto disponivel para usar no perfil neste momento."));

        assertThat(formaAcessoRepositorio.findAll().stream()
                .filter(forma -> forma.getTipo() == TipoFormaAcesso.SOCIAL)
                .map(FormaAcesso::getUrlAvatarExterno)
                .toList())
                .containsExactly((String) null);
    }

    @Test
    void deveSincronizarVinculoSocialFacebookSemFotoDisponivel() throws Exception {
        keycloakStub.definirIdentidadesFederadas(
                "sub-123",
                List.of(new IdentidadeFederadaKeycloak(
                        ProvedorVinculoSocial.FACEBOOK,
                        "facebook-sub-1",
                        "usuario.facebook.test",
                        "Pessoa Facebook",
                        "")));

        mockMvc.perform(post(ENDPOINT + "/facebook/sincronizacao")
                        .param("aplicacaoId", "eickrono-thimisu-app")
                        .with(jwtEscopo("vinculos:escrever")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provedores[2].provedor").value("facebook"))
                .andExpect(jsonPath("$.provedores[2].vinculado").value(true))
                .andExpect(jsonPath("$.provedores[2].avatarPrincipalNoProjeto").value(false))
                .andExpect(jsonPath("$.provedores[2].statusAvatarSocial").value("FOTO_NAO_DISPONIVEL"));

        assertThat(formaAcessoRepositorio.findAll().stream()
                .filter(forma -> forma.getTipo() == TipoFormaAcesso.SOCIAL)
                .filter(forma -> "FACEBOOK".equals(forma.getProvedor()))
                .map(FormaAcesso::getUrlAvatarExterno)
                .toList())
                .containsExactly((String) null);
    }

    @Test
    void deveInformarQuandoProvedorNaoSuportaFotoNoProjetoAtual() throws Exception {
        keycloakStub.definirIdentidadesFederadas(
                "sub-123",
                List.of(new IdentidadeFederadaKeycloak(
                        ProvedorVinculoSocial.APPLE,
                        "apple-sub-1",
                        "usuario@icloud.test",
                        "Pessoa Apple",
                        null)));

        mockMvc.perform(post(ENDPOINT + "/apple/sincronizacao")
                        .param("aplicacaoId", "eickrono-thimisu-app")
                        .with(jwtEscopo("vinculos:escrever")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provedores[1].provedor").value("apple"))
                .andExpect(jsonPath("$.provedores[1].vinculado").value(true))
                .andExpect(jsonPath("$.provedores[1].statusAvatarSocial")
                        .value("PROVEDOR_SEM_SUPORTE_DE_FOTO"))
                .andExpect(jsonPath("$.provedores[1].mensagemAvatarSocial")
                        .value("Esta conta esta vinculada, mas este provedor nao disponibiliza foto para uso no perfil neste aplicativo."));
    }

    @Test
    void deveRejeitarAvatarPreferidoSocialQuandoRedeNaoPossuiFoto() throws Exception {
        keycloakStub.definirIdentidadesFederadas(
                "sub-123",
                List.of(new IdentidadeFederadaKeycloak(
                        ProvedorVinculoSocial.GOOGLE,
                        "google-sub-1",
                        "teste@gmail.com",
                        "Pessoa Google",
                        null)));

        mockMvc.perform(post(ENDPOINT + "/google/sincronizacao")
                        .param("aplicacaoId", "eickrono-thimisu-app")
                        .with(jwtEscopo("vinculos:escrever")))
                .andExpect(status().isOk());

        mockMvc.perform(put(ENDPOINT + "/avatar-preferido")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "aplicacaoId": "eickrono-thimisu-app",
                                  "origem": "SOCIAL",
                                  "provedor": "google"
                                }
                                """)
                        .with(jwtEscopo("vinculos:escrever")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("avatar_social_indisponivel"))
                .andExpect(jsonPath("$.detalhes.provedor").value("google"));
    }

    @Test
    void deveRejeitarAvatarPreferidoSocialFacebookQuandoRedeNaoPossuiFoto() throws Exception {
        keycloakStub.definirIdentidadesFederadas(
                "sub-123",
                List.of(new IdentidadeFederadaKeycloak(
                        ProvedorVinculoSocial.FACEBOOK,
                        "facebook-sub-1",
                        "usuario.facebook.test",
                        "Pessoa Facebook",
                        null)));

        mockMvc.perform(post(ENDPOINT + "/facebook/sincronizacao")
                        .param("aplicacaoId", "eickrono-thimisu-app")
                        .with(jwtEscopo("vinculos:escrever")))
                .andExpect(status().isOk());

        mockMvc.perform(put(ENDPOINT + "/avatar-preferido")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "aplicacaoId": "eickrono-thimisu-app",
                                  "origem": "SOCIAL",
                                  "provedor": "facebook"
                                }
                                """)
                        .with(jwtEscopo("vinculos:escrever")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("avatar_social_indisponivel"))
                .andExpect(jsonPath("$.detalhes.provedor").value("facebook"));
    }

    @Test
    void deveLimparAvatarPreferidoQuandoProvedorPerderFotoNaSincronizacao() throws Exception {
        keycloakStub.definirIdentidadesFederadas(
                "sub-123",
                List.of(new IdentidadeFederadaKeycloak(
                        ProvedorVinculoSocial.GOOGLE,
                        "google-sub-1",
                        "teste@gmail.com",
                        "Pessoa Google",
                        "https://cdn.eickrono.test/google.png")));

        mockMvc.perform(post(ENDPOINT + "/google/sincronizacao")
                        .param("aplicacaoId", "eickrono-thimisu-app")
                        .with(jwtEscopo("vinculos:escrever")))
                .andExpect(status().isOk());

        mockMvc.perform(put(ENDPOINT + "/avatar-preferido")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "aplicacaoId": "eickrono-thimisu-app",
                                  "origem": "SOCIAL",
                                  "provedor": "google"
                                }
                                """)
                        .with(jwtEscopo("vinculos:escrever")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarPreferidoOrigem").value("SOCIAL"));

        keycloakStub.definirIdentidadesFederadas(
                "sub-123",
                List.of(new IdentidadeFederadaKeycloak(
                        ProvedorVinculoSocial.GOOGLE,
                        "google-sub-1",
                        "teste@gmail.com",
                        "Pessoa Google",
                        "   ")));

        mockMvc.perform(post(ENDPOINT + "/google/sincronizacao")
                        .param("aplicacaoId", "eickrono-thimisu-app")
                        .with(jwtEscopo("vinculos:escrever")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarPreferidoOrigem").value("NENHUM"))
                .andExpect(jsonPath("$.provedores[0].avatarPrincipalNoProjeto").value(false))
                .andExpect(jsonPath("$.provedores[0].statusAvatarSocial")
                        .value("FOTO_REMOVIDA_APOS_SINCRONIZACAO"))
                .andExpect(jsonPath("$.provedores[0].mensagemAvatarSocial")
                        .value("A foto desta rede social nao esta mais disponivel. Por isso ela deixou de poder ser usada como foto de perfil."));

        assertThat(formaAcessoRepositorio.findAll().stream()
                .filter(forma -> forma.getTipo() == TipoFormaAcesso.SOCIAL)
                .map(FormaAcesso::getUrlAvatarExterno)
                .toList())
                .containsExactly((String) null);
    }

    @Test
    void deveManterAvatarPreferidoQuandoOutroProvedorSemFotoForSincronizado() throws Exception {
        keycloakStub.definirIdentidadesFederadas(
                "sub-123",
                List.of(
                        new IdentidadeFederadaKeycloak(
                                ProvedorVinculoSocial.GOOGLE,
                                "google-sub-1",
                                "teste@gmail.com",
                                "Pessoa Google",
                                "https://cdn.eickrono.test/google.png"),
                        new IdentidadeFederadaKeycloak(
                                ProvedorVinculoSocial.FACEBOOK,
                                "facebook-sub-1",
                                "usuario.facebook.test",
                                "Pessoa Facebook",
                                "https://cdn.eickrono.test/facebook.png")));

        mockMvc.perform(post(ENDPOINT + "/google/sincronizacao")
                        .param("aplicacaoId", "eickrono-thimisu-app")
                        .with(jwtEscopo("vinculos:escrever")))
                .andExpect(status().isOk());

        mockMvc.perform(put(ENDPOINT + "/avatar-preferido")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "aplicacaoId": "eickrono-thimisu-app",
                                  "origem": "SOCIAL",
                                  "provedor": "google"
                                }
                                """)
                        .with(jwtEscopo("vinculos:escrever")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarPreferidoOrigem").value("SOCIAL"))
                .andExpect(jsonPath("$.provedores[0].avatarPrincipalNoProjeto").value(true));

        keycloakStub.definirIdentidadesFederadas(
                "sub-123",
                List.of(
                        new IdentidadeFederadaKeycloak(
                                ProvedorVinculoSocial.GOOGLE,
                                "google-sub-1",
                                "teste@gmail.com",
                                "Pessoa Google",
                                "https://cdn.eickrono.test/google.png"),
                        new IdentidadeFederadaKeycloak(
                                ProvedorVinculoSocial.FACEBOOK,
                                "facebook-sub-1",
                                "usuario.facebook.test",
                                "Pessoa Facebook",
                                "   ")));

        mockMvc.perform(post(ENDPOINT + "/facebook/sincronizacao")
                        .param("aplicacaoId", "eickrono-thimisu-app")
                        .with(jwtEscopo("vinculos:escrever")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarPreferidoOrigem").value("SOCIAL"))
                .andExpect(jsonPath("$.avatarPreferidoUrl").value("https://cdn.eickrono.test/google.png"))
                .andExpect(jsonPath("$.provedores[0].avatarPrincipalNoProjeto").value(true))
                .andExpect(jsonPath("$.provedores[2].avatarPrincipalNoProjeto").value(false))
                .andExpect(jsonPath("$.provedores[2].statusAvatarSocial")
                        .value("FOTO_REMOVIDA_APOS_SINCRONIZACAO"));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor jwtEscopo(final String escopo) {
        return jwt().jwt(builder -> builder
                        .subject("sub-123")
                        .claim("email", "teste@eickrono.com")
                        .claim("name", "Pessoa Teste"))
                .authorities(new SimpleGrantedAuthority("SCOPE_" + Objects.requireNonNull(escopo)));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor jwtSemEscopo() {
        return jwt().jwt(builder -> builder
                .subject("sub-123")
                .claim("email", "teste@eickrono.com")
                .claim("name", "Pessoa Teste"));
    }
}
