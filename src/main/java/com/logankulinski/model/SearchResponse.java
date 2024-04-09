package com.logankulinski.model;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.List;

public record SearchResponse(
    @JsonAlias("Pagination") Pagination pagination,

    @JsonAlias("Results") List<Result> results
) {
}
