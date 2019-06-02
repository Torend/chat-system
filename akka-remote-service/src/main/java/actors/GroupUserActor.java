package actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;

import java.util.LinkedList;
import java.util.List;

public class GroupUserActor extends AbstractActor {
    /**
     this class if for every user of a group. It knows which client it is linked to and passes relevant messages to it.
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
