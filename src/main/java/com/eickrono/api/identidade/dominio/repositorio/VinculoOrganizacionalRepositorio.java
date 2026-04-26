package com.eickrono.api.identidade.dominio.repositorio;

import com.eickrono.api.identidade.dominio.modelo.VinculoOrganizacional;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VinculoOrganizacionalRepositorio extends JpaRepository<VinculoOrganizacional, Long> {

    Optional<VinculoOrganizacional> findByOrganizacaoIdAndUsuarioIdPerfil(String organizacaoId, String usuarioIdPerfil);

    List<VinculoOrganizacional> findAllByUsuarioIdPerfilOrderByCriadoEmAsc(String usuarioIdPerfil);
}
