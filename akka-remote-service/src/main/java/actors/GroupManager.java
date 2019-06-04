package actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.routing.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class GroupData {
    /*
    this is the class that represent the data that the server uses to track a group
    TODO: complete the data structure. should be able to know who's the main admin, who's co admin, who's muted, who's invited and the other users.
     */
    //public ActorRef groupRef; // this will be the router ref
    public String groupName;
    public String admin;
    public Map<String, ActorRef> adminsList;
    public Map<String, ActorRef> activeUsers;
    ;
    public HashMap<String, Duration> mutedUsers;
    public Router groupRouter;

    //List<Routee> routees;
    public GroupData(Router groupRouter, String groupName, String admin, ActorRef adminRef) {
        this.groupRouter = groupRouter;
        this.groupName = groupName;
        this.admin = admin;
        this.activeUsers = new HashMap<String, ActorRef>();
        this.mutedUsers = new HashMap<String, Duration>();
    }

    public void addUser(String username, ActorRef actorRef) {
        this.activeUsers.put(username, actorRef);
    }

    public void deleteUser(String username) {
        this.groupRouter.removeRoutee(activeUsers.get(username));
        this.activeUsers.remove(username);
        this.adminsList.remove(username);

    }

    public void addCoAdmin(String username, ActorRef actorRef) {
        this.adminsList.put(username, actorRef);
    }

    public void deleteCoAdmin(String username) {
        this.adminsList.remove(username);
    }
}

/**
 * This is the server.GroupManagementActor, a part of the server that creates groups actors and stores them.
 * Only responsible for group management and not a part of sending messages.
 * TODO: implement the entire groups management
 * the flow should be as such-
 * the groups DB will hold each group. sending messages to a group is done via a router.
 * to add a user just use the router adding functions.
 * //
 * How do we manage user-group permissions? in here, the group manager and on the client itself.
 * The roles are- admin, co admin, user, muted.
 * How will a front end user know the state of the action?
 * the chain will be-
 * 1. user client "Asks" action message to GroupManager which responds if its possible and uses error codes from Errors.Error
 * 2. Returns answer to user client and performs action if possible.
 **/
public class GroupManager extends AbstractActor {
    private Map<String, GroupData> groupsData; // this is the database to keep track of groups
    final String groupPath = "/user/server/groups";

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Action.CreateGroup.class, groupCreation -> {
                    GroupData findGroup = this.groupsData.get(groupCreation.groupName);
                    Action.ActionResult result;
                    if (findGroup == null) {
                        //ActorRef newAdmin = getContext().actorOf(GroupUserActor.class, "")
                        //List<String> paths = Arrays.asList();
                        //Props newAdminProps =  (Props.create(GroupUserActor.class));
                        //ActorRef newAdmin = getContext().actorOf(newAdminProps, "");
                        List<Routee> routees = new ArrayList<Routee>();
                        routees.add(new ActorRefRoutee(groupCreation.adminRef));
                        Router router = new Router(new RoundRobinRoutingLogic(), routees);
                        GroupData newGroup = new GroupData(router, groupCreation.groupName, groupCreation.adminName, groupCreation.adminRef);
                        groupsData.put(newGroup.groupName, newGroup);
                        newGroup.addUser(groupCreation.adminName, groupCreation.adminRef);
                        // List<String> adminPath = Arrays.asList(groupCreation.adminRef.path().toSerializationFormat());
                        //ActorRef newGroupRouter = getContext().actorOf(new RoundRobinGroup(adminPath).props(), groupCreation.groupName);//context().actorOf(Props.create(GroupActor.class), groupCreation.groupName);
                        //GroupData newGroup = new GroupData(newGroupRouter, groupCreation.groupName, groupCreation.adminName, groupCreation.adminRef);
                        //groupsData.put(newGroup.groupName, newGroup);
                        //newGroupRouter.tell();
                        result = new Action.ActionResult(Errors.Error.SUCCESS);
                    }
                    // in case we already hav a group with that name
                    else result = new Action.ActionResult(Errors.Error.DUPLICATE_GROUP);
                    sender().tell(result, self());
                })
                .match(Action.GroupTextMessage.class, groupTextMessage -> {
                    Action.ActionResult result;
                    GroupData findGroup = this.groupsData.get(groupTextMessage.groupName);
                    if (findGroup == null)
                        result = new Action.ActionResult(Errors.Error.NO_SUCH_GROUP);
                    else {
                        if (!findGroup.mutedUsers.containsKey(groupTextMessage.senderName)) {
                            findGroup.groupRouter.route(new Broadcast(groupTextMessage), self());
                            result = new Action.ActionResult(Errors.Error.SUCCESS);
                        }
                        else {
                            result = new Action.ActionResult(Errors.Error.MUTED);
                        }
                    }
                    sender().tell(result, self());
                })
                .match(Action.InviteToGroup.class, groupInvitation -> {
                    Action.ActionResult result;
                    GroupData findGroup = this.groupsData.get(groupInvitation.groupName);
                    if (findGroup == null)
                        result = new Action.ActionResult(Errors.Error.NO_SUCH_GROUP);
                    else {
                        String inviteeName = groupInvitation.inviteeName;
                        String inviterName = groupInvitation.inviterName;
                        if (inviterName.equals(findGroup.admin) || findGroup.adminsList.containsKey(inviterName)) { //check privilege
                            if (!findGroup.activeUsers.containsKey(inviteeName)) // check if inviteeName is not already member in this group
                                result = new Action.ActionResult(Errors.Error.SUCCESS);
                            else result = new Action.ActionResult(Errors.Error.ALREADY_REGISTERED);
                        } else result = new Action.ActionResult(Errors.Error.NO_PRIVILEGE);
                    }
                    sender().tell(result, self());
                })
                .match(Action.AddToGroup.class, groupAddition -> {
                    GroupData findGroup = this.groupsData.get(groupAddition.groupName);
                    findGroup.groupRouter.addRoutee(groupAddition.inviteeRef);
                    findGroup.addUser(groupAddition.inviteeName, groupAddition.inviteeRef);
                })
                .match(Action.RemoveFromGroup.class, groupRemoval -> {
                    Action.ActionResult result;
                    GroupData findGroup = this.groupsData.get(groupRemoval.groupName);
                    if (findGroup == null)
                        result = new Action.ActionResult(Errors.Error.NO_SUCH_GROUP);
                    else {
                        if (groupRemoval.senderName.equals(findGroup.admin) || findGroup.adminsList.containsKey(groupRemoval.senderName)) { //check privilege
                            if (findGroup.activeUsers.containsKey(groupRemoval.removedName)) { // check if removedName member in this group
                                findGroup.deleteUser(groupRemoval.removedName);
                                result = new Action.ActionResult(Errors.Error.SUCCESS);
                            } else
                                result = new Action.ActionResult(Errors.Error.NO_SUCH_MEMBER);
                        } else result = new Action.ActionResult(Errors.Error.NO_PRIVILEGE);
                    }
                    sender().tell(result, self());
                })
                .match(Action.AddCoAdmin.class, groupCoAdminAddition -> {
                    Action.ActionResult result;
                    GroupData findGroup = this.groupsData.get(groupCoAdminAddition.groupName);
                    if (findGroup == null)
                        result = new Action.ActionResult(Errors.Error.NO_SUCH_GROUP);
                    else {
                        if (findGroup.admin.equals(groupCoAdminAddition.senderName)) { //check privilege
                            if (findGroup.activeUsers.containsKey(groupCoAdminAddition.coAdminName)) { // check if coAdminName member in this group
                                ActorRef coAdmin = findGroup.activeUsers.get(groupCoAdminAddition.coAdminName);
                                findGroup.addCoAdmin(groupCoAdminAddition.coAdminName, coAdmin);
                                result = new Action.ActionResult(Errors.Error.SUCCESS);
                            } else
                                result = new Action.ActionResult(Errors.Error.NO_SUCH_MEMBER);
                        } else result = new Action.ActionResult(Errors.Error.NO_PRIVILEGE);
                    }
                    sender().tell(result, self());
                })
                .match(Action.DeleteCoAdmin.class, groupCoAdminRemoval -> {
                    Action.ActionResult result;
                    GroupData findGroup = this.groupsData.get(groupCoAdminRemoval.groupName);
                    if (findGroup == null)
                        result = new Action.ActionResult(Errors.Error.NO_SUCH_GROUP);
                    else {
                        if (findGroup.admin.equals(groupCoAdminRemoval.senderName)) { //check privilege
                            if (findGroup.activeUsers.containsKey(groupCoAdminRemoval.coAdminName)) { // check if coAdminName is coAdmin in this group
                                findGroup.deleteCoAdmin(groupCoAdminRemoval.coAdminName);
                                result = new Action.ActionResult(Errors.Error.SUCCESS);
                            } else
                                result = new Action.ActionResult(Errors.Error.NO_SUCH_MEMBER);
                        } else result = new Action.ActionResult(Errors.Error.NO_PRIVILEGE);
                    }
                    sender().tell(result, self());
                })
                .match(Action.MuteMember.class, groupMute -> {
                    Action.ActionResult result;
                    GroupData findGroup = this.groupsData.get(groupMute.groupName);
                    if (findGroup == null)
                        result = new Action.ActionResult(Errors.Error.NO_SUCH_GROUP);
                    else {
                        if (groupMute.senderName.equals(findGroup.admin) || findGroup.adminsList.containsKey(groupMute.senderName)) { //check privilege
                            if (findGroup.activeUsers.containsKey(groupMute.muteName)) { // check if muteName is in this group
                                findGroup.mutedUsers.put(groupMute.muteName, Duration.ofSeconds(groupMute.time));
                                result = new Action.ActionResult(Errors.Error.SUCCESS);
                                getContext().getSystem().
                                        scheduler()
                                        .scheduleOnce(
                                                Duration.ofSeconds(groupMute.time),
                                                new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        findGroup.mutedUsers.remove(groupMute.muteName);
                                                        ActorRef actorRef = findGroup.activeUsers.get(groupMute.muteName);
                                                        actorRef.tell(new Action.MutingTimeUp(findGroup.groupName), self());
                                                    }
                                                },
                                                getContext().getSystem().dispatcher());
                            } else result = new Action.ActionResult(Errors.Error.NO_SUCH_MEMBER);
                        } else result = new Action.ActionResult(Errors.Error.NO_PRIVILEGE);
                    }
                    sender().tell(result, self());
                })
                .match(Action.UnMuteMember.class, groupUnMute -> {
                    Action.ActionResult result;
                    GroupData findGroup = this.groupsData.get(groupUnMute.groupName);
                    if (findGroup == null)
                        result = new Action.ActionResult(Errors.Error.NO_SUCH_GROUP);
                    else {
                        if (groupUnMute.senderName.equals(findGroup.admin) || findGroup.adminsList.containsKey(groupUnMute.senderName)) { //check privilege
                            if (findGroup.activeUsers.containsKey(groupUnMute.unMuteName)) { // check if muteName is in this group
                                if (findGroup.mutedUsers.containsKey(groupUnMute.unMuteName)) {
                                    findGroup.mutedUsers.remove(groupUnMute.unMuteName);
                                    result = new Action.ActionResult(Errors.Error.SUCCESS);
                                } else result = new Action.ActionResult(Errors.Error.NOT_MUTED);
                            } else result = new Action.ActionResult(Errors.Error.NO_SUCH_MEMBER);
                        } else result = new Action.ActionResult(Errors.Error.NO_PRIVILEGE);
                    }
                    sender().tell(result, self());
                })
                .build();
    }


    @Override
    public void preStart() {
        this.groupsData = new HashMap<String, GroupData>();
    }
}
