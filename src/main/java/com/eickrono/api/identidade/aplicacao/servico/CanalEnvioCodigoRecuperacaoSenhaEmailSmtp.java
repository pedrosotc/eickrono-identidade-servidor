package com.eickrono.api.identidade.aplicacao.servico;

import com.eickrono.api.identidade.aplicacao.excecao.EntregaEmailException;
import com.eickrono.api.identidade.dominio.modelo.RecuperacaoSenha;
import com.eickrono.api.identidade.infraestrutura.configuracao.CadastroEmailProperties;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;

public class CanalEnvioCodigoRecuperacaoSenhaEmailSmtp implements CanalEnvioCodigoRecuperacaoSenhaEmail {

    private static final Logger LOGGER = LoggerFactory.getLogger(CanalEnvioCodigoRecuperacaoSenhaEmailSmtp.class);
    private final JavaMailSender javaMailSender;
    private final CadastroEmailProperties cadastroEmailProperties;

    public CanalEnvioCodigoRecuperacaoSenhaEmailSmtp(final JavaMailSender javaMailSender,
                                                     final CadastroEmailProperties cadastroEmailProperties) {
        this.javaMailSender = Objects.requireNonNull(javaMailSender, "javaMailSender e obrigatorio");
        this.cadastroEmailProperties = Objects.requireNonNull(
                cadastroEmailProperties, "cadastroEmailProperties e obrigatorio");
    }

    @Override
    public void enviar(final RecuperacaoSenha recuperacaoSenha, final String codigo) {
        RecuperacaoSenha recuperacao = Objects.requireNonNull(recuperacaoSenha, "recuperacaoSenha e obrigatoria");
        String codigoConfirmacao = Objects.requireNonNull(codigo, "codigo e obrigatorio").trim();
        if (codigoConfirmacao.isBlank()) {
            throw new IllegalArgumentException("codigo e obrigatorio");
        }

        try {
            new EnvioEmailSmtpRastreavel(javaMailSender, cadastroEmailProperties, LOGGER).enviar(
                    new EnvioEmailSmtpRastreavel.MensagemEmailRastreavel(
                            "recuperacao_senha",
                            "recuperacao_email_smtp_tentativa",
                            "recuperacao_email_smtp_enviado",
                            "recuperacao_email_smtp_falhou",
                            recuperacao.getEmailPrincipal(),
                            cadastroEmailProperties.getAssuntoRecuperacaoSenha(),
                            criarCorpoMensagem(recuperacao, codigoConfirmacao),
                            recuperacao.getFluxoId().toString(),
                            recuperacao.getProtocoloSuporte()
                    )
            );
        } catch (MailException ex) {
            throw new EntregaEmailException(
                    "recuperacao_email_indisponivel",
                    "Não foi possível enviar o código de recuperação por e-mail agora. Tente novamente.",
                    "Falha ao enviar o codigo de recuperacao de senha por SMTP.",
                    ex
            );
        }
    }

    private String criarCorpoMensagem(final RecuperacaoSenha recuperacaoSenha, final String codigo) {
        return """
                Ola,%n%n\
                Voce solicitou a recuperacao de senha da sua conta no %s.%n%n\
                Codigo de recuperacao: %s%n\
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
                                        recuperacaoSenha.getLocaleSolicitante(),
                                        recuperacaoSenha.getTimeZoneSolicitante(),
                                        recuperacaoSenha.getTipoProdutoExibicao(),
                                        recuperacaoSenha.getProdutoExibicao(),
                                        recuperacaoSenha.getCanalExibicao(),
                                        recuperacaoSenha.getEmpresaExibicao(),
                                        recuperacaoSenha.getAmbienteExibicao()
                                )),
                        codigo,
                        recuperacaoSenha.getProtocoloSuporte(),
                        FormatadorContextoEmailFluxoPublico.formatarValidadeLocal(
                                recuperacaoSenha.getCodigoEmailExpiraEm(),
                                new com.eickrono.api.identidade.aplicacao.modelo.ContextoSolicitacaoFluxoPublico(
                                        recuperacaoSenha.getLocaleSolicitante(),
                                        recuperacaoSenha.getTimeZoneSolicitante(),
                                        recuperacaoSenha.getTipoProdutoExibicao(),
                                        recuperacaoSenha.getProdutoExibicao(),
                                        recuperacaoSenha.getCanalExibicao(),
                                        recuperacaoSenha.getEmpresaExibicao(),
                                        recuperacaoSenha.getAmbienteExibicao()
                                )),
                        FormatadorContextoEmailFluxoPublico.formatarReferenciaUtc(
                                recuperacaoSenha.getCodigoEmailExpiraEm())
                );
    }
}
