package actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.routing.Router;

import java.util.LinkedList;
import java.util.List;

public class GroupActor extends AbstractActor {
    /**
     This is the server.GroupServerActor, will host all messaging functionality for channels and is responsible over
     sending messages for groups's users. Will know if a user is an admin or not.
     TODO: implement the group functionality.
     update- this actor is probably not needed
     **/
    Router groupRouter;
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match("TOCOMPLETE".getClass(), joinMessage -> {


                }).build();
    }

    @Override
    public void preStart() {

    }
}
