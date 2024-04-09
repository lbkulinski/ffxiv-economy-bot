package com.logankulinski.runner;

import com.logankulinski.client.XIVAPIClient;
import com.logankulinski.model.Ingredient;
import com.logankulinski.model.Item;
import com.logankulinski.model.Recipe;
import com.logankulinski.model.Result;
import discord4j.core.DiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.spec.InteractionApplicationCommandCallbackReplyMono;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class ItemRunner implements ApplicationRunner {
    private final DiscordClient client;

    private final XIVAPIClient xivapiClient;

    private final long guildId;

    @Autowired
    public ItemRunner(DiscordClient client, XIVAPIClient xivapiClient, @Value("${discord.guild-id}") long guildId) {
        this.client = Objects.requireNonNull(client);

        this.xivapiClient = Objects.requireNonNull(xivapiClient);

        this.guildId = guildId;
    }

    private void createApplicationCommand() {
        Long applicationId = this.client.getApplicationId()
                                        .block();

        if (applicationId == null) {
            return;
        }

        var nameOption = ApplicationCommandOptionData.builder()
                                                     .name("name")
                                                     .description("The name of the item")
                                                     .type(ApplicationCommandOption.Type.STRING.getValue())
                                                     .required(true)
                                                     .build();

        var worldOption = ApplicationCommandOptionData.builder()
                                                      .name("world")
                                                      .description("The world to source the ingredients from")
                                                      .type(ApplicationCommandOption.Type.STRING.getValue())
                                                      .required(false)
                                                      .build();

        var request = ApplicationCommandRequest.builder()
                                               .name("item")
                                               .description("""
                                               Get the recipes for an item and the location of \
                                               its cheapest components""")
                                               .addOption(nameOption)
                                               .addOption(worldOption)
                                               .build();

        this.client.getApplicationService().createGuildApplicationCommand(applicationId, this.guildId, request)
                   .subscribe();
    }

    private InteractionApplicationCommandCallbackReplyMono handleEvent(ChatInputInteractionEvent event) {
        Objects.requireNonNull(event);

        String name = event.getOption("name")
                           .orElseThrow()
                           .getValue()
                           .orElseThrow()
                           .asString();

        List<Result> results = this.xivapiClient.search(name)
                                                .results();

        if (results.isEmpty()) {
            return event.reply("No items were found with the name \"%s\"".formatted(name));
        }

        Result result = results.getFirst();

        String id = result.id();

        Item item = this.xivapiClient.getItem(id);

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("Item found! ID: %s%n".formatted(id));

        for (Item.Recipe itemRecipe : item.recipes()) {
            String recipeId = itemRecipe.id();

            Recipe recipe = this.xivapiClient.getRecipe(recipeId);

            stringBuilder.append("%nRecipe found! ID: %s%n".formatted(recipeId));

            for (Ingredient ingredient : recipe.ingredients()) {
                if (ingredient == null) {
                    continue;
                }

                String ingredientName = ingredient.name();

                int amount = ingredient.amount();

                stringBuilder.append("- %d of %s%n".formatted(amount, ingredientName));
            }
        }

        String message = stringBuilder.toString();

        return event.reply(message);
    }

    @Override
    public void run(ApplicationArguments args) {
        this.client.withGateway(client -> client.on(ChatInputInteractionEvent.class, this::handleEvent))
                   .block();
    }
}
