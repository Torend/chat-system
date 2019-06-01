package actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;

import java.util.HashMap;
import java.util.Map;

class GroupData
{
    /*
    this is the class that represent the data that the server uses to track a group
    TODO: complete the data structure. should be able to know who's the main admin, who's co admin, who's muted, who's invited and the other users.
     */
    public ActorRef groupRef;
    public String admin;
    public Map admins;
    public Map activeUsers;
    public GroupData(ActorRef ref)
    {
    }

}
/**
 This is the server.GroupManagementActor, a part of the server that creates groups actors and stores them.
 Only responsible for group management and not a part of sending messages.
 TODO: implement the entire groups management
 **/
public class GroupManager extends AbstractActor {
    private Map groupsData; // this is the database to keep track of groups
    final String groupPath = "/user/Server/Groups";
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Action.CreateGroup.class, handledMsg -> {
                    //TODO: handle


                }).build();
    }

    @Override
    public void preStart() {
        this.groupsData = new HashMap();
    }
}
