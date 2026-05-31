package com.eickrono.api.identidade.infraestrutura.configuracao;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@ConditionalOnProperty(prefix = "identidade.avatar.storage", name = "tipo", havingValue = "s3")
public class AvatarStorageS3Configuracao {

    @Bean
    public S3Client avatarS3Client(final AvatarStorageProperties properties) {
        return S3Client.builder()
                .region(Region.of(properties.getRegion()))
                .build();
    }
}
