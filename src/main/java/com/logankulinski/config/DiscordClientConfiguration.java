package com.logankulinski.config;

import discord4j.core.DiscordClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DiscordClientConfiguration {
    @Bean
    public DiscordClient discordClient(@Value("${discord.token}") String token) {
        return DiscordClient.create(token);
    }
}
