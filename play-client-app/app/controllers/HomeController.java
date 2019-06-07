package controllers;

import actors.Action;
import actors.Op;
import akka.actor.ActorRef;
import akka.pattern.Patterns;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.util.Timeout;
import play.mvc.WebSocket;
import play.data.DynamicForm;
import play.data.Form;
import play.libs.streams.ActorFlow;
import play.mvc.*;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;


import javax.inject.Inject;
import javax.inject.Named;
import java.util.concurrent.TimeUnit;

/**
 * This controller contains an action to handle HTTP requests
 * to the application's home page.
 * TODO: understand how play framework works to make a frontend with it.
 */
public class HomeController extends Controller {

    private final ActorRef lookupActor;

    @Inject
    public HomeController(@Named("lookup-actor") ActorRef lookupActor) {
        this.lookupActor = lookupActor;
    }

    private String handleOutput() {
        try {
            TimeUnit.SECONDS.sleep(7);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Action.AskOutput asker = new Action.AskOutput();
        Timeout timer = new Timeout(Duration.create(1, TimeUnit.SECONDS));
        Future<Object> rt = Patterns.ask(this.lookupActor, asker, timer);
        Action.GetOutput output;
        String toPrint = "";
        try {
            output = (Action.GetOutput) Await.result(rt, timer.duration());
            if (output.hasContent) {
                for (int i = 0; i < output.lines.size(); i++) {
                    toPrint.concat(output.lines.get(i));
                }
                return toPrint;
            }

        } catch (Exception e) {

            return "";
        }
        return "";


    }

    private void handleInput(String text) {
        lookupActor.tell(new Action.SendText("fucker", text), null);
    }

    public WebSocket socket() {
        return WebSocket.Text.accept(
                request -> {
                    return Flow.<String>create()
                            .map(
                                    msg -> {
                                        handleInput(msg);
                                        return "I received your message: " + msg;
                                    });
                });
    }

    /**
     * An action that renders an HTML page with a welcome message.
     * The configuration in the <code>routes</code> file means that
     * this method will be called when the application receives a
     * <code>GET</code> request with a path of <code>/</code>.
     */
    public Result index() {
        /*
        this is the homepage, when you enter localhost:9000 the actions here will happen.
        currently I used it to test messaging.
         */
        Http.Request request = request();
        String url = routes.HomeController.socket().webSocketURL(request);
        lookupActor.tell(new Action.Connect("fucker"), null);
        lookupActor.tell(new Action.SendText("fucker", "fuck"), null);
        return ok(views.html.index.render(url));
    }

    public Result add(int a, int b) {
        // Fire-and-forget
        //lookupActor.tell(new Action.Connect("fucker"), null);
        //lookupActor.tell(new Action.SendText("fucker", "fuck"), null);
        lookupActor.tell(new Op.Add(a, b), null);
        return ok("Add: See the logs for result");
    }

    public Result subtract(int a, int b) {
        // Fire-and-forget
        lookupActor.tell(new Op.Subtract(a, b), null);
        return ok("Subtract: See the logs for result");
    }

    public Result sendTo()
    {
        //DynamicForm form = Form.form().bindFromRequest();
        return ok("fuc kyo");

    }
}
