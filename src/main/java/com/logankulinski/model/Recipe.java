package com.logankulinski.model;

import com.fasterxml.jackson.annotation.JsonAlias;

public record Recipe(
    @JsonAlias("ID") int id,

    @JsonAlias("Level") int level,

    @JsonAlias("ClassJobID") int classJobId
) {
}
