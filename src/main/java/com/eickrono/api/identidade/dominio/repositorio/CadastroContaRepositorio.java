package com.eickrono.api.identidade.dominio.repositorio;

import com.eickrono.api.identidade.dominio.modelo.CadastroConta;
import com.eickrono.api.identidade.dominio.modelo.StatusCadastroConta;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CadastroContaRepositorio extends JpaRepository<CadastroConta, Long> {

    Optional<CadastroConta> findByCadastroId(UUID cadastroId);

    Optional<CadastroConta> findByEmailPrincipal(String emailPrincipal);

    List<CadastroConta> findAllByEmailPrincipal(String emailPrincipal);

    Optional<CadastroConta> findTopByEmailPrincipalOrderByAtualizadoEmDesc(String emailPrincipal);

    Optional<CadastroConta> findByUsuarioIgnoreCaseAndSistemaSolicitanteIgnoreCase(
            String usuario,
            String sistemaSolicitante
    );

    Optional<CadastroConta> findBySubjectRemoto(String subjectRemoto);

    List<CadastroConta> findByStatusAndCriadoEmBefore(StatusCadastroConta status, OffsetDateTime criadoEm);

    List<CadastroConta> findByCriadoEmBefore(OffsetDateTime criadoEm);
}
