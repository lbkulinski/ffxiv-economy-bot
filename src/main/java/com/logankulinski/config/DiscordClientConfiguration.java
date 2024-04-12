package com.logankulinski.config;

import com.logankulinski.listener.MessageListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DiscordClientConfiguration {
    @Bean
    public JDA jda(@Value("${discord.token}") String token, MessageListener listener) {
        JDA jda = JDABuilder.createDefault(token)
                            .build();

        jda.addEventListener(listener);

        return jda;
    }
}
