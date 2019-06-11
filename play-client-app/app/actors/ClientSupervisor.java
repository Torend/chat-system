package actors;

import static akka.actor.SupervisorStrategy.escalate;
import static akka.actor.SupervisorStrategy.restart;
import static akka.actor.SupervisorStrategy.resume;
import static akka.actor.SupervisorStrategy.stop;

import actors.LookupActor;
import akka.actor.*;
import akka.japi.Creator;
import scala.concurrent.duration.Duration;
import akka.actor.SupervisorStrategy.Directive;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Function;


public class ClientSupervisor extends AbstractActor {
    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    ActorRef lookupActor;
//    ActorRef workerActor = getContext().actorOf(new Props(LookupActor.class),
//            "workerActor");

    public Props props(ActorRef out)
    {
        Props supervisor  = Props.create(ClientSupervisor.class, out);
        Props lookupCreator  = Props.create(LookupActor.class, out);
        this.lookupActor = getContext().actorOf(lookupCreator);
        return  lookupCreator;
    }


    @Override
    public void preStart() {

    }

    @Override
    public Receive createReceive() {
        return null;
    }

    private static SupervisorStrategy strategy = new OneForOneStrategy(10,
            Duration.create("10 second"), new Function<Throwable, Directive>() {
        public Directive apply(Throwable t) {
            if (t instanceof Exception) {
                return resume();
            }
//            else if (t instanceof NullPointerException) {
//                return restart();
//            } else if (t instanceof IllegalArgumentException) {
//                return stop();
//            }
            else {
                return escalate();
            }
        }
    });

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return strategy;
    }


    public ActorRef getWorker() {
        return null;
//        return workerActor;
    }
}