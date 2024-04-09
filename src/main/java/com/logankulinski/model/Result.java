package com.logankulinski.model;

import com.fasterxml.jackson.annotation.JsonAlias;

public record Result(
    @JsonAlias("ID") String id,

    @JsonAlias("Icon") String icon,

    @JsonAlias("Name") String name,

    @JsonAlias("Url") String url,

    @JsonAlias("UrlType") String urlType
) {
}
