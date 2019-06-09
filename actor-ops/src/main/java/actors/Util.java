package actors;

import akka.actor.*;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.pattern.Patterns;
import akka.util.Timeout;
import scala.concurrent.duration.Duration;
import scala.concurrent.Await;
import scala.concurrent.Future;
import akka.pattern.AskableActorSelection;

import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Util {
    public static ActorRef getClientActorRef(String username, ActorContext context, ActorRef server, Logger logger)
    {
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
            if(client.didFind)
            {
                return client.result;
            }
        } catch (Exception e) {
            if(logger != null)
            {
                logger.debug(e.getMessage());
            }
            return null;
        }
        return null;
    }
}
