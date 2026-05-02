package com.eickrono.api.identidade.dominio.modelo;

import com.eickrono.api.identidade.aplicacao.modelo.ContextoSolicitacaoFluxoPublico;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "cadastros_conta")
public class CadastroConta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cadastro_id", nullable = false, unique = true)
    private UUID cadastroId;

    @Column(name = "protocolo_suporte", nullable = false, unique = true, length = 40)
    private String protocoloSuporte;

    @Column(name = "subject_remoto", nullable = false, unique = true)
    private String subjectRemoto;

    @Column(name = "pessoa_id_perfil")
    private Long pessoaIdPerfil;

    @Column(name = "usuario_id_perfil", length = 36)
    private String usuarioIdPerfil;

    @Column(name = "nome_completo", nullable = false)
    private String nomeCompleto;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_pessoa", nullable = false, length = 16)
    private TipoPessoaCadastro tipoPessoa;

    @Column(name = "nome_fantasia")
    private String nomeFantasia;

    @Column(name = "usuario", nullable = false, length = 255)
    private String usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "sexo", length = 16)
    private SexoPessoaCadastro sexo;

    @Column(name = "pais_nascimento", length = 8)
    private String paisNascimento;

    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;

    @Column(name = "email_principal", nullable = false, unique = true)
    private String emailPrincipal;

    @Column(name = "telefone_principal", length = 32)
    private String telefonePrincipal;

    @Enumerated(EnumType.STRING)
    @Column(name = "canal_validacao_telefone", length = 16)
    private CanalValidacaoTelefoneCadastro canalValidacaoTelefone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private StatusCadastroConta status;

    @Column(name = "codigo_email_hash", nullable = false, length = 64)
    private String codigoEmailHash;

    @Column(name = "codigo_email_gerado_em", nullable = false)
    private OffsetDateTime codigoEmailGeradoEm;

    @Column(name = "codigo_email_expira_em", nullable = false)
    private OffsetDateTime codigoEmailExpiraEm;

    @Column(name = "tentativas_confirmacao_email", nullable = false)
    private int tentativasConfirmacaoEmail;

    @Column(name = "reenvios_email", nullable = false)
    private int reenviosEmail;

    @Column(name = "email_confirmado_em")
    private OffsetDateTime emailConfirmadoEm;

    @Column(name = "codigo_telefone_hash", length = 64)
    private String codigoTelefoneHash;

    @Column(name = "codigo_telefone_gerado_em")
    private OffsetDateTime codigoTelefoneGeradoEm;

    @Column(name = "codigo_telefone_expira_em")
    private OffsetDateTime codigoTelefoneExpiraEm;

    @Column(name = "telefone_confirmado_em")
    private OffsetDateTime telefoneConfirmadoEm;

    @Column(name = "sistema_solicitante", nullable = false, length = 64)
    private String sistemaSolicitante;

    @Column(name = "ip_solicitante", length = 64)
    private String ipSolicitante;

    @Column(name = "user_agent_solicitante", length = 512)
    private String userAgentSolicitante;

    @Column(name = "cliente_ecossistema_id")
    private Long clienteEcossistemaId;

    @Column(name = "locale_solicitante", length = 16)
    private String localeSolicitante;

    @Column(name = "time_zone_solicitante", length = 64)
    private String timeZoneSolicitante;

    @Column(name = "tipo_produto_exibicao", length = 32)
    private String tipoProdutoExibicao;

    @Column(name = "produto_exibicao", length = 128)
    private String produtoExibicao;

    @Column(name = "canal_exibicao", length = 64)
    private String canalExibicao;

    @Column(name = "empresa_exibicao", length = 128)
    private String empresaExibicao;

    @Column(name = "ambiente_exibicao", length = 32)
    private String ambienteExibicao;

    @Column(name = "exige_validacao_telefone_snapshot", nullable = false)
    private boolean exigeValidacaoTelefoneSnapshot;

    @Column(name = "vinculo_social_pendente_provedor", length = 32)
    private String vinculoSocialPendenteProvedor;

    @Column(name = "vinculo_social_pendente_identificador_externo", length = 255)
    private String vinculoSocialPendenteIdentificadorExterno;

    @Column(name = "vinculo_social_pendente_nome_usuario_externo", length = 255)
    private String vinculoSocialPendenteNomeUsuarioExterno;

    @Column(name = "convite_organizacional_codigo", length = 128)
    private String conviteOrganizacionalCodigo;

    @Column(name = "convite_organizacional_organizacao_id", length = 128)
    private String conviteOrganizacionalOrganizacaoId;

    @Column(name = "convite_organizacional_nome_organizacao", length = 255)
    private String conviteOrganizacionalNomeOrganizacao;

    @Column(name = "convite_organizacional_email_convidado", length = 255)
    private String conviteOrganizacionalEmailConvidado;

    @Column(name = "convite_organizacional_exige_conta_separada")
    private Boolean conviteOrganizacionalExigeContaSeparada;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    protected CadastroConta() {
        // construtor do JPA
    }

    public CadastroConta(final UUID cadastroId,
                         final String subjectRemoto,
                         final TipoPessoaCadastro tipoPessoa,
                         final String nomeCompleto,
                         final String nomeFantasia,
                         final String usuario,
                         final SexoPessoaCadastro sexo,
                         final String paisNascimento,
                         final LocalDate dataNascimento,
                         final String emailPrincipal,
                         final String telefonePrincipal,
                         final CanalValidacaoTelefoneCadastro canalValidacaoTelefone,
                         final String codigoEmailHash,
                         final OffsetDateTime codigoEmailGeradoEm,
                         final OffsetDateTime codigoEmailExpiraEm,
                         final String sistemaSolicitante,
                         final String ipSolicitante,
                         final String userAgentSolicitante,
                         final OffsetDateTime criadoEm,
                         final OffsetDateTime atualizadoEm) {
        this(
                cadastroId,
                subjectRemoto,
                tipoPessoa,
                nomeCompleto,
                nomeFantasia,
                usuario,
                sexo,
                paisNascimento,
                dataNascimento,
                emailPrincipal,
                telefonePrincipal,
                canalValidacaoTelefone,
                codigoEmailHash,
                codigoEmailGeradoEm,
                codigoEmailExpiraEm,
                sistemaSolicitante,
                ipSolicitante,
                userAgentSolicitante,
                criadoEm,
                atualizadoEm,
                null
        );
    }

    public CadastroConta(final UUID cadastroId,
                         final String subjectRemoto,
                         final TipoPessoaCadastro tipoPessoa,
                         final String nomeCompleto,
                         final String nomeFantasia,
                         final String usuario,
                         final SexoPessoaCadastro sexo,
                         final String paisNascimento,
                         final LocalDate dataNascimento,
                         final String emailPrincipal,
                         final String telefonePrincipal,
                         final CanalValidacaoTelefoneCadastro canalValidacaoTelefone,
                         final String codigoEmailHash,
                         final OffsetDateTime codigoEmailGeradoEm,
                         final OffsetDateTime codigoEmailExpiraEm,
                         final String sistemaSolicitante,
                         final String ipSolicitante,
                         final String userAgentSolicitante,
                         final OffsetDateTime criadoEm,
                         final OffsetDateTime atualizadoEm,
                         final ContextoSolicitacaoFluxoPublico contextoSolicitacao) {
        this.cadastroId = Objects.requireNonNull(cadastroId, "cadastroId é obrigatório");
        this.protocoloSuporte = ProtocoloSuporte.gerarCadastro();
        this.subjectRemoto = Objects.requireNonNull(subjectRemoto, "subjectRemoto é obrigatório");
        this.tipoPessoa = Objects.requireNonNull(tipoPessoa, "tipoPessoa é obrigatório");
        this.nomeCompleto = Objects.requireNonNull(nomeCompleto, "nomeCompleto é obrigatório");
        this.nomeFantasia = nomeFantasia;
        this.usuario = Objects.requireNonNull(usuario, "usuario é obrigatório");
        this.sexo = sexo;
        this.paisNascimento = paisNascimento;
        this.dataNascimento = dataNascimento;
        this.emailPrincipal = Objects.requireNonNull(emailPrincipal, "emailPrincipal é obrigatório");
        this.telefonePrincipal = telefonePrincipal;
        this.canalValidacaoTelefone = canalValidacaoTelefone;
        this.status = StatusCadastroConta.PENDENTE_EMAIL;
        this.codigoEmailHash = Objects.requireNonNull(codigoEmailHash, "codigoEmailHash é obrigatório");
        this.codigoEmailGeradoEm = Objects.requireNonNull(codigoEmailGeradoEm, "codigoEmailGeradoEm é obrigatório");
        this.codigoEmailExpiraEm = Objects.requireNonNull(codigoEmailExpiraEm, "codigoEmailExpiraEm é obrigatório");
        this.tentativasConfirmacaoEmail = 0;
        this.reenviosEmail = 0;
        this.sistemaSolicitante = Objects.requireNonNull(sistemaSolicitante, "sistemaSolicitante é obrigatório");
        this.ipSolicitante = ipSolicitante;
        this.userAgentSolicitante = userAgentSolicitante;
        aplicarContextoSolicitacao(contextoSolicitacao);
        this.criadoEm = Objects.requireNonNull(criadoEm, "criadoEm é obrigatório");
        this.atualizadoEm = Objects.requireNonNull(atualizadoEm, "atualizadoEm é obrigatório");
    }

    public Long getId() {
        return id;
    }

    public UUID getCadastroId() {
        return cadastroId;
    }

    public String getProtocoloSuporte() {
        return protocoloSuporte;
    }

    public String getSubjectRemoto() {
        return subjectRemoto;
    }

    public String getNomeCompleto() {
        return nomeCompleto;
    }

    public Long getPessoaIdPerfil() {
        return pessoaIdPerfil;
    }

    public String getUsuarioIdPerfil() {
        return usuarioIdPerfil;
    }

    public String getEmailPrincipal() {
        return emailPrincipal;
    }

    public TipoPessoaCadastro getTipoPessoa() {
        return tipoPessoa;
    }

    public String getNomeFantasia() {
        return nomeFantasia;
    }

    public String getUsuario() {
        return usuario;
    }

    public SexoPessoaCadastro getSexo() {
        return sexo;
    }

    public String getPaisNascimento() {
        return paisNascimento;
    }

    public LocalDate getDataNascimento() {
        return dataNascimento;
    }

    public String getTelefonePrincipal() {
        return telefonePrincipal;
    }

    public CanalValidacaoTelefoneCadastro getCanalValidacaoTelefone() {
        return canalValidacaoTelefone;
    }

    public StatusCadastroConta getStatus() {
        return status;
    }

    public String getCodigoEmailHash() {
        return codigoEmailHash;
    }

    public OffsetDateTime getCodigoEmailGeradoEm() {
        return codigoEmailGeradoEm;
    }

    public OffsetDateTime getCodigoEmailExpiraEm() {
        return codigoEmailExpiraEm;
    }

    public int getTentativasConfirmacaoEmail() {
        return tentativasConfirmacaoEmail;
    }

    public int getReenviosEmail() {
        return reenviosEmail;
    }

    public OffsetDateTime getEmailConfirmadoEm() {
        return emailConfirmadoEm;
    }

    public String getCodigoTelefoneHash() {
        return codigoTelefoneHash;
    }

    public OffsetDateTime getCodigoTelefoneGeradoEm() {
        return codigoTelefoneGeradoEm;
    }

    public OffsetDateTime getCodigoTelefoneExpiraEm() {
        return codigoTelefoneExpiraEm;
    }

    public OffsetDateTime getTelefoneConfirmadoEm() {
        return telefoneConfirmadoEm;
    }

    public String getSistemaSolicitante() {
        return sistemaSolicitante;
    }

    public String getIpSolicitante() {
        return ipSolicitante;
    }

    public String getUserAgentSolicitante() {
        return userAgentSolicitante;
    }

    public Long getClienteEcossistemaId() {
        return clienteEcossistemaId;
    }

    public String getLocaleSolicitante() {
        return localeSolicitante;
    }

    public String getTimeZoneSolicitante() {
        return timeZoneSolicitante;
    }

    public String getTipoProdutoExibicao() {
        return tipoProdutoExibicao;
    }

    public String getProdutoExibicao() {
        return produtoExibicao;
    }

    public String getCanalExibicao() {
        return canalExibicao;
    }

    public String getEmpresaExibicao() {
        return empresaExibicao;
    }

    public String getAmbienteExibicao() {
        return ambienteExibicao;
    }

    public boolean getExigeValidacaoTelefoneSnapshot() {
        return exigeValidacaoTelefoneSnapshot;
    }

    public String getVinculoSocialPendenteProvedor() {
        return vinculoSocialPendenteProvedor;
    }

    public String getVinculoSocialPendenteIdentificadorExterno() {
        return vinculoSocialPendenteIdentificadorExterno;
    }

    public String getVinculoSocialPendenteNomeUsuarioExterno() {
        return vinculoSocialPendenteNomeUsuarioExterno;
    }

    public String getConviteOrganizacionalCodigo() {
        return conviteOrganizacionalCodigo;
    }

    public String getConviteOrganizacionalOrganizacaoId() {
        return conviteOrganizacionalOrganizacaoId;
    }

    public String getConviteOrganizacionalNomeOrganizacao() {
        return conviteOrganizacionalNomeOrganizacao;
    }

    public String getConviteOrganizacionalEmailConvidado() {
        return conviteOrganizacionalEmailConvidado;
    }

    public boolean isConviteOrganizacionalExigeContaSeparada() {
        return Boolean.TRUE.equals(conviteOrganizacionalExigeContaSeparada);
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }

    public OffsetDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public boolean emailJaConfirmado() {
        return emailConfirmadoEm != null;
    }

    public boolean possuiTelefoneParaValidacao() {
        return exigeValidacaoTelefoneSnapshot && telefonePrincipal != null && !telefonePrincipal.isBlank();
    }

    public boolean telefoneJaConfirmado() {
        return telefoneConfirmadoEm != null;
    }

    public boolean etapaTelefoneConcluida() {
        return !possuiTelefoneParaValidacao() || telefoneJaConfirmado();
    }

    public boolean codigoEmailExpirado(final OffsetDateTime instante) {
        return codigoEmailExpiraEm.isBefore(Objects.requireNonNull(instante, "instante é obrigatório"));
    }

    public void registrarTentativaConfirmacao(final OffsetDateTime atualizadoEm) {
        this.tentativasConfirmacaoEmail += 1;
        this.atualizadoEm = Objects.requireNonNull(atualizadoEm, "atualizadoEm é obrigatório");
    }

    public void marcarEmailConfirmado(final OffsetDateTime confirmadoEm) {
        OffsetDateTime instante = Objects.requireNonNull(confirmadoEm, "confirmadoEm é obrigatório");
        this.status = StatusCadastroConta.EMAIL_CONFIRMADO;
        this.emailConfirmadoEm = instante;
        this.atualizadoEm = instante;
    }

    public boolean codigoTelefoneExpirado(final OffsetDateTime instante) {
        if (!possuiTelefoneParaValidacao() || codigoTelefoneExpiraEm == null) {
            return false;
        }
        return codigoTelefoneExpiraEm.isBefore(Objects.requireNonNull(instante, "instante é obrigatório"));
    }

    public boolean ultrapassouReenviosEmail(final int reenviosMaximos) {
        return reenviosEmail >= reenviosMaximos;
    }

    public void atualizarCodigoEmail(final String codigoEmailHash,
                                     final OffsetDateTime codigoEmailGeradoEm,
                                     final OffsetDateTime codigoEmailExpiraEm,
                                     final OffsetDateTime atualizadoEm) {
        this.codigoEmailHash = Objects.requireNonNull(codigoEmailHash, "codigoEmailHash é obrigatório");
        this.codigoEmailGeradoEm = Objects.requireNonNull(
                codigoEmailGeradoEm, "codigoEmailGeradoEm é obrigatório");
        this.codigoEmailExpiraEm = Objects.requireNonNull(
                codigoEmailExpiraEm, "codigoEmailExpiraEm é obrigatório");
        this.reenviosEmail += 1;
        this.atualizadoEm = Objects.requireNonNull(atualizadoEm, "atualizadoEm é obrigatório");
    }

    public void definirCodigoTelefone(final String codigoTelefoneHash,
                                      final OffsetDateTime codigoTelefoneGeradoEm,
                                      final OffsetDateTime codigoTelefoneExpiraEm,
                                      final OffsetDateTime atualizadoEm) {
        this.codigoTelefoneHash = Objects.requireNonNull(codigoTelefoneHash, "codigoTelefoneHash é obrigatório");
        this.codigoTelefoneGeradoEm = Objects.requireNonNull(
                codigoTelefoneGeradoEm, "codigoTelefoneGeradoEm é obrigatório");
        this.codigoTelefoneExpiraEm = Objects.requireNonNull(
                codigoTelefoneExpiraEm, "codigoTelefoneExpiraEm é obrigatório");
        this.atualizadoEm = Objects.requireNonNull(atualizadoEm, "atualizadoEm é obrigatório");
    }

    public void marcarContatosConfirmados(final OffsetDateTime confirmadoEm) {
        OffsetDateTime instante = Objects.requireNonNull(confirmadoEm, "confirmadoEm é obrigatório");
        this.status = StatusCadastroConta.EMAIL_CONFIRMADO;
        this.emailConfirmadoEm = instante;
        if (possuiTelefoneParaValidacao()) {
            this.telefoneConfirmadoEm = instante;
        }
        this.atualizadoEm = instante;
    }

    public void definirPessoaIdPerfil(final Long pessoaIdPerfil, final OffsetDateTime atualizadoEm) {
        this.pessoaIdPerfil = pessoaIdPerfil;
        this.atualizadoEm = Objects.requireNonNull(atualizadoEm, "atualizadoEm é obrigatório");
    }

    public void definirProvisionamentoPerfil(final Long pessoaIdPerfil,
                                             final String usuarioIdPerfil,
                                             final OffsetDateTime atualizadoEm) {
        this.pessoaIdPerfil = pessoaIdPerfil;
        this.usuarioIdPerfil = usuarioIdPerfil;
        this.atualizadoEm = Objects.requireNonNull(atualizadoEm, "atualizadoEm é obrigatório");
    }

    public boolean possuiVinculoSocialPendente() {
        return vinculoSocialPendenteProvedor != null && !vinculoSocialPendenteProvedor.isBlank()
                && vinculoSocialPendenteIdentificadorExterno != null
                && !vinculoSocialPendenteIdentificadorExterno.isBlank();
    }

    public void definirVinculoSocialPendente(final ProvedorVinculoSocial provedor,
                                             final String identificadorExterno,
                                             final String nomeUsuarioExterno,
                                             final OffsetDateTime atualizadoEm) {
        this.vinculoSocialPendenteProvedor = Objects.requireNonNull(provedor, "provedor é obrigatório").getAliasApi();
        this.vinculoSocialPendenteIdentificadorExterno = Objects.requireNonNull(
                identificadorExterno, "identificadorExterno é obrigatório");
        this.vinculoSocialPendenteNomeUsuarioExterno = nomeUsuarioExterno;
        this.atualizadoEm = Objects.requireNonNull(atualizadoEm, "atualizadoEm é obrigatório");
    }

    public void limparVinculoSocialPendente(final OffsetDateTime atualizadoEm) {
        this.vinculoSocialPendenteProvedor = null;
        this.vinculoSocialPendenteIdentificadorExterno = null;
        this.vinculoSocialPendenteNomeUsuarioExterno = null;
        this.atualizadoEm = Objects.requireNonNull(atualizadoEm, "atualizadoEm é obrigatório");
    }

    public void definirConviteOrganizacionalPendente(final String codigo,
                                                     final String organizacaoId,
                                                     final String nomeOrganizacao,
                                                     final String emailConvidado,
                                                     final boolean exigeContaSeparada,
                                                     final OffsetDateTime atualizadoEm) {
        this.conviteOrganizacionalCodigo = Objects.requireNonNull(codigo, "codigo é obrigatório");
        this.conviteOrganizacionalOrganizacaoId = Objects.requireNonNull(
                organizacaoId, "organizacaoId é obrigatório");
        this.conviteOrganizacionalNomeOrganizacao = Objects.requireNonNull(
                nomeOrganizacao, "nomeOrganizacao é obrigatório");
        this.conviteOrganizacionalEmailConvidado = emailConvidado;
        this.conviteOrganizacionalExigeContaSeparada = exigeContaSeparada;
        this.atualizadoEm = Objects.requireNonNull(atualizadoEm, "atualizadoEm é obrigatório");
    }

    public void registrarProjetoFluxoPublico(final Long clienteEcossistemaId,
                                             final boolean exigeValidacaoTelefoneSnapshot,
                                             final OffsetDateTime atualizadoEm) {
        this.clienteEcossistemaId = clienteEcossistemaId;
        this.exigeValidacaoTelefoneSnapshot = exigeValidacaoTelefoneSnapshot;
        this.atualizadoEm = Objects.requireNonNull(atualizadoEm, "atualizadoEm é obrigatório");
    }

    private void aplicarContextoSolicitacao(final ContextoSolicitacaoFluxoPublico contextoSolicitacao) {
        if (contextoSolicitacao == null) {
            return;
        }
        ContextoSolicitacaoFluxoPublico contexto = contextoSolicitacao.sanitizado();
        this.localeSolicitante = contexto.locale();
        this.timeZoneSolicitante = contexto.timeZone();
        this.tipoProdutoExibicao = contexto.tipoProdutoExibicao();
        this.produtoExibicao = contexto.produtoExibicao();
        this.canalExibicao = contexto.canalExibicao();
        this.empresaExibicao = contexto.empresaExibicao();
        this.ambienteExibicao = contexto.ambienteExibicao();
    }
}
