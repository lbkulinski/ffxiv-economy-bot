package com.logankulinski.serialization;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.logankulinski.model.Ingredient;
import com.logankulinski.model.Recipe;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class RecipeDeserializer extends StdDeserializer<Recipe> {
    private static final int MAX_INGREDIENTS;

    static {
        MAX_INGREDIENTS = 9;
    }

    public RecipeDeserializer(Class<?> vc) {
        super(vc);
    }

    public RecipeDeserializer() {
        this(null);
    }

    public Ingredient getIngredient(JsonParser parser, JsonNode node, int index) throws JsonMappingException {
        Objects.requireNonNull(parser);

        Objects.requireNonNull(node);

        if ((index < 0) || (index > RecipeDeserializer.MAX_INGREDIENTS)) {
            String message = "%d is not a valid index".formatted(index);

            throw new IllegalArgumentException(message);
        }

        String ingredientKey = "ItemIngredient%d".formatted(index);

        if (!node.has(ingredientKey)) {
            String message = "Failed to deserialize the ingredient at index %d".formatted(index);

            throw new JsonMappingException(parser, message);
        }

        JsonNode ingredientNode = node.get(ingredientKey);

        if (ingredientNode.isNull()) {
            return null;
        }

        String idKey = "ID";

        if (!ingredientNode.has(idKey)) {
            String message = "Failed to deserialize the ingredient at index %d".formatted(index);

            throw new JsonMappingException(parser, message);
        }

        int id = ingredientNode.get(idKey)
                               .asInt();

        String nameKey = "Name";

        if (!ingredientNode.has(nameKey)) {
            String message = "Failed to deserialize the ingredient at index %d".formatted(index);

            throw new JsonMappingException(parser, message);
        }

        String name = ingredientNode.get(nameKey)
                                    .asText();

        String amountKey = "AmountIngredient%d".formatted(index);

        if (!node.has(amountKey)) {
            String message = "Failed to deserialize the ingredient at index %d".formatted(index);

            throw new JsonMappingException(parser, message);
        }

        int amount = node.get(amountKey)
                         .asInt();

        return new Ingredient(id, name, amount);
    }

    @Override
    public Recipe deserialize(JsonParser parser, DeserializationContext context) throws IOException, JacksonException {
        Objects.requireNonNull(parser);

        Objects.requireNonNull(context);

        JsonNode node = parser.getCodec()
                              .readTree(parser);

        JsonNode idNode = node.get("ID");

        if (idNode == null) {
            String message = "Failed to deserialize the recipe";

            throw new JsonMappingException(parser, message);
        }

        int id = idNode.asInt();

        JsonNode nameNode = node.get("Name");

        if (nameNode == null) {
            String message = "Failed to deserialize the recipe";

            throw new JsonMappingException(parser, message);
        }

        String name = nameNode.asText();

        List<Ingredient> ingredients = new ArrayList<>();

        for (int i = 0; i <= RecipeDeserializer.MAX_INGREDIENTS; i++) {
            Ingredient ingredient = this.getIngredient(parser, node, i);

            ingredients.add(ingredient);
        }

        return new Recipe(id, name, ingredients);
    }
}
