package com.eickrono.api.identidade.aplicacao.servico;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.eickrono.api.identidade.aplicacao.excecao.EntregaEmailException;
import com.eickrono.api.identidade.aplicacao.modelo.ContextoSolicitacaoFluxoPublico;
import com.eickrono.api.identidade.dominio.modelo.CanalValidacaoTelefoneCadastro;
import com.eickrono.api.identidade.dominio.modelo.CadastroConta;
import com.eickrono.api.identidade.dominio.modelo.TipoPessoaCadastro;
import com.eickrono.api.identidade.infraestrutura.configuracao.CadastroEmailProperties;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class CanalEnvioCodigoCadastroEmailSmtpTest {

    @Mock
    private JavaMailSender javaMailSender;

    @Captor
    private ArgumentCaptor<SimpleMailMessage> mensagemCaptor;

    @Test
    @DisplayName("deve montar e enviar o e-mail de confirmacao do cadastro por SMTP")
    void deveEnviarEmailPorSmtp() {
        CadastroEmailProperties properties = new CadastroEmailProperties();
        properties.setFornecedor("smtp");
        properties.setRemetente("nao-responda@eickrono.com");
        properties.setResponderPara("suporte@eickrono.com");
        properties.setAssunto("Confirme seu cadastro");
        properties.setNomeAplicacao("Eickrono Thimisu");

        CanalEnvioCodigoCadastroEmailSmtp canal = new CanalEnvioCodigoCadastroEmailSmtp(javaMailSender, properties);
        CadastroConta cadastro = new CadastroConta(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "sub-teste",
                TipoPessoaCadastro.FISICA,
                "Ana Souza",
                null,
                "",
                null,
                null,
                null,
                "ana@eickrono.com",
                "+5511999999999",
                CanalValidacaoTelefoneCadastro.SMS,
                "hash",
                OffsetDateTime.of(2026, 3, 19, 10, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 3, 19, 19, 0, 0, 0, ZoneOffset.UTC),
                "thimisu-backend",
                "127.0.0.1",
                "JUnit",
                OffsetDateTime.of(2026, 3, 19, 10, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 3, 19, 10, 0, 0, 0, ZoneOffset.UTC),
                new ContextoSolicitacaoFluxoPublico(
                        "pt-BR",
                        "America/Sao_Paulo",
                        "app",
                        "Thimisu",
                        "ios",
                        "Eickrono",
                        "HML"
                )
        );

        canal.enviar(cadastro, "123456");

        verify(javaMailSender).send(mensagemCaptor.capture());
        SimpleMailMessage mensagem = mensagemCaptor.getValue();
        assertThat(mensagem.getTo()).containsExactly("ana@eickrono.com");
        assertThat(mensagem.getFrom()).isEqualTo("nao-responda@eickrono.com");
        assertThat(mensagem.getReplyTo()).isEqualTo("suporte@eickrono.com");
        assertThat(mensagem.getSubject()).isEqualTo("Confirme seu cadastro");
        assertThat(mensagem.getText()).contains("123456");
        assertThat(mensagem.getText()).contains("app Thimisu da Eickrono [HML]");
        assertThat(mensagem.getText()).contains("Protocolo de atendimento: CAD-");
        assertThat(mensagem.getText()).contains("Validade ate: 19/03/2026 16:00 (horario de Sao Paulo)");
        assertThat(mensagem.getText()).contains("Referencia tecnica: 19/03/2026 19:00 UTC");
        assertThat(mensagem.getText()).contains("Se voce precisar falar com o suporte");
        assertThat(mensagem.getText()).doesNotContain("Cadastro:");
        assertThat(mensagem.getText()).doesNotContain("11111111-1111-1111-1111-111111111111");
    }

    @Test
    @DisplayName("deve traduzir falha SMTP para excecao tipada de entrega de e-mail")
    void deveTraduzirFalhaSmtpParaExcecaoTipada() {
        CadastroEmailProperties properties = new CadastroEmailProperties();
        properties.setFornecedor("smtp");
        properties.setRemetente("nao-responda@eickrono.com");
        properties.setAssunto("Confirme seu cadastro");
        properties.setNomeAplicacao("Eickrono Thimisu");
        CanalEnvioCodigoCadastroEmailSmtp canal = new CanalEnvioCodigoCadastroEmailSmtp(javaMailSender, properties);
        CadastroConta cadastro = new CadastroConta(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "sub-teste",
                TipoPessoaCadastro.FISICA,
                "Ana Souza",
                null,
                "",
                null,
                null,
                null,
                "ana@eickrono.com",
                "+5511999999999",
                CanalValidacaoTelefoneCadastro.SMS,
                "hash",
                OffsetDateTime.of(2026, 3, 19, 10, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 3, 19, 19, 0, 0, 0, ZoneOffset.UTC),
                "thimisu-backend",
                "127.0.0.1",
                "JUnit",
                OffsetDateTime.of(2026, 3, 19, 10, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 3, 19, 10, 0, 0, 0, ZoneOffset.UTC)
        );
        doThrow(new MailAuthenticationException("Authentication failed"))
                .when(javaMailSender)
                .send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> canal.enviar(cadastro, "123456"))
                .isInstanceOf(EntregaEmailException.class)
                .hasMessage("Falha ao enviar o codigo de confirmacao do cadastro por SMTP.")
                .extracting("codigo", "mensagemPublica")
                .containsExactly(
                        "cadastro_email_indisponivel",
                        "Não foi possível enviar o código de confirmação por e-mail agora. Tente novamente."
                );
    }
}
