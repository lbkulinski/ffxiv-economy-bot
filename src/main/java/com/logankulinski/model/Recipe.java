package com.logankulinski.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.logankulinski.serialization.RecipeDeserializer;

import java.util.List;

@JsonDeserialize(using = RecipeDeserializer.class)
public record Recipe(
    int id,

    String name,

    List<Ingredient> ingredients
) {
}
