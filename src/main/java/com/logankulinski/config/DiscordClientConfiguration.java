package com.logankulinski.config;

import com.logankulinski.listener.ItemCostListener;
import com.logankulinski.listener.RecipeListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Objects;

@Configuration
public class DiscordClientConfiguration {
    @Bean
    public JDA jda(@Value("${discord.token}") String token, ItemCostListener itemCostListener,
        RecipeListener recipeListener) throws InterruptedException {
        Objects.requireNonNull(token);

        Objects.requireNonNull(itemCostListener);

        Objects.requireNonNull(recipeListener);

        JDA jda = JDABuilder.createDefault(token)
                            .build();

        jda.awaitReady();

        jda.addEventListener(itemCostListener);

        jda.addEventListener(recipeListener);

        return jda;
    }
}
