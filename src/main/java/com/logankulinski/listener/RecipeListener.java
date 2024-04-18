package com.logankulinski.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logankulinski.client.XIVAPIClient;
import com.logankulinski.model.*;
import de.chojo.universalis.entities.Listing;
import de.chojo.universalis.entities.Price;
import de.chojo.universalis.rest.UniversalisRest;
import de.chojo.universalis.rest.response.MarketBoardResponse;
import de.chojo.universalis.worlds.DataCenter;
import de.chojo.universalis.worlds.Worlds;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

@Component
public final class RecipeListener extends ListenerAdapter {
    private final XIVAPIClient xivapiClient;

    private final UniversalisRest universalisClient;

    private final ObjectMapper mapper;

    private static final String COMMAND_NAME;

    private static final String NAME_OPTION_NAME;

    private static final String DATA_CENTER_OPTION_NAME;

    private static final Logger LOGGER;

    static {
        COMMAND_NAME = "recipe";

        NAME_OPTION_NAME = "name";

        DATA_CENTER_OPTION_NAME = "data-center";

        LOGGER = LoggerFactory.getLogger(RecipeListener.class);
    }

    @Autowired
    public RecipeListener(XIVAPIClient xivapiClient, UniversalisRest universalisClient, ObjectMapper mapper) {
        this.xivapiClient = Objects.requireNonNull(xivapiClient);

        this.universalisClient = Objects.requireNonNull(universalisClient);

        this.mapper = Objects.requireNonNull(mapper);
    }

    private String getOptionValue(SlashCommandInteractionEvent event, String optionName) {
        OptionMapping optionMapping = event.getOption(optionName);

        if (optionMapping == null) {
            return null;
        }

        return optionMapping.getAsString();
    }

    private List<Listing> getListings(DataCenter dataCenter, int itemId) {
        Objects.requireNonNull(dataCenter);

        MarketBoardResponse response;

        try {
            response = this.universalisClient.marketBoard()
                                             .dataCenter(dataCenter)
                                             .itemsIds(itemId)
                                             .queue()
                                             .get();
        } catch (InterruptedException | ExecutionException e) {
            String message = e.getMessage();

            RecipeListener.LOGGER.error(message, e);

            return List.of();
        }

        return response.listings()
                       .stream()
                       .sorted(Comparator.comparingInt(listing -> listing.price()
                                                                         .pricePerUnit()))
                       .limit(2L)
                       .toList();
    }

    private void respondToInteraction(DataCenter dataCenter, int itemId, InteractionHook hook) {
        Objects.requireNonNull(dataCenter);

        Objects.requireNonNull(hook);

        Item item = this.xivapiClient.getItem(itemId);

        List<Item.Recipe> recipes = item.recipes();

        if (recipes.isEmpty()) {
            String message = "No recipes were found for this item";

            hook.sendMessage(message)
                .setEphemeral(true)
                .queue();

            return;
        }

        StringBuilder stringBuilder = new StringBuilder();

        String itemName = item.name();

        stringBuilder.append("**%s Recipe**%n".formatted(itemName));

        int recipeId = recipes.getFirst()
                              .id();

        Recipe recipe = this.xivapiClient.getRecipe(recipeId);

        for (Ingredient ingredient : recipe.ingredients()) {
            if (ingredient == null) {
                continue;
            }

            String ingredientName = ingredient.name();

            int amount = ingredient.amount();

            int ingredientId = ingredient.id();

            List<Listing> listings = this.getListings(dataCenter, ingredientId);

            if (listings.isEmpty()) {
                String message = "- **%s** (%d) (No listings)%n".formatted(ingredientName, amount);

                stringBuilder.append(message);

                continue;
            }

            String message = "- **%s** (%d)%n".formatted(ingredientName, amount);

            stringBuilder.append(message);

            for (Listing listing : listings) {
                Price price = listing.price();

                int pricePerUnit = price.pricePerUnit();

                int quantity = price.quantity();

                String worldName = listing.world()
                                          .name();

                String listingMessage = "  - %,d gil on %s (%d available)%n".formatted(pricePerUnit, worldName,
                    quantity);

                stringBuilder.append(listingMessage);
            }
        }

        String message = stringBuilder.toString();

        hook.sendMessage(message)
            .setEphemeral(true)
            .queue();
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        String commandName = event.getName();

        if (!Objects.equals(commandName, RecipeListener.COMMAND_NAME)) {
            return;
        }

        String name = this.getOptionValue(event, RecipeListener.NAME_OPTION_NAME);

        if (name == null) {
            String message = "A name is required";

            event.reply(message)
                 .setEphemeral(true)
                 .queue();

            return;
        }

        String dataCenterString = this.getOptionValue(event, RecipeListener.DATA_CENTER_OPTION_NAME);

        if (dataCenterString == null) {
            String message = "A data center is required";

            event.reply(message)
                 .setEphemeral(true)
                 .queue();

            return;
        }

        DataCenter dataCenter = Worlds.datacenterByName(dataCenterString);

        SearchResponse searchResponse = this.xivapiClient.search(name);

        List<Result> results = searchResponse.results();

        if (results.isEmpty()) {
            String message = "No items were found with the name \"%s\"".formatted(name);

            event.reply(message)
                 .setEphemeral(true)
                 .queue();

            return;
        }

        event.deferReply()
             .queue();

        Result result = results.getFirst();

        int itemId = result.id();

        InteractionHook hook = event.getHook();

        this.respondToInteraction(dataCenter, itemId, hook);
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String componentId = event.getComponentId();

        byte[] buttonMetadataBytes = Base64.getDecoder()
                                           .decode(componentId);

        String buttonMetadataString = new String(buttonMetadataBytes);

        ButtonMetadata buttonMetadata;

        try {
            buttonMetadata = this.mapper.readValue(buttonMetadataString, ButtonMetadata.class);
        } catch (JsonProcessingException e) {
            String errorMessage = e.getMessage();

            RecipeListener.LOGGER.error(errorMessage, e);

            return;
        }

        event.deferReply()
             .queue();

        String dataCenterName = buttonMetadata.dataCenter();

        DataCenter dataCenter = Worlds.datacenterByName(dataCenterName);

        if (dataCenter == null) {
            String message = "Sorry, an error occurred. Please try again later";

            event.getHook()
                 .sendMessage(message)
                 .setEphemeral(true)
                 .queue();

            return;
        }

        int itemId = buttonMetadata.id();

        InteractionHook hook = event.getHook();

        this.respondToInteraction(dataCenter, itemId, hook);
    }
}
