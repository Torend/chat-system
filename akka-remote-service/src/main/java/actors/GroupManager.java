package actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class UserData {
    /*
    this is the class that represent the data that the server uses to track a user
     */
    public ActorRef clientRef;
    public List<String> activeGroups;

    public UserData(ActorRef ref) {
        this.clientRef = ref;
        this.activeGroups = new ArrayList<>();
    }

    public boolean isInGroup(String groupName) {
        return activeGroups.contains(groupName);
    }

    public boolean joinedGroup(String groupName) {
        boolean result = true;
        if (this.isInGroup(groupName))
            result = false;
        else
            this.activeGroups.add(groupName);
        return result;
    }
}

public class ServerActor extends AbstractActor {
    private Map<String, UserData> map; // holds all existing user. it is a map of class UserData
    private ActorRef groupsManager;

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Action.Connect.class, connect -> {
                    Action.MessageResult result;
                    System.out.println("fucker happened");
                    // checking user is non existent
                    if (map.get(connect.username) == null) {
                        //sender()
                        String toPrinto = String.format("CREATING %s", getSender().path().toString());
                        System.out.println(toPrinto);
                        UserData newUser = new UserData(connect.myRef);
                        map.put(connect.username, newUser);
                        result = new Action.ActionResult(Errors.Error.SUCCESS);
                    } else result = new Action.ActionResult(Errors.Error.DUPLICATE_USER);

                    sender().tell(result, self());
                })
                .match(Action.Disconnect.class, disconnect -> {
                    Action.MessageResult result;
                    UserData userData = map.get(disconnect.username);
                    if (userData != null) {
                        // leave from all his groups
                        userData.activeGroups.forEach(name -> this.groupsManager.tell(new Action.LeaveGroup(disconnect.username, name),sender()));
                        // remove from the server
                        map.remove(disconnect.username);
                        result = new Action.ActionResult(Errors.Error.SUCCESS);
                    } else result = new Action.ActionResult(Errors.Error.NO_SUCH_MEMBER);
                    sender().tell(result, self());
                })
                .match(Action.GetClient.class, getClient -> {
                    //will return the ActorPath of the actor in serializable format // TODO: handle of user does not exists?
                    String toPrint = String.format("AM FINDING %s", sender().toString());
                    System.out.println(toPrint);
                    UserData foundUser = map.get(getClient.username);
                    Action.GetClientResult result = new Action.GetClientResult(foundUser.clientRef, true);
                    sender().tell(result, self());
                })
                .match(Action.CreateGroup.class, createGroup -> {
                    //will return the ActorPath of the actor in serializable format // TODO: handle of user does not exists?
                    UserData userData = map.get(createGroup.adminName);
                    if (userData != null) {
                        userData.activeGroups.add(createGroup.groupName);
                        this.groupsManager.forward(createGroup, getContext());
                    }
                })
                .match(Action.GroupMessage.class, groupMessage -> {
                    //will return the ActorPath of the actor in serializable format // TODO: handle of user does not exists?
                    this.groupsManager.forward(groupMessage, getContext());
                })


                .build();
    }

    @Override
    public void preStart() {
        // used to initialize the users DB and the groups manager.
        this.map = new HashMap<>();
        this.groupsManager = getContext().actorOf(Props.create(GroupManager.class), "groups");
    }
}
