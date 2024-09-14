package net.cluncur.auction;

import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisAPI;
import io.vertx.core.Vertx;
import io.vertx.mutiny.core.eventbus.EventBus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Arrays;
import java.util.List;
import org.jboss.logging.Logger;

@ApplicationScoped
public class CountdownTimerService {
    private static final Logger LOG = Logger.getLogger(CountdownTimerService.class);

    @Inject Redis redis;
    @Inject Vertx vertx;
    @Inject EventBus eventBus;
    @Inject RedisAPI redisAPI;

    public void startCountdown(String groupId, int duration) {
        redisAPI.setex("timer:" + groupId, String.valueOf(duration), String.valueOf(duration))
                .onSuccess(response -> updateTimer(groupId))
                .onFailure(throwable -> LOG.error("Failed to start timer: " + throwable.getMessage()));
    }

    public void updateTimer(String groupId) {
        redisAPI.decrby("timer:" + groupId, "1")
                .onSuccess(response -> {
                    int timeRemaining = Integer.parseInt(response.toString());

                    // Publish time updates to all instances via event bus
                    eventBus.publish("timer-updates", groupId + ":" + timeRemaining);

                    if (timeRemaining > 0) {
                        vertx.setTimer(1000, id -> updateTimer(groupId)); // Schedule next decrement
                    } else {
                        // Timer finished
                        List<String> keysToDelete = Arrays.asList("timer:" + groupId);
                        redisAPI.del(keysToDelete)
                                .onSuccess(result -> eventBus.publish("timer-end", groupId))
                                .onFailure(throwable -> LOG.error("Failed to delete timer: " + throwable.getMessage()));
                    }
                })
                .onFailure(throwable -> LOG.error("Failed to decrement timer: " + throwable.getMessage()));
    }

    /**
     * Reset timer when a new bid is placed
     * @param groupId
     */
    public void handleBid(String groupId) {
        startCountdown(groupId, 100);
    }
}