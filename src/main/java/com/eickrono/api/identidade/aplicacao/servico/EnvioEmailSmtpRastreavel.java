package com.eickrono.api.identidade.aplicacao.servico;

import com.eickrono.api.identidade.infraestrutura.configuracao.CadastroEmailProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.slf4j.Logger;
import org.springframework.mail.MailException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

final class EnvioEmailSmtpRastreavel {

    private final JavaMailSender javaMailSender;
    private final CadastroEmailProperties cadastroEmailProperties;
    private final Logger logger;

    EnvioEmailSmtpRastreavel(final JavaMailSender javaMailSender,
                             final CadastroEmailProperties cadastroEmailProperties,
                             final Logger logger) {
        this.javaMailSender = Objects.requireNonNull(javaMailSender, "javaMailSender e obrigatorio");
        this.cadastroEmailProperties = Objects.requireNonNull(
                cadastroEmailProperties, "cadastroEmailProperties e obrigatorio");
        this.logger = Objects.requireNonNull(logger, "logger e obrigatorio");
    }

    void enviar(final MensagemEmailRastreavel mensagemEmailRastreavel) {
        MensagemEmailRastreavel mensagem = Objects.requireNonNull(
                mensagemEmailRastreavel, "mensagemEmailRastreavel e obrigatoria");
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    false,
                    StandardCharsets.UTF_8.name()
            );
            helper.setTo(mensagem.destinatario());
            helper.setFrom(cadastroEmailProperties.getRemetente());
            if (cadastroEmailProperties.getResponderPara() != null
                    && !cadastroEmailProperties.getResponderPara().isBlank()) {
                helper.setReplyTo(cadastroEmailProperties.getResponderPara());
            }
            helper.setSubject(mensagem.assunto());
            helper.setText(mensagem.corpo(), false);

            mimeMessage.setHeader("X-Eickrono-Tipo-Email", mensagem.tipoEmail());
            if (mensagem.protocoloSuporte() != null && !mensagem.protocoloSuporte().isBlank()) {
                mimeMessage.setHeader("X-Eickrono-Protocolo-Suporte", mensagem.protocoloSuporte());
            }
            if (mensagem.referenciaId() != null && !mensagem.referenciaId().isBlank()) {
                mimeMessage.setHeader("X-Eickrono-Referencia-Id", mensagem.referenciaId());
            }
            mimeMessage.saveChanges();

            logger.info(
                    "{} tipoEmail={} referenciaId={} protocoloSuporte={} destinatario={} remetente={} "
                            + "replyTo={} host={} porta={} username={} messageId={}",
                    mensagem.eventoTentativa(),
                    mensagem.tipoEmail(),
                    valorOuTraco(mensagem.referenciaId()),
                    valorOuTraco(mensagem.protocoloSuporte()),
                    mensagem.destinatario(),
                    cadastroEmailProperties.getRemetente(),
                    valorOuTraco(cadastroEmailProperties.getResponderPara()),
                    hostSmtp(),
                    portaSmtp(),
                    usernameMascarado(),
                    messageIdOuTraco(mimeMessage)
            );

            javaMailSender.send(mimeMessage);
            mimeMessage.saveChanges();

            logger.info(
                    "{} tipoEmail={} referenciaId={} protocoloSuporte={} destinatario={} remetente={} "
                            + "host={} porta={} username={} messageId={}",
                    mensagem.eventoSucesso(),
                    mensagem.tipoEmail(),
                    valorOuTraco(mensagem.referenciaId()),
                    valorOuTraco(mensagem.protocoloSuporte()),
                    mensagem.destinatario(),
                    cadastroEmailProperties.getRemetente(),
                    hostSmtp(),
                    portaSmtp(),
                    usernameMascarado(),
                    messageIdOuTraco(mimeMessage)
            );
        } catch (MailException ex) {
            logger.error(
                    "{} tipoEmail={} referenciaId={} protocoloSuporte={} destinatario={} remetente={} "
                            + "host={} porta={} username={} motivo={}",
                    mensagem.eventoFalha(),
                    mensagem.tipoEmail(),
                    valorOuTraco(mensagem.referenciaId()),
                    valorOuTraco(mensagem.protocoloSuporte()),
                    mensagem.destinatario(),
                    cadastroEmailProperties.getRemetente(),
                    hostSmtp(),
                    portaSmtp(),
                    usernameMascarado(),
                    ex.getMessage(),
                    ex
            );
            throw ex;
        } catch (MessagingException ex) {
            throw new MailPreparationException("Falha ao preparar e-mail SMTP rastreavel.", ex);
        }
    }

    private String hostSmtp() {
        if (javaMailSender instanceof JavaMailSenderImpl impl && impl.getHost() != null) {
            return impl.getHost();
        }
        return "desconhecido";
    }

    private String portaSmtp() {
        if (javaMailSender instanceof JavaMailSenderImpl impl && impl.getPort() > 0) {
            return Integer.toString(impl.getPort());
        }
        return "-";
    }

    private String usernameMascarado() {
        if (javaMailSender instanceof JavaMailSenderImpl impl) {
            return mascararEmail(impl.getUsername());
        }
        return "-";
    }

    private static String valuePrefix(final String valor, final int limite) {
        return valor.substring(0, Math.min(valor.length(), limite));
    }

    private static String mascararEmail(final String email) {
        if (email == null || email.isBlank()) {
            return "-";
        }
        int separador = email.indexOf('@');
        if (separador <= 0 || separador == email.length() - 1) {
            return valuePrefix(email, 3) + "***";
        }
        String local = email.substring(0, separador);
        String dominio = email.substring(separador + 1);
        return valuePrefix(local, 3) + "***@" + dominio;
    }

    private static String valorOuTraco(final String valor) {
        if (valor == null || valor.isBlank()) {
            return "-";
        }
        return valor;
    }

    private static String messageIdOuTraco(final MimeMessage mimeMessage) {
        try {
            String messageId = mimeMessage.getMessageID();
            if (messageId == null || messageId.isBlank()) {
                return "-";
            }
            return messageId;
        } catch (MessagingException ex) {
            return "-";
        }
    }

    record MensagemEmailRastreavel(
            String tipoEmail,
            String eventoTentativa,
            String eventoSucesso,
            String eventoFalha,
            String destinatario,
            String assunto,
            String corpo,
            String referenciaId,
            String protocoloSuporte) {

        MensagemEmailRastreavel {
            Objects.requireNonNull(tipoEmail, "tipoEmail e obrigatorio");
            Objects.requireNonNull(eventoTentativa, "eventoTentativa e obrigatorio");
            Objects.requireNonNull(eventoSucesso, "eventoSucesso e obrigatorio");
            Objects.requireNonNull(eventoFalha, "eventoFalha e obrigatorio");
            Objects.requireNonNull(destinatario, "destinatario e obrigatorio");
            Objects.requireNonNull(assunto, "assunto e obrigatorio");
            Objects.requireNonNull(corpo, "corpo e obrigatorio");
        }
    }
}
