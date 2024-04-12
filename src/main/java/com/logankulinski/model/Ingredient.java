package com.logankulinski.model;

import com.fasterxml.jackson.annotation.JsonAlias;

public record Ingredient(
    @JsonAlias("ID") int id,

    @JsonAlias("Name") String name,

    int amount
) {
}
