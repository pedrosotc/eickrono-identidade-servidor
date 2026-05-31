package com.eickrono.api.identidade.infraestrutura.configuracao;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "identidade.avatar.storage")
public class AvatarStorageProperties {

    private String tipo = "local";
    private Path diretorio = Path.of(System.getProperty("java.io.tmpdir"), "eickrono-avatares");
    private String publicUrlBase = "http://localhost:8081/identidade/avatares/publicos";
    private long maxBytes = 5L * 1024L * 1024L;
    private String bucket;
    private String region = "sa-east-1";

    public String getTipo() {
        return tipo;
    }

    public void setTipo(final String tipo) {
        this.tipo = tipo;
    }

    public Path getDiretorio() {
        return diretorio;
    }

    public void setDiretorio(final Path diretorio) {
        this.diretorio = diretorio;
    }

    public String getPublicUrlBase() {
        return publicUrlBase;
    }

    public void setPublicUrlBase(final String publicUrlBase) {
        this.publicUrlBase = publicUrlBase;
    }

    public long getMaxBytes() {
        return maxBytes;
    }

    public void setMaxBytes(final long maxBytes) {
        this.maxBytes = maxBytes;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(final String bucket) {
        this.bucket = bucket;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(final String region) {
        this.region = region;
    }
}
