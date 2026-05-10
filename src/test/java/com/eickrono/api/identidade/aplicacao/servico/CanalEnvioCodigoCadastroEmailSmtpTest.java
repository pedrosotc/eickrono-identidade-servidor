package com.eickrono.api.identidade.aplicacao.servico;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eickrono.api.identidade.aplicacao.excecao.EntregaEmailException;
import com.eickrono.api.identidade.aplicacao.modelo.ContextoSolicitacaoFluxoPublico;
import com.eickrono.api.identidade.dominio.modelo.CanalValidacaoTelefoneCadastro;
import com.eickrono.api.identidade.dominio.modelo.CadastroConta;
import com.eickrono.api.identidade.dominio.modelo.TipoPessoaCadastro;
import com.eickrono.api.identidade.infraestrutura.configuracao.CadastroEmailProperties;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Properties;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class CanalEnvioCodigoCadastroEmailSmtpTest {

    @Mock
    private JavaMailSender javaMailSender;

    @Captor
    private ArgumentCaptor<MimeMessage> mensagemCaptor;

    @Test
    @DisplayName("deve montar e enviar o e-mail de confirmacao do cadastro por SMTP")
    void deveEnviarEmailPorSmtp() throws Exception {
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
        when(javaMailSender.createMimeMessage()).thenReturn(novaMimeMessage());

        canal.enviar(cadastro, "123456");

        verify(javaMailSender).send(mensagemCaptor.capture());
        MimeMessage mensagem = mensagemCaptor.getValue();
        assertThat(mensagem.getAllRecipients())
                .extracting(Object::toString)
                .containsExactly("ana@eickrono.com");
        assertThat(((InternetAddress) mensagem.getFrom()[0]).getAddress()).isEqualTo("nao-responda@eickrono.com");
        assertThat(((InternetAddress) mensagem.getReplyTo()[0]).getAddress()).isEqualTo("suporte@eickrono.com");
        assertThat(mensagem.getSubject()).isEqualTo("Confirme seu cadastro");
        assertThat(mensagem.getHeader("X-Eickrono-Tipo-Email", null)).isEqualTo("cadastro_confirmacao");
        assertThat(mensagem.getHeader("X-Eickrono-Referencia-Id", null))
                .isEqualTo("11111111-1111-1111-1111-111111111111");
        assertThat(mensagem.getHeader("X-Eickrono-Protocolo-Suporte", null)).startsWith("CAD-");
        String corpo = mensagem.getContent().toString();
        assertThat(corpo).contains("123456");
        assertThat(corpo).contains("app Thimisu da Eickrono [HML]");
        assertThat(corpo).contains("Protocolo de atendimento: CAD-");
        assertThat(corpo).contains("Validade ate: 19/03/2026 16:00 (horario de Sao Paulo)");
        assertThat(corpo).contains("Referencia tecnica: 19/03/2026 19:00 UTC");
        assertThat(corpo).contains("Se voce precisar falar com o suporte");
        assertThat(corpo).doesNotContain("Cadastro:");
        assertThat(corpo).doesNotContain("11111111-1111-1111-1111-111111111111");
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
        when(javaMailSender.createMimeMessage()).thenReturn(novaMimeMessage());
        doThrow(new MailAuthenticationException("Authentication failed"))
                .when(javaMailSender)
                .send(any(MimeMessage.class));

        assertThatThrownBy(() -> canal.enviar(cadastro, "123456"))
                .isInstanceOf(EntregaEmailException.class)
                .hasMessage("Falha ao enviar o codigo de confirmacao do cadastro por SMTP.")
                .extracting("codigo", "mensagemPublica")
                .containsExactly(
                        "cadastro_email_indisponivel",
                        "Não foi possível enviar o código de confirmação por e-mail agora. Tente novamente."
                );
    }

    private static MimeMessage novaMimeMessage() {
        return new MimeMessage(Session.getInstance(new Properties()));
    }
}
