package actors;

import akka.actor.ActorPath;
import akka.actor.ActorRef;

import java.io.Serializable;
/*
this is where all the action messages are defined.
whenever you want to add a message do it here
 */
public class Action {

    public interface Message extends Serializable {
    }

    public interface MessageResult extends Serializable {
    }

    public static class Connect implements Message {
        private static final long serialVersionUID = 1L;
        public final String username;

        public Connect(String username) {
            this.username = username;
        }

    }

    public static class SendMessage implements Message {
        private static final long serialVersionUID = 1L;
        public final String username;
        public final String message;

        public SendMessage(String username, String message) {
            this.username = username;
            this.message = message;
        }

    }

    public static class CreateGroup implements Message {
        public final String adminName;
        public final String groupName;
        public final ActorRef adminRef;

        public CreateGroup(String adminName, String groupName, ActorRef adminRef) {
            this.adminName = adminName;
            this.groupName = groupName;
            this.adminRef = adminRef;
        }

    }


    public static class InviteToGroup implements Message {
        public final String inviteeName;
        public final String groupName;

        public InviteToGroup(String inviteeName, String groupName) {
            this.inviteeName = inviteeName;
            this.groupName = groupName;
        }

    }

    public static class AddToGroup implements Message {
        public final ActorRef inviteeRef;
        public final String groupName;

        public AddToGroup(ActorRef inviteeRef, String groupName) {
            this.inviteeRef = inviteeRef;
            this.groupName = groupName;
        }

    }


    public static class SendText implements Message {
        private static final long serialVersionUID = 1L;
        public final String fromUsername;
        public final String message;

        public SendText(String username, String message) {
            this.fromUsername = username;
            this.message = message;
        }

    }
    public static class Disconnect implements Message {
        private static final long serialVersionUID = 1L;
        public final String username;

        public Disconnect(String username) {
            this.username = username;
        }

    }

    public static class GetClient implements Message {
        private static final long serialVersionUID = 1L;
        public final String username;

        public GetClient(String username) {
            this.username = username;
        }
    }

    static class GetClientResult implements MessageResult {
        private static final long serialVersionUID = 1L;
        public final String result;
        public final boolean didFind;

        public GetClientResult(String result, boolean didFind) {

            this.result = result;
            this.didFind = didFind;
        }

    }


    static class ActionResult implements MessageResult {
        private static final long serialVersionUID = 1L;
        private final Errors.Error result;


        public ActionResult(Errors.Error result) {

            this.result = result;
        }

        public Errors.Error getResult() {
            return result;
        }
    }
}

