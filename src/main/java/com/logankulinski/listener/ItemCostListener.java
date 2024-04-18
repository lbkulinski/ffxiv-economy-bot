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
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
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
public final class ItemCostListener extends ListenerAdapter {
    private final XIVAPIClient xivapiClient;

    private final UniversalisRest universalisClient;

    private final ObjectMapper mapper;

    private static final String COMMAND_NAME;

    private static final String NAME_OPTION_NAME;

    private static final String DATA_CENTER_OPTION_NAME;

    private static final Logger LOGGER;

    static {
        COMMAND_NAME = "item-cost";

        NAME_OPTION_NAME = "name";

        DATA_CENTER_OPTION_NAME = "data-center";

        LOGGER = LoggerFactory.getLogger(ItemCostListener.class);
    }

    @Autowired
    public ItemCostListener(XIVAPIClient xivapiClient, UniversalisRest universalisClient, ObjectMapper mapper) {
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

    private enum Quality {
        NORMAL,
        HIGH
    }

    private Listing getCheapestListing(DataCenter dataCenter, int itemId, Quality quality) {
        Objects.requireNonNull(dataCenter);

        Objects.requireNonNull(quality);

        boolean highQuality = quality == Quality.HIGH;

        MarketBoardResponse response;

        try {
            response = this.universalisClient.marketBoard()
                                             .dataCenter(dataCenter)
                                             .itemsIds(itemId)
                                             .highQuality(highQuality)
                                             .queue()
                                             .get();
        } catch (InterruptedException | ExecutionException e) {
            String message = e.getMessage();

            ItemCostListener.LOGGER.error(message, e);

            return null;
        }

        return response.listings()
                       .stream()
                       .sorted(Comparator.comparingInt(listing -> listing.price()
                                                                         .pricePerUnit()))
                       .limit(1L)
                       .findAny()
                       .orElse(null);
    }

    private Listing getCheapestListing(DataCenter dataCenter, int itemId) {
        return this.getCheapestListing(dataCenter, itemId, Quality.NORMAL);
    }

    private Integer getCheapestIngredientCost(DataCenter dataCenter, int itemId) {
        Objects.requireNonNull(dataCenter);

        Item item = this.xivapiClient.getItem(itemId);

        return item.recipes()
                   .stream()
                   .map(Item.Recipe::id)
                   .map(this.xivapiClient::getRecipe)
                   .map(Recipe::ingredients)
                   .map(ingredients -> ingredients.stream()
                                                  .map(Ingredient::id)
                                                  .map(id -> this.getCheapestListing(dataCenter, id))
                                                  .map(Listing::price)
                                                  .mapToInt(Price::pricePerUnit)
                                                  .sum())
                   .min(Integer::compare)
                   .orElse(null);
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        String commandName = event.getName();

        if (!Objects.equals(commandName, ItemCostListener.COMMAND_NAME)) {
            return;
        }

        String name = this.getOptionValue(event, ItemCostListener.NAME_OPTION_NAME);

        if (name == null) {
            String message = "A name is required";

            event.reply(message)
                 .queue();

            return;
        }

        String dataCenterString = this.getOptionValue(event, ItemCostListener.DATA_CENTER_OPTION_NAME);

        if (dataCenterString == null) {
            String message = "A data center is required";

            event.reply(message)
                 .queue();

            return;
        }

        DataCenter dataCenter = Worlds.datacenterByName(dataCenterString);

        if (dataCenter == null) {
            String message = "The specified data center is invalid";

            event.reply(message)
                 .queue();

            return;
        }

        event.deferReply()
             .setEphemeral(true)
             .queue();

        SearchResponse searchResponse = this.xivapiClient.search(name);

        List<Result> results = searchResponse.results();

        if (results.isEmpty()) {
            String message = "No items were found with the name \"%s\"".formatted(name);

            event.getHook()
                 .sendMessage(message)
                 .queue();

            return;
        }

        Result result = results.getFirst();

        String itemName = result.name();

        int itemId = result.id();

        Listing listing = this.getCheapestListing(dataCenter, itemId);

        Listing highQualityListing = this.getCheapestListing(dataCenter, itemId, Quality.HIGH);

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("**%s**%n".formatted(itemName));

        if (listing == null) {
            stringBuilder.append("- **No normal quality listings found**\n");
        } else {
            int cost = listing.price()
                             .pricePerUnit();

            String world = listing.world()
                                 .name();

            stringBuilder.append("""
            - **Cheapest normal quality listing**
              - %,d gil (%s)
            """.formatted(cost, world));
        }

        if (highQualityListing == null) {
            stringBuilder.append("- **No high quality listings found**\n");
        } else {
            int highQualityCost = highQualityListing.price()
                                                     .pricePerUnit();

            String highQualityWorld = highQualityListing.world()
                                                       .name();

            stringBuilder.append("""
            - **Cheapest high quality listing**
              - %,d gil (%s)
            """.formatted(highQualityCost, highQualityWorld));
        }

        Integer ingredientCost = this.getCheapestIngredientCost(dataCenter, itemId);

        if (ingredientCost != null) {
            stringBuilder.append("""
            - **Cheapest ingredient cost**
              - %,d gil""".formatted(ingredientCost));
        }

        String message = stringBuilder.toString();

        String dataCenterName = dataCenter.name();

        ButtonMetadata buttonMetadata = new ButtonMetadata(dataCenterName, itemId);

        String buttonMetadataJson;

        try {
            buttonMetadataJson = this.mapper.writeValueAsString(buttonMetadata);
        } catch (JsonProcessingException e) {
            String errorMessage = e.getMessage();

            ItemCostListener.LOGGER.error(errorMessage, e);

            event.getHook()
                 .sendMessage(message)
                 .queue();

            return;
        }

        byte[] buttonMetadataBytes = buttonMetadataJson.getBytes();

        String id = Base64.getEncoder()
                          .encodeToString(buttonMetadataBytes);

        ItemCostListener.LOGGER.info("Button metadata: {}", id);

        String label = "Get recipe";

        String toolsUnicode = "U+1F6E0";

        event.getHook()
             .sendMessage(message)
             .addActionRow(
                 Button.primary(id, label)
                       .withEmoji(Emoji.fromUnicode(toolsUnicode))
             )
             .queue();
    }
}
