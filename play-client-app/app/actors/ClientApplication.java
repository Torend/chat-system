package actors;

import akka.actor.ActorSelection;
import akka.actor.AbstractActor;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.remote.*;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class ClientApplication {
    public static void main(String[] args) {
        Config config = ConfigFactory.defaultApplication();

        //final ActorSystem system = ActorSystem.create("ClientSystem", config);
        //system.actorOf(Props.create(CalculatorActor.class), "server");
        //ActorSystem sys = ActorSystem.
        //System.out.println("Started Cleint");
    }
}