package com.logankulinski.model;

import com.fasterxml.jackson.annotation.JsonAlias;

public record Ingredient(
    @JsonAlias("ID") String id,

    @JsonAlias("Name") String name,

    int amount
) {
}
