package actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;

import java.util.HashMap;
import java.util.Map;
class UserData
{
    /*
    this is the class that represent the data that the server uses to track a user
     */
    public ActorRef clientRef;
    public Map activeGroups;

    public UserData(ActorRef ref)
    {
        this.clientRef = ref;
        this.activeGroups = new HashMap();
    }

    public boolean isInGroup(String groupName)
    {
        if(this.activeGroups.get(groupName) != null)
        {
            return true;
        }
        return false;
    }

    public boolean joinedGroup(String groupName, ActorRef groupRef)
    {
        boolean result = true;
        if(this.isInGroup(groupName))
        {
            result = false;
        }
        else
        {
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
                .match(Action.Connect.class, connect -> {
                    boolean success = false;
                    // checking user is non existent
                    if (map.get(connect.username) == null)
                    {
                        UserData newUser = new UserData(sender());
                      map.put(connect.username, newUser);
                      success = true;
                    }
                    Action.MessageResult result = new Action.ActionResult(Errors.Error.SUCCESS);//success);

                    sender().tell(result, self());
                })
                .match(Action.Disconnect.class, disconnect -> {
                    //TODO: implement leaving groups
                    boolean success = false;
                    // checking user is non existent
                    if (map.get(disconnect.username) != null)
                    {
                        map.remove(disconnect.username);
                        success = true;
                    }

                    Action.MessageResult result = new Action.ActionResult(Errors.Error.SUCCESS);
                    sender().tell(result, self());
                })
                .match(Action.GetClient.class, getClient -> {
                    //will return the ActorPath of the actor in serializable format
                    UserData foundUser = new UserData((ActorRef) map.get(getClient.username));
                    Action.GetClientResult result = new Action.GetClientResult(foundUser.clientRef.path().toSerializationFormat(), true);
                    sender().tell(result, self());
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
