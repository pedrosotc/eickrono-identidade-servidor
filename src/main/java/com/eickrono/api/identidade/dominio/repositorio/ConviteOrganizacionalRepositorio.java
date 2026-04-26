package com.eickrono.api.identidade.dominio.repositorio;

import com.eickrono.api.identidade.dominio.modelo.ConviteOrganizacional;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConviteOrganizacionalRepositorio extends JpaRepository<ConviteOrganizacional, Long> {

    Optional<ConviteOrganizacional> findByCodigoIgnoreCase(String codigo);
}
