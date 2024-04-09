package com.logankulinski.model;

import com.fasterxml.jackson.annotation.JsonAlias;

public record Recipes(
    @JsonAlias("ID") int id,

    @JsonAlias("Level") int level,

    @JsonAlias("ClassJobID") int classJobId
) {
}
