package actors;

import akka.actor.*;
import akka.routing.*;
import akka.routing.ActorRefRoutee;

import java.util.*;

class GroupData
{
    /*
    this is the class that represent the data that the server uses to track a group
    TODO: complete the data structure. should be able to know who's the main admin, who's co admin, who's muted, who's invited and the other users.
     */
    //public ActorRef groupRef; // this will be the router ref
    public String groupName;
    public String admin;
    public Map adminsList;
    public Map activeUsers;
    public Map invitedUsers;
    public Map mutedUsers;
    public Router groupRouter;
    //List<Routee> routees;
    public GroupData(Router groupRouter, String groupName, String admin, ActorRef adminRef)
    {
        this.groupRouter = groupRouter;
        this.groupName = groupName;
        this.admin = admin;
        this.activeUsers = new HashMap();
        //this.activeUsers.put(admin, adminRef);
        //routees =  new ArrayList<Routee>();
        //routees.add(new ActorRefRoutee(adminRef));
    }

    public void addUser(String username, ActorRef actorRef)
    {
        this.activeUsers.put(username, actorRef);
    }
}
/**
 This is the server.GroupManagementActor, a part of the server that creates groups actors and stores them.
 Only responsible for group management and not a part of sending messages.
 TODO: implement the entire groups management
 the flow should be as such-
 the groups DB will hold each group. sending messages to a group is done via a router.
 to add a user just use the router adding functions.
 //
 How do we manage user-group permissions? in here, the group manager and on the client itself.
 The roles are- admin, co admin, user, muted.
 How will a front end user know the state of the action?
 the chain will be-
 1. message sent from user client to server to obtain ActorRef for his GroupUserActor in the group he wants to perform action in.
 2. user client "Asks" action message to GroupManager which responds if its possible and uses error codes from Errors.Error
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
                    Action.ActionResult result;
                    if(findGroup == null)
                    {
                        //ActorRef newAdmin = getContext().actorOf(GroupUserActor.class, "")
                        //List<String> paths = Arrays.asList();
                        //Props newAdminProps =  (Props.create(GroupUserActor.class));
                        //ActorRef newAdmin = getContext().actorOf(newAdminProps, "");
                        List<Routee> routees =  new ArrayList<Routee>();
                        routees.add(new ActorRefRoutee(groupCreation.adminRef));
                        Router router = new Router(new RoundRobinRoutingLogic(), routees);
                        GroupData newGroup = new GroupData(router, groupCreation.groupName, groupCreation.adminName, groupCreation.adminRef);
                        groupsData.put(newGroup.groupName, newGroup);
                        // List<String> adminPath = Arrays.asList(groupCreation.adminRef.path().toSerializationFormat());
                        //ActorRef newGroupRouter = getContext().actorOf(new RoundRobinGroup(adminPath).props(), groupCreation.groupName);//context().actorOf(Props.create(GroupActor.class), groupCreation.groupName);
                        //GroupData newGroup = new GroupData(newGroupRouter, groupCreation.groupName, groupCreation.adminName, groupCreation.adminRef);
                        //groupsData.put(newGroup.groupName, newGroup);
                        //newGroupRouter.tell();
                        result = new Action.ActionResult(Errors.Error.SUCCESS);
                    }
                    // in case we already hav a group with that name
                    else
                    {
                        result = new Action.ActionResult(Errors.Error.DUPLICATE_GROUP);
                    }
                    sender().tell(result, self());
                })
                .match(Action.InviteToGroup.class, groupInvitation -> {
                    //TODO: handle
                    GroupData findGroup = (GroupData) this.groupsData.get(groupInvitation.groupName);
                    if(findGroup == null)
                    {
                        //ActorRef newAdmin = getContext().actorOf(GroupUserActor.class, "")
                        //List<String> paths = Arrays.asList();



                    }
                    else
                    {

                    }

                })
                .match(Action.AddToGroup.class, groupAddition -> {
                    //TODO: handle
                   GroupData findGroup = (GroupData) this.groupsData.get(groupAddition.groupName);
                   findGroup.groupRouter.addRoutee(groupAddition.inviteeRef);
                })
                .build();
    }

    @Override
    public void preStart() {
        this.groupsData = new HashMap();
    }
}
