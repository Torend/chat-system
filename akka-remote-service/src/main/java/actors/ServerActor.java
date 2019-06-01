package actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;

import java.util.HashMap;
import java.util.Map;
class UserData
{
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
    private Map map;

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

                    Action.MessageResult result = new Action.ActionResult(success);
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

                    Action.MessageResult result = new Action.ActionResult(success);
                    sender().tell(result, self());
                })
                .match(Action.GetClient.class, getClient -> {
                    //will retorn the actorpath of the actor in serializable format
                    UserData foundUser = new UserData((ActorRef) map.get(getClient.username));
                    Action.GetClientResult result = new Action.GetClientResult(foundUser.clientRef.path().toSerializationFormat(), true);
                    sender().tell(result, self());
                })
                .match(Op.Subtract.class, subtract -> {
                    System.out.println("Calculating " + subtract.getN1() + " - "
                            + subtract.getN2());
                    Op.SubtractResult result = new Op.SubtractResult(subtract.getN1(),
                            subtract.getN2(), subtract.getN1() - subtract.getN2());
                    sender().tell(result, self());
                })
                .match(Op.Multiply.class, multiply -> {
                    System.out.println("Calculating " + multiply.getN1() + " * "
                            + multiply.getN2());
                    Op.MultiplicationResult result = new Op.MultiplicationResult(
                            multiply.getN1(), multiply.getN2(), multiply.getN1()
                            * multiply.getN2());
                    sender().tell(result, self());
                })
                .match(Op.Divide.class, divide -> {
                    System.out.println("Calculating " + divide.getN1() + " / "
                            + divide.getN2());
                    Op.DivisionResult result = new Op.DivisionResult(divide.getN1(),
                            divide.getN2(), divide.getN1() / divide.getN2());
                    sender().tell(result, self());
                })
                .build();
    }

    @Override
    public void preStart() {
        this.map = new HashMap();
    }
}
