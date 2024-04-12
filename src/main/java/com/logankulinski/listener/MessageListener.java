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
public final class MessageListener extends ListenerAdapter {
    private final XIVAPIClient xivapiClient;

    private final UniversalisClient universalisClient;

    private static final String COMMAND_NAME;

    private static final Logger LOGGER;

    static {
        COMMAND_NAME = "item";

        LOGGER = LoggerFactory.getLogger(MessageListener.class);
    }

    @Autowired
    public MessageListener(XIVAPIClient xivapiClient, UniversalisClient universalisClient) {
        this.xivapiClient = Objects.requireNonNull(xivapiClient);

        this.universalisClient = Objects.requireNonNull(universalisClient);
    }

    private List<ListingView> getListings(int itemId) {
        CurrentlyShownView currentlyShownView = this.universalisClient.getMarketBoardData("Aether", itemId);

        List<ListingView> listingViews = currentlyShownView.listings();

        return listingViews.stream()
                           .sorted(Comparator.comparingInt(ListingView::pricePerUnit))
                           .limit(3L)
                           .toList();
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        String commandName = event.getName();

        if (!Objects.equals(commandName, MessageListener.COMMAND_NAME)) {
            return;
        }

        OptionMapping nameMapping = event.getOption("name");

        if (nameMapping == null) {
            return;
        }

        String name = nameMapping.getAsString();

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

                List<ListingView> listings = this.getListings(ingredientId);

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
