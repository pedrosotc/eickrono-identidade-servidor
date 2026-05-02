package com.eickrono.api.identidade.aplicacao.servico;

import com.eickrono.api.identidade.aplicacao.excecao.EntregaEmailException;
import com.eickrono.api.identidade.dominio.modelo.CadastroConta;
import com.eickrono.api.identidade.infraestrutura.configuracao.CadastroEmailProperties;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

public class CanalEnvioCodigoCadastroEmailSmtp implements CanalEnvioCodigoCadastroEmail {

    private static final Logger LOGGER = LoggerFactory.getLogger(CanalEnvioCodigoCadastroEmailSmtp.class);
    private final JavaMailSender javaMailSender;
    private final CadastroEmailProperties cadastroEmailProperties;

    public CanalEnvioCodigoCadastroEmailSmtp(final JavaMailSender javaMailSender,
                                             final CadastroEmailProperties cadastroEmailProperties) {
        this.javaMailSender = Objects.requireNonNull(javaMailSender, "javaMailSender e obrigatorio");
        this.cadastroEmailProperties = Objects.requireNonNull(
                cadastroEmailProperties, "cadastroEmailProperties e obrigatorio");
    }

    @Override
    public void enviar(final CadastroConta cadastroConta, final String codigo) {
        CadastroConta cadastro = Objects.requireNonNull(cadastroConta, "cadastroConta e obrigatorio");
        String codigoConfirmacao = Objects.requireNonNull(codigo, "codigo e obrigatorio").trim();
        if (codigoConfirmacao.isBlank()) {
            throw new IllegalArgumentException("codigo e obrigatorio");
        }

        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setTo(cadastro.getEmailPrincipal());
        mensagem.setFrom(cadastroEmailProperties.getRemetente());
        if (cadastroEmailProperties.getResponderPara() != null
                && !cadastroEmailProperties.getResponderPara().isBlank()) {
            mensagem.setReplyTo(cadastroEmailProperties.getResponderPara());
        }
        mensagem.setSubject(cadastroEmailProperties.getAssunto());
        mensagem.setText(criarCorpoMensagem(cadastro, codigoConfirmacao));

        try {
            javaMailSender.send(mensagem);
            LOGGER.info(
                    "Codigo de confirmacao de cadastro enviado por SMTP para {} (cadastroId={}, protocoloSuporte={}, sistema={})",
                    cadastro.getEmailPrincipal(),
                    cadastro.getCadastroId(),
                    cadastro.getProtocoloSuporte(),
                    cadastro.getSistemaSolicitante()
            );
        } catch (MailException ex) {
            LOGGER.error(
                    "cadastro_email_smtp_falhou cadastroId={} protocoloSuporte={} sistema={} motivo={}",
                    cadastro.getCadastroId(),
                    cadastro.getProtocoloSuporte(),
                    cadastro.getSistemaSolicitante(),
                    ex.getMessage(),
                    ex
            );
            throw new EntregaEmailException(
                    "cadastro_email_indisponivel",
                    "Não foi possível enviar o código de confirmação por e-mail agora. Tente novamente.",
                    "Falha ao enviar o codigo de confirmacao do cadastro por SMTP.",
                    ex
            );
        }
    }

    private String criarCorpoMensagem(final CadastroConta cadastroConta, final String codigo) {
        return """
                Ola,%n%n\
                Voce solicitou a confirmacao de cadastro da sua conta no %s.%n%n\
                Codigo de confirmacao: %s%n\
                Protocolo de atendimento: %s%n\
                Validade ate: %s%n%n\
                Referencia tecnica: %s%n%n\
                Se voce precisar falar com o suporte, informe o protocolo acima.%n\
                Se voce nao reconhece esta solicitacao, ignore esta mensagem.%n\
                """
                .formatted(
                        FormatadorContextoEmailFluxoPublico.descreverOrigem(
                                cadastroEmailProperties,
                                new com.eickrono.api.identidade.aplicacao.modelo.ContextoSolicitacaoFluxoPublico(
                                        cadastroConta.getLocaleSolicitante(),
                                        cadastroConta.getTimeZoneSolicitante(),
                                        cadastroConta.getTipoProdutoExibicao(),
                                        cadastroConta.getProdutoExibicao(),
                                        cadastroConta.getCanalExibicao(),
                                        cadastroConta.getEmpresaExibicao(),
                                        cadastroConta.getAmbienteExibicao()
                                )),
                        codigo,
                        cadastroConta.getProtocoloSuporte(),
                        FormatadorContextoEmailFluxoPublico.formatarValidadeLocal(
                                cadastroConta.getCodigoEmailExpiraEm(),
                                new com.eickrono.api.identidade.aplicacao.modelo.ContextoSolicitacaoFluxoPublico(
                                        cadastroConta.getLocaleSolicitante(),
                                        cadastroConta.getTimeZoneSolicitante(),
                                        cadastroConta.getTipoProdutoExibicao(),
                                        cadastroConta.getProdutoExibicao(),
                                        cadastroConta.getCanalExibicao(),
                                        cadastroConta.getEmpresaExibicao(),
                                        cadastroConta.getAmbienteExibicao()
                                )),
                        FormatadorContextoEmailFluxoPublico.formatarReferenciaUtc(
                                cadastroConta.getCodigoEmailExpiraEm())
                );
    }
}
