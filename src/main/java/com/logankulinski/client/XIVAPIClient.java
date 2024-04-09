package com.logankulinski.client;

import com.logankulinski.model.Item;
import com.logankulinski.model.SearchResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;

public interface XIVAPIClient {
    @GetExchange("/search?indexes=Item&string_algo=match")
    SearchResponse search(@RequestParam("string") String name);

    @GetExchange("/Item/{id}")
    Item getItem(@PathVariable String id);
}
