package actors;

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

    public static class AskOutput implements Message {
        private static final long serialVersionUID = 1L;

        public AskOutput() {

        }

    }


    public static class GetOutput implements Message {
        private static final long serialVersionUID = 1L;
        public final String text; // TODO: turn into list of lines to print

        public GetOutput(String text) {
            this.text = text;
        }

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

    public static class GroupTextMessage implements Message {
        private static final long serialVersionUID = 1L;
        public final String groupName;
        public final String senderName;
        public final Message message;

        public GroupTextMessage(String groupName, String senderName, Message message) {
            this.groupName = groupName;
            this.senderName = senderName;
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
        public final String inviterName;
        public final String inviteeName;
        public final String groupName;

        public InviteToGroup(String inviterName, String inviteeName, String groupName) {
            this.inviterName = inviterName;
            this.inviteeName = inviteeName;
            this.groupName = groupName;
        }

    }

    public static class AddToGroup implements Message {
        public final ActorRef inviteeRef;
        public final String inviteeName;
        public final String groupName;

        public AddToGroup(ActorRef inviteeRef, String inviteeName, String groupName) {
            this.inviteeRef = inviteeRef;
            this.inviteeName = inviteeName;
            this.groupName = groupName;
        }

    }

    public static class RemoveFromGroup implements Message {
        public final String senderName;
        public final String removedName;
        public final String groupName;

        public RemoveFromGroup(String senderName, String removedName, String groupName) {
            this.senderName = senderName;
            this.removedName = removedName;
            this.groupName = groupName;
        }

    }


    public static class AddCoAdmin implements Message {
        public final String senderName;
        public final String coAdminName;
        public final String groupName;

        public AddCoAdmin(String senderName, String coAdminName, String groupName) {
            this.senderName = senderName;
            this.coAdminName = coAdminName;
            this.groupName = groupName;
        }
    }


    public static class DeleteCoAdmin implements Message {
        public final String senderName;
        public final String coAdminName;
        public final String groupName;

        public DeleteCoAdmin(String senderName, String coAdminName, String groupName) {
            this.senderName = senderName;
            this.coAdminName = coAdminName;
            this.groupName = groupName;
        }
    }


    public static class MuteMember implements Message {
        public final String senderName;
        public final String muteName;
        public final String groupName;
        public final int time;

        public MuteMember(String senderName, String muteName, String groupName, int time) {
            this.senderName = senderName;
            this.muteName = muteName;
            this.groupName = groupName;
            this.time = time;
        }
    }


    public static class UnMuteMember implements Message {
        public final String senderName;
        public final String unMuteName;
        public final String groupName;

        public UnMuteMember(String senderName, String unMuteName, String groupName) {
            this.senderName = senderName;
            this.unMuteName = unMuteName;
            this.groupName = groupName;
        }
    }


    public static class MutingTimeUp implements Message{
        public final String groupName;

        public MutingTimeUp(String groupName) {
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

