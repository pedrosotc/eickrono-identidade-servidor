package com.eickrono.api.identidade.apresentacao.api;

import com.eickrono.api.identidade.aplicacao.modelo.ArquivoAvatarArmazenado;
import com.eickrono.api.identidade.aplicacao.servico.AvatarUsuarioUploadService;
import com.eickrono.api.identidade.aplicacao.servico.PendenciaRemocaoAvatarUsuarioService;
import com.eickrono.api.identidade.apresentacao.dto.avatar.MaterializarPendenciasRemocaoAvatarInternoApiRequest;
import com.eickrono.api.identidade.apresentacao.dto.avatar.MaterializarPendenciasRemocaoAvatarInternoApiResponse;
import com.eickrono.api.identidade.apresentacao.dto.avatar.UploadAvatarUsuarioInternoApiRequest;
import com.eickrono.api.identidade.apresentacao.dto.avatar.UploadAvatarUsuarioInternoApiResponse;
import com.eickrono.api.identidade.infraestrutura.configuracao.ValidadorChamadaInterna;
import jakarta.validation.Valid;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/identidade/avatares/interna")
public class AvatarUsuarioInternoController {

    private static final String HEADER_SEGREDO_INTERNO = "X-Eickrono-Internal-Secret";
    private static final Logger LOGGER = LoggerFactory.getLogger(AvatarUsuarioInternoController.class);

    private final AvatarUsuarioUploadService avatarUsuarioUploadService;
    private final PendenciaRemocaoAvatarUsuarioService pendenciaRemocaoAvatarUsuarioService;
    private final ValidadorChamadaInterna validadorChamadaInterna;

    public AvatarUsuarioInternoController(final AvatarUsuarioUploadService avatarUsuarioUploadService,
                                          final PendenciaRemocaoAvatarUsuarioService pendenciaRemocaoAvatarUsuarioService,
                                          final ValidadorChamadaInterna validadorChamadaInterna) {
        this.avatarUsuarioUploadService = Objects.requireNonNull(
                avatarUsuarioUploadService,
                "avatarUsuarioUploadService é obrigatório");
        this.pendenciaRemocaoAvatarUsuarioService = Objects.requireNonNull(
                pendenciaRemocaoAvatarUsuarioService,
                "pendenciaRemocaoAvatarUsuarioService é obrigatório");
        this.validadorChamadaInterna = Objects.requireNonNull(
                validadorChamadaInterna,
                "validadorChamadaInterna é obrigatório");
    }

    @PostMapping("/uploads")
    public UploadAvatarUsuarioInternoApiResponse upload(
            @RequestHeader(HEADER_SEGREDO_INTERNO) final String segredoInterno,
            @AuthenticationPrincipal final Jwt jwt,
            @Valid @RequestBody final UploadAvatarUsuarioInternoApiRequest request) {
        validadorChamadaInterna.validar(segredoInterno, jwt, "AvatarUsuarioInternoController");
        LOGGER.info(
                "qa_avatar_upload_interno_recebido origem={} contentType={} tamanhoBytesDeclarado={} nomeArquivoPresente={} jwtSubjectPresente={}",
                request.origem(),
                request.contentType(),
                request.tamanhoBytes(),
                request.nomeArquivo() != null && !request.nomeArquivo().isBlank(),
                jwt != null && jwt.getSubject() != null && !jwt.getSubject().isBlank()
        );
        ArquivoAvatarArmazenado arquivo = avatarUsuarioUploadService.armazenar(
                request.origem(),
                request.nomeArquivo(),
                request.contentType(),
                request.tamanhoBytes(),
                request.conteudoBase64()
        );
        return UploadAvatarUsuarioInternoApiResponse.de(arquivo);
    }

    @PostMapping("/remocoes/pendencias")
    public MaterializarPendenciasRemocaoAvatarInternoApiResponse materializarPendenciasRemocao(
            @RequestHeader(HEADER_SEGREDO_INTERNO) final String segredoInterno,
            @AuthenticationPrincipal final Jwt jwt,
            @Valid @RequestBody final MaterializarPendenciasRemocaoAvatarInternoApiRequest request) {
        validadorChamadaInterna.validar(segredoInterno, jwt, "AvatarUsuarioInternoController");
        LOGGER.info(
                "qa_avatar_remocao_pendencia_materializacao_recebida correlacaoId={} produto={} usuariosCliente={}",
                request.correlacaoId(),
                request.produto(),
                request.usuarioClienteIds() == null ? 0 : request.usuarioClienteIds().size()
        );
        return pendenciaRemocaoAvatarUsuarioService.materializar(request);
    }
}
