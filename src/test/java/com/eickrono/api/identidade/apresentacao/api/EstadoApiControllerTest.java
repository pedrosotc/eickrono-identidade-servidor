package com.eickrono.api.identidade.apresentacao.api;

import java.time.Instant;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.info.BuildProperties;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EstadoApiControllerTest {

    @Test
    void deveRetornarEstadoDaApiComVersao() throws Exception {
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new EstadoApiController("api-identidade-eickrono", buildPropertiesProvider()))
                .build();

        mockMvc.perform(get("/api/v1/estado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.servico").value("api-identidade-eickrono"))
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.versao").value("1.0.0-SNAPSHOT"))
                .andExpect(jsonPath("$.buildTime").value("2026-04-26T12:00:00Z"));
    }

    private org.springframework.beans.factory.ObjectProvider<BuildProperties> buildPropertiesProvider() {
        Properties properties = new Properties();
        properties.setProperty("version", "1.0.0-SNAPSHOT");
        properties.setProperty("time", Instant.parse("2026-04-26T12:00:00Z").toString());

        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton("buildProperties", new BuildProperties(properties));
        return beanFactory.getBeanProvider(BuildProperties.class);
    }
}
