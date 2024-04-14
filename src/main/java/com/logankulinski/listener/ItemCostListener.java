package com.logankulinski.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logankulinski.client.UniversalisClient;
import com.logankulinski.client.XIVAPIClient;
import com.logankulinski.model.*;
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

@Component
public final class ItemCostListener extends ListenerAdapter {
    private final XIVAPIClient xivapiClient;

    private final UniversalisClient universalisClient;

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
    public ItemCostListener(XIVAPIClient xivapiClient, UniversalisClient universalisClient, ObjectMapper mapper) {
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

    private ListingView getCheapestListing(String dataCenter, int itemId, Quality quality) {
        Objects.requireNonNull(dataCenter);

        Objects.requireNonNull(quality);

        CurrentlyShownView marketBoardItem = this.universalisClient.getMarketBoardData(dataCenter, itemId);

        return marketBoardItem.listings()
                              .stream()
                              .sorted(Comparator.comparingInt(ListingView::pricePerUnit))
                              .filter(listing -> {
                                  if (quality == Quality.HIGH) {
                                      return listing.highQuality();
                                  }

                                  return true;
                              })
                              .limit(1L)
                              .findAny()
                              .orElse(null);
    }

    private ListingView getCheapestListing(String dataCenter, int itemId) {
        return this.getCheapestListing(dataCenter, itemId, Quality.NORMAL);
    }

    private Integer getCheapestIngredientCost(String dataCenter, int itemId) {
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
                                                  .mapToInt(ListingView::pricePerUnit)
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
                 .setEphemeral(true)
                 .queue();

            return;
        }

        String dataCenter = this.getOptionValue(event, ItemCostListener.DATA_CENTER_OPTION_NAME);

        if (dataCenter == null) {
            String message = "A data center is required";

            event.reply(message)
                 .setEphemeral(true)
                 .queue();

            return;
        }

        event.deferReply()
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

        ListingView listing = this.getCheapestListing(dataCenter, itemId);

        ListingView highQualityListing = this.getCheapestListing(dataCenter, itemId, Quality.HIGH);

        if (listing == null) {
            event.getHook()
                 .editOriginal("No listings were found for the specified item")
                 .queue();

            return;
        }

        int cost = listing.pricePerUnit();

        String world = listing.worldName();

        int highQualityCost = highQualityListing.pricePerUnit();

        String highQualityWorld = highQualityListing.worldName();

        String message = """
        **%s**
        - **Cheapest listing**
          - %,d gil (%s)
        - **Cheapest high quality listing**
          - %,d gil (%s)""".formatted(itemName, cost, world, highQualityCost, highQualityWorld);

        Integer ingredientCost = this.getCheapestIngredientCost(dataCenter, itemId);

        if (ingredientCost != null) {
            message += """
            
            - **Cheapest ingredient cost**
              - %,d gil""".formatted(ingredientCost);
        }

        ButtonMetadata buttonMetadata = new ButtonMetadata(itemId, dataCenter);

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
