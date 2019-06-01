package actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;

import java.util.LinkedList;
import java.util.List;

public class GroupActor extends AbstractActor {
    /**
     This is the server.GroupServerActor, will host all messaging functionality for channels and is responsible over
     sending messages for groups's users. Will know if a user is an admin or not.
     TODO: implement createReceive- needs to create channels and delete them on demand from the server.
     **/
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
