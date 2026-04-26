package com.eickrono.api.identidade.aplicacao.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eickrono.api.identidade.dominio.modelo.CadastroConta;
import com.eickrono.api.identidade.dominio.modelo.CanalValidacaoTelefoneCadastro;
import com.eickrono.api.identidade.dominio.modelo.RegistroDispositivo;
import com.eickrono.api.identidade.dominio.modelo.StatusRegistroDispositivo;
import com.eickrono.api.identidade.dominio.modelo.TipoPessoaCadastro;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@ExtendWith(MockitoExtension.class)
class SincronizacaoModeloMultiappServiceTest {

    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Captor
    private ArgumentCaptor<String> sqlCaptor;

    @Captor
    private ArgumentCaptor<MapSqlParameterSource> paramsCaptor;

    private SincronizacaoModeloMultiappService service;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        when(jdbcTemplate.update(anyString(), any(MapSqlParameterSource.class))).thenReturn(1);
        when(jdbcTemplate.<java.util.Optional<Long>>query(
                anyString(),
                any(MapSqlParameterSource.class),
                org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.ResultSetExtractor<java.util.Optional<Long>>>any()))
                .thenReturn(java.util.Optional.of(1L));
        service = new SincronizacaoModeloMultiappService(jdbcTemplate);
    }

    @Test
    @DisplayName("deve projetar email confirmado do cadastro como verificado no modelo multiapp")
    void deveProjetarEmailConfirmadoDoCadastroComoVerificadoNoModeloMultiapp() {
        OffsetDateTime criadoEm = OffsetDateTime.parse("2026-04-10T10:00:00Z");
        OffsetDateTime confirmadoEm = OffsetDateTime.parse("2026-04-10T10:30:00Z");
        CadastroConta cadastro = new CadastroConta(
                UUID.randomUUID(),
                "sub-cadastro",
                TipoPessoaCadastro.FISICA,
                "Pessoa Cadastro",
                null,
                "pessoa.cadastro",
                null,
                null,
                null,
                "cadastro@eickrono.com",
                "+5511999999999",
                CanalValidacaoTelefoneCadastro.SMS,
                "hash-cadastro",
                criadoEm,
                criadoEm.plusHours(1),
                "ios",
                "127.0.0.1",
                "JUnit",
                criadoEm,
                criadoEm);
        cadastro.marcarEmailConfirmado(confirmadoEm);

        service.sincronizarCadastro(cadastro);

        MapSqlParameterSource params = capturarParametrosFormaAcessoEmail();
        assertThat(params.getValue("verificadoEm")).isEqualTo(confirmadoEm);
    }

    @Test
    @DisplayName("deve manter email sem verificacao no modelo multiapp quando a origem for registro de dispositivo")
    void deveManterEmailSemVerificacaoQuandoOrigemForRegistroDeDispositivo() {
        OffsetDateTime criadoEm = OffsetDateTime.parse("2026-04-10T10:00:00Z");
        OffsetDateTime confirmadoEm = OffsetDateTime.parse("2026-04-10T10:15:00Z");
        RegistroDispositivo registro = new RegistroDispositivo(
                UUID.randomUUID(),
                "sub-registro",
                "registro@eickrono.com",
                null,
                "ios|iphone15,2|instalacao-1",
                "IOS",
                "1.0.0",
                "chave-publica",
                StatusRegistroDispositivo.PENDENTE,
                criadoEm,
                criadoEm.plusHours(12));
        registro.definirStatus(StatusRegistroDispositivo.CONFIRMADO, confirmadoEm);

        service.sincronizarRegistroDispositivo(registro);

        MapSqlParameterSource params = capturarParametrosFormaAcessoEmail();
        assertThat(params.getValue("verificadoEm")).isNull();
    }

    private MapSqlParameterSource capturarParametrosFormaAcessoEmail() {
        verify(jdbcTemplate, atLeastOnce()).update(sqlCaptor.capture(), paramsCaptor.capture());
        List<String> sqls = sqlCaptor.getAllValues();
        List<MapSqlParameterSource> parametros = paramsCaptor.getAllValues();
        for (int indice = 0; indice < sqls.size(); indice++) {
            if (sqls.get(indice).contains("INSERT INTO autenticacao.usuarios_formas_acesso")) {
                return parametros.get(indice);
            }
        }
        throw new AssertionError("Nenhuma projecao de usuarios_formas_acesso foi executada.");
    }
}
