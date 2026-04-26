package com.eickrono.api.identidade.dominio.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Objects;

@Entity
@Table(name = "convites_organizacionais")
public class ConviteOrganizacional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 128)
    private String codigo;

    @Column(name = "organizacao_id", nullable = false, length = 128)
    private String organizacaoId;

    @Column(name = "nome_organizacao", nullable = false, length = 255)
    private String nomeOrganizacao;

    @Column(name = "email_convidado", length = 255)
    private String emailConvidado;

    @Column(name = "nome_convidado", length = 255)
    private String nomeConvidado;

    @Column(name = "exige_conta_separada", nullable = false)
    private boolean exigeContaSeparada;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private StatusConviteOrganizacional status;

    @Column(name = "expira_em", nullable = false)
    private OffsetDateTime expiraEm;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    protected ConviteOrganizacional() {
    }

    public ConviteOrganizacional(final String codigo,
                                 final String organizacaoId,
                                 final String nomeOrganizacao,
                                 final String emailConvidado,
                                 final String nomeConvidado,
                                 final boolean exigeContaSeparada,
                                 final OffsetDateTime expiraEm) {
        this.codigo = normalizarCodigo(codigo);
        this.organizacaoId = normalizarObrigatorio(organizacaoId, "organizacaoId");
        this.nomeOrganizacao = normalizarObrigatorio(nomeOrganizacao, "nomeOrganizacao");
        this.emailConvidado = normalizarOpcional(emailConvidado);
        this.nomeConvidado = normalizarOpcional(nomeConvidado);
        this.exigeContaSeparada = exigeContaSeparada;
        this.status = StatusConviteOrganizacional.ATIVO;
        this.expiraEm = Objects.requireNonNull(expiraEm, "expiraEm e obrigatorio");
        this.criadoEm = OffsetDateTime.now();
        this.atualizadoEm = this.criadoEm;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getOrganizacaoId() {
        return organizacaoId;
    }

    public String getNomeOrganizacao() {
        return nomeOrganizacao;
    }

    public String getEmailConvidado() {
        return emailConvidado;
    }

    public String getNomeConvidado() {
        return nomeConvidado;
    }

    public boolean isExigeContaSeparada() {
        return exigeContaSeparada;
    }

    public StatusConviteOrganizacional getStatus() {
        return status;
    }

    public OffsetDateTime getExpiraEm() {
        return expiraEm;
    }

    public boolean estaExpirado(final OffsetDateTime referencia) {
        return expiraEm.isBefore(Objects.requireNonNull(referencia, "referencia e obrigatoria"));
    }

    public boolean estaDisponivel(final OffsetDateTime referencia) {
        return status == StatusConviteOrganizacional.ATIVO && !estaExpirado(referencia);
    }

    public void atualizarStatus(final StatusConviteOrganizacional novoStatus) {
        this.status = Objects.requireNonNull(novoStatus, "novoStatus e obrigatorio");
        this.atualizadoEm = OffsetDateTime.now();
    }

    public void marcarConsumido() {
        atualizarStatus(StatusConviteOrganizacional.CONSUMIDO);
    }

    private static String normalizarCodigo(final String codigo) {
        String valor = normalizarObrigatorio(codigo, "codigo");
        return valor.toUpperCase(Locale.ROOT);
    }

    private static String normalizarObrigatorio(final String valor, final String campo) {
        String texto = Objects.requireNonNull(valor, campo + " e obrigatorio").trim();
        if (texto.isEmpty()) {
            throw new IllegalArgumentException(campo + " e obrigatorio");
        }
        return texto;
    }

    private static String normalizarOpcional(final String valor) {
        if (valor == null) {
            return null;
        }
        String texto = valor.trim();
        return texto.isEmpty() ? null : texto;
    }
}
