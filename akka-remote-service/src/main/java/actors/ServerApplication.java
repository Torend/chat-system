package actors;

import akka.actor.ActorSystem;
import akka.actor.Props;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class ServerApplication {
    public static void main(String[] args) {
        Config config = ConfigFactory.defaultApplication();
        final ActorSystem system = ActorSystem.create("ServerSystem", config);
        system.actorOf(Props.create(ServerActor.class), "server");
        System.out.println("Started ServerSystem");
    }
}
