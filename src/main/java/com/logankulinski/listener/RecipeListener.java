package com.logankulinski.listener;

import com.logankulinski.client.UniversalisClient;
import com.logankulinski.client.XIVAPIClient;
import com.logankulinski.model.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Component
public final class RecipeListener extends ListenerAdapter {
    private final XIVAPIClient xivapiClient;

    private final UniversalisClient universalisClient;

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
    public RecipeListener(XIVAPIClient xivapiClient, UniversalisClient universalisClient) {
        this.xivapiClient = Objects.requireNonNull(xivapiClient);

        this.universalisClient = Objects.requireNonNull(universalisClient);
    }

    private String getOptionValue(SlashCommandInteractionEvent event, String optionName) {
        OptionMapping optionMapping = event.getOption(optionName);

        if (optionMapping == null) {
            return null;
        }

        return optionMapping.getAsString();
    }

    private List<ListingView> getListings(String dataCenter, int itemId) {
        Objects.requireNonNull(dataCenter);

        CurrentlyShownView currentlyShownView = this.universalisClient.getMarketBoardData(dataCenter, itemId);

        List<ListingView> listingViews = currentlyShownView.listings();

        return listingViews.stream()
                           .sorted(Comparator.comparingInt(ListingView::pricePerUnit))
                           .limit(3L)
                           .toList();
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

        String dataCenter = this.getOptionValue(event, RecipeListener.DATA_CENTER_OPTION_NAME);

        if (dataCenter == null) {
            String message = "A data center is required";

            event.reply(message)
                 .setEphemeral(true)
                 .queue();

            return;
        }

        SearchResponse searchResponse = this.xivapiClient.search(name);

        List<Result> results = searchResponse.results();

        if (results.isEmpty()) {
            String message = "No items were found with the name \"%s\"".formatted(name);

            event.reply(message)
                 .queue();

            return;
        }

        event.deferReply()
             .queue();

        Result result = results.getFirst();

        int resultId = result.id();

        Item item = this.xivapiClient.getItem(resultId);

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("Item found! ID: %d%n".formatted(resultId));

        for (Item.Recipe itemRecipe : item.recipes()) {
            int recipeId = itemRecipe.id();

            Recipe recipe = this.xivapiClient.getRecipe(recipeId);

            stringBuilder.append("%nRecipe found! ID: %d%n".formatted(recipeId));

            for (Ingredient ingredient : recipe.ingredients()) {
                if (ingredient == null) {
                    continue;
                }

                String ingredientName = ingredient.name();

                int amount = ingredient.amount();

                int ingredientId = ingredient.id();

                List<ListingView> listings = this.getListings(dataCenter, ingredientId);

                if (listings.isEmpty()) {
                    String message = "- %s (%d) (No listings)%n".formatted(ingredientName, amount);

                    stringBuilder.append(message);

                    continue;
                }

                String message = "- %s (%d)%n".formatted(ingredientName, amount);

                stringBuilder.append(message);

                for (ListingView listing : listings) {
                    int pricePerUnit = listing.pricePerUnit();

                    int quantity = listing.quantity();

                    String worldName = listing.worldName();

                    String listingMessage = "  - %d gil/unit (%d units) on %s%n".formatted(pricePerUnit, quantity,
                        worldName);

                    stringBuilder.append(listingMessage);
                }
            }
        }

        String message = stringBuilder.toString();

        event.getHook()
             .editOriginal(message)
             .queue();
    }
}
