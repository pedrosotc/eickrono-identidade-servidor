package com.eickrono.api.identidade.aplicacao.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import com.eickrono.api.identidade.infraestrutura.configuracao.CadastroEmailProperties;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class CanalNotificacaoTentativaCadastroEmailSmtpTest {

    @Mock
    private JavaMailSender javaMailSender;

    @Captor
    private ArgumentCaptor<MimeMessage> mensagemCaptor;

    @Test
    @DisplayName("deve montar e enviar o aviso de tentativa de cadastro para e-mail já vinculado")
    void deveEnviarAvisoPorSmtp() throws Exception {
        CadastroEmailProperties properties = new CadastroEmailProperties();
        properties.setFornecedor("smtp");
        properties.setRemetente("nao-responda@eickrono.com");
        properties.setResponderPara("suporte@eickrono.com");
        properties.setAssuntoTentativaCadastroEmailExistente("Tentativa de cadastro detectada");
        properties.setNomeAplicacao("Eickrono Thimisu");

        CanalNotificacaoTentativaCadastroEmailSmtp canal =
                new CanalNotificacaoTentativaCadastroEmailSmtp(javaMailSender, properties);
        when(javaMailSender.createMimeMessage()).thenReturn(novaMimeMessage());

        canal.notificar("ana@eickrono.com");

        verify(javaMailSender).send(mensagemCaptor.capture());
        MimeMessage mensagem = mensagemCaptor.getValue();
        assertThat(mensagem.getAllRecipients())
                .extracting(Object::toString)
                .containsExactly("ana@eickrono.com");
        assertThat(((InternetAddress) mensagem.getFrom()[0]).getAddress()).isEqualTo("nao-responda@eickrono.com");
        assertThat(((InternetAddress) mensagem.getReplyTo()[0]).getAddress()).isEqualTo("suporte@eickrono.com");
        assertThat(mensagem.getSubject()).isEqualTo("Tentativa de cadastro detectada");
        assertThat(mensagem.getHeader("X-Eickrono-Tipo-Email", null))
                .isEqualTo("notificacao_tentativa_cadastro");
        String corpo = mensagem.getContent().toString();
        assertThat(corpo).contains("Eickrono Thimisu");
        assertThat(corpo).contains("recuperação de senha");
    }

    private static MimeMessage novaMimeMessage() {
        return new MimeMessage(Session.getInstance(new Properties()));
    }
}
