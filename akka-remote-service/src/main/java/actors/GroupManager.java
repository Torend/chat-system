package actors;

import akka.actor.*;
import akka.routing.RoundRobinPool;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class GroupData
{
    /*
    this is the class that represent the data that the server uses to track a group
    TODO: complete the data structure. should be able to know who's the main admin, who's co admin, who's muted, who's invited and the other users.
     */
    public ActorRef groupRef;
    public String groupName;
    public String admin;
    public Map adminsList;
    public Map activeUsers;
    public GroupData(ActorRef groupRef, String groupName, String admin)
    {
        this.groupRef = groupRef;
        this.groupName = groupName;
        this.admin = admin;
    }

}
/**
 This is the server.GroupManagementActor, a part of the server that creates groups actors and stores them.
 Only responsible for group management and not a part of sending messages.
 TODO: implement the entire groups management
 the flow probably should be like  this-
 the groups DB will hold each group. sending messages to a group is done via a router.
 Since routers are not dynamic- each time we remove\add a client we must create a new router with the user list and update it in the group DB.
 How do we manage user-group permissions? Inside the GroupUserActor.
 It is the representative of the user in EACH group. it will have a role and will "become" a different role each time a new one is assigned.
 The roles are- admin, co admin, user, muted.
 How will a front end user know the state of the action?
 the chain will be-
 1. message sent from user client to server to obtain ActorRef for his GroupUserActor in the group he wants to preform action in.
 2. user client "Asks" action message to GroupUserActor which by using his role knows if its possible.
 3. Returns answer to user client and performs action if possible.
 **/
public class GroupManager extends AbstractActor {
    private Map groupsData; // this is the database to keep track of groups
    final String groupPath = "/user/server/groups";
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Action.CreateGroup.class, groupCreation -> {
                    //TODO: handle
                    GroupData findGroup = (GroupData) this.groupsData.get(groupCreation.groupName);
                    if(findGroup == null)
                    {
                        //ActorRef newAdmin = getContext().actorOf(GroupUserActor.class, "")
                        //List<String> paths = Arrays.asList();
                        // TODO: change line below to have the router created from EXISTING GroupUserActor that will be created for each user in each group
                        ActorRef newGroup = getContext().actorOf(new RoundRobinPool(1).props(Props.create(GroupUserActor.class)), groupCreation.groupName);//context().actorOf(Props.create(GroupActor.class), groupCreation.groupName);


                    }
                    else
                    {

                    }

                }).build();
    }

    @Override
    public void preStart() {
        this.groupsData = new HashMap();
    }
}
