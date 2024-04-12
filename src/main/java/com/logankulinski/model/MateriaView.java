package com.logankulinski.model;

import com.fasterxml.jackson.annotation.JsonAlias;

public record MateriaView(
    @JsonAlias("slotID") int slotId,

    @JsonAlias("materiaID") int materiaId
) {
}
