package net.cluncur.auction;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

@Path("/auction")
public class AuctionResource {

    @Inject CountdownTimerService countdownTimerService;

    @POST
    @Path("/{groupId}/bid")
    public void placeBid(@PathParam("groupId") String groupId) {
        countdownTimerService.handleBid(groupId);
    }
}
