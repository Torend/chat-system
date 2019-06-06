package actors;

import static java.security.AccessController.getContext;
import static java.util.concurrent.TimeUnit.SECONDS;

import akka.pattern.AskableActorSelection;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Option;
import scala.concurrent.Await;
import scala.concurrent.Awaitable;
import scala.concurrent.duration.Duration;
import scala.concurrent.Future;
import akka.actor.ActorRef;
import akka.actor.ActorIdentity;
import akka.actor.Identify;
import akka.actor.Terminated;
import akka.actor.AbstractActor;
import akka.actor.ReceiveTimeout;
import scala.util.Try;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/*
this is the actor of the client itself.
does all the messaging functionality.

 */
public class LookupActor extends AbstractActor {

    private static final Logger logger = LoggerFactory.getLogger(LookupActor.class);

    private final String path;
    private ActorRef server = null;
    public String username;

    @Inject
    public LookupActor(Config config) {
        this(config.getString("lookup.path"));
    }

    public LookupActor(String path) {
        this.path = path;
        sendIdentifyRequest();
    }

    private void sendIdentifyRequest() {
        getContext().actorSelection(path).tell(new Identify(path), self());
        getContext()
                .system()
                .scheduler()
                .scheduleOnce(Duration.create(3, SECONDS), self(),
                        ReceiveTimeout.getInstance(), getContext().dispatcher(), self());
    }

    private ActorRef getClientActorRef(String username) {
        /*
        function that is used to detect other clients we want to send direct messages to.
         */
        Action.GetClient getClient = new Action.GetClient(username);
        Timeout timer = new Timeout(Duration.create(1, TimeUnit.SECONDS));
        Future<Object> rt = Patterns.ask(server, getClient, timer);
        Action.GetClientResult client;
        try {
            client = (Action.GetClientResult) Await.result(rt, timer.duration());
            timer = new Timeout(Duration.create(1, TimeUnit.SECONDS));
            if (client.didFind) {
                return getContext().actorSelection(client.result).resolveOne(timer).value().get().get();
            }
        } catch (Exception e) {
            logger.debug(e.getMessage());
            return null;
        }
        return null;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(ActorIdentity.class, identity -> {
                    Optional<ActorRef> serverRef = identity.getActorRef();
                    if (serverRef.isPresent()) {
                        server = serverRef.get();
                        getContext().watch(server);
                        getContext().become(active, true);
                    } else {
                        logger.info("Remote actor not available: " + path);
                    }
                })
                .match(ReceiveTimeout.class, x -> {
                    sendIdentifyRequest();
                })
                .match(Action.InviteToGroup.class, invitation -> {
                    logger.info("You have been invited to {}, Accept?", invitation.groupName);
                    //TODO <targetusername> may accept [Yes] or deny [No] the invite. Response will be sent back to <sourceusername<
                    String response = ""; // get response from the user
                    ActorRef inviterRef = getClientActorRef(invitation.inviterName);
                    assert inviterRef != null;
                    if (response.equals("Yes"))
                        inviterRef.tell(new Action.Requset.Accept(this.username), self());
                    else inviterRef.tell(new Action.Requset.Deny(this.username), self());
                })
                .match(Action.RemoveFromGroup.class, removeFromGroup -> {
                    logger.info("You have been removed from {} by {}!", removeFromGroup.groupName, removeFromGroup.removedName);
                })
                .match(Action.MuteMember.class, muteMember -> {
                    logger.info("You have been muted for {} in {} by {}!", muteMember.time, muteMember.groupName, muteMember.senderName);
                })
                .match(Action.UnMuteMember.class, unMuteMember -> {
                    logger.info("You have been unmuted in {} by {}", unMuteMember.groupName, unMuteMember.senderName);
                })
                .match(Action.MutingTimeUp.class, mutingTimeUp -> {
                    logger.info("You have been unmuted! in {} Muting time is up!", mutingTimeUp.groupName);
                })
                .match(Action.AddCoAdmin.class, addCoAdmin -> {
                    logger.info("You have been promoted to co-admin in {}!", addCoAdmin.groupName);
                })
                .match(Action.DeleteCoAdmin.class, deleteCoAdmin -> {
                    logger.info("You have been demoted to user in {}!", deleteCoAdmin.groupName);
                })
                .build();
    }

    Receive active = receiveBuilder()
            .match(Action.Connect.class, connect -> {
                // send message to server actor
                logger.info("Connecting");
                this.username = connect.username;
                Action.Connect conMessage = new Action.Connect(this.username);
                server.tell(conMessage, self()); // TODO: should be ASK to know if the user was indeed created
            })
            .match(Action.SendMessage.class, message -> {
                // send  text message to server actor- works. //TODO: add similar function to send files, add group logic

                ActorRef sendeeRef = Util.getClientActorRef(message.username, getContext(), server, logger);//getClientActorRef(message.username);
                if (sendeeRef != null) {
                    // ActorRef sendeeRef = getContext().actorSelection(client.result).resolveOne(timer).value().get().get();
                    Action.SendText textMessage = new Action.SendText(this.username, message.message);
                    sendeeRef.tell(textMessage, self());
                    //server.tell(message, self());

                } else {
                    //TODO: if client we send message to is not found
                }


            })
            .match(Action.SendText.class, text -> {
                // in case a message arrives
                logger.info("Text: {}", text.message);
            })
            .match(Op.AddResult.class, result -> {
                logger.info("Add result: {} + {} = {}", result.getN1(), result.getN2(), result.getResult());
            })
            .match(Op.SubtractResult.class, result -> {
                logger.info("Sub result: {} - {} = {}", result.getN1(), result.getN2(), result.getResult());
            })
            .match(Terminated.class, terminated -> {
                logger.info("Calculator terminated");
                sendIdentifyRequest();
                getContext().unbecome();
            })
            .match(ReceiveTimeout.class, message -> {
                // ignore
            })
            .build();

    public void createGroup(String groupName) {
        Action.CreateGroup createGroup = new Action.CreateGroup(this.username, groupName, self());
        Timeout timer = new Timeout(Duration.create(1, TimeUnit.SECONDS));
        Future<Object> rt = Patterns.ask(server, createGroup, timer);
        Action.ActionResult result;
        try {
            result = (Action.ActionResult) Await.result(rt, timer.duration());
            if (result != null) {
                if (result.getResult() == Errors.Error.SUCCESS) {
                    logger.info(groupName + " created successfully!");
                } else if (result.getResult() == Errors.Error.DUPLICATE_GROUP) {
                    logger.info(groupName + " " + result.getResult().getDescription());
                }
            }
        } catch (Exception e) {
            logger.debug(e.getMessage());
        }
    }

    public void leaveGroup(String groupName) {
        Action.CreateGroup createGroup = new Action.CreateGroup(this.username, groupName, self());
        Timeout timer = new Timeout(Duration.create(1, TimeUnit.SECONDS));
        Future<Object> rt = Patterns.ask(server, createGroup, timer);
        Action.ActionResult result;
        try {
            result = (Action.ActionResult) Await.result(rt, timer.duration());
            if (result != null) {
                if (result.getResult() == Errors.Error.NO_SUCH_GROUP)
                    logger.info(groupName + " " + result.getResult().getDescription());
            }
        } catch (Exception e) {
            logger.debug(e.getMessage());
        }
    }

    public void inviteToGroup(String groupName, String invitee) {
        // check if invitee exist in the server
        ActorRef inviteeRef = getClientActorRef(invitee);
        if (inviteeRef == null) {
            logger.info(invitee + " does not exist!");
            return;
        }

        Action.InviteToGroup inviteToGroup = new Action.InviteToGroup(this.username, invitee, groupName);
        Timeout timer = new Timeout(Duration.create(1, TimeUnit.SECONDS));
        Future<Object> rt = Patterns.ask(server, inviteToGroup, timer);
        Action.ActionResult result;
        try {
            result = (Action.ActionResult) Await.result(rt, timer.duration());
            if (result != null) {
                if (result.getResult() == Errors.Error.NO_SUCH_GROUP)
                    logger.info(groupName + " " + result.getResult().getDescription());
                else if (result.getResult() == Errors.Error.NO_PRIVILEGE)
                    logger.info(result.getResult().getDescription() + groupName);
                else if (result.getResult() == Errors.Error.ALREADY_MEMBER)
                    logger.info("{} is already in {}", invitee, groupName);
                else if (result.getResult() == Errors.Error.SUCCESS) {
                    Future<Object> rt2 = Patterns.ask(inviteeRef, inviteToGroup, timer);
                    Action.Requset answer;
                    try {
                        answer = (Action.Requset) Await.result(rt, timer.duration());
                        if (answer != null) {
                            if (answer instanceof Action.Requset.Accept) { // accept the invitation
                                server.tell(new Action.AddToGroup(inviteeRef, invitee, groupName), self()); // add the invitee to group
                                inviteeRef.tell(new Action.SendText(this.username, "Welcome to " + groupName), self()); // send the invitee welcome message
                            } else { // deny the invitation
                                logger.info(invitee + " deny the invitation"); // not sure if we need to print this
                            }
                        }
                    } catch (Exception e) {
                        logger.debug(e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            logger.debug(e.getMessage());
        }
    }

    public void GroupTextMessage(String groupName, String message) {
        Action.GroupMessage.Text textMsg = new Action.GroupMessage.Text(groupName, this.username, message);
        SendMessage(groupName, textMsg);
    }

    public void GroupFileMessage(String groupName, String sourcefilePath) {
        Path fileLocation = Paths.get(sourcefilePath);
        try { // need to check if work
            byte[] msg = Files.readAllBytes(fileLocation);
            Action.GroupMessage.File fileMsg = new Action.GroupMessage.File(groupName, this.username, msg);
            SendMessage(groupName, fileMsg);
        } catch (IOException e) {
            logger.info(sourcefilePath + " does not exist!");
        }

    }

    private void SendMessage(String groupName, Action.GroupMessage message) {
        Timeout timer = new Timeout(Duration.create(1, TimeUnit.SECONDS));
        Future<Object> rt = Patterns.ask(server, message, timer);
        Action.ActionResult result;
        try {
            result = (Action.ActionResult) Await.result(rt, timer.duration());
            if (result != null) {
                if (result.getResult() == Errors.Error.NO_SUCH_GROUP)
                    logger.info(groupName + " " + result.getResult().getDescription());
                else if (result.getResult() == Errors.Error.NO_SUCH_MEMBER)
                    logger.info("You are not part of " + groupName);
                else if (result.getResult() == Errors.Error.MUTED)
                    logger.info(result.getResult().getDescription() + "in " + groupName);
            }
        } catch (Exception e) {
            logger.debug(e.getMessage());
        }
    }

    public void RemoveFromGroup(String groupName, String targetusername) {
        Action.RemoveFromGroup removeFromGroup = new Action.RemoveFromGroup(this.username, targetusername, groupName);
        AdminMessage(removeFromGroup, groupName, targetusername);
    }

    public void Mute(String groupName, String targetusername, int time) {
        Action.MuteMember muteMember = new Action.MuteMember(this.username, targetusername, groupName, time);
        AdminMessage(muteMember, groupName, targetusername);
    }

    public void UnMute(String groupName, String targetusername) {
        Action.UnMuteMember unMuteMember = new Action.UnMuteMember(this.username, targetusername, groupName);
        AdminMessage(unMuteMember, groupName, targetusername);
    }

    public void AddCoAdmin(String groupName, String targetusername) {
        Action.AddCoAdmin addCoAdmin = new Action.AddCoAdmin(this.username, targetusername, groupName);
        AdminMessage(addCoAdmin, groupName, targetusername);
    }

    public void RemoveCoAdmin(String groupName, String targetusername) {
        Action.DeleteCoAdmin deleteCoAdmin = new Action.DeleteCoAdmin(this.username, targetusername, groupName);
        AdminMessage(deleteCoAdmin, groupName, targetusername);
    }

    private void AdminMessage(Action.Message msg, String groupName, String targetusername) {
        // check if invitee exist in the server
        ActorRef targetRef = getClientActorRef(targetusername);
        if (targetRef == null) {
            logger.info(targetusername + " does not exist!");
            return;
        }

        Timeout timer = new Timeout(Duration.create(1, TimeUnit.SECONDS));
        Future<Object> rt = Patterns.ask(server, msg, timer);

        try {
            Action.ActionResult result = (Action.ActionResult) Await.result(rt, timer.duration());
            if (result != null) {
                if (result.getResult() == Errors.Error.NO_SUCH_GROUP)
                    logger.info(groupName + " " + result.getResult().getDescription());
                else if (result.getResult() == Errors.Error.NO_PRIVILEGE)
                    logger.info(result.getResult().getDescription() + groupName);
                else if (result.getResult() == Errors.Error.NO_SUCH_MEMBER)
                    logger.info(msg + result.getResult().getDescription());
                else if (result.getResult() == Errors.Error.SUCCESS)
                    targetRef.tell(msg, self());
            }
        } catch (Exception e) {
            logger.debug(e.getMessage());
        }
    }
}
