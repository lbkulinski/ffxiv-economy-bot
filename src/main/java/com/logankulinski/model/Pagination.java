package com.logankulinski.model;

import com.fasterxml.jackson.annotation.JsonAlias;

public record Pagination(
    @JsonAlias("Page") int page,

    @JsonAlias("PageNext") Integer pageNext,

    @JsonAlias("PagePrev") Integer pagePrev,

    @JsonAlias("PageTotal") int pageTotal,

    @JsonAlias("Results") int results,

    @JsonAlias("ResultsPerPage") int resultsPerPage,

    @JsonAlias("ResultsTotal") int resultsTotal
) {
}
