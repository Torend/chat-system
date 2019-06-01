package actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
/**
 This is the server.GroupManagementActor, a part of the server that creates groups actors and stores them.
 Only responsible for group management and not a part of sending messages.
 TODO: implement createReceive- needs to create groups and delete them on demand from the server.
 **/
/*public class GroupManager extends AbstractActor {

    final String groupPath = "/user/Server/Groups";
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(CreateGroup.class, handledMsg -> {
                    ActorRef resolvedRef = UtilFunctions.resolveActorRef(getContext().actorSelection(groupPath + handledMsg.groupname));
                    if(resolvedRef != null)
                    {
                        //TODO: handle alredy existing group

                    } else
                    {
                        // wasn't able to find the group- create it
                        ActorRef newGroup = getContext().actorOf(Props.create(GroupServerActor.class,
                                handledMsg.groupname), handledMsg.groupname);
                    }
                    //TODO: return response

                }).build();
    }

    @Override
    public void preStart() {

    }
}*/
