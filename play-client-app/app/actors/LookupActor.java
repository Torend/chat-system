package actors;

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

import javax.inject.Inject;
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
                // send  text message to server actor- works. //TODO: add similar function to send files
                Action.GetClient getClient = new Action.GetClient(message.username);
                Timeout timer = new Timeout(Duration.create(1, TimeUnit.SECONDS));
                Future<Object> rt = Patterns.ask(server, getClient, timer);
                Action.GetClientResult client = (Action.GetClientResult) Await.result(rt, timer.duration());
                timer = new Timeout(Duration.create(1, TimeUnit.SECONDS));
                if(client.didFind)
                {
                    ActorRef sendeeRef = getContext().actorSelection(client.result).resolveOne(timer).value().get().get();
                    Action.SendText textMessage = new Action.SendText(this.username, message.message);
                    sendeeRef.tell(textMessage, self());
                    //server.tell(message, self());

                }
                else
                {
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

}
