package com.logankulinski.model;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.List;

public record Item(
    @JsonAlias("ID") int id,

    @JsonAlias("Name") String name,

    @JsonAlias("Recipes") List<Recipe> recipes
) {
    public record Recipe(
        @JsonAlias("ID") int id,

        @JsonAlias("Level") int level,

        @JsonAlias("ClassJobID") int classJobId
    ) {
    }

    @Override
    public List<Recipe> recipes() {
        return List.copyOf(this.recipes);
    }
}
