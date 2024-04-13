package com.logankulinski.model;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CurrentlyShownView(
    @JsonAlias("itemID") int itemId,

    Instant lastUploadTime,

    List<ListingView> listings,

    String regionName,

    BigDecimal currentAveragePrice,

    @JsonAlias("currentAveragePriceNQ") BigDecimal currentAveragePriceNormalQuality,

    @JsonAlias("currentAveragePriceHQ") BigDecimal currentAveragePriceHighQuality,

    BigDecimal regularSaleVelocity,

    @JsonAlias("nqSaleVelocity") BigDecimal normalQualitySaleVelocity,

    @JsonAlias("hqSaleVelocity") BigDecimal highQualitySaleVelocity,

    BigDecimal averagePrice,

    @JsonAlias("averagePriceNQ") BigDecimal averagePriceNormalQuality,

    @JsonAlias("averagePriceHQ") BigDecimal averagePriceHighQuality,

    int minPrice,

    @JsonAlias("minPriceNQ") int minPriceNormalQuality,

    @JsonAlias("minPriceHQ") int minPriceHighQuality,

    int maxPrice,

    @JsonAlias("maxPriceNQ") int maxPriceNormalQuality,

    @JsonAlias("maxPriceHQ") int maxPriceHighQuality,

    int listingsCount,

    int unitsForSale,

    int unitsSold
) {
    @Override
    public List<ListingView> listings() {
        return List.copyOf(this.listings);
    }
}
