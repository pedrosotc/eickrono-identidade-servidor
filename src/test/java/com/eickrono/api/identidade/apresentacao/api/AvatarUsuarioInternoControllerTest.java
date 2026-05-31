package com.eickrono.api.identidade.apresentacao.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eickrono.api.identidade.aplicacao.modelo.ArquivoAvatarArmazenado;
import com.eickrono.api.identidade.aplicacao.servico.AvatarUsuarioUploadService;
import com.eickrono.api.identidade.aplicacao.servico.PendenciaRemocaoAvatarUsuarioService;
import com.eickrono.api.identidade.apresentacao.dto.avatar.MaterializarPendenciasRemocaoAvatarInternoApiRequest;
import com.eickrono.api.identidade.apresentacao.dto.avatar.MaterializarPendenciasRemocaoAvatarInternoApiResponse;
import com.eickrono.api.identidade.apresentacao.dto.avatar.UploadAvatarUsuarioInternoApiRequest;
import com.eickrono.api.identidade.apresentacao.dto.avatar.UploadAvatarUsuarioInternoApiResponse;
import com.eickrono.api.identidade.infraestrutura.configuracao.ValidadorChamadaInterna;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class AvatarUsuarioInternoControllerTest {

    @Mock
    private AvatarUsuarioUploadService avatarUsuarioUploadService;

    @Mock
    private PendenciaRemocaoAvatarUsuarioService pendenciaRemocaoAvatarUsuarioService;

    @Mock
    private ValidadorChamadaInterna validadorChamadaInterna;

    @Mock
    private Jwt jwt;

    @Test
    @DisplayName("deve validar chamada interna e devolver metadados do avatar armazenado")
    void deveValidarChamadaInternaEDevolverMetadadosDoAvatarArmazenado() {
        AvatarUsuarioInternoController controller = new AvatarUsuarioInternoController(
                avatarUsuarioUploadService,
                pendenciaRemocaoAvatarUsuarioService,
                validadorChamadaInterna
        );
        UploadAvatarUsuarioInternoApiRequest request = new UploadAvatarUsuarioInternoApiRequest(
                "THIMISU",
                "avatar.png",
                "image/png",
                68L,
                "base64-avatar"
        );
        when(avatarUsuarioUploadService.armazenar(
                "THIMISU",
                "avatar.png",
                "image/png",
                68L,
                "base64-avatar"
        )).thenReturn(new ArquivoAvatarArmazenado(
                "https://cdn.eickrono.test/avatares/thimisu/avatar.png",
                "avatares/thimisu/hash.png",
                "image/png",
                68L,
                "hash-conteudo",
                "versao-avatar"
        ));

        UploadAvatarUsuarioInternoApiResponse response = controller.upload(
                "segredo-interno",
                jwt,
                request
        );

        verify(validadorChamadaInterna).validar(
                "segredo-interno",
                jwt,
                "AvatarUsuarioInternoController"
        );
        assertThat(response.urlAvatar()).isEqualTo("https://cdn.eickrono.test/avatares/thimisu/avatar.png");
        assertThat(response.storageKey()).isEqualTo("avatares/thimisu/hash.png");
        assertThat(response.contentType()).isEqualTo("image/png");
        assertThat(response.tamanhoBytes()).isEqualTo(68L);
        assertThat(response.hashConteudo()).isEqualTo("hash-conteudo");
        assertThat(response.versao()).isEqualTo("versao-avatar");
    }

    @Test
    @DisplayName("deve validar chamada interna e materializar pendencias de remocao de avatar")
    void deveValidarChamadaInternaEMaterializarPendenciasDeRemocaoDeAvatar() {
        AvatarUsuarioInternoController controller = new AvatarUsuarioInternoController(
                avatarUsuarioUploadService,
                pendenciaRemocaoAvatarUsuarioService,
                validadorChamadaInterna
        );
        MaterializarPendenciasRemocaoAvatarInternoApiRequest request =
                new MaterializarPendenciasRemocaoAvatarInternoApiRequest(
                        "11111111-1111-1111-1111-111111111111",
                        "THIMISU",
                        List.of("22222222-2222-2222-2222-222222222222")
                );
        MaterializarPendenciasRemocaoAvatarInternoApiResponse respostaEsperada =
                new MaterializarPendenciasRemocaoAvatarInternoApiResponse(
                        "11111111-1111-1111-1111-111111111111",
                        "THIMISU",
                        1,
                        List.of("33333333-3333-3333-3333-333333333333"),
                        List.of("usuarios/222/avatar.png")
                );
        when(pendenciaRemocaoAvatarUsuarioService.materializar(request)).thenReturn(respostaEsperada);

        MaterializarPendenciasRemocaoAvatarInternoApiResponse response =
                controller.materializarPendenciasRemocao("segredo-interno", jwt, request);

        verify(validadorChamadaInterna).validar(
                "segredo-interno",
                jwt,
                "AvatarUsuarioInternoController"
        );
        assertThat(response).isEqualTo(respostaEsperada);
    }
}
