package com.logankulinski.model;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.time.Instant;
import java.util.List;

public record ListingView(
    @JsonAlias("listingID") String id,

    Instant lastReviewTime,

    int pricePerUnit,

    int quantity,

    @JsonAlias("stainID") int stainId,

    String worldName,

    @JsonAlias("worldID") int worldId,

    String creatorName,

    @JsonAlias("creatorID") String creatorId,

    @JsonAlias("hq") boolean highQuality,

    @JsonAlias("isCrafted") boolean crafted,

    List<MateriaView> materia,

    boolean onMannequin,

    int retainerCity,

    @JsonAlias("retainerID") String retainerId,

    String retainerName,

    @JsonAlias("sellerID") String sellerId,

    int total,

    int tax
) {
}
