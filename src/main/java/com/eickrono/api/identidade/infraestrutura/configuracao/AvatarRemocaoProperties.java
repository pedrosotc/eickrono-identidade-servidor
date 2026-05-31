package com.eickrono.api.identidade.infraestrutura.configuracao;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "identidade.avatar.remocao")
public class AvatarRemocaoProperties {

    private int pendenciaRetencaoDias = 30;
    private boolean workerHabilitado = false;
    private int loteMaximo = 20;
    private int tentativasMaximas = 5;

    public int getPendenciaRetencaoDias() {
        return pendenciaRetencaoDias;
    }

    public void setPendenciaRetencaoDias(final int pendenciaRetencaoDias) {
        this.pendenciaRetencaoDias = pendenciaRetencaoDias;
    }

    public boolean isWorkerHabilitado() {
        return workerHabilitado;
    }

    public void setWorkerHabilitado(final boolean workerHabilitado) {
        this.workerHabilitado = workerHabilitado;
    }

    public int getLoteMaximo() {
        return loteMaximo;
    }

    public void setLoteMaximo(final int loteMaximo) {
        this.loteMaximo = loteMaximo;
    }

    public int getTentativasMaximas() {
        return tentativasMaximas;
    }

    public void setTentativasMaximas(final int tentativasMaximas) {
        this.tentativasMaximas = tentativasMaximas;
    }
}
