package com.logankulinski.client;

import com.logankulinski.model.CurrentlyShownView;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;

public interface UniversalisClient {
    @GetExchange("/{worldDcRegion}/{itemId}")
    CurrentlyShownView getMarketBoardData(@PathVariable String worldDcRegion, @PathVariable int itemId);
}
