package com.eickrono.api.identidade.aplicacao.servico;

import com.eickrono.api.identidade.aplicacao.modelo.ContextoPessoaPerfil;
import com.eickrono.api.identidade.apresentacao.dto.VinculoOrganizacionalDto;
import com.eickrono.api.identidade.apresentacao.dto.VinculosOrganizacionaisDto;
import com.eickrono.api.identidade.dominio.modelo.VinculoOrganizacional;
import com.eickrono.api.identidade.dominio.repositorio.VinculoOrganizacionalRepositorio;
import java.util.List;
import java.util.Objects;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço para leitura dos vínculos organizacionais do usuário autenticado.
 */
@Service
public class VinculoOrganizacionalService {

    private final VinculoOrganizacionalRepositorio vinculoOrganizacionalRepositorio;
    private final ClienteContextoPessoaPerfil clienteContextoPessoaPerfil;

    public VinculoOrganizacionalService(final VinculoOrganizacionalRepositorio vinculoOrganizacionalRepositorio,
                                        final ClienteContextoPessoaPerfil clienteContextoPessoaPerfil) {
        this.vinculoOrganizacionalRepositorio = Objects.requireNonNull(
                vinculoOrganizacionalRepositorio,
                "vinculoOrganizacionalRepositorio é obrigatório"
        );
        this.clienteContextoPessoaPerfil = Objects.requireNonNull(
                clienteContextoPessoaPerfil,
                "clienteContextoPessoaPerfil é obrigatório"
        );
    }

    @Transactional(readOnly = true)
    public VinculosOrganizacionaisDto listar(final Jwt jwt) {
        String sub = Objects.requireNonNull(jwt, "jwt é obrigatório").getSubject();
        List<VinculoOrganizacionalDto> vinculos = clienteContextoPessoaPerfil.buscarPorSub(sub)
                .map(ContextoPessoaPerfil::usuarioId)
                .map(vinculoOrganizacionalRepositorio::findAllByUsuarioIdPerfilOrderByCriadoEmAsc)
                .stream()
                .flatMap(List::stream)
                .map(this::montarItem)
                .toList();
        return new VinculosOrganizacionaisDto(vinculos);
    }

    private VinculoOrganizacionalDto montarItem(final VinculoOrganizacional vinculo) {
        return new VinculoOrganizacionalDto(
                vinculo.getOrganizacaoId(),
                vinculo.getNomeOrganizacao(),
                vinculo.getConviteCodigo(),
                vinculo.getEmailConvidado(),
                vinculo.isExigeContaSeparada(),
                vinculo.getCriadoEm()
        );
    }
}
