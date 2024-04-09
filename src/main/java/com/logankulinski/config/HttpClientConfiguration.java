package com.logankulinski.config;

import com.logankulinski.client.XIVAPIClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class HttpClientConfiguration {
    @Bean
    public XIVAPIClient xivapiClient() {
        String baseUrl = "https://xivapi.com";

        RestClient restClient = RestClient.create(baseUrl);

        RestClientAdapter restClientAdapter = RestClientAdapter.create(restClient);

        HttpServiceProxyFactory httpServiceProxyFactory = HttpServiceProxyFactory.builderFor(restClientAdapter)
                                                                                 .build();

        return httpServiceProxyFactory.createClient(XIVAPIClient.class);
    }
}
