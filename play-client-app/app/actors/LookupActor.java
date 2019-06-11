package actors;

import static java.util.concurrent.TimeUnit.SECONDS;

import akka.actor.*;
import akka.pattern.AskableActorSelection;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;
import scala.concurrent.Future;
import scala.util.Try;
import akka.actor.ActorRef;
import akka.actor.ActorIdentity;
import akka.actor.Identify;
import akka.actor.Terminated;
import akka.actor.AbstractActor;
import akka.actor.ReceiveTimeout;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.text.SimpleDateFormat;
import java.util.Calendar;


/*
this is the actor of the client itself.
does all the messaging functionality.

 */
public class LookupActor extends AbstractActor
{

    public static Props props(ActorRef out)
    {
        return Props.create(LookupActor.class, out);
    }

    private static final Logger logger = LoggerFactory.getLogger(LookupActor.class);

    private final String path;
    private ActorRef server = null;
    private ActorRef output = null;
    private String username;
    private Queue<Action.InviteToGroup> inviteQueue = new LinkedList<>();


//    @Inject
//    public LookupActor(Config config) {
//        this(config.getString("lookup.path"));
//    }

    public LookupActor(String path)
    {
        this.path = path;
        sendIdentifyRequest();
    }
    public LookupActor(ActorRef out)
    {
        //this.path = config.getString("lookup.path");
        this.path = context().system().settings().config().getString("lookup.path");
        this.output = out;
        logger.info("PATHO: {} {}", path, self().path().toString());
        sendIdentifyRequest();
    }

    private void sendIdentifyRequest()
    {
        getContext().actorSelection(path).tell(new Identify(path), self());
        getContext()
                .system()
                .scheduler()
                .scheduleOnce(Duration.create(3, SECONDS), self(),
                        ReceiveTimeout.getInstance(), getContext().dispatcher(), self());
    }

    private ActorRef getClientActorRef(String username)
    {
        /*
        function that is used to detect other clients we want to send direct messages to.
         */
        Action.GetClient getClient = new Action.GetClient(username);
        Timeout timer = new Timeout(Duration.create(5, TimeUnit.SECONDS));
        Future<Object> rt = Patterns.ask(server, getClient, timer);
        Action.GetClientResult client;
        try
        {
            client = (Action.GetClientResult) Await.result(rt, timer.duration());
            timer = new Timeout(Duration.create(5, TimeUnit.SECONDS));
            if(client.didFind)
            {
                return client.result;
            }
        }
        catch (Exception e)
        {
            logger.debug(e.getMessage());
            return null;
        }
        return null;
    }

    private String currentTime() { // return the current time
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        return sdf.format(cal.getTime());
    }

    @Override
    public Receive createReceive() {
        String toPrint = "";
        return receiveBuilder()
                .match(ActorIdentity.class, identity ->
                {
                    Optional<ActorRef> serverRef = identity.getActorRef();
                    if (serverRef.isPresent()) {
                        server = serverRef.get();
                        getContext().watch(server);
                        getContext().become(active, true);
                    } else {
                        logger.info("Remote actor not available: " + path);
                    }
                })
                .match(ReceiveTimeout.class, x ->
                {
                    sendIdentifyRequest();
                })

                .build();
    }

    Receive active = receiveBuilder()
            .match(String.class, message ->
            {
                //output.tell("I received your message: " + message, self());
                try{
                    parseCommand(message);
                }
                catch (Exception e)
                {
                    wrongInput();
                }

            }).match(Action.Connect.class, connect ->
            {
                // send message to server actor
                logger.info("Connecting");
                this.username = connect.username;
                Action.Connect conMessage = new Action.Connect(this.username, self());
                server.tell(conMessage, self());
            })
            .match(Action.FrameworkCommand.class, frameworkCommand ->
            {
                parseCommand(frameworkCommand.command);
            })
            .match(Action.SendText.class, sendText ->
            {
                String time = currentTime();
                String toPrint = String.format("[%s][%s][%s]%s",time, this.username, sendText.fromUsername, sendText.message);
                output.tell(toPrint, self());
                logger.info(toPrint);
            })
            .match(Action.SendFile.class, sendFile ->
            {
                String time = currentTime();
                String currentPath = System.getProperty("user.dir");
                try{

                    //create a temp file
                    File temp = File.createTempFile("tempfile", ".tmp");

                    //write it
                    FileOutputStream stream = new FileOutputStream(temp);
                    stream.write(sendFile.message);
                    stream.close();
                    String toPrint = String.format("[%s][%s][%s] File received: %s",time, this.username, sendFile.fromUsername, temp.getAbsolutePath());
                    output.tell(toPrint, self());
                    logger.info(toPrint);

                }catch(IOException e) {

                    e.printStackTrace();

                }

            })
            .match(Action.SendMessage.class, message -> {
                // send  text message to server actor- works.
                //logger.info("SENDO: {} {}", message.message, message.username);
                ActorRef sendeeRef = Util.getClientActorRef(message.username, getContext(), server, logger);//getClientActorRef(message.username);
                if(sendeeRef != null)
                {
                    // ActorRef sendeeRef = getContext().actorSelection(client.result).resolveOne(timer).value().get().get();
                    Action.SendText textMessage = new Action.SendText(this.username, message.message);
                    sendeeRef.tell(textMessage, self());
                    //server.tell(message, self());

                }

            })
            .match(Action.GroupMessage.class, groupMessage ->
            {
                String time = currentTime();
                String toPrint;

                if (groupMessage instanceof Action.GroupMessage.Text)
                {
                    toPrint = String.format("[%s][%s][%s]%s",time, groupMessage.groupName, groupMessage.senderName, ((Action.GroupMessage.Text) groupMessage).message);
                    output.tell(toPrint, self());
                    logger.info(toPrint);
                }
                else if (groupMessage instanceof Action.GroupMessage.File)
                { // GroupMessage.File
                    String currentPath = System.getProperty("user.dir");
                    try{

                        //create a temp file
                        File temp = File.createTempFile("tempfile", ".tmp");

                        //write it
                        FileOutputStream stream = new FileOutputStream(temp);
                        stream.write(((Action.GroupMessage.File) groupMessage).message);
                        stream.close();
                        toPrint = String.format("[%s][%s][%s] File received: %s",time, groupMessage.groupName, groupMessage.senderName,  temp.getAbsolutePath());
                        output.tell(toPrint, self());
                        logger.info(toPrint);

                    }catch(IOException e) {

                        e.printStackTrace();

                    }
                }
            })
            .match(Action.InviteToGroup.class, invitation -> {
                logger.info("You have been invited to {}, Accept?", invitation.groupName);
                String toPrint = String.format("You have been invited to %s, Accept?", invitation.groupName);
                output.tell(toPrint, self());
                inviteQueue.add(invitation);
            })
            .match(Action.RemoveFromGroup.class, removeFromGroup ->
            {
                String toPrint = String.format("You have been removed from %s by %s!", removeFromGroup.groupName, removeFromGroup.removedName);
                output.tell(toPrint, self());
                logger.info(toPrint);
            })
            .match(Action.MuteMember.class, muteMember ->
            {
                String toPrint = String.format("You have been muted for %s in %s by %s!", muteMember.time, muteMember.groupName, muteMember.senderName);
                output.tell(toPrint, self());
                logger.info(toPrint);
            })
            .match(Action.UnMuteMember.class, unMuteMember ->
            {
                String toPrint = String.format("You have been unmuted in %s by %s", unMuteMember.groupName, unMuteMember.senderName);
                output.tell(toPrint, self());
                logger.info(toPrint);
            })
            .match(Action.MutingTimeUp.class, mutingTimeUp ->
            {
                String toPrint = String.format("You have been unmuted! in %s Muting time is up!", mutingTimeUp.groupName);
                output.tell(toPrint, self());
                logger.info(toPrint);
            })
            .match(Action.AddCoAdmin.class, addCoAdmin ->
            {
                String toPrint = String.format("You have been promoted to co-admin in %s!", addCoAdmin.groupName);
                output.tell(toPrint, self());
                logger.info(toPrint);
            })
            .match(Action.DeleteCoAdmin.class, deleteCoAdmin ->
            {
                String toPrint = String.format("You have been demoted to user in %s!", deleteCoAdmin.groupName);
                output.tell(toPrint, self());
                logger.info(toPrint);
            })
            .match(Action.Requset.Accept.class, acceptedGroup ->
            {
                //String toPrint = String.format("You have been demoted to user in %s!", deleteCoAdmin.groupName);
                server.tell(new Action.AddToGroup(sender(), acceptedGroup.username, acceptedGroup.groupName), self()); // add the invitee to group
                sender().tell(new Action.SendText(this.username, "Welcome to " + acceptedGroup.groupName), self()); // send the invitee welcome message
                //output.tell(toPrint, self());
                //logger.info(toPrint);
            })
            .match(Action.Requset.Deny.class, acceptedGroup ->
            {
                output.tell( acceptedGroup.username + " deny the invitation", self()); //TODO: not sure if nned to print
            })
            .match(Terminated.class, terminated ->
            {
                logger.info("Actor terminated");
                sendIdentifyRequest();
                getContext().unbecome();
            })
            .match(ReceiveTimeout.class, message ->
            {
                // ignore
            })
            .build();


    public void parseCommand(String command) { // parse the user commands
        String[] cmdArr = command.split(" ");
        switch (cmdArr[0])
        {
            case "/user": //user commands
                switch (cmdArr[1])
                {
                    case "connect":
                        connect(cmdArr[2]);
                        break;
                    case "disconnect":
                        disconnect();
                        break;
                    case "text":
                        sendText(cmdArr[2], buildMsg(3, cmdArr));
                        break;
                    case "file":
                        sendFile(cmdArr[2], cmdArr[3]);
                        break;
                    default:
                        wrongInput();
                }
                break;
            case "/group": //group commands
                switch (cmdArr[1])
                {
                    case "create":
                        createGroup(cmdArr[2]);
                        break;
                    case "leave":
                        leaveGroup(cmdArr[2]);
                        break;
                    case "send":
                        switch (cmdArr[2])
                        {
                            case "text":
                                groupTextMessage(cmdArr[3], buildMsg(4, cmdArr));
                                break;
                            case "file":
                                groupFileMessage(cmdArr[3], cmdArr[4]);
                                break;
                            default:
                                wrongInput();
                        }
                        break;
                    case "user":
                        switch (cmdArr[2])
                        {
                            case "invite":
                                inviteToGroup(cmdArr[3], cmdArr[4]);
                                break;
                            case "remove":
                                removeFromGroup(cmdArr[3], cmdArr[4]);
                                break;
                            case "mute":
                                mute(cmdArr[3], cmdArr[4], Integer.parseInt(cmdArr[5]));
                                break;
                            case "unmute":
                                unMute(cmdArr[3], cmdArr[4]);
                                break;
                            default:
                                wrongInput();
                        }
                        break;
                    case "coadmin":
                        switch (cmdArr[2])
                        {
                            case "add":
                                addCoAdmin(cmdArr[3], cmdArr[4]);
                                break;
                            case "remove":
                                removeCoAdmin(cmdArr[3], cmdArr[4]);
                                break;
                            default:
                                wrongInput();
                        }
                        break;
                    default:
                        wrongInput();
                }
                break;
            case "YES":
            {
                if (inviteQueue.size()>0)
                {
                    Action.InviteToGroup invitation=inviteQueue.remove();
                    ActorRef inviterRef=getClientActorRef(invitation.inviterName);
                    assert inviterRef!=null;
                    inviterRef.tell(new Action.Requset.Accept(this.username, invitation.groupName), self());
                }
                else wrongInput();
                break;
            }
            case "NO":
            {
                if (inviteQueue.size()>0)
                {
                    Action.InviteToGroup invitation=inviteQueue.remove();
                    ActorRef inviterRef=getClientActorRef(invitation.inviterName);
                    assert inviterRef!=null;
                    inviterRef.tell(new Action.Requset.Deny(this.username, invitation.groupName), self());
                }
                else wrongInput();
                break;
            }
            default:
                wrongInput();
        }
    }

    private void connect(String username) // connect to the server
    {
        this.username = username;
        Action.Connect conMessage = new Action.Connect(this.username, self());
        Timeout timer = new Timeout(Duration.create(5, TimeUnit.SECONDS));
        Future<Object> rt = Patterns.ask(server, conMessage, timer);
        Action.ActionResult result;
        String toPrint = "";

        try
        {
            result = (Action.ActionResult) Await.result(rt, timer.duration());
            if (result != null)
            {
                if (result.getResult() == Errors.Error.SUCCESS)
                {
                    toPrint = String.format("%s has connected successfully!", this.username);

                }
                else if (result.getResult() == Errors.Error.DUPLICATE_USER)
                {
                    toPrint = String.format("%s user in use!", this.username);
                }
            }
        } catch (Exception e)
        {
            toPrint = "server is offline!";
        }
        finally
        {
            output.tell(toPrint, self());
            logger.info(toPrint);
        }
    }

    private void disconnect() // disconnect from the server
    {
        Action.Disconnect disconnectMessage = new Action.Disconnect(this.username);
        Timeout timer = new Timeout(Duration.create(5, TimeUnit.SECONDS));
        Future<Object> rt = Patterns.ask(server, disconnectMessage, timer);
        Action.ActionResult result;
        String toPrint = "";
        try
        {
            result = (Action.ActionResult) Await.result(rt, timer.duration());
            if (result != null)
            {
                if (result.getResult() == Errors.Error.SUCCESS)
                {
                    toPrint = String.format("%s has been disconnected successfully!", this.username);
                }
            }
        }
        catch (Exception e)
        {
            toPrint = "server is offline! try again later!";
        }
        finally
        {
            output.tell(toPrint, self());
            logger.info(toPrint);
        }
    }

    private void sendText(String target, String msg) // send text to other user
    {
        ActorRef sendeeRef = getClientActorRef(target);
        if (sendeeRef != null)
        {
            Action.SendText textMessage = new Action.SendText(this.username, msg);
            sendeeRef.tell(textMessage, self());
        }
        else
        {
            String toPrint = String.format("%s does not exist!", target);
            output.tell(toPrint, self());
            logger.info(toPrint);
        }
    }

    private void sendFile(String target, String sourcefilePath) // send file to other user
    {
        Path fileLocation = Paths.get(sourcefilePath);
        String toPrint = "";
        try
        {
            byte[] file = Files.readAllBytes(fileLocation);
            Action.SendFile sendFile = new Action.SendFile(this.username, file);
            ActorRef sendeeRef = getClientActorRef(target);
            if (sendeeRef != null)
            {
                sendeeRef.tell(sendFile, self());
            }
            else
            {
                toPrint = String.format("%s does not exist!", target);
                output.tell(toPrint, self());
                logger.info(toPrint);
            }
        } catch (IOException e)
        {
            toPrint = sourcefilePath + " does not exist!";
            output.tell(toPrint, self());
            logger.info(toPrint);
        }
    }

    private void createGroup(String groupName) // create new group
    {
        Action.CreateGroup createGroup = new Action.CreateGroup(this.username, groupName, self());
        Timeout timer = new Timeout(Duration.create(1, TimeUnit.SECONDS));
        Future<Object> rt = Patterns.ask(server, createGroup, timer);
        Action.ActionResult result;
        String toPrint = "";
        try
        {
            result = (Action.ActionResult) Await.result(rt, timer.duration());
            if (result != null)
            {
                if (result.getResult() == Errors.Error.SUCCESS)
                {
                    toPrint = groupName + " created successfully!";
                    logger.info(toPrint);
                }
                else if (result.getResult() == Errors.Error.DUPLICATE_GROUP)
                {
                    toPrint = groupName + " " + result.getResult().getDescription();
                    logger.info(toPrint);
                }
                output.tell(toPrint, self());
            }
        }
        catch (Exception e)
        {
            logger.debug(e.getMessage());
        }
    }

    private void leaveGroup(String groupName) // leave group
    {
        Action.LeaveGroup leaveGroup = new Action.LeaveGroup(this.username, groupName);
        Timeout timer = new Timeout(Duration.create(1, TimeUnit.SECONDS));
        Future<Object> rt = Patterns.ask(server, leaveGroup, timer);
        Action.ActionResult result;
        String toPrint = "";
        try
        {
            result = (Action.ActionResult) Await.result(rt, timer.duration());
            if (result != null)
            {
                if (result.getResult() == Errors.Error.NO_SUCH_GROUP)
                {
                    toPrint = groupName + " " + result.getResult().getDescription();
                    output.tell(toPrint, self());
                    logger.info(toPrint);
                }
            }
        }
        catch (Exception e)
        {
            logger.debug(e.getMessage());
        }
    }

    private void inviteToGroup(String groupName, String invitee) // invite new user to the group
    {
        // check if invitee exist in the server
        ActorRef inviteeRef = getClientActorRef(invitee);
        String toPrint = "";
        if (inviteeRef == null)
        {
            toPrint = invitee + " does not exist!";
            output.tell(toPrint, self());
            logger.info(toPrint);
            return;
        }

        Action.InviteToGroup inviteToGroup = new Action.InviteToGroup(this.username, invitee, groupName);
        Timeout timer = new Timeout(Duration.create(5, TimeUnit.SECONDS));
        Future<Object> rt = Patterns.ask(server, inviteToGroup, timer);
        Action.ActionResult result;
        try
        {
            result = (Action.ActionResult) Await.result(rt, timer.duration());
            if (result != null)
            {
                if (result.getResult() == Errors.Error.NO_SUCH_GROUP)
                {
                    toPrint = groupName + " " + result.getResult().getDescription();
                }
                else if (result.getResult() == Errors.Error.NO_PRIVILEGE)
                {
                    toPrint = result.getResult().getDescription() + groupName;
                }
                else if (result.getResult() == Errors.Error.ALREADY_MEMBER)
                {
                    toPrint = String.format("%s is already in %s", invitee, groupName);
                }
                else if (result.getResult() == Errors.Error.SUCCESS)
                {
                    inviteToGroup = new Action.InviteToGroup(this.username, invitee, groupName);
                    inviteeRef.tell(inviteToGroup, self());
//                    timer = new Timeout(Duration.create(60, TimeUnit.SECONDS));
//                    Future<Object> rt2 = Patterns.ask(inviteeRef, inviteToGroup, timer);
//                    Action.Requset answer;
//                    try
//                    {
//                        answer = (Action.Requset) Await.result(rt2, timer.duration());
//                        if (answer != null)
//                        {
//                            if (answer instanceof Action.Requset.Accept)
//                            { // accept the invitation
//                                server.tell(new Action.AddToGroup(inviteeRef, invitee, groupName), self()); // add the invitee to group
//                                inviteeRef.tell(new Action.SendText(this.username, "Welcome to " + groupName), self()); // send the invitee welcome message
//                            }
//                            else
//                                { // deny the invitation
//                                toPrint =  invitee + " deny the invitation";
//                            }
//                        }
//                    }
//                    catch (Exception e)
//                    {
//                        logger.debug(e.getMessage());
//                    }
                }
            }
        }
        catch (Exception e)
        {
            logger.debug(e.getMessage());
        }
        finally
        {
            output.tell(toPrint, self());
            logger.info(toPrint);
        }
    }

    private void groupTextMessage(String groupName, String message) //send a group text message
    {
        Action.GroupMessage.Text textMsg = new Action.GroupMessage.Text(groupName, this.username, message);
        sendMessage(groupName, textMsg);
    }

    private void groupFileMessage(String groupName, String sourcefilePath) //send a group file message
    {
        Path fileLocation = Paths.get(sourcefilePath);
        try
        { // need to check if work
            byte[] msg = Files.readAllBytes(fileLocation);
            Action.GroupMessage.File fileMsg = new Action.GroupMessage.File(groupName, this.username, msg);
            sendMessage(groupName, fileMsg);
        }
        catch (IOException e)
        {
            String toPrint = sourcefilePath + " does not exist!";
            output.tell(toPrint, self());
            logger.info(toPrint);
        }

    }

    private void sendMessage(String groupName, Action.GroupMessage message)
    {
        Timeout timer = new Timeout(Duration.create(10, TimeUnit.SECONDS));
        Future<Object> rt = Patterns.ask(server, message, timer);
        Action.ActionResult result;
        String toPrint = "" ;
        try
        {
            timer = new Timeout(Duration.create(10, TimeUnit.SECONDS));
            result = (Action.ActionResult) Await.result(rt, timer.duration());
            if (result != null)
            {
                if (result.getResult() == Errors.Error.NO_SUCH_GROUP)
                {
                    toPrint = groupName + " " + result.getResult().getDescription();
                }
                else if (result.getResult() == Errors.Error.NO_SUCH_MEMBER)
                {
                    toPrint = "You are not part of " + groupName;
                }
                else if (result.getResult() == Errors.Error.MUTED)
                {
                   toPrint = result.getResult().getDescription() + " in " + groupName;
                }
                output.tell(toPrint, self());
                logger.info(toPrint);
            }
        }
        catch (Exception e)
        {
            logger.debug(e.getMessage());
        }
    }

    private void removeFromGroup(String groupName, String targetusername)
    {
        Action.RemoveFromGroup removeFromGroup = new Action.RemoveFromGroup(this.username, targetusername, groupName);
        AdminMessage(removeFromGroup, groupName, targetusername);
    }

    private void mute(String groupName, String targetusername, int time)
    {
        Duration duration = Duration.create(time, "seconds");
        Action.MuteMember muteMember = new Action.MuteMember(this.username, targetusername, groupName, duration);
        AdminMessage(muteMember, groupName, targetusername);
    }

    private void unMute(String groupName, String targetusername)
    {
        Action.UnMuteMember unMuteMember = new Action.UnMuteMember(this.username, targetusername, groupName);
        AdminMessage(unMuteMember, groupName, targetusername);
    }

    private void addCoAdmin(String groupName, String targetusername)
    {
        Action.AddCoAdmin addCoAdmin = new Action.AddCoAdmin(this.username, targetusername, groupName);
        AdminMessage(addCoAdmin, groupName, targetusername);
    }

    private void removeCoAdmin(String groupName, String targetusername)
    {
        Action.DeleteCoAdmin deleteCoAdmin = new Action.DeleteCoAdmin(this.username, targetusername, groupName);
        AdminMessage(deleteCoAdmin, groupName, targetusername);
    }

    private void AdminMessage(Action.Message msg, String groupName, String targetusername)
    // send all the admin/coAdmin messages
    {
        // check if invitee exist in the server
        ActorRef targetRef = getClientActorRef(targetusername);
        String toPrint = "";
        if (targetRef == null)
        {

            toPrint = targetusername + " does not exist!";
            output.tell(toPrint, self());
            logger.info(toPrint);
            return;
        }

        Timeout timer = new Timeout(Duration.create(10, TimeUnit.SECONDS));
        Future<Object> rt = Patterns.ask(server, msg, timer);

        try
        {
            Action.ActionResult result = (Action.ActionResult) Await.result(rt, timer.duration());
            if (result != null)
            {
                if (result.getResult() == Errors.Error.NO_SUCH_GROUP)
                {
                    toPrint = groupName + " " + result.getResult().getDescription();
                }
                else if (result.getResult() == Errors.Error.NO_PRIVILEGE)
                {
                    toPrint = result.getResult().getDescription() + groupName;
                }
                else if (result.getResult() == Errors.Error.NO_SUCH_MEMBER)
                {
                    toPrint = msg + result.getResult().getDescription();
                }
                else if (result.getResult() == Errors.Error.SUCCESS)
                {
                    targetRef.tell(msg, self());
                }
                if (toPrint != "")
                {
                    output.tell(toPrint, self());
                    logger.info(toPrint);
                }
            }
        }
        catch (Exception e)
        {
            logger.debug(e.getMessage());
        }
    }
    private void wrongInput(){ //print wrong input
        String toPrint = "wrong input try again";
        output.tell(toPrint, self());
        logger.info(toPrint);
    }

    private String buildMsg(int from, String[] strings){ // build string from index to the end of the array
        StringBuilder builder = new StringBuilder();
        int count = 0;
        for (String string : strings)
        {
            if (count >= from)
            {
                if (builder.length()>0)
                    builder.append(" ");
                builder.append(string);
            }
            count++;
        }
        return builder.toString();
    }
}
