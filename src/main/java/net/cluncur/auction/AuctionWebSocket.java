package net.cluncur.auction;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import io.quarkus.vertx.ConsumeEvent;
import io.vertx.core.eventbus.EventBus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.jboss.logging.Logger;

@ServerEndpoint("/auction/{groupId}")
@ApplicationScoped
public class AuctionWebSocket {
    private static final Logger LOG = Logger.getLogger(AuctionWebSocket.class);
    private Map<String, Session> sessions = new ConcurrentHashMap<>();
    
    @Inject EventBus eventBus;

    @OnOpen
    public void onOpen(Session session, @PathParam("groupId") String groupId) {
        sessions.put(groupId, session);
        LOG.info("Connected to group " + groupId);

        // Send the current timer value to the newly connected client
        eventBus.request("get-timer", groupId, ar -> {
            if (ar.succeeded()) {
                session.getAsyncRemote().sendText("Current Time Remaining: " + ar.result().body().toString());
            }
        });
    }

    /**
     * Handle bid, reset countdown
     * @param message
     * @param groupId
     */
    @OnMessage
    public void onMessage(String message, @PathParam("groupId") String groupId) {
        eventBus.publish("bid-placed", groupId);
    }

    @OnClose
    public void onClose(Session session, @PathParam("groupId") String groupId) {
        sessions.remove(groupId);
        LOG.info("Disconnected from group " + groupId);
    }

    @ConsumeEvent("timer-updates")
    public void onTimerUpdate(String message) {
        String[] parts = message.split(":");
        String groupId = parts[0];
        String timeRemaining = parts[1];

        Session session = sessions.get(groupId);
        if (session != null && session.isOpen()) {
            session.getAsyncRemote().sendText("Time Remaining: " + timeRemaining);
        }
    }

    @ConsumeEvent("timer-end")
    public void onTimerEnd(String groupId) {
        Session session = sessions.get(groupId);
        if (session != null && session.isOpen()) {
            session.getAsyncRemote().sendText("Auction Ended!");
            try {
                session.close();
            } catch (IOException e) {
                LOG.error(e);
            }
        }
    }
}
