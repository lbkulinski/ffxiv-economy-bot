package com.logankulinski.runner;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.logankulinski.client.XIVAPIClient;
import de.chojo.universalis.entities.Item;
import de.chojo.universalis.entities.Listing;
import de.chojo.universalis.entities.Price;
import de.chojo.universalis.events.listings.impl.ListingAddEvent;
import de.chojo.universalis.listener.ListenerAdapter;
import de.chojo.universalis.rest.UniversalisRest;
import de.chojo.universalis.rest.response.MarketBoardResponse;
import de.chojo.universalis.websocket.UniversalisWs;
import de.chojo.universalis.websocket.subscriber.Subscriptions;
import de.chojo.universalis.worlds.Region;
import de.chojo.universalis.worlds.Worlds;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Component
public final class MateriaNotificationRunner implements ApplicationRunner {
    private final UniversalisRest universalisRest;

    private final XIVAPIClient xivApiClient;

    private final JDA jda;

    private final String channelId;

    private final Cache<Integer, Double> cache;

    private static final Set<Integer> MATERIA_IDS;

    private static final Logger LOGGER;

    static {
        MATERIA_IDS = Set.of(
            41758, //Heavens' Eye Materia XI
            41759, //Savage Aim Materia XI
            41760, //Savage Might Materia XI
            41762, //Gatherer's Guerdon Materia XI
            41763, //Gatherer's Guile Materia XI,
            41764, //Gatherer's Grasp Materia XI
            41765, //Craftsman's Competence Materia XI
            41766, //Craftsman's Cunning Materia XI
            41767, //Craftsman's Command Materia XI
            //41768, //Quickarm Materia XI
            //41769, //Quicktongue Materia XI
            41771, //Heavens' Eye Materia XII
            41772, //Savage Aim Materia XII
            41773, //Savage Might Materia XII
            41775, //Gatherer's Guerdon Materia XII,
            41776, //Gatherer's Guile Materia XII
            41777, //Gatherer's Grasp Materia XII
            41778, //Craftsman's Competence Materia XII
            41779, //Craftsman's Cunning Materia XII
            41780, //Craftsman's Command Materia XII
            41781, //Quickarm Materia XII
            41782  //Quicktongue Materia XII
        );

        LOGGER = LoggerFactory.getLogger(MateriaNotificationRunner.class);
    }

    @Autowired
    public MateriaNotificationRunner(UniversalisRest universalisRest, XIVAPIClient xivApiClient, JDA jda,
        @Value("${discord.channel-id}") String channelId) {
        this.universalisRest = Objects.requireNonNull(universalisRest);

        this.xivApiClient = Objects.requireNonNull(xivApiClient);

        this.jda = Objects.requireNonNull(jda);

        this.channelId = Objects.requireNonNull(channelId);

        int duration = 2;

        this.cache = Caffeine.newBuilder()
                             .expireAfterWrite(duration, TimeUnit.MINUTES)
                             .build(this::getAdjustedAverage);
    }

    private String getItemName(int id) {
        return this.xivApiClient.getItem(id)
                                .name();
    }

    private Double getAdjustedAverage(int materiaId) {
        MateriaNotificationRunner.LOGGER.info("Calculating adjusted average for materia ID: {}", materiaId);

        Region region = Worlds.northAmerica();

        MarketBoardResponse response = null;

        int retryCount = 0;

        int maxRetries = 5;

        while ((response == null) && (retryCount < maxRetries)) {
            try {
                response = this.universalisRest.marketBoard()
                                               .region(region)
                                               .itemsIds(materiaId)
                                               .queue()
                                               .get();
            } catch (InterruptedException | ExecutionException e) {
                /*
                MateriaNotificationRunner.LOGGER.error("Failed to retrieve market board data for materia ID: {}",
                    materiaId, e);
                 */

                retryCount++;

                try {
                    Thread.sleep(1_000);
                } catch (InterruptedException e1) {
                    MateriaNotificationRunner.LOGGER.error("Thread interrupted while waiting to retry", e1);
                }
            }
        }

        if (response == null) {
            MateriaNotificationRunner.LOGGER.error("""
            Failed to retrieve market board data for materia ID {} after {} retries""", materiaId, maxRetries);

            return this.cache.getIfPresent(materiaId);
        }

        DescriptiveStatistics statistics = new DescriptiveStatistics();

        response.listings()
                .stream()
                .map(Listing::price)
                .mapToInt(Price::pricePerUnit)
                .forEach(statistics::addValue);

        double mean = statistics.getMean();

        double standardDeviation = statistics.getStandardDeviation();

        double lowerBound = mean - (3 * standardDeviation);

        double upperBound = mean + (3 * standardDeviation);

        DescriptiveStatistics filteredStatistics = new DescriptiveStatistics();

        response.listings()
                .stream()
                .map(Listing::price)
                .mapToInt(Price::pricePerUnit)
                .filter(price -> (price >= lowerBound) && (price <= upperBound))
                .forEach(filteredStatistics::addValue);

        return filteredStatistics.getMean();
    }

    private void handleEvent(ListingAddEvent event, int id) {
        Objects.requireNonNull(event);

        String name = this.getItemName(id);

        Double average = this.cache.get(id, this::getAdjustedAverage);

        event.listings()
             .forEach(listing -> {
                 String listingId = listing.listingId();

                 int price = listing.price()
                                    .pricePerUnit();

                 String world = listing.world()
                                       .name();

                 String dataCenter = listing.world()
                                            .dataCenter()
                                            .name();

                 double value = (price / average) * 100;

                 if (value <= 10.0) {
                     String message = """
                     Materia price alert!
                     - %s (%d)
                       - Listing ID: %s
                       - Location: %s (%s)
                       - Average: %,.2f gil
                       - Price: %,d gil
                     """.formatted(name, id, listingId, world, dataCenter, average, price);

                     System.out.println(message);

                     MessageChannel channel = this.jda.getChannelById(MessageChannel.class, this.channelId);

                     if (channel != null) {
                         channel.sendMessage(message)
                                .queue();
                     }
                 }
             });
    }

    @Override
    public void run(ApplicationArguments args) {
        UniversalisWs.getDefault()
                     .subscribe(Subscriptions.listingAdd()
                                             .restrict(Worlds.northAmerica()
                                                             .aether().faerie))
                     .registerListener(new ListenerAdapter() {
                         @Override
                         public void onListingAdd(ListingAddEvent event) {
                             Item item = event.item();

                             int id = item.id();

                             if (MateriaNotificationRunner.MATERIA_IDS.contains(id)) {
                                 MateriaNotificationRunner.this.handleEvent(event, id);
                             }
                         }
                     })
                     .build();
    }
}
