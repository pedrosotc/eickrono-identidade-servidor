package com.eickrono.api.identidade.aplicacao.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.eickrono.api.identidade.aplicacao.excecao.FluxoPublicoException;
import com.eickrono.api.identidade.aplicacao.modelo.ConviteOrganizacionalValidado;
import com.eickrono.api.identidade.aplicacao.modelo.ContextoPessoaPerfilSistema;
import com.eickrono.api.identidade.dominio.modelo.ConviteOrganizacional;
import com.eickrono.api.identidade.dominio.modelo.StatusConviteOrganizacional;
import com.eickrono.api.identidade.dominio.repositorio.ConviteOrganizacionalRepositorio;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class ConviteOrganizacionalServiceTest {

    @Mock
    private ConviteOrganizacionalRepositorio conviteOrganizacionalRepositorio;

    @Mock
    private ClienteContextoPessoaPerfilSistema clienteContextoPessoaPerfilSistema;

    private ConviteOrganizacionalService servico;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-04-24T18:00:00Z"), ZoneOffset.UTC);
        servico = new ConviteOrganizacionalService(
                conviteOrganizacionalRepositorio,
                clienteContextoPessoaPerfilSistema,
                clock
        );
    }

    @Test
    @DisplayName("deve validar convite organizacional publico quando estiver ativo")
    void deveValidarConviteOrganizacionalPublicoQuandoAtivo() {
        ConviteOrganizacional convite = new ConviteOrganizacional(
                "ORG-ACME-2026",
                "org-acme",
                "Acme Educacao",
                "convite@acme.test",
                "Jane Doe",
                true,
                OffsetDateTime.parse("2026-05-01T00:00:00Z")
        );
        when(conviteOrganizacionalRepositorio.findByCodigoIgnoreCase("ORG-ACME-2026"))
                .thenReturn(Optional.of(convite));

        ConviteOrganizacionalValidado resultado = servico.consultarPublico(" org-acme-2026 ");

        assertThat(resultado.codigo()).isEqualTo("ORG-ACME-2026");
        assertThat(resultado.organizacaoId()).isEqualTo("org-acme");
        assertThat(resultado.nomeOrganizacao()).isEqualTo("Acme Educacao");
        assertThat(resultado.emailConvidado()).isEqualTo("convite@acme.test");
        assertThat(resultado.nomeConvidado()).isEqualTo("Jane Doe");
        assertThat(resultado.exigeContaSeparada()).isTrue();
        assertThat(resultado.contaExistenteDetectada()).isFalse();
    }

    @Test
    @DisplayName("deve sinalizar conta existente quando o convite exige conta separada")
    void deveSinalizarContaExistenteQuandoConviteExigeContaSeparada() {
        ConviteOrganizacional convite = new ConviteOrganizacional(
                "ORG-ACME-2026",
                "org-acme",
                "Acme Educacao",
                "convite@acme.test",
                "Jane Doe",
                true,
                OffsetDateTime.parse("2026-05-01T00:00:00Z")
        );
        when(conviteOrganizacionalRepositorio.findByCodigoIgnoreCase("ORG-ACME-2026"))
                .thenReturn(Optional.of(convite));
        when(clienteContextoPessoaPerfilSistema.buscarPorEmail("convite@acme.test"))
                .thenReturn(Optional.of(new ContextoPessoaPerfilSistema(
                        10L,
                        "sub-usuario",
                        "convite@acme.test",
                        "Jane Doe",
                        "usuario-001",
                        "ATIVO"
                )));

        ConviteOrganizacionalValidado resultado = servico.consultarPublico("ORG-ACME-2026");

        assertThat(resultado.contaExistenteDetectada()).isTrue();
    }

    @Test
    @DisplayName("deve rejeitar convite expirado com erro tipado")
    void deveRejeitarConviteExpiradoComErroTipado() {
        ConviteOrganizacional convite = new ConviteOrganizacional(
                "ORG-EXPIRADO-2026",
                "org-acme",
                "Acme Educacao",
                "convite@acme.test",
                "Jane Doe",
                true,
                OffsetDateTime.parse("2026-04-20T00:00:00Z")
        );
        when(conviteOrganizacionalRepositorio.findByCodigoIgnoreCase("ORG-EXPIRADO-2026"))
                .thenReturn(Optional.of(convite));

        assertThatThrownBy(() -> servico.consultarPublico("ORG-EXPIRADO-2026"))
                .isInstanceOf(FluxoPublicoException.class)
                .satisfies(erro -> {
                    FluxoPublicoException excecao = (FluxoPublicoException) erro;
                    assertThat(excecao.getStatus()).isEqualTo(HttpStatus.GONE);
                    assertThat(excecao.getCodigo()).isEqualTo("convite_invalido");
                    assertThat(excecao.getDetalhes()).containsEntry("motivo", "expirado");
                });
    }

    @Test
    @DisplayName("deve rejeitar convite inexistente com erro tipado")
    void deveRejeitarConviteInexistenteComErroTipado() {
        when(conviteOrganizacionalRepositorio.findByCodigoIgnoreCase("ORG-AUSENTE-2026"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> servico.consultarPublico("ORG-AUSENTE-2026"))
                .isInstanceOf(FluxoPublicoException.class)
                .satisfies(erro -> {
                    FluxoPublicoException excecao = (FluxoPublicoException) erro;
                    assertThat(excecao.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(excecao.getCodigo()).isEqualTo("convite_invalido");
                    assertThat(excecao.getDetalhes()).containsEntry("motivo", "nao_encontrado");
                });
    }

    @Test
    @DisplayName("deve rejeitar convite revogado com erro tipado")
    void deveRejeitarConviteRevogadoComErroTipado() {
        ConviteOrganizacional convite = new ConviteOrganizacional(
                "ORG-REVOGADO-2026",
                "org-acme",
                "Acme Educacao",
                "convite@acme.test",
                "Jane Doe",
                true,
                OffsetDateTime.parse("2026-05-01T00:00:00Z")
        );
        convite.atualizarStatus(StatusConviteOrganizacional.REVOGADO);
        when(conviteOrganizacionalRepositorio.findByCodigoIgnoreCase("ORG-REVOGADO-2026"))
                .thenReturn(Optional.of(convite));

        assertThatThrownBy(() -> servico.consultarPublico("ORG-REVOGADO-2026"))
                .isInstanceOf(FluxoPublicoException.class)
                .satisfies(erro -> {
                    FluxoPublicoException excecao = (FluxoPublicoException) erro;
                    assertThat(excecao.getStatus()).isEqualTo(HttpStatus.GONE);
                    assertThat(excecao.getCodigo()).isEqualTo("convite_invalido");
                    assertThat(excecao.getDetalhes()).containsEntry("motivo", "revogado");
                });
    }
}
