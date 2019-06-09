package actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;

import java.util.HashMap;
import java.util.Map;

class UserData {
    /*
    this is the class that represent the data that the server uses to track a user
     */
    public ActorRef clientRef;
    public Map activeGroups;

    public UserData(ActorRef ref) {
        this.clientRef = ref;
        this.activeGroups = new HashMap();
    }

    public boolean isInGroup(String groupName) {
        if (this.activeGroups.get(groupName) != null) {
            return true;
        }
        return false;
    }

    public boolean joinedGroup(String groupName, ActorRef groupRef) {
        boolean result = true;
        if (this.isInGroup(groupName)) {
            result = false;
        } else {
            this.activeGroups.put(groupName, groupName);
        }
        return result;
    }
}

public class ServerActor extends AbstractActor {
    private Map map; // holds all existing user. it is a map of class UserData
    private ActorRef groupsManager;

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Action.Connect.class, connect ->
                {
                    Action.MessageResult result;
                    System.out.println("fucker happened");
                    // checking user is non existent
                    if (map.get(connect.username) == null)
                    {
                        //sender()
                        String toPrinto = String.format("CREATING %s", getSender().path().toString()); //TODO: take this off
                        System.out.println(toPrinto);
                        UserData newUser = new UserData(connect.myRef);
                        map.put(connect.username, newUser);
                        result = new Action.ActionResult(Errors.Error.SUCCESS);
                    } else result = new Action.ActionResult(Errors.Error.DUPLICATE_USER);

                    sender().tell(result, self());
                })
                .match(Action.Disconnect.class, disconnect ->
                {
                    Action.MessageResult result;
                    if (map.get(disconnect.username) != null)
                    {
                        map.remove(disconnect.username);
                        result = new Action.ActionResult(Errors.Error.SUCCESS);
                        //TODO leave all his groups
                    }
                    else
                    {
                        result = new Action.ActionResult(Errors.Error.NO_SUCH_MEMBER);
                    }

                    sender().tell(result, self());
                })
                .match(Action.GetClient.class, getClient ->
                {
                    //will return the ActorPath of the actor in serializable format // TODO: handle of user does not exists?
                    String toPrint = String.format("AM FINDING %s", sender().toString());
                    System.out.println(toPrint);
                    UserData foundUser = (UserData) map.get(getClient.username);
                    Action.GetClientResult result = new Action.GetClientResult(foundUser.clientRef, true);
                    sender().tell(result, self());
                })
                .match(Action.CreateGroup.class, createGroup ->
                {
                    //will return the ActorPath of the actor in serializable format // TODO: handle of user does not exists?
                    this.groupsManager.forward(createGroup, getContext());
                })
                .match(Action.GroupMessage.class, groupMessage ->
                {
                    //will return the ActorPath of the actor in serializable format // TODO: handle of user does not exists?
                    this.groupsManager.forward(groupMessage, getContext());
                })
                .match(Action.InviteToGroup.class, groupMessage ->
                {
                    //will return the ActorPath of the actor in serializable format // TODO: handle of user does not exists?
                    this.groupsManager.forward(groupMessage, getContext());
                })
                .match(Action.AddToGroup.class, groupMessage ->
                {
                    //will return the ActorPath of the actor in serializable format // TODO: handle of user does not exists?
                    this.groupsManager.forward(groupMessage, getContext());
                })
                .match(Action.RemoveFromGroup.class, groupMessage ->
                {
                    //will return the ActorPath of the actor in serializable format // TODO: handle of user does not exists?
                    this.groupsManager.forward(groupMessage, getContext());
                })
                .match(Action.AddCoAdmin.class, groupMessage ->
                {
                    //will return the ActorPath of the actor in serializable format // TODO: handle of user does not exists?
                    this.groupsManager.forward(groupMessage, getContext());
                })
                .match(Action.DeleteCoAdmin.class, groupMessage ->
                {
                    //will return the ActorPath of the actor in serializable format // TODO: handle of user does not exists?
                    this.groupsManager.forward(groupMessage, getContext());
                })
                .match(Action.MuteMember.class, groupMessage ->
                {
                    //will return the ActorPath of the actor in serializable format // TODO: handle of user does not exists?
                    this.groupsManager.forward(groupMessage, getContext());
                })
                .match(Action.UnMuteMember.class, groupMessage ->
                {
                    //will return the ActorPath of the actor in serializable format // TODO: handle of user does not exists?
                    this.groupsManager.forward(groupMessage, getContext());
                })


                .build();
    }

    @Override
    public void preStart() {
        // used to initialize the users DB and the groups manager.
        this.map = new HashMap();
        this.groupsManager = getContext().actorOf(Props.create(GroupManager.class), "groups");
    }
}
